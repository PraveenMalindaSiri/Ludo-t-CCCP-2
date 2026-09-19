package server.session;

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
    private final long receivedNanos;
    private final long enqueuedNanos;
    private final Consumer<ResponseMessage> completion;
    private final AtomicBoolean completed = new AtomicBoolean();

    public RequestEnvelope(
            RequestMessage request,
            SessionSubscriber subscriber,
            long receivedNanos,
            long enqueuedNanos,
            Consumer<ResponseMessage> completion) {
        this.request = Objects.requireNonNull(request, "request");
        this.subscriber = Objects.requireNonNull(subscriber, "subscriber");
        this.receivedNanos = receivedNanos;
        this.enqueuedNanos = enqueuedNanos;
        this.completion = Objects.requireNonNull(completion, "completion");
    }

    public RequestMessage request() {
        return request;
    }

    public SessionSubscriber subscriber() {
        return subscriber;
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

    public boolean complete(ResponseMessage response) {
        if (!completed.compareAndSet(false, true)) {
            return false;
        }
        completion.accept(Objects.requireNonNull(response, "response"));
        return true;
    }
}
