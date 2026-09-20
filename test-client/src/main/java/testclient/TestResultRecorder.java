package testclient;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import protocol.ResponseMessage;
import testclient.ClientCsvLogger.ClientEvidenceRecord;
import testclient.RapidClientConnection.PendingRequest;

/** Correlates sent requests, responses, failures and duplicates into one summary. */
public final class TestResultRecorder {

    private final ClientCsvLogger csv;
    private final Instant startedAt = Instant.now();
    private final ConcurrentMap<UUID, Entry> entries = new ConcurrentHashMap<>();
    private final AtomicLong responses = new AtomicLong();
    private final AtomicLong successes = new AtomicLong();
    private final AtomicLong rejections = new AtomicLong();
    private final AtomicLong lost = new AtomicLong();
    private final AtomicLong duplicates = new AtomicLong();
    private final AtomicInteger maximumQueueDepth = new AtomicInteger();

    public TestResultRecorder(ClientCsvLogger csv) {
        this.csv = java.util.Objects.requireNonNull(csv, "csv");
    }

    public void track(PendingRequest pending) {
        Entry entry = new Entry(pending);
        Entry previous = entries.putIfAbsent(pending.request().requestId(), entry);
        if (previous != null) {
            throw new IllegalStateException("Duplicate generated request ID");
        }
        pending.future().whenComplete((response, failure) -> complete(entry, response, failure));
    }

    public void duplicateResponse(UUID requestId) {
        duplicates.incrementAndGet();
        System.err.println("Duplicate or unknown response ID " + requestId);
    }

    public void awaitAll(long timeoutMillis) throws Exception {
        List<CompletableFuture<ResponseMessage>> futures =
                entries.values().stream().map(entry -> entry.pending.future()).toList();
        CompletableFuture<?>[] settled =
                futures.stream()
                        .map(future -> future.handle((value, failure) -> null))
                        .toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(settled).get(timeoutMillis, TimeUnit.MILLISECONDS);
    }

    public Summary summary() {
        long duration = Math.max(0, Duration.between(startedAt, Instant.now()).toMillis());
        return new Summary(
                entries.size(),
                responses.get(),
                successes.get(),
                rejections.get(),
                lost.get(),
                duplicates.get(),
                maximumQueueDepth.get(),
                duration);
    }

    private void complete(Entry entry, ResponseMessage response, Throwable failure) {
        if (!entry.completed.compareAndSet(false, true)) {
            duplicates.incrementAndGet();
            return;
        }
        Instant responseAt = Instant.now();
        String status;
        if (failure != null) {
            lost.incrementAndGet();
            status = "LOST:" + rootType(failure);
        } else {
            responses.incrementAndGet();
            if (response.success()) {
                successes.incrementAndGet();
                status = "SUCCESS";
            } else {
                rejections.incrementAndGet();
                status = "REJECTED:" + response.errorCode();
            }
            if (response.snapshot() != null) {
                maximumQueueDepth.accumulateAndGet(response.snapshot().queueDepth(), Math::max);
            }
        }
        long elapsed =
                Math.max(
                        0,
                        Duration.between(entry.pending.request().sentAt(), responseAt).toMillis());
        csv.record(
                new ClientEvidenceRecord(
                        entry.pending.request().clientId(),
                        entry.pending.request().requestId(),
                        entry.pending.request().sessionId(),
                        entry.pending.request().requestType(),
                        entry.pending.request().sentAt(),
                        responseAt,
                        elapsed,
                        status));
    }

    private static String rootType(Throwable failure) {
        Throwable current = failure;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getClass().getSimpleName();
    }

    private static final class Entry {

        private final PendingRequest pending;
        private final AtomicBoolean completed = new AtomicBoolean();

        private Entry(PendingRequest pending) {
            this.pending = pending;
        }
    }

    public record Summary(
            long totalSent,
            long totalResponses,
            long totalSuccess,
            long totalRejected,
            long totalLost,
            long totalDuplicate,
            int maxQueueDepth,
            long testDurationMillis) {

        public boolean reconciled() {
            return totalSent == totalResponses && totalLost == 0 && totalDuplicate == 0;
        }
    }
}
