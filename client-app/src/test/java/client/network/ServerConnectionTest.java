package client.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import client.ClientConfig;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import protocol.BoundedLineReader;
import protocol.EventMessage;
import protocol.EventType;
import protocol.JsonLineCodec;
import protocol.MessageKind;
import protocol.RequestMessage;
import protocol.RequestType;
import protocol.ResponseMessage;

class ServerConnectionTest {

    private final JsonLineCodec codec = new JsonLineCodec();

    @Test
    void correlatesOutOfOrderResponsesAndRoutesEventsSeparately() throws Exception {
        CountDownLatch eventReceived = new CountDownLatch(1);
        AtomicReference<String> eventText = new AtomicReference<>();
        try (FakeServer server =
                new FakeServer(
                        connection -> {
                            RequestMessage connect = connection.readRequest();
                            connection.respond(connect, "CONNECTED");
                            RequestMessage first = connection.readRequest();
                            RequestMessage second = connection.readRequest();
                            connection.respond(second, second.requestType().name());
                            connection.write(
                                    new EventMessage(
                                            1,
                                            MessageKind.EVENT,
                                            UUID.randomUUID(),
                                            null,
                                            EventType.SESSION_UPDATED,
                                            0,
                                            "push-event",
                                            List.of(),
                                            null,
                                            Instant.now()));
                            connection.respond(first, first.requestType().name());
                        })) {
            ServerConnection connection = new ServerConnection(config(server.port()));
            connection.setEventListener(
                    event -> {
                        eventText.set(event.message());
                        eventReceived.countDown();
                    });
            connection.connect("127.0.0.1", server.port()).get(3, TimeUnit.SECONDS);

            var ping = connection.request(RequestType.PING, null, Map.of());
            var list = connection.request(RequestType.LIST_SESSIONS, null, Map.of());

            assertEquals("PING", ping.get(3, TimeUnit.SECONDS).message());
            assertEquals("LIST_SESSIONS", list.get(3, TimeUnit.SECONDS).message());
            assertTrue(eventReceived.await(3, TimeUnit.SECONDS));
            assertEquals("push-event", eventText.get());
            assertTrue(connection.networkingThreadsAreVirtual());
            connection.close();
            server.assertNoFailure();
        }
    }

    @Test
    void unknownResponseIdIsIgnoredWithoutBreakingTheConnection() throws Exception {
        try (FakeServer server =
                new FakeServer(
                        connection -> {
                            RequestMessage connect = connection.readRequest();
                            connection.respond(connect, "CONNECTED");
                            connection.write(
                                    response(UUID.randomUUID(), connect.clientId(), "unknown"));
                            RequestMessage ping = connection.readRequest();
                            connection.respond(ping, "PONG");
                        })) {
            ServerConnection connection = new ServerConnection(config(server.port()));
            connection.connect("127.0.0.1", server.port()).get(3, TimeUnit.SECONDS);

            assertEquals("PONG", connection.ping().get(3, TimeUnit.SECONDS).message());
            connection.close();
            server.assertNoFailure();
        }
    }

    private ClientConfig config(int port) {
        return new ClientConfig(
                "127.0.0.1", port, 64, JsonLineCodec.DEFAULT_MAX_LINE_LENGTH, 2_000, 2_000, 20);
    }

    private ResponseMessage response(UUID requestId, UUID clientId, String message) {
        return new ResponseMessage(
                1,
                MessageKind.RESPONSE,
                requestId,
                clientId,
                null,
                true,
                null,
                message,
                null,
                List.of(),
                null,
                Instant.now());
    }

    @FunctionalInterface
    private interface ConnectionHandler {
        void handle(FakeConnection connection) throws Exception;
    }

    private final class FakeServer implements AutoCloseable {

        private final ServerSocket serverSocket = new ServerSocket(0);
        private final AtomicReference<Throwable> failure = new AtomicReference<>();
        private final Thread thread;

        private FakeServer(ConnectionHandler handler) throws Exception {
            thread =
                    Thread.ofVirtual()
                            .start(
                                    () -> {
                                        try (Socket socket = serverSocket.accept()) {
                                            handler.handle(new FakeConnection(socket));
                                        } catch (Throwable exception) {
                                            if (!serverSocket.isClosed()) {
                                                failure.set(exception);
                                            }
                                        }
                                    });
        }

        private int port() {
            return serverSocket.getLocalPort();
        }

        private void assertNoFailure() throws Exception {
            thread.join(3_000);
            Throwable problem = failure.get();
            if (problem != null) {
                throw new AssertionError(problem);
            }
        }

        @Override
        public void close() throws Exception {
            serverSocket.close();
            thread.join(3_000);
        }
    }

    private final class FakeConnection {

        private final BoundedLineReader reader;
        private final BufferedWriter writer;

        private FakeConnection(Socket socket) throws Exception {
            socket.setSoTimeout(3_000);
            reader =
                    new BoundedLineReader(
                            new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8),
                            JsonLineCodec.DEFAULT_MAX_LINE_LENGTH);
            writer =
                    new BufferedWriter(
                            new OutputStreamWriter(
                                    socket.getOutputStream(), StandardCharsets.UTF_8));
        }

        private RequestMessage readRequest() throws Exception {
            return codec.decode(reader.readLine(), RequestMessage.class);
        }

        private void respond(RequestMessage request, String message) throws Exception {
            write(response(request.requestId(), request.clientId(), message));
        }

        private void write(Object message) throws Exception {
            writer.write(codec.encodeLine(message));
            writer.flush();
        }
    }
}
