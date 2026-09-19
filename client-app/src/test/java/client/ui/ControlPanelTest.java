package client.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import protocol.SessionStatus;

class ControlPanelTest {

    @Test
    void controlsReflectSessionStatus() throws Exception {
        AtomicReference<ControlPanel> reference = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> reference.set(new ControlPanel()));
        ControlPanel controls = reference.get();

        SwingUtilities.invokeAndWait(
                () -> {
                    controls.applyStatus(SessionStatus.CREATED);
                    assertTrue(controls.isStartEnabled());
                    assertFalse(controls.isPauseEnabled());

                    controls.applyStatus(SessionStatus.RUNNING);
                    assertTrue(controls.isPauseEnabled());
                    assertTrue(controls.isStopEnabled());

                    controls.applyStatus(SessionStatus.PAUSED);
                    assertTrue(controls.isResumeEnabled());
                    assertTrue(controls.isStepEnabled());

                    controls.applyStatus(SessionStatus.COMPLETED);
                    assertFalse(controls.isStartEnabled());
                    assertFalse(controls.isStopEnabled());
                });
    }
}
