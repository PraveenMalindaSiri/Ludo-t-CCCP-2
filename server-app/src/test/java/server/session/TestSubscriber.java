package server.session;

import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import protocol.EventMessage;
import protocol.ResponseMessage;
import server.application.port.SessionSubscriber;

final class TestSubscriber implements SessionSubscriber {

    private final UUID id = UUID.randomUUID();
    private final LinkedBlockingQueue<Object> messages = new LinkedBlockingQueue<>();
    private volatile boolean open = true;

    @Override
    public UUID connectionId() {
        return id;
    }

    @Override
    public boolean offer(Object message) {
        return open && messages.offer(message);
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    ResponseMessage takeResponse() throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (System.nanoTime() < deadline) {
            Object message = messages.poll(100, TimeUnit.MILLISECONDS);
            if (message instanceof ResponseMessage response) return response;
        }
        throw new AssertionError("Timed out waiting for a response");
    }

    EventMessage takeSnapshotEvent(long minimumVersion) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (System.nanoTime() < deadline) {
            Object message = messages.poll(100, TimeUnit.MILLISECONDS);
            if (message instanceof EventMessage event
                    && event.snapshot() != null
                    && event.version() >= minimumVersion) {
                return event;
            }
        }
        throw new AssertionError("Timed out waiting for a snapshot event");
    }

    void closeSubscriber() {
        open = false;
    }
}
