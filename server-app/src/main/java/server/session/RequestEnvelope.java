package server.session;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import protocol.RequestMessage;
import protocol.ResponseMessage;
import server.application.port.SessionSubscriber;

/** One accepted session request plus timing and its exactly-once completion path. */
public final class RequestEnvelope {

    private final RequestMessage request;
    private final SessionSubscriber subscriber;
    private final Instant receivedAt;
    private final Instant queuedAt;
    private final long receivedNanos;
    private final long enqueuedNanos;
    private final int queueDepth;
    private final Consumer<ResponseMessage> completion;
    private final AtomicBoolean completed = new AtomicBoolean();

    public RequestEnvelope(
            RequestMessage request,
            SessionSubscriber subscriber,
            Instant receivedAt,
            Instant queuedAt,
            long receivedNanos,
            long enqueuedNanos,
            int queueDepth,
            Consumer<ResponseMessage> completion) {
        this.request = Objects.requireNonNull(request, "request");
        this.subscriber = Objects.requireNonNull(subscriber, "subscriber");
        this.receivedAt = Objects.requireNonNull(receivedAt, "receivedAt");
        this.queuedAt = Objects.requireNonNull(queuedAt, "queuedAt");
        this.receivedNanos = receivedNanos;
        this.enqueuedNanos = enqueuedNanos;
        if (queueDepth < 0) {
            throw new IllegalArgumentException("queueDepth must not be negative");
        }
        this.queueDepth = queueDepth;
        this.completion = Objects.requireNonNull(completion, "completion");
    }

    public RequestMessage request() {
        return request;
    }

    public SessionSubscriber subscriber() {
        return subscriber;
    }

    public Instant receivedAt() {
        return receivedAt;
    }

    public Instant queuedAt() {
        return queuedAt;
    }

    public long receivedNanos() {
        return receivedNanos;
    }

    public long enqueuedNanos() {
        return enqueuedNanos;
    }

    public long queueWaitNanos(long startedNanos) {
        return Math.max(0, startedNanos - enqueuedNanos);
    }

    public int queueDepth() {
        return queueDepth;
    }

    public boolean complete(ResponseMessage response) {
        if (!completed.compareAndSet(false, true)) {
            return false;
        }
        completion.accept(Objects.requireNonNull(response, "response"));
        return true;
    }
}
