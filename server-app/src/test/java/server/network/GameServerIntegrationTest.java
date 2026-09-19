package server.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import protocol.BoundedLineReader;
import protocol.JsonLineCodec;
import protocol.RequestMessage;
import protocol.RequestType;
import protocol.ResponseMessage;
import protocol.SessionStatus;
import server.ServerConfig;

class GameServerIntegrationTest {

    private final JsonLineCodec codec = new JsonLineCodec();
    private GameServer server;

    @BeforeEach
    void startServer() throws Exception {
        server = new GameServer(ServerConfig.defaultsForPort(0));
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.close();
    }

    @Test
    void onePersistentConnectionHandlesConnectPingListAndDisconnect() throws Exception {
        try (RawClient client = new RawClient(server.port())) {
            assertTrue(client.exchange(client.request(RequestType.CONNECT)).success());

            for (int index = 0; index < 40; index++) {
                RequestMessage ping = client.request(RequestType.PING);
                ResponseMessage response = client.exchange(ping);
                assertEquals(ping.requestId(), response.requestId());
                assertEquals("PONG", response.message());
            }

            ResponseMessage lobby = client.exchange(client.request(RequestType.LIST_SESSIONS));
            assertTrue(lobby.sessions().isEmpty());
            ResponseMessage disconnected = client.exchange(client.request(RequestType.DISCONNECT));
            assertTrue(disconnected.success());
            assertNull(client.readLine());
        }
    }

    @Test
    void twoClientsRemainIndependent() throws Exception {
        try (RawClient first = new RawClient(server.port());
                RawClient second = new RawClient(server.port())) {
            assertTrue(first.exchange(first.request(RequestType.CONNECT)).success());
            assertTrue(second.exchange(second.request(RequestType.CONNECT)).success());
            assertEquals(2, server.connectionCount());
            assertEquals("PONG", first.exchange(first.request(RequestType.PING)).message());
            assertEquals("PONG", second.exchange(second.request(RequestType.PING)).message());
        }
    }

    @Test
    void twoClientsCreateJoinAndControlOneServerOwnedSession() throws Exception {
        try (RawClient creator = new RawClient(server.port());
                RawClient second = new RawClient(server.port())) {
            assertTrue(creator.exchange(creator.request(RequestType.CONNECT)).success());
            assertTrue(second.exchange(second.request(RequestType.CONNECT)).success());

            ResponseMessage created =
                    creator.exchange(
                            creator.request(
                                    RequestType.CREATE_SESSION,
                                    null,
                                    Map.of("name", "Game-1", "seed", "42")));
            UUID sessionId = created.sessionId();
            assertTrue(created.success());
            assertEquals(SessionStatus.CREATED, created.snapshot().status());

            ResponseMessage lobby = second.exchange(second.request(RequestType.LIST_SESSIONS));
            assertEquals(sessionId, lobby.sessions().getFirst().sessionId());
            assertTrue(
                    second.exchange(second.request(RequestType.JOIN_SESSION, sessionId, Map.of()))
                            .success());

            ResponseMessage started =
                    creator.exchange(creator.request(RequestType.START_GAME, sessionId, Map.of()));
            assertEquals(SessionStatus.RUNNING, started.snapshot().status());
            ResponseMessage paused =
                    creator.exchange(creator.request(RequestType.PAUSE_GAME, sessionId, Map.of()));
            assertEquals(SessionStatus.PAUSED, paused.snapshot().status());
            long turnBefore = paused.snapshot().turn();
            ResponseMessage stepped =
                    second.exchange(second.request(RequestType.STEP_GAME, sessionId, Map.of()));
            assertEquals(turnBefore + 1, stepped.snapshot().turn());
            assertEquals(SessionStatus.PAUSED, stepped.snapshot().status());

            assertTrue(
                    creator.exchange(
                                    creator.request(RequestType.LEAVE_SESSION, sessionId, Map.of()))
                            .success());
            ResponseMessage snapshot =
                    second.exchange(second.request(RequestType.GET_SNAPSHOT, sessionId, Map.of()));
            assertEquals(SessionStatus.PAUSED, snapshot.snapshot().status());
            assertEquals(1, snapshot.session().connectedClients());
        }
    }

    @Test
    void twoMessagesInOneWriteRemainSeparateNdjsonMessages() throws Exception {
        try (RawClient client = new RawClient(server.port())) {
            RequestMessage connect = client.request(RequestType.CONNECT);
            RequestMessage ping = client.request(RequestType.PING);
            client.write(codec.encodeLine(connect) + codec.encodeLine(ping));

            assertEquals(connect.requestId(), client.readResponse().requestId());
            assertEquals(ping.requestId(), client.readResponse().requestId());
        }
    }

    @Test
    void oneMessageSplitAcrossWritesIsReconstructed() throws Exception {
        try (RawClient client = new RawClient(server.port())) {
            String line = codec.encodeLine(client.request(RequestType.CONNECT));
            int middle = line.length() / 2;
            client.write(line.substring(0, middle));
            client.write(line.substring(middle));

            assertTrue(client.readResponse().success());
        }
    }

    @Test
    void malformedJsonClosesOnlyThatConnection() throws Exception {
        try (RawClient invalid = new RawClient(server.port());
                RawClient healthy = new RawClient(server.port())) {
            invalid.write("{not-json}\n");
            assertNull(invalid.readLine());

            assertTrue(healthy.exchange(healthy.request(RequestType.CONNECT)).success());
            assertEquals("PONG", healthy.exchange(healthy.request(RequestType.PING)).message());
        }
    }

    @Test
    void serverApplicationNetworkingThreadsAreVirtual() throws Exception {
        try (RawClient client = new RawClient(server.port())) {
            assertTrue(client.exchange(client.request(RequestType.CONNECT)).success());
            assertTrue(server.acceptThreadIsVirtual());
            assertTrue(server.allConnectionThreadsAreVirtual());
        }
    }

    private final class RawClient implements AutoCloseable {

        private final UUID clientId = UUID.randomUUID();
        private final Socket socket;
        private final BufferedWriter writer;
        private final BoundedLineReader reader;

        private RawClient(int port) throws Exception {
            socket = new Socket("127.0.0.1", port);
            socket.setSoTimeout(3_000);
            writer =
                    new BufferedWriter(
                            new OutputStreamWriter(
                                    socket.getOutputStream(), StandardCharsets.UTF_8));
            reader =
                    new BoundedLineReader(
                            new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8),
                            JsonLineCodec.DEFAULT_MAX_LINE_LENGTH);
        }

        private RequestMessage request(RequestType type) {
            return request(type, null, Map.of());
        }

        private RequestMessage request(
                RequestType type, UUID sessionId, Map<String, String> parameters) {
            return RequestMessage.create(
                    UUID.randomUUID(), clientId, type, sessionId, parameters, Instant.now());
        }

        private ResponseMessage exchange(RequestMessage request) throws Exception {
            write(codec.encodeLine(request));
            return readResponse();
        }

        private ResponseMessage readResponse() throws Exception {
            return codec.decode(readLine(), ResponseMessage.class);
        }

        private String readLine() throws Exception {
            return reader.readLine();
        }

        private void write(String value) throws Exception {
            writer.write(value);
            writer.flush();
        }

        @Override
        public void close() throws Exception {
            socket.close();
        }
    }
}
