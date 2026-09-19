package server.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import protocol.SessionStatus;
import server.application.port.GameRepository.GameResultRecord;

class JdbcGameRepositoryTest {

    @Test
    void mapsSessionStatusToStableSqlValue() {
        assertEquals("PAUSED", JdbcGameRepository.statusValue(SessionStatus.PAUSED));
        assertEquals("INTERRUPTED", JdbcGameRepository.statusValue(SessionStatus.INTERRUPTED));
    }

    @Test
    void winnerMustMatchFirstPlacement() {
        GameResultRecord result =
                new GameResultRecord(
                        UUID.randomUUID(), "BLUE", List.of("RED", "BLUE"), 9, Instant.now());

        assertThrows(IllegalArgumentException.class, () -> JdbcGameRepository.winnerValue(result));
    }
}
