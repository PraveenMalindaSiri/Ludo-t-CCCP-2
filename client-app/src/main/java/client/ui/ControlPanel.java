package client.ui;

import java.awt.FlowLayout;
import java.util.function.LongConsumer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import protocol.SessionStatus;

/** Session controls whose actions are delegated to the protocol-only client controller. */
public final class ControlPanel extends JPanel {

    private final JButton start = new JButton("Start");
    private final JButton pause = new JButton("Pause");
    private final JButton resume = new JButton("Resume");
    private final JButton step = new JButton("Step");
    private final JButton stop = new JButton("Stop");
    private final JButton leave = new JButton("Leave");
    private final JComboBox<String> speed =
            new JComboBox<>(new String[] {"250 ms", "500 ms", "1000 ms"});

    public ControlPanel() {
        this(() -> {}, () -> {}, () -> {}, () -> {}, () -> {}, ignored -> {}, () -> {});
    }

    public ControlPanel(
            Runnable startAction,
            Runnable pauseAction,
            Runnable resumeAction,
            Runnable stepAction,
            Runnable stopAction,
            LongConsumer speedAction,
            Runnable leaveAction) {
        super(new FlowLayout(FlowLayout.LEFT));
        start.addActionListener(ignored -> startAction.run());
        pause.addActionListener(ignored -> pauseAction.run());
        resume.addActionListener(ignored -> resumeAction.run());
        step.addActionListener(ignored -> stepAction.run());
        stop.addActionListener(ignored -> stopAction.run());
        leave.addActionListener(ignored -> leaveAction.run());
        speed.addActionListener(
                ignored -> {
                    Object selected = speed.getSelectedItem();
                    if (selected != null) {
                        String milliseconds = selected.toString().replace(" ms", "");
                        speedAction.accept(Long.parseLong(milliseconds));
                    }
                });
        add(start);
        add(pause);
        add(resume);
        add(step);
        add(stop);
        add(speed);
        add(leave);
        applyStatus(null);
    }

    public void applyStatus(SessionStatus status) {
        start.setEnabled(status == SessionStatus.CREATED);
        pause.setEnabled(status == SessionStatus.RUNNING);
        resume.setEnabled(status == SessionStatus.PAUSED);
        step.setEnabled(status == SessionStatus.PAUSED);
        stop.setEnabled(
                status == SessionStatus.CREATED
                        || status == SessionStatus.RUNNING
                        || status == SessionStatus.PAUSED);
        speed.setEnabled(
                status == SessionStatus.CREATED
                        || status == SessionStatus.RUNNING
                        || status == SessionStatus.PAUSED);
        leave.setEnabled(status != null);
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
