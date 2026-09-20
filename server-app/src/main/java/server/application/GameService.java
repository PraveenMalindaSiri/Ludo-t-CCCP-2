package server.application;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import protocol.ErrorCode;
import protocol.MessageKind;
import protocol.RequestMessage;
import protocol.RequestType;
import protocol.ResponseMessage;
import server.ServerConfig;
import server.application.port.GameRepository;
import server.application.port.RepositoryException;
import server.application.port.SessionSubscriber;
import server.evidence.ServerCsvLogger;
import server.evidence.ServerCsvLogger.ServerEvidenceRecord;
import server.session.GameSession;
import server.session.SessionRegistry;

/** Application use cases between network connections and the session registry. */
public final class GameService implements AutoCloseable {

    private final SessionRegistry registry;
    private final GameRepository repository;
    private final ServerCsvLogger evidence;
    private final AtomicBoolean stopping = new AtomicBoolean();

    public GameService(ServerConfig config) {
        this(
                new SessionRegistry(config.sessionQueueCapacity(), config.defaultTurnDelayMillis()),
                ServerCsvLogger.disabled());
    }

    public GameService(ServerConfig config, GameRepository repository) {
        this(config, repository, ServerCsvLogger.disabled());
    }

    public GameService(ServerConfig config, GameRepository repository, ServerCsvLogger evidence) {
        this(
                new SessionRegistry(
                        config.sessionQueueCapacity(),
                        config.defaultTurnDelayMillis(),
                        repository,
                        evidence,
                        config.shutdownDrainMillis()),
                evidence);
    }

    public GameService(SessionRegistry registry) {
        this(registry, ServerCsvLogger.disabled());
    }

    private GameService(SessionRegistry registry, ServerCsvLogger evidence) {
        this.registry = registry;
        this.repository = registry.repository();
        this.evidence = evidence;
    }

    public ResponseMessage handleOrEnqueue(
            RequestMessage request, SessionSubscriber subscriber, long receivedNanos) {
        return handleOrEnqueue(request, subscriber, Instant.now(), receivedNanos);
    }

    public ResponseMessage handleOrEnqueue(
            RequestMessage request,
            SessionSubscriber subscriber,
            Instant receivedAt,
            long receivedNanos) {
        if (stopping.get()) {
            return failure(request, ErrorCode.SERVER_ERROR, "Server is shutting down");
        }
        return switch (request.requestType()) {
            case LIST_SESSIONS -> list(request);
            case CREATE_SESSION -> create(request, subscriber);
            case JOIN_SESSION,
                    LEAVE_SESSION,
                    GET_SNAPSHOT,
                    START_GAME,
                    PAUSE_GAME,
                    RESUME_GAME,
                    STEP_GAME,
                    STOP_GAME,
                    SET_SPEED ->
                    enqueue(request, subscriber, receivedAt, receivedNanos);
            default ->
                    failure(
                            request,
                            ErrorCode.INVALID_REQUEST,
                            request.requestType() + " is not a game service operation");
        };
    }

    public SessionRegistry registry() {
        return registry;
    }

    public void connectionClosed(SessionSubscriber subscriber) {
        registry.removeSubscriber(subscriber);
    }

    public void beginShutdown() {
        stopping.set(true);
    }

    private ResponseMessage list(RequestMessage request) {
        return new ResponseMessage(
                RequestMessage.CURRENT_PROTOCOL_VERSION,
                MessageKind.RESPONSE,
                request.requestId(),
                request.clientId(),
                null,
                true,
                null,
                "Sessions listed",
                null,
                registry.list(),
                null,
                Instant.now());
    }

    private ResponseMessage create(RequestMessage request, SessionSubscriber subscriber) {
        String name = request.parameters().get("name");
        long seed;
        try {
            String rawSeed = request.parameters().get("seed");
            seed = rawSeed == null ? System.nanoTime() : Long.parseLong(rawSeed);
        } catch (NumberFormatException exception) {
            return failure(request, ErrorCode.INVALID_REQUEST, "seed must be a long integer");
        }

        final GameSession session;
        try {
            session = registry.create(name, seed);
        } catch (RepositoryException failure) {
            return failure(
                    request,
                    ErrorCode.SERVER_ERROR,
                    "Unable to persist new session: " + safeMessage(failure));
        }
        RequestMessage join =
                new RequestMessage(
                        request.protocolVersion(),
                        request.kind(),
                        request.requestId(),
                        request.clientId(),
                        RequestType.CREATE_SESSION,
                        session.sessionId(),
                        request.parameters(),
                        request.sentAt());
        ResponseMessage immediate = enqueue(join, subscriber, Instant.now(), System.nanoTime());
        if (immediate != null) {
            registry.remove(session.sessionId());
            return immediate;
        }
        return null;
    }

    private ResponseMessage enqueue(
            RequestMessage request,
            SessionSubscriber subscriber,
            Instant receivedAt,
            long receivedNanos) {
        GameSession session = registry.find(request.sessionId());
        if (session == null) {
            return failure(request, ErrorCode.UNKNOWN_SESSION, "Unknown game session");
        }
        if (!session.submit(request, subscriber, receivedAt, receivedNanos)) {
            recordQueueFull(request, receivedAt, receivedNanos, session.pendingRequestCount());
            return failure(request, ErrorCode.QUEUE_FULL, "Session request queue is full");
        }
        return null;
    }

    private ResponseMessage failure(RequestMessage request, ErrorCode code, String message) {
        return new ResponseMessage(
                RequestMessage.CURRENT_PROTOCOL_VERSION,
                MessageKind.RESPONSE,
                request.requestId(),
                request.clientId(),
                request.sessionId(),
                false,
                code,
                message,
                null,
                List.of(),
                null,
                Instant.now());
    }

    private static String safeMessage(Throwable failure) {
        String message = failure.getMessage();
        return message == null || message.isBlank() ? failure.getClass().getSimpleName() : message;
    }

    private void recordQueueFull(
            RequestMessage request, Instant receivedAt, long receivedNanos, int queueDepth) {
        Instant now = Instant.now();
        long nowNanos = System.nanoTime();
        Thread current = Thread.currentThread();
        evidence.record(
                new ServerEvidenceRecord(
                        request.requestId(),
                        request.clientId(),
                        request.sessionId(),
                        request.requestType(),
                        receivedAt,
                        now,
                        now,
                        now,
                        0,
                        TimeUnit.NANOSECONDS.toMillis(Math.max(0, nowNanos - receivedNanos)),
                        queueDepth,
                        "REJECTED:" + ErrorCode.QUEUE_FULL,
                        current.getName(),
                        current.isVirtual()));
    }

    @Override
    public void close() {
        beginShutdown();
        try {
            registry.close();
        } finally {
            try {
                evidence.close();
            } finally {
                repository.close();
            }
        }
    }
}
