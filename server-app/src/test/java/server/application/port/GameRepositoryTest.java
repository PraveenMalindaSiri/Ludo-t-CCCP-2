package server.application.port;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import protocol.SessionStatus;
import server.application.port.GameRepository.GameResultRecord;
import server.application.port.GameRepository.SessionRecord;

class GameRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-09-19T10:15:30Z");

    @Test
    void sessionRecordRejectsBlankName() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new SessionRecord(
                                UUID.randomUUID(),
                                " ",
                                1L,
                                SessionStatus.CREATED,
                                NOW,
                                null,
                                null,
                                NOW));
    }

    @Test
    void completedResultRequiresPlacements() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new GameResultRecord(UUID.randomUUID(), "RED", List.of(), 4, NOW));
    }
}
