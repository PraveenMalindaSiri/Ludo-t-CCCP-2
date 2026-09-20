package testclient;

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
import testclient.ClientCsvLogger.ClientEvidenceRecord;

class ClientCsvLoggerTest {

    @TempDir Path temporaryDirectory;

    @Test
    void writesOneHeaderAndDrainsConcurrentProducers() throws Exception {
        Path output = temporaryDirectory.resolve("client.csv");
        ClientCsvLogger logger = new ClientCsvLogger(output, 256);
        assertTrue(logger.writerIsVirtual());
        int count = 80;
        CountDownLatch done = new CountDownLatch(count);
        for (int index = 0; index < count; index++) {
            Thread.ofVirtual()
                    .start(
                            () -> {
                                Instant now = Instant.now();
                                logger.record(
                                        new ClientEvidenceRecord(
                                                UUID.randomUUID(),
                                                UUID.randomUUID(),
                                                UUID.randomUUID(),
                                                RequestType.STEP_GAME,
                                                now,
                                                now,
                                                0,
                                                "SUCCESS"));
                                done.countDown();
                            });
        }
        done.await();
        logger.close();

        assertEquals(count, logger.acceptedCount());
        assertEquals(0, logger.droppedCount());
        assertEquals(count + 1, Files.readAllLines(output).size());
    }
}
