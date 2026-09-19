package server.session;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import protocol.RequestType;

class DifferentSessionConcurrencyIT {

    @Test
    void separateVirtualWorkersProgressDuringTheSameWallClockWindow() throws Exception {
        try (SessionRegistry registry = new SessionRegistry(32, 10)) {
            GameSession first = registry.create("Game-1", 11L);
            GameSession second = registry.create("Game-2", 22L);
            TestSubscriber firstSubscriber = new TestSubscriber();
            TestSubscriber secondSubscriber = new TestSubscriber();
            SnapshotVersionTest.submit(first, firstSubscriber, RequestType.JOIN_SESSION, Map.of());
            SnapshotVersionTest.submit(
                    second, secondSubscriber, RequestType.JOIN_SESSION, Map.of());

            long started = System.nanoTime();
            SnapshotVersionTest.submit(first, firstSubscriber, RequestType.START_GAME, Map.of());
            SnapshotVersionTest.submit(second, secondSubscriber, RequestType.START_GAME, Map.of());
            waitForTurn(first, 2);
            waitForTurn(second, 2);
            long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);

            assertNotEquals(first.workerName(), second.workerName());
            assertTrue(first.latestSnapshot().turn() >= 2);
            assertTrue(second.latestSnapshot().turn() >= 2);
            assertTrue(elapsedMillis < 2_000, "Both sessions should progress in one test window");
        }
    }

    private static void waitForTurn(GameSession session, long turn) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (session.latestSnapshot().turn() < turn && System.nanoTime() < deadline) {
            Thread.sleep(5);
        }
    }
}
