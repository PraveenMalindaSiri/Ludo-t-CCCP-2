package client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import protocol.GameSnapshotDto;
import protocol.SessionStatus;

class StaleSnapshotTest {

    @Test
    void duplicateAndOlderVersionsCannotReplaceDisplayedSessionState() {
        UUID sessionId = UUID.randomUUID();
        ClientViewState state =
                ClientViewState.initial(UUID.randomUUID())
                        .withSnapshotIfNewer(snapshot(sessionId, 5, "new"), "new");

        state = state.withSnapshotIfNewer(snapshot(sessionId, 5, "duplicate"), "duplicate");
        state = state.withSnapshotIfNewer(snapshot(sessionId, 4, "old"), "old");

        assertEquals(5, state.snapshot().version());
        assertEquals("new", state.snapshot().lastAction());
        assertEquals("new", state.lastAction());
    }

    @Test
    void aDifferentSessionCanStartItsOwnVersionSequenceAtZero() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        ClientViewState state =
                ClientViewState.initial(UUID.randomUUID())
                        .withSnapshotIfNewer(snapshot(first, 9, "first"), "first")
                        .leaveSession("left");
        assertNull(state.snapshot());

        state = state.withSnapshotIfNewer(snapshot(second, 0, "second"), "second");

        assertEquals(second, state.snapshot().sessionId());
        assertEquals(0, state.snapshot().version());
    }

    private static GameSnapshotDto snapshot(UUID sessionId, long version, String action) {
        return new GameSnapshotDto(
                sessionId,
                version,
                0,
                SessionStatus.CREATED,
                List.of(),
                new GameSnapshotDto.MysteryDto(false, -1, 0),
                "",
                0,
                action);
    }
}
