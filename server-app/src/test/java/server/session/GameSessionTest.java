package server.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import factory.GameFactory;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import protocol.ErrorCode;
import protocol.RequestMessage;
import protocol.RequestType;
import protocol.ResponseMessage;
import protocol.SessionStatus;
import server.application.port.SessionSubscriber;

class GameSessionTest {

    private final List<AutoCloseable> closeables = new ArrayList<>();

    @AfterEach
    void closeSessions() throws Exception {
        for (AutoCloseable closeable : closeables) {
            closeable.close();
        }
    }

    @Test
    void registryCreatesUniqueIndependentSessionsAndRemovesThem() {
        SessionRegistry registry = registry(8, 500);
        GameSession first = registry.create("First", 11L);
        GameSession second = registry.create("Second", 22L);

        assertNotEquals(first.sessionId(), second.sessionId());
        assertNotSame(first.latestSnapshot(), second.latestSnapshot());
        assertNotSame(first.latestSnapshot().players(), second.latestSnapshot().players());
        assertEquals(2, registry.list().size());
        assertTrue(first.workerIsVirtual());
        assertNotEquals(first.workerName(), second.workerName());
        assertTrue(registry.remove(first.sessionId()));
        assertEquals(1, registry.size());
    }

    @Test
    void boundedQueueRejectsWhenItsSingleSlotIsOccupied() {
        GameSession session =
                new GameSession(
                        UUID.randomUUID(), "No worker", GameFactory.createGame(1L), 1, 500, false);
        closeables.add(session);
        RecordingSubscriber subscriber = new RecordingSubscriber();

        assertTrue(session.submit(request(RequestType.GET_SNAPSHOT, session), subscriber, 1));
        assertFalse(session.submit(request(RequestType.GET_SNAPSHOT, session), subscriber, 2));
        assertEquals(1, session.pendingRequestCount());
    }

    @Test
    void lifecycleTransitionsAndStepAreSerializedByTheWorker() throws Exception {
        SessionRegistry registry = registry(16, 10_000);
        GameSession session = registry.create("Lifecycle", 42L);
        RecordingSubscriber subscriber = new RecordingSubscriber();

        assertSuccess(session, subscriber, RequestType.JOIN_SESSION);
        assertStatus(session, subscriber, RequestType.START_GAME, SessionStatus.RUNNING);
        ResponseMessage duplicateStart = submit(session, subscriber, RequestType.START_GAME);
        assertFalse(duplicateStart.success());
        assertEquals(ErrorCode.INVALID_STATE, duplicateStart.errorCode());
        assertStatus(session, subscriber, RequestType.PAUSE_GAME, SessionStatus.PAUSED);

        long turnBefore = session.latestSnapshot().turn();
        assertStatus(session, subscriber, RequestType.STEP_GAME, SessionStatus.PAUSED);
        assertEquals(turnBefore + 1, session.latestSnapshot().turn());

        assertStatus(session, subscriber, RequestType.RESUME_GAME, SessionStatus.RUNNING);
        assertStatus(session, subscriber, RequestType.STOP_GAME, SessionStatus.STOPPED);
        ResponseMessage afterStop = submit(session, subscriber, RequestType.RESUME_GAME);
        assertEquals(ErrorCode.INVALID_STATE, afterStop.errorCode());
    }

    @Test
    void concurrentProducersReceiveOneResponsePerAcceptedRequest() throws Exception {
        SessionRegistry registry = registry(128, 10_000);
        GameSession session = registry.create("Ordering", 7L);
        RecordingSubscriber subscriber = new RecordingSubscriber();
        assertSuccess(session, subscriber, RequestType.JOIN_SESSION);

        int producers = 40;
        CountDownLatch ready = new CountDownLatch(producers);
        CountDownLatch release = new CountDownLatch(1);
        Thread[] threads = new Thread[producers];
        for (int index = 0; index < producers; index++) {
            long delay = 100 + index;
            threads[index] =
                    Thread.ofVirtual()
                            .start(
                                    () -> {
                                        ready.countDown();
                                        try {
                                            release.await();
                                        } catch (InterruptedException exception) {
                                            Thread.currentThread().interrupt();
                                            return;
                                        }
                                        RequestMessage request =
                                                request(
                                                        RequestType.SET_SPEED,
                                                        session,
                                                        Map.of(
                                                                "turnDelayMillis",
                                                                Long.toString(delay)));
                                        if (!session.submit(
                                                request, subscriber, System.nanoTime())) {
                                            throw new AssertionError("Request was not accepted");
                                        }
                                    });
        }
        assertTrue(ready.await(3, TimeUnit.SECONDS));
        release.countDown();
        for (Thread thread : threads) {
            thread.join();
        }

        List<UUID> responseIds = new ArrayList<>();
        for (int index = 0; index < producers; index++) {
            responseIds.add(subscriber.take().requestId());
        }
        assertEquals(producers, responseIds.stream().distinct().count());
        assertTrue(session.turnDelayMillis() >= 100);
        assertTrue(session.turnDelayMillis() < 140);
    }

    @Test
    void separateSessionWorkersProgressIndependently() throws Exception {
        SessionRegistry registry = registry(16, 10);
        GameSession first = registry.create("One", 101L);
        GameSession second = registry.create("Two", 202L);
        RecordingSubscriber firstSubscriber = new RecordingSubscriber();
        RecordingSubscriber secondSubscriber = new RecordingSubscriber();

        assertSuccess(first, firstSubscriber, RequestType.JOIN_SESSION);
        assertSuccess(second, secondSubscriber, RequestType.JOIN_SESSION);
        assertSuccess(first, firstSubscriber, RequestType.START_GAME);
        assertSuccess(second, secondSubscriber, RequestType.START_GAME);

        waitForTurn(first, 1);
        waitForTurn(second, 1);
        assertNotEquals(first.workerName(), second.workerName());
    }

    @Test
    void multipleSubscribersCanJoinAndCreatorDisconnectDoesNotStopGame() throws Exception {
        SessionRegistry registry = registry(16, 10);
        GameSession session = registry.create("Shared", 303L);
        RecordingSubscriber creator = new RecordingSubscriber();
        RecordingSubscriber other = new RecordingSubscriber();

        assertSuccess(session, creator, RequestType.JOIN_SESSION);
        assertSuccess(session, other, RequestType.JOIN_SESSION);
        assertEquals(2, session.summary().connectedClients());
        assertSuccess(session, creator, RequestType.START_GAME);
        session.removeSubscriber(creator);

        waitForTurn(session, 1);
        assertEquals(SessionStatus.RUNNING, session.status());
        assertEquals(1, session.summary().connectedClients());
        assertSuccess(session, other, RequestType.GET_SNAPSHOT);
    }

    private SessionRegistry registry(int capacity, long delayMillis) {
        SessionRegistry registry = new SessionRegistry(capacity, delayMillis);
        closeables.add(registry);
        return registry;
    }

    private static void assertStatus(
            GameSession session,
            RecordingSubscriber subscriber,
            RequestType type,
            SessionStatus expected)
            throws Exception {
        ResponseMessage response = submit(session, subscriber, type);
        assertTrue(response.success(), response.message());
        assertEquals(expected, response.snapshot().status());
    }

    private static ResponseMessage assertSuccess(
            GameSession session, RecordingSubscriber subscriber, RequestType type)
            throws Exception {
        ResponseMessage response = submit(session, subscriber, type);
        assertTrue(response.success(), response.message());
        return response;
    }

    private static ResponseMessage submit(
            GameSession session, RecordingSubscriber subscriber, RequestType type)
            throws Exception {
        RequestMessage request = request(type, session);
        assertTrue(session.submit(request, subscriber, System.nanoTime()));
        ResponseMessage response = subscriber.take();
        assertEquals(request.requestId(), response.requestId());
        return response;
    }

    private static RequestMessage request(RequestType type, GameSession session) {
        return request(type, session, Map.of());
    }

    private static RequestMessage request(
            RequestType type, GameSession session, Map<String, String> parameters) {
        return RequestMessage.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                type,
                session.sessionId(),
                parameters,
                Instant.now());
    }

    private static void waitForTurn(GameSession session, long expected) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (System.nanoTime() < deadline && session.latestSnapshot().turn() < expected) {
            Thread.onSpinWait();
        }
        assertTrue(session.latestSnapshot().turn() >= expected);
    }

    private static final class RecordingSubscriber implements SessionSubscriber {

        private final UUID id = UUID.randomUUID();
        private final LinkedBlockingQueue<ResponseMessage> responses = new LinkedBlockingQueue<>();

        @Override
        public UUID connectionId() {
            return id;
        }

        @Override
        public boolean offer(Object message) {
            return responses.offer((ResponseMessage) message);
        }

        private ResponseMessage take() throws InterruptedException {
            ResponseMessage response = responses.poll(3, TimeUnit.SECONDS);
            if (response == null) {
                throw new AssertionError("Timed out waiting for session response");
            }
            return response;
        }
    }
}
