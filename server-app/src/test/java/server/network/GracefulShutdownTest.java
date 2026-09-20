package server.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.ConnectException;
import java.net.Socket;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import protocol.EventType;
import protocol.RequestType;
import server.ServerConfig;

class GracefulShutdownTest {

    @Test
    void shutdownNotifiesClientsTerminatesWorkersAndRejectsNewConnections() throws Exception {
        GameServer server = new GameServer(ServerConfig.defaultsForPort(0));
        server.start();
        int port = server.port();
        try (PushTestClient client = new PushTestClient(port)) {
            UUID sessionId =
                    client.exchange(
                                    client.request(
                                            RequestType.CREATE_SESSION,
                                            null,
                                            Map.of("name", "Shutdown", "seed", "42")))
                            .sessionId();
            assertTrue(
                    client.exchange(client.request(RequestType.START_GAME, sessionId, Map.of()))
                            .success());

            server.close();

            assertEquals(
                    EventType.SERVER_SHUTTING_DOWN,
                    client.readEvent(EventType.SERVER_SHUTTING_DOWN).eventType());
            server.awaitTermination();
            assertThrows(ConnectException.class, () -> new Socket("127.0.0.1", port));
        } finally {
            server.close();
        }
    }
}
