package server.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import protocol.RequestType;
import protocol.SessionStatus;
import server.ServerConfig;

class TwoClientPushIT {

    @Test
    void pauseThroughFirstAndResumeThroughSecondPushesIdenticalVersions() throws Exception {
        try (GameServer server = new GameServer(ServerConfig.defaultsForPort(0))) {
            server.start();
            try (PushTestClient first = new PushTestClient(server.port());
                    PushTestClient second = new PushTestClient(server.port())) {
                var created =
                        first.exchange(
                                first.request(
                                        RequestType.CREATE_SESSION,
                                        null,
                                        Map.of("name", "Game-1", "seed", "42")));
                UUID sessionId = created.sessionId();
                assertTrue(
                        second.exchange(
                                        second.request(
                                                RequestType.JOIN_SESSION, sessionId, Map.of()))
                                .success());

                var started =
                        first.exchange(first.request(RequestType.START_GAME, sessionId, Map.of()));
                assertEquals(started.snapshot(), second.readSnapshotEvent(1).snapshot());

                var paused =
                        first.exchange(first.request(RequestType.PAUSE_GAME, sessionId, Map.of()));
                var pushedPause = second.readSnapshotEvent(paused.snapshot().version()).snapshot();
                assertEquals(SessionStatus.PAUSED, pushedPause.status());
                assertEquals(paused.snapshot(), pushedPause);

                var resumed =
                        second.exchange(
                                second.request(RequestType.RESUME_GAME, sessionId, Map.of()));
                var pushedResume = first.readSnapshotEvent(resumed.snapshot().version()).snapshot();
                assertEquals(SessionStatus.RUNNING, pushedResume.status());
                assertEquals(resumed.snapshot(), pushedResume);
            }
        }
    }
}
