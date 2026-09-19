package client.ui;

import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import protocol.SessionStatus;

/** Presentation-only control enablement; game commands are wired in a later packet. */
public final class ControlPanel extends JPanel {

    private final JButton start = new JButton("Start");
    private final JButton pause = new JButton("Pause");
    private final JButton resume = new JButton("Resume");
    private final JButton step = new JButton("Step");
    private final JButton stop = new JButton("Stop");
    private final JComboBox<String> speed =
            new JComboBox<>(new String[] {"250 ms", "500 ms", "1000 ms"});

    public ControlPanel() {
        super(new FlowLayout(FlowLayout.LEFT));
        add(start);
        add(pause);
        add(resume);
        add(step);
        add(stop);
        add(speed);
        applyStatus(null);
    }

    public void applyStatus(SessionStatus status) {
        start.setEnabled(status == SessionStatus.CREATED);
        pause.setEnabled(status == SessionStatus.RUNNING);
        resume.setEnabled(status == SessionStatus.PAUSED);
        step.setEnabled(status == SessionStatus.PAUSED);
        stop.setEnabled(status == SessionStatus.RUNNING || status == SessionStatus.PAUSED);
        speed.setEnabled(status == SessionStatus.RUNNING || status == SessionStatus.PAUSED);
    }

    public boolean isStartEnabled() {
        return start.isEnabled();
    }

    public boolean isPauseEnabled() {
        return pause.isEnabled();
    }

    public boolean isResumeEnabled() {
        return resume.isEnabled();
    }

    public boolean isStepEnabled() {
        return step.isEnabled();
    }

    public boolean isStopEnabled() {
        return stop.isEnabled();
    }
}
