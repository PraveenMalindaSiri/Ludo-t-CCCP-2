package testclient;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
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
