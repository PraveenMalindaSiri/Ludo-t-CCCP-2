package testclient;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import protocol.JsonLineCodec;
import protocol.RequestType;
import protocol.ResponseMessage;
import testclient.TestResultRecorder.Summary;

/** Command-line entry point for one independently runnable asynchronous load process. */
public final class RapidTestClientMain {

    private RapidTestClientMain() {}

    public static void main(String[] args) throws Exception {
        Options options = Options.parse(args);
        Path output = options.outputDirectory();
        String safeLabel = options.clientLabel().replaceAll("[^A-Za-z0-9_-]", "_");
        ClientCsvLogger csv =
                new ClientCsvLogger(
                        output.resolve("client-" + safeLabel + ".csv"), options.evidenceCapacity());
        Summary summary;
        int maximumOutstanding;
        try (csv;
                RapidClientConnection connection =
                        new RapidClientConnection(
                                options.host(),
                                options.port(),
                                options.outboundCapacity(),
                                JsonLineCodec.DEFAULT_MAX_LINE_LENGTH,
                                options.timeoutMillis())) {
            connection.connect();
            ResponseMessage joined =
                    connection
                            .requestAsync(RequestType.JOIN_SESSION, options.sessionId(), Map.of())
                            .future()
                            .get(options.timeoutMillis(), TimeUnit.MILLISECONDS);
            if (!joined.success()) {
                throw new IllegalStateException("Unable to join session: " + joined.message());
            }
            TestResultRecorder recorder = new TestResultRecorder(csv);
            connection.setDuplicateResponseListener(recorder::duplicateResponse);
            RequestBurstRunner runner = new RequestBurstRunner(connection, recorder);
            summary =
                    runner.run(
                            options.sessionId(),
                            options.requestCount(),
                            options.scenario(),
                            options.timeoutMillis() + 5000);
            maximumOutstanding = connection.maximumOutstanding();
            connection.disconnect();
        }
        TestSummaryWriter.write(output.resolve("summary-" + safeLabel + ".csv"), summary);
        System.out.printf(
                Locale.ROOT,
                "%s sent=%d responses=%d success=%d rejected=%d lost=%d duplicate=%d "
                        + "maxOutstanding=%d maxQueueDepth=%d durationMs=%d%n",
                options.clientLabel(),
                summary.totalSent(),
                summary.totalResponses(),
                summary.totalSuccess(),
                summary.totalRejected(),
                summary.totalLost(),
                summary.totalDuplicate(),
                maximumOutstanding,
                summary.maxQueueDepth(),
                summary.testDurationMillis());
        if (!summary.reconciled() || csv.droppedCount() != 0) {
            throw new IllegalStateException("Rapid-client evidence did not reconcile");
        }
    }

    record Options(
            String host,
            int port,
            String clientLabel,
            int requestCount,
            UUID sessionId,
            String scenario,
            Path outputDirectory,
            int outboundCapacity,
            int evidenceCapacity,
            long timeoutMillis) {

        static Options parse(String[] args) {
            Map<String, String> values = new HashMap<>();
            for (String argument : args) {
                if (!argument.startsWith("--") || !argument.contains("=")) {
                    throw new IllegalArgumentException("Arguments must use --name=value");
                }
                int separator = argument.indexOf('=');
                values.put(argument.substring(2, separator), argument.substring(separator + 1));
            }
            String label = required(values, "client");
            UUID session = UUID.fromString(required(values, "session"));
            return new Options(
                    values.getOrDefault("host", "127.0.0.1"),
                    integer(values, "port", 5050),
                    label,
                    integer(values, "requests", 200),
                    session,
                    values.getOrDefault("scenario", "step"),
                    Path.of(values.getOrDefault("output", "test-results")),
                    integer(values, "outbound-capacity", 512),
                    integer(values, "evidence-capacity", 1024),
                    longValue(values, "timeout-ms", 30_000));
        }

        private static String required(Map<String, String> values, String key) {
            String value = values.get(key);
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("Missing --" + key + "=value");
            }
            return value;
        }

        private static int integer(Map<String, String> values, String key, int fallback) {
            int value = Integer.parseInt(values.getOrDefault(key, Integer.toString(fallback)));
            if (value <= 0) {
                throw new IllegalArgumentException(key + " must be positive");
            }
            return value;
        }

        private static long longValue(Map<String, String> values, String key, long fallback) {
            long value = Long.parseLong(values.getOrDefault(key, Long.toString(fallback)));
            if (value <= 0 || value > Integer.MAX_VALUE) {
                throw new IllegalArgumentException(
                        key + " must be between 1 and Integer.MAX_VALUE");
            }
            return value;
        }
    }
}
