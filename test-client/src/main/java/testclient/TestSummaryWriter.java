package testclient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import protocol.CsvEncoder;
import testclient.TestResultRecorder.Summary;

/** Writes the deterministic one-row reconciliation summary for one client. */
public final class TestSummaryWriter {

    private static final String HEADER =
            "total_sent,total_responses,total_success,total_rejected,total_lost,total_duplicate,"
                    + "max_queue_depth,test_duration_ms";

    private TestSummaryWriter() {}

    public static void write(Path file, Summary summary) throws IOException {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(summary, "summary");
        Path absolute = file.toAbsolutePath();
        Path parent = absolute.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        String content =
                HEADER
                        + System.lineSeparator()
                        + CsvEncoder.row(
                                summary.totalSent(),
                                summary.totalResponses(),
                                summary.totalSuccess(),
                                summary.totalRejected(),
                                summary.totalLost(),
                                summary.totalDuplicate(),
                                summary.maxQueueDepth(),
                                summary.testDurationMillis());
        Files.writeString(
                absolute,
                content,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
    }
}
