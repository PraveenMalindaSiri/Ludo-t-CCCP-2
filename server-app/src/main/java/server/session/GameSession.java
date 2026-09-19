package server.session;

import engine.GameEngine;
import event.GameSnapshot;
import factory.GameFactory;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import protocol.ErrorCode;
import protocol.GameSnapshotDto;
import protocol.MessageKind;
import protocol.RequestMessage;
import protocol.RequestType;
import protocol.ResponseMessage;
import protocol.SessionStatus;
import protocol.SessionSummaryDto;
import server.application.port.SessionSubscriber;

/** Owns one isolated game, one bounded request queue and exactly one mutation worker. */
public final class GameSession implements AutoCloseable {

    private static final long MIN_TURN_DELAY_MILLIS = 10;
    private static final long MAX_TURN_DELAY_MILLIS = 60_000;

    private final UUID sessionId;
    private final String name;
    private final GameEngine engine;
    private final BlockingQueue<RequestEnvelope> requests;
    private final Set<SessionSubscriber> subscribers = ConcurrentHashMap.newKeySet();
    private final Instant createdAt = Instant.now();
    private final CountDownLatch ready = new CountDownLatch(1);
    private final AtomicBoolean closing = new AtomicBoolean();
    private final Thread worker;

    private volatile SessionStatus status = SessionStatus.CREATED;
    private volatile long turnDelayMillis;
    private volatile long version;
    private volatile long turn;
    private volatile GameSnapshotDto latestSnapshot;
    private volatile Instant startedAt;
    private volatile Instant completedAt;
    private volatile Throwable startupFailure;
    private String lastAction = "Session created";
    private long nextTurnDeadlineNanos;

    public GameSession(
            UUID sessionId,
            String name,
            long randomSeed,
            int queueCapacity,
            long defaultTurnDelayMillis) {
        this(
                sessionId,
                name,
                GameFactory.createGame(randomSeed),
                queueCapacity,
                defaultTurnDelayMillis,
                true);
    }

    GameSession(
            UUID sessionId,
            String name,
            GameEngine engine,
            int queueCapacity,
            long defaultTurnDelayMillis,
            boolean startWorker) {
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (queueCapacity <= 0 || defaultTurnDelayMillis <= 0) {
            throw new IllegalArgumentException("Queue capacity and turn delay must be positive");
        }
        this.name = name.trim();
        this.engine = Objects.requireNonNull(engine, "engine");
        this.requests = new ArrayBlockingQueue<>(queueCapacity);
        this.turnDelayMillis = defaultTurnDelayMillis;
        if (startWorker) {
            worker = Thread.ofVirtual().name("session-" + sessionId).start(this::runLoop);
        } else {
            worker = null;
        }
    }

    public void awaitReady() {
        try {
            if (!ready.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Session worker did not initialize in time");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while creating session", exception);
        }
        if (startupFailure != null) {
            throw new IllegalStateException("Unable to initialize game session", startupFailure);
        }
    }

    public UUID sessionId() {
        return sessionId;
    }

    public SessionStatus status() {
        return status;
    }

    public long turnDelayMillis() {
        return turnDelayMillis;
    }

    public GameSnapshotDto latestSnapshot() {
        return latestSnapshot;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public Instant completedAt() {
        return completedAt;
    }

    public int pendingRequestCount() {
        return requests.size();
    }

    public boolean workerIsVirtual() {
        return worker != null && worker.isVirtual();
    }

    public String workerName() {
        return worker == null ? "" : worker.getName();
    }

    public SessionSummaryDto summary() {
        return new SessionSummaryDto(
                sessionId, name, status, subscribers.size(), turnDelayMillis, version, createdAt);
    }

    public boolean submit(
            RequestMessage request, SessionSubscriber subscriber, long receivedNanos) {
        if (closing.get()) {
            return false;
        }
        RequestEnvelope envelope =
                new RequestEnvelope(
                        request, subscriber, receivedNanos, System.nanoTime(), subscriber::offer);
        return requests.offer(envelope);
    }

    public void removeSubscriber(SessionSubscriber subscriber) {
        subscribers.remove(subscriber);
    }

    private void runLoop() {
        try {
            engine.initializeGame();
            refreshSnapshot();
            ready.countDown();
            while (!closing.get()) {
                RequestEnvelope envelope = waitForWork();
                if (envelope != null) {
                    process(envelope);
                }
                if (status == SessionStatus.RUNNING && System.nanoTime() >= nextTurnDeadlineNanos) {
                    advanceAutomatically();
                }
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (Throwable failure) {
            startupFailure = latestSnapshot == null ? failure : null;
            if (latestSnapshot != null) {
                System.err.printf("Session %s worker failed: %s%n", sessionId, failure);
            }
        } finally {
            ready.countDown();
            if (status != SessionStatus.COMPLETED && status != SessionStatus.STOPPED) {
                status = SessionStatus.INTERRUPTED;
                version++;
                lastAction = "Session interrupted";
                if (latestSnapshot != null) {
                    refreshSnapshot();
                }
            }
            failPendingRequests();
        }
    }

    private RequestEnvelope waitForWork() throws InterruptedException {
        if (status != SessionStatus.RUNNING) {
            return requests.take();
        }
        long remaining = nextTurnDeadlineNanos - System.nanoTime();
        if (remaining <= 0) {
            return requests.poll();
        }
        return requests.poll(remaining, TimeUnit.NANOSECONDS);
    }

    private void process(RequestEnvelope envelope) {
        try {
            ResponseMessage response = handle(envelope.request(), envelope.subscriber());
            envelope.complete(response);
        } catch (RuntimeException failure) {
            envelope.complete(
                    failure(
                            envelope.request(),
                            ErrorCode.SERVER_ERROR,
                            "Session request failed: " + safeMessage(failure)));
        }
    }

    private ResponseMessage handle(RequestMessage request, SessionSubscriber subscriber) {
        if (request.requestType() != RequestType.JOIN_SESSION
                && !subscribers.contains(subscriber)) {
            return failure(
                    request,
                    ErrorCode.INVALID_STATE,
                    "Join the session before requesting game operations");
        }
        return switch (request.requestType()) {
            case JOIN_SESSION -> join(request, subscriber);
            case LEAVE_SESSION -> leave(request, subscriber);
            case GET_SNAPSHOT -> success(request, "Snapshot retrieved", true);
            case START_GAME -> start(request);
            case PAUSE_GAME -> pause(request);
            case RESUME_GAME -> resume(request);
            case STEP_GAME -> step(request);
            case STOP_GAME -> stop(request);
            case SET_SPEED -> setSpeed(request);
            default ->
                    failure(
                            request,
                            ErrorCode.INVALID_REQUEST,
                            request.requestType() + " is not a session operation");
        };
    }

    private ResponseMessage join(RequestMessage request, SessionSubscriber subscriber) {
        if (!subscriber.isOpen()) {
            return failure(request, ErrorCode.INVALID_STATE, "Connection is already closed");
        }
        subscribers.add(subscriber);
        if (!subscriber.isOpen()) {
            subscribers.remove(subscriber);
            return failure(request, ErrorCode.INVALID_STATE, "Connection closed while joining");
        }
        return success(request, "Joined " + name, true);
    }

    private ResponseMessage leave(RequestMessage request, SessionSubscriber subscriber) {
        subscribers.remove(subscriber);
        return success(request, "Left " + name, false);
    }

    private ResponseMessage start(RequestMessage request) {
        if (status != SessionStatus.CREATED) {
            return invalidState(request, "Only a created session can be started");
        }
        status = SessionStatus.RUNNING;
        startedAt = Instant.now();
        lastAction = "Game started";
        version++;
        nextTurnDeadlineNanos = System.nanoTime() + delayNanos();
        refreshSnapshot();
        return success(request, lastAction, true);
    }

    private ResponseMessage pause(RequestMessage request) {
        if (status != SessionStatus.RUNNING) {
            return invalidState(request, "Only a running session can be paused");
        }
        status = SessionStatus.PAUSED;
        lastAction = "Game paused";
        version++;
        refreshSnapshot();
        return success(request, lastAction, true);
    }

    private ResponseMessage resume(RequestMessage request) {
        if (status != SessionStatus.PAUSED) {
            return invalidState(request, "Only a paused session can be resumed");
        }
        status = SessionStatus.RUNNING;
        lastAction = "Game resumed";
        version++;
        nextTurnDeadlineNanos = System.nanoTime() + delayNanos();
        refreshSnapshot();
        return success(request, lastAction, true);
    }

    private ResponseMessage step(RequestMessage request) {
        if (status != SessionStatus.PAUSED) {
            return invalidState(request, "Step is available only while paused");
        }
        advanceOneTurn("Advanced one paused turn");
        return success(request, lastAction, true);
    }

    private ResponseMessage stop(RequestMessage request) {
        if (status != SessionStatus.CREATED
                && status != SessionStatus.RUNNING
                && status != SessionStatus.PAUSED) {
            return invalidState(request, "This session cannot be stopped");
        }
        status = SessionStatus.STOPPED;
        completedAt = Instant.now();
        lastAction = "Game stopped";
        version++;
        refreshSnapshot();
        return success(request, lastAction, true);
    }

    private ResponseMessage setSpeed(RequestMessage request) {
        if (status != SessionStatus.CREATED
                && status != SessionStatus.RUNNING
                && status != SessionStatus.PAUSED) {
            return invalidState(request, "Speed cannot be changed in this session state");
        }
        String raw = request.parameters().get("turnDelayMillis");
        final long requestedDelay;
        try {
            requestedDelay = Long.parseLong(raw);
        } catch (NumberFormatException exception) {
            return failure(
                    request, ErrorCode.INVALID_REQUEST, "turnDelayMillis must be an integer");
        }
        if (requestedDelay < MIN_TURN_DELAY_MILLIS || requestedDelay > MAX_TURN_DELAY_MILLIS) {
            return failure(
                    request,
                    ErrorCode.INVALID_REQUEST,
                    "turnDelayMillis must be between "
                            + MIN_TURN_DELAY_MILLIS
                            + " and "
                            + MAX_TURN_DELAY_MILLIS);
        }
        turnDelayMillis = requestedDelay;
        lastAction = "Turn speed set to " + requestedDelay + " ms";
        version++;
        if (status == SessionStatus.RUNNING) {
            nextTurnDeadlineNanos = System.nanoTime() + delayNanos();
        }
        refreshSnapshot();
        return success(request, lastAction, true);
    }

    private void advanceAutomatically() {
        advanceOneTurn("Automatic turn completed");
        if (status == SessionStatus.RUNNING) {
            nextTurnDeadlineNanos = System.nanoTime() + delayNanos();
        }
    }

    private void advanceOneTurn(String action) {
        boolean continues = engine.advanceOneTurn();
        turn++;
        version++;
        lastAction = action;
        if (!continues || engine.isCompleted()) {
            status = SessionStatus.COMPLETED;
            completedAt = Instant.now();
            lastAction = "Game completed";
        }
        refreshSnapshot();
    }

    private void refreshSnapshot() {
        GameSnapshot core = engine.getSnapshot();
        List<GameSnapshotDto.PlayerDto> players = new ArrayList<>();
        for (GameSnapshot.PlayerView player : core.getPlayers()) {
            List<GameSnapshotDto.PieceDto> pieces = new ArrayList<>();
            for (GameSnapshot.PieceView piece : player.getPieces()) {
                pieces.add(
                        new GameSnapshotDto.PieceDto(
                                piece.getName(), piece.getFullName(), piece.getPosition()));
            }
            players.add(
                    new GameSnapshotDto.PlayerDto(
                            player.getColor(),
                            player.getBoardCount(),
                            player.getBaseCount(),
                            pieces));
        }
        latestSnapshot =
                new GameSnapshotDto(
                        sessionId,
                        version,
                        core.getRound(),
                        status,
                        players,
                        new GameSnapshotDto.MysteryDto(
                                core.isMysteryActive(),
                                core.getMysteryPosition(),
                                core.getMysteryRoundsRemaining()),
                        "",
                        turn,
                        lastAction);
    }

    private ResponseMessage success(RequestMessage request, String message, boolean withSnapshot) {
        return new ResponseMessage(
                RequestMessage.CURRENT_PROTOCOL_VERSION,
                MessageKind.RESPONSE,
                request.requestId(),
                request.clientId(),
                sessionId,
                true,
                null,
                message,
                summary(),
                List.of(),
                withSnapshot ? latestSnapshot : null,
                Instant.now());
    }

    private ResponseMessage invalidState(RequestMessage request, String message) {
        return failure(request, ErrorCode.INVALID_STATE, message);
    }

    private ResponseMessage failure(RequestMessage request, ErrorCode code, String message) {
        return new ResponseMessage(
                RequestMessage.CURRENT_PROTOCOL_VERSION,
                MessageKind.RESPONSE,
                request.requestId(),
                request.clientId(),
                sessionId,
                false,
                code,
                message,
                summary(),
                List.of(),
                latestSnapshot,
                Instant.now());
    }

    private long delayNanos() {
        return TimeUnit.MILLISECONDS.toNanos(turnDelayMillis);
    }

    private void failPendingRequests() {
        RequestEnvelope envelope;
        while ((envelope = requests.poll()) != null) {
            envelope.complete(
                    failure(
                            envelope.request(),
                            ErrorCode.SERVER_ERROR,
                            "Session is shutting down"));
        }
    }

    private static String safeMessage(Throwable failure) {
        String message = failure.getMessage();
        return message == null || message.isBlank() ? failure.getClass().getSimpleName() : message;
    }

    @Override
    public void close() {
        if (!closing.compareAndSet(false, true)) {
            return;
        }
        if (worker != null) {
            worker.interrupt();
        }
    }
}
