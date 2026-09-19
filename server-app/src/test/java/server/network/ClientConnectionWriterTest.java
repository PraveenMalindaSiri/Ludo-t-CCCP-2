package server.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import protocol.BoundedLineReader;
import protocol.JsonLineCodec;
import protocol.MessageKind;
import protocol.ResponseMessage;
import server.ServerConfig;

class ClientConnectionWriterTest {

    @Test
    void concurrentProducersCannotInterleaveSocketLines() throws Exception {
        JsonLineCodec codec = new JsonLineCodec();
        UUID clientId = UUID.randomUUID();
        try (ServerSocket listener = new ServerSocket(0);
                Socket receivingSocket = new Socket("127.0.0.1", listener.getLocalPort());
                Socket serverSide = listener.accept()) {
            receivingSocket.setSoTimeout(3_000);
            ClientConnection connection =
                    new ClientConnection(
                            serverSide,
                            new ServerConfig(
                                    "127.0.0.1", 0, 128, JsonLineCodec.DEFAULT_MAX_LINE_LENGTH),
                            new RequestDispatcher(),
                            () -> {});
            connection.start();

            int count = 80;
            CountDownLatch ready = new CountDownLatch(count);
            CountDownLatch release = new CountDownLatch(1);
            AtomicBoolean allEnqueued = new AtomicBoolean(true);
            Thread[] producers = new Thread[count];
            Set<UUID> expectedIds = new HashSet<>();
            for (int index = 0; index < count; index++) {
                UUID requestId = UUID.randomUUID();
                expectedIds.add(requestId);
                ResponseMessage response =
                        new ResponseMessage(
                                1,
                                MessageKind.RESPONSE,
                                requestId,
                                clientId,
                                null,
                                true,
                                null,
                                "response-" + index,
                                null,
                                null,
                                null,
                                Instant.now());
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
                                            if (!connection.send(response)) {
                                                allEnqueued.set(false);
                                            }
                                        });
            }
            ready.await();
            release.countDown();
            for (Thread producer : producers) {
                producer.join();
            }
            assertTrue(allEnqueued.get());

            BoundedLineReader reader =
                    new BoundedLineReader(
                            new InputStreamReader(
                                    receivingSocket.getInputStream(), StandardCharsets.UTF_8),
                            JsonLineCodec.DEFAULT_MAX_LINE_LENGTH);
            Set<UUID> actualIds = new HashSet<>();
            for (int index = 0; index < count; index++) {
                ResponseMessage response = codec.decode(reader.readLine(), ResponseMessage.class);
                actualIds.add(response.requestId());
            }
            assertEquals(expectedIds, actualIds);
            connection.close();
        }
    }
}
