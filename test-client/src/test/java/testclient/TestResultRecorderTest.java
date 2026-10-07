package testclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import protocol.ErrorCode;
import protocol.MessageKind;
import protocol.RequestMessage;
import protocol.RequestType;
import protocol.ResponseMessage;
import testclient.RapidClientConnection.PendingRequest;
import testclient.TestResultRecorder.Summary;

class TestResultRecorderTest {

    @TempDir Path temporaryDirectory;

    @Test
    void reconcilesSuccessRejectionLossAndDuplicate() throws Exception {
        ClientCsvLogger csv = new ClientCsvLogger(temporaryDirectory.resolve("client.csv"), 16);
        TestResultRecorder recorder = new TestResultRecorder(csv);
        PendingRequest success = pending();
        PendingRequest rejected = pending();
        PendingRequest lost = pending();
        recorder.track(success);
        recorder.track(rejected);
        recorder.track(lost);

        success.future().complete(response(success.request(), true));
        rejected.future().complete(response(rejected.request(), false));
        lost.future().completeExceptionally(new IOException("closed"));
        recorder.duplicateResponse(success.request().requestId());
        recorder.awaitAll(1000);
        Summary summary = recorder.summary();
        csv.close();

        assertEquals(3, summary.totalSent());
        assertEquals(2, summary.totalResponses());
        assertEquals(1, summary.totalSuccess());
        assertEquals(1, summary.totalRejected());
        assertEquals(1, summary.totalLost());
        assertEquals(1, summary.totalDuplicate());
    }

    private static PendingRequest pending() {
        RequestMessage request =
                RequestMessage.create(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        RequestType.STEP_GAME,
                        UUID.randomUUID(),
                        Map.of(),
                        Instant.now());
        return new PendingRequest(request, new CompletableFuture<>());
    }

    @Test
    void waitsForRecordingAfterNetworkFutureCompletes() throws Exception {
        Path file = temporaryDirectory.resolve("recording.csv");
        try (ClientCsvLogger csv = new ClientCsvLogger(file, 16)) {
            TestResultRecorder recorder = new TestResultRecorder(csv);
            PausedCallbackFuture<ResponseMessage> future = new PausedCallbackFuture<>();
            PendingRequest pending = new PendingRequest(pending().request(), future);
            recorder.track(pending);
            Thread completer =
                    Thread.ofVirtual()
                            .start(() -> future.complete(response(pending.request(), true)));
            try {
                assertTrue(future.entered.await(3, TimeUnit.SECONDS));
                assertTrue(future.isDone());
                assertEquals(0, recorder.summary().totalResponses());
                assertThrows(TimeoutException.class, () -> recorder.awaitAll(100));
                future.release.countDown();
                recorder.awaitAll(3000);
                assertTrue(recorder.summary().reconciled());
                assertEquals(1, csv.acceptedCount());
            } finally {
                future.release.countDown();
                completer.join(3000);
            }
        }
        assertEquals(2, Files.readAllLines(file).size());
    }

    /** Holds a response callback to simulate scheduling after the network result is available. */
    private static final class PausedCallbackFuture<T> extends CompletableFuture<T> {

        private final CountDownLatch entered = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);

        @Override
        public CompletableFuture<T> whenComplete(BiConsumer<? super T, ? super Throwable> action) {
            return super.whenComplete(
                    (value, failure) -> {
                        entered.countDown();
                        try {
                            if (!release.await(5, TimeUnit.SECONDS)) {
                                throw new IllegalStateException(
                                        "Recording callback was not released");
                            }
                        } catch (InterruptedException exception) {
                            Thread.currentThread().interrupt();
                            throw new IllegalStateException(exception);
                        }
                        action.accept(value, failure);
                    });
        }
    }

    private static ResponseMessage response(RequestMessage request, boolean success) {
        return new ResponseMessage(
                1,
                MessageKind.RESPONSE,
                request.requestId(),
                request.clientId(),
                request.sessionId(),
                success,
                success ? null : ErrorCode.QUEUE_FULL,
                success ? "ok" : "full",
                null,
                List.of(),
                null,
                Instant.now());
    }
}
