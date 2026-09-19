package server.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import protocol.EventMessage;
import protocol.RequestMessage;
import protocol.RequestType;
import protocol.ResponseMessage;
import server.application.port.SessionSubscriber;

class SlowSubscriberPolicyTest {

    @Test
    void subscriberThatRejectsPushIsRemovedWithoutBlockingTheSessionWorker() throws Exception {
        try (SessionRegistry registry = new SessionRegistry(16, 10_000)) {
            GameSession session = registry.create("Slow-client", 42L);
            RejectingPushSubscriber subscriber = new RejectingPushSubscriber();
            submit(session, subscriber, RequestType.JOIN_SESSION);
            assertEquals(1, session.summary().connectedClients());

            submit(session, subscriber, RequestType.START_GAME);

            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
            while (session.summary().connectedClients() != 0 && System.nanoTime() < deadline) {
                Thread.sleep(5);
            }
            assertEquals(0, session.summary().connectedClients());
            assertTrue(session.latestSnapshot().version() >= 1);
        }
    }

    private static void submit(
            GameSession session, RejectingPushSubscriber subscriber, RequestType type)
            throws Exception {
        RequestMessage request =
                RequestMessage.create(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        type,
                        session.sessionId(),
                        Map.of(),
                        Instant.now());
        assertTrue(session.submit(request, subscriber, System.nanoTime()));
        assertTrue(subscriber.takeResponse().success());
    }

    private static final class RejectingPushSubscriber implements SessionSubscriber {

        private final UUID id = UUID.randomUUID();
        private final LinkedBlockingQueue<ResponseMessage> responses = new LinkedBlockingQueue<>();

        @Override
        public UUID connectionId() {
            return id;
        }

        @Override
        public boolean offer(Object message) {
            if (message instanceof EventMessage) return false;
            return responses.offer((ResponseMessage) message);
        }

        private ResponseMessage takeResponse() throws InterruptedException {
            ResponseMessage response = responses.poll(3, TimeUnit.SECONDS);
            if (response == null) throw new AssertionError("Timed out waiting for a response");
            return response;
        }
    }
}
