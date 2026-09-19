package server.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;
import protocol.RequestType;

class SessionIsolationIT {

    @Test
    void subscribersOnlyReceiveVersionsFromTheirJoinedSession() throws Exception {
        try (SessionRegistry registry = new SessionRegistry(32, 10_000)) {
            GameSession first = registry.create("Game-1", 101L);
            GameSession second = registry.create("Game-2", 202L);
            TestSubscriber firstSubscriber = new TestSubscriber();
            TestSubscriber secondSubscriber = new TestSubscriber();
            SnapshotVersionTest.submit(first, firstSubscriber, RequestType.JOIN_SESSION, Map.of());
            SnapshotVersionTest.submit(
                    second, secondSubscriber, RequestType.JOIN_SESSION, Map.of());

            SnapshotVersionTest.submit(
                    first,
                    firstSubscriber,
                    RequestType.SET_SPEED,
                    Map.of("turnDelayMillis", "250"));
            var firstPush = firstSubscriber.takeSnapshotEvent(1);

            assertEquals(first.sessionId(), firstPush.sessionId());
            assertEquals(first.sessionId(), firstPush.snapshot().sessionId());
            assertNotEquals(
                    first.latestSnapshot().sessionId(), second.latestSnapshot().sessionId());
            assertEquals(0, second.latestSnapshot().version());
        }
    }
}
