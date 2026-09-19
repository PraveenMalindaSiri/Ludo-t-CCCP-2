package server.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import protocol.RequestMessage;
import protocol.RequestType;

class SnapshotVersionTest {

    @Test
    void versionAdvancesOncePerAcceptedMutationAndNotForReadsOrSubscriptions() throws Exception {
        try (SessionRegistry registry = new SessionRegistry(32, 10_000)) {
            GameSession session = registry.create("Versioned", 42L);
            TestSubscriber subscriber = new TestSubscriber();

            submit(session, subscriber, RequestType.JOIN_SESSION, Map.of());
            assertEquals(0, session.latestSnapshot().version());
            submit(session, subscriber, RequestType.GET_SNAPSHOT, Map.of());
            assertEquals(0, session.latestSnapshot().version());

            submit(session, subscriber, RequestType.SET_SPEED, Map.of("turnDelayMillis", "500"));
            assertEquals(1, session.latestSnapshot().version());
            submit(session, subscriber, RequestType.START_GAME, Map.of());
            assertEquals(2, session.latestSnapshot().version());
            submit(session, subscriber, RequestType.PAUSE_GAME, Map.of());
            assertEquals(3, session.latestSnapshot().version());
            submit(session, subscriber, RequestType.STEP_GAME, Map.of());
            assertEquals(4, session.latestSnapshot().version());
            submit(session, subscriber, RequestType.GET_SNAPSHOT, Map.of());
            assertEquals(4, session.latestSnapshot().version());
            assertTrue(session.latestSnapshot().turn() >= 1);
        }
    }

    static void submit(
            GameSession session,
            TestSubscriber subscriber,
            RequestType type,
            Map<String, String> parameters)
            throws Exception {
        RequestMessage request =
                RequestMessage.create(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        type,
                        session.sessionId(),
                        parameters,
                        Instant.now());
        assertTrue(session.submit(request, subscriber, System.nanoTime()));
        assertTrue(subscriber.takeResponse().success());
    }
}
