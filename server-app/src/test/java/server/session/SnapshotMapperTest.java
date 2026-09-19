package server.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import engine.GameEngine;
import factory.GameFactory;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import protocol.GameSnapshotDto;
import protocol.SessionStatus;
import server.session.SnapshotMapper.SessionMetadata;

class SnapshotMapperTest {

    @Test
    void mapsCompleteCoreAndSessionMetadataWithoutDomainObjects() {
        GameEngine engine = GameFactory.createGame(42L);
        engine.initializeGame();
        UUID sessionId = UUID.randomUUID();

        GameSnapshotDto dto =
                new SnapshotMapper()
                        .map(
                                engine.getSnapshot(),
                                new SessionMetadata(
                                        sessionId,
                                        7,
                                        SessionStatus.PAUSED,
                                        3,
                                        250,
                                        "Game paused",
                                        6,
                                        "Yellow moved Y1",
                                        2));

        assertEquals(sessionId, dto.sessionId());
        assertEquals(7, dto.version());
        assertEquals(250, dto.turnDelayMillis());
        assertEquals(6, dto.lastDice());
        assertEquals("Yellow moved Y1", dto.lastResult());
        assertEquals(2, dto.queueDepth());
        assertEquals(16, dto.players().stream().mapToInt(player -> player.pieces().size()).sum());
        assertTrue(
                dto.players().stream()
                        .flatMap(player -> player.pieces().stream())
                        .allMatch(piece -> piece.area() == GameSnapshotDto.PieceArea.BASE));
        assertThrows(UnsupportedOperationException.class, () -> dto.players().clear());
        assertThrows(
                UnsupportedOperationException.class,
                () -> dto.players().getFirst().pieces().clear());
    }
}
