package server.lifecycle;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import server.network.GameServer;

/** Runs the bounded application shutdown sequence once from the JVM hook or main thread. */
public final class ShutdownCoordinator implements Runnable {

    private final GameServer server;
    private final long waitMillis;
    private final AtomicBoolean started = new AtomicBoolean();

    public ShutdownCoordinator(GameServer server, long waitMillis) {
        this.server = Objects.requireNonNull(server, "server");
        if (waitMillis <= 0) {
            throw new IllegalArgumentException("waitMillis must be positive");
        }
        this.waitMillis = waitMillis;
    }

    @Override
    public void run() {
        if (!started.compareAndSet(false, true)) {
            return;
        }
        Thread worker = Thread.ofVirtual().name("server-shutdown-worker").start(server::close);
        try {
            worker.join(waitMillis);
            if (worker.isAlive()) {
                worker.interrupt();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            worker.interrupt();
        }
    }
}
