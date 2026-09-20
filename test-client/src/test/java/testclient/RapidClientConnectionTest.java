package testclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import protocol.BoundedLineReader;
import protocol.JsonLineCodec;
import protocol.MessageKind;
import protocol.RequestMessage;
import protocol.RequestType;
import protocol.ResponseMessage;
import testclient.RapidClientConnection.PendingRequest;

class RapidClientConnectionTest {

    private final JsonLineCodec codec = new JsonLineCodec();

    @Test
    void sendsManyRequestsBeforeEarlierResponsesCompleteAndCorrelatesThem() throws Exception {
        int count = 25;
        CountDownLatch allReceived = new CountDownLatch(count);
        CountDownLatch releaseResponses = new CountDownLatch(1);
        try (ServerSocket listener = new ServerSocket(0)) {
            Thread server =
                    Thread.ofVirtual()
                            .start(
                                    () ->
                                            serveBurst(
                                                    listener,
                                                    count,
                                                    allReceived,
                                                    releaseResponses));
            try (RapidClientConnection connection =
                    new RapidClientConnection(
                            "127.0.0.1",
                            listener.getLocalPort(),
                            64,
                            JsonLineCodec.DEFAULT_MAX_LINE_LENGTH,
                            3000)) {
                connection.connect();
                List<PendingRequest> pending = new ArrayList<>();
                for (int index = 0; index < count; index++) {
                    pending.add(
                            connection.requestAsync(
                                    RequestType.GET_SNAPSHOT, UUID.randomUUID(), Map.of()));
                }
                assertTrue(allReceived.await(3, TimeUnit.SECONDS));
                assertEquals(count, connection.pendingCount());
                assertTrue(connection.maximumOutstanding() >= count);
                releaseResponses.countDown();
                for (PendingRequest request : pending) {
                    assertEquals(
                            request.request().requestId(),
                            request.future().get(3, TimeUnit.SECONDS).requestId());
                }
                assertTrue(connection.networkingThreadsAreVirtual());
            }
            server.join();
        }
    }

    @Test
    void reportsDuplicateResponseIds() throws Exception {
        CountDownLatch duplicateSeen = new CountDownLatch(1);
        try (ServerSocket listener = new ServerSocket(0)) {
            Thread server = Thread.ofVirtual().start(() -> serveDuplicate(listener));
            try (RapidClientConnection connection =
                    new RapidClientConnection(
                            "127.0.0.1",
                            listener.getLocalPort(),
                            8,
                            JsonLineCodec.DEFAULT_MAX_LINE_LENGTH,
                            3000)) {
                AtomicInteger duplicates = new AtomicInteger();
                connection.setDuplicateResponseListener(
                        ignored -> {
                            duplicates.incrementAndGet();
                            duplicateSeen.countDown();
                        });
                connection.connect();
                PendingRequest pending =
                        connection.requestAsync(
                                RequestType.GET_SNAPSHOT, UUID.randomUUID(), Map.of());
                pending.future().get(3, TimeUnit.SECONDS);
                assertTrue(duplicateSeen.await(3, TimeUnit.SECONDS));
                assertEquals(1, duplicates.get());
            }
            server.join();
        }
    }

    @Test
    void serverDisconnectCompletesOutstandingFutureExceptionally() throws Exception {
        try (ServerSocket listener = new ServerSocket(0)) {
            Thread server =
                    Thread.ofVirtual()
                            .start(
                                    () -> {
                                        try (Socket socket = listener.accept()) {
                                            BoundedLineReader reader = reader(socket);
                                            BufferedWriter writer = writer(socket);
                                            respond(writer, readRequest(reader));
                                            readRequest(reader);
                                        } catch (Exception failure) {
                                            throw new AssertionError(failure);
                                        }
                                    });
            try (RapidClientConnection connection =
                    new RapidClientConnection(
                            "127.0.0.1",
                            listener.getLocalPort(),
                            8,
                            JsonLineCodec.DEFAULT_MAX_LINE_LENGTH,
                            3000)) {
                connection.connect();
                PendingRequest pending =
                        connection.requestAsync(
                                RequestType.GET_SNAPSHOT, UUID.randomUUID(), Map.of());
                org.junit.jupiter.api.Assertions.assertThrows(
                        ExecutionException.class, () -> pending.future().get(3, TimeUnit.SECONDS));
                assertEquals(0, connection.pendingCount());
            }
            server.join();
        }
    }

    private void serveBurst(
            ServerSocket listener,
            int count,
            CountDownLatch allReceived,
            CountDownLatch releaseResponses) {
        try (Socket socket = listener.accept()) {
            BoundedLineReader reader = reader(socket);
            BufferedWriter writer = writer(socket);
            respond(writer, readRequest(reader));
            List<RequestMessage> requests = new ArrayList<>();
            for (int index = 0; index < count; index++) {
                requests.add(readRequest(reader));
                allReceived.countDown();
            }
            releaseResponses.await();
            for (RequestMessage request : requests) {
                respond(writer, request);
            }
        } catch (Exception failure) {
            throw new AssertionError(failure);
        }
    }

    private void serveDuplicate(ServerSocket listener) {
        try (Socket socket = listener.accept()) {
            BoundedLineReader reader = reader(socket);
            BufferedWriter writer = writer(socket);
            respond(writer, readRequest(reader));
            RequestMessage request = readRequest(reader);
            respond(writer, request);
            respond(writer, request);
        } catch (Exception failure) {
            throw new AssertionError(failure);
        }
    }

    private RequestMessage readRequest(BoundedLineReader reader) throws Exception {
        return codec.decode(reader.readLine(), RequestMessage.class);
    }

    private void respond(BufferedWriter writer, RequestMessage request) throws Exception {
        ResponseMessage response =
                new ResponseMessage(
                        1,
                        MessageKind.RESPONSE,
                        request.requestId(),
                        request.clientId(),
                        request.sessionId(),
                        true,
                        null,
                        "ok",
                        null,
                        List.of(),
                        null,
                        Instant.now());
        writer.write(codec.encodeLine(response));
        writer.flush();
    }

    private static BoundedLineReader reader(Socket socket) throws Exception {
        return new BoundedLineReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8),
                JsonLineCodec.DEFAULT_MAX_LINE_LENGTH);
    }

    private static BufferedWriter writer(Socket socket) throws Exception {
        return new BufferedWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
    }
}
