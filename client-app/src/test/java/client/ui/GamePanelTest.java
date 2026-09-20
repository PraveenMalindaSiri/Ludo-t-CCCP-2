package client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import protocol.GameSnapshotDto;
import protocol.SessionStatus;

class GamePanelTest {

    @Test
    void copiesTheCompleteRenderedSessionId() throws Exception {
        UUID sessionId = UUID.fromString("7e47cf31-75fa-4c58-ab4e-2a17b55fe07e");
        AtomicReference<String> clipboard = new AtomicReference<>();

        SwingUtilities.invokeAndWait(
                () -> {
                    GamePanel panel =
                            new GamePanel(
                                    () -> {},
                                    () -> {},
                                    () -> {},
                                    () -> {},
                                    () -> {},
                                    ignored -> {},
                                    () -> {},
                                    clipboard::set);

                    assertFalse(panel.isSessionIdCopyEnabled());
                    panel.render(snapshot(sessionId), List.of(), "Ready");
                    assertTrue(panel.isSessionIdCopyEnabled());

                    panel.copyRenderedSessionId();
                    assertEquals(sessionId.toString(), clipboard.get());

                    panel.render(null, List.of(), "");
                    assertFalse(panel.isSessionIdCopyEnabled());
                });
    }

    private GameSnapshotDto snapshot(UUID sessionId) {
        return new GameSnapshotDto(
                sessionId,
                1,
                1,
                SessionStatus.PAUSED,
                List.of(),
                new GameSnapshotDto.MysteryDto(false, -1, 0),
                "",
                0,
                "Created");
    }
}
