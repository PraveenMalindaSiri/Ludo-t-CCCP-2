package server.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import protocol.RequestType;
import protocol.SessionStatus;
import server.ServerConfig;

class LateJoinSnapshotIT {

    @Test
    void lateJoinReceivesCurrentCompleteSnapshotWithoutReplay() throws Exception {
        try (GameServer server = new GameServer(ServerConfig.defaultsForPort(0))) {
            server.start();
            try (PushTestClient first = new PushTestClient(server.port());
                    PushTestClient late = new PushTestClient(server.port())) {
                var created =
                        first.exchange(
                                first.request(
                                        RequestType.CREATE_SESSION,
                                        null,
                                        Map.of("name", "Late-join", "seed", "99")));
                UUID sessionId = created.sessionId();
                first.exchange(
                        first.request(
                                RequestType.SET_SPEED,
                                sessionId,
                                Map.of("turnDelayMillis", "60000")));
                first.exchange(first.request(RequestType.START_GAME, sessionId, Map.of()));
                first.exchange(first.request(RequestType.PAUSE_GAME, sessionId, Map.of()));
                var stepped =
                        first.exchange(first.request(RequestType.STEP_GAME, sessionId, Map.of()));

                var joined =
                        late.exchange(late.request(RequestType.JOIN_SESSION, sessionId, Map.of()));

                assertTrue(joined.success());
                assertEquals(stepped.snapshot(), joined.snapshot());
                assertEquals(SessionStatus.PAUSED, joined.snapshot().status());
                assertEquals(
                        16,
                        joined.snapshot().players().stream()
                                .mapToInt(player -> player.pieces().size())
                                .sum());
            }
        }
    }
}
