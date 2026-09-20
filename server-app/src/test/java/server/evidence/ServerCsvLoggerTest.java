package server.evidence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import protocol.RequestType;
import server.evidence.ServerCsvLogger.ServerEvidenceRecord;

class ServerCsvLoggerTest {

    @TempDir Path temporaryDirectory;

    @Test
    void concurrentProducersUseOneVirtualWriterAndDrainOnClose() throws Exception {
        Path output = temporaryDirectory.resolve("server.csv");
        ServerCsvLogger logger = new ServerCsvLogger(output, 256);
        assertTrue(logger.writerIsVirtual());
        int count = 100;
        CountDownLatch ready = new CountDownLatch(count);
        CountDownLatch release = new CountDownLatch(1);
        Thread[] producers = new Thread[count];
        for (int index = 0; index < count; index++) {
            int number = index;
            producers[index] =
                    Thread.ofVirtual()
                            .start(
                                    () -> {
                                        ready.countDown();
                                        try {
                                            release.await();
                                        } catch (InterruptedException exception) {
                                            Thread.currentThread().interrupt();
                                            return;
                                        }
                                        logger.record(record(number));
                                    });
        }
        ready.await();
        release.countDown();
        for (Thread producer : producers) {
            producer.join();
        }
        logger.close();

        assertEquals(count, logger.acceptedCount());
        assertEquals(0, logger.droppedCount());
        assertEquals(count + 1, Files.readAllLines(output).size());
        assertTrue(Files.readString(output).startsWith("request_id,client_id"));
    }

    @Test
    void csvValuesAreEscaped() throws Exception {
        Path output = temporaryDirectory.resolve("escaped.csv");
        try (ServerCsvLogger logger = new ServerCsvLogger(output, 8)) {
            ServerEvidenceRecord record = record(1);
            logger.record(
                    new ServerEvidenceRecord(
                            record.requestId(),
                            record.clientId(),
                            record.sessionId(),
                            record.requestType(),
                            record.receivedAt(),
                            record.queuedAt(),
                            record.startedAt(),
                            record.completedAt(),
                            0,
                            0,
                            1,
                            "value,\"quoted\"\nnext",
                            "worker",
                            true));
        }
        assertTrue(Files.readString(output).contains("\"value,\"\"quoted\"\"\nnext\""));
    }

    private static ServerEvidenceRecord record(int number) {
        Instant now = Instant.now();
        return new ServerEvidenceRecord(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                RequestType.STEP_GAME,
                now,
                now,
                now,
                now,
                number,
                0,
                number,
                "SUCCESS",
                "worker-" + number,
                true);
    }
}
