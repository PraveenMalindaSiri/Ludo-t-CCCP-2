package testclient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import protocol.RequestType;
import testclient.RapidClientConnection.PendingRequest;
import testclient.TestResultRecorder.Summary;

/** Sends the complete burst first and waits for correlated responses afterwards. */
public final class RequestBurstRunner {

    private final RapidClientConnection connection;
    private final TestResultRecorder recorder;

    public RequestBurstRunner(RapidClientConnection connection, TestResultRecorder recorder) {
        this.connection = java.util.Objects.requireNonNull(connection, "connection");
        this.recorder = java.util.Objects.requireNonNull(recorder, "recorder");
    }

    public Summary run(UUID sessionId, int requestCount, String scenario, long timeoutMillis)
            throws Exception {
        if (requestCount <= 0) {
            throw new IllegalArgumentException("requestCount must be positive");
        }
        RequestType type = scenarioType(scenario);
        List<PendingRequest> burst = new ArrayList<>(requestCount);
        for (int index = 0; index < requestCount; index++) {
            PendingRequest pending = connection.requestAsync(type, sessionId, Map.of());
            burst.add(pending);
            recorder.track(pending);
            if ((index & 15) == 15) {
                Thread.yield();
            }
        }
        if (requestCount > 1 && connection.maximumOutstanding() < 2) {
            throw new IllegalStateException(
                    "Burst did not create simultaneous outstanding requests");
        }
        recorder.awaitAll(timeoutMillis);
        return recorder.summary();
    }

    static RequestType scenarioType(String scenario) {
        String normalized = scenario == null ? "step" : scenario.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "step", "queue-full" -> RequestType.STEP_GAME;
            case "snapshot" -> RequestType.GET_SNAPSHOT;
            default ->
                    throw new IllegalArgumentException(
                            "scenario must be step, snapshot or queue-full");
        };
    }
}
