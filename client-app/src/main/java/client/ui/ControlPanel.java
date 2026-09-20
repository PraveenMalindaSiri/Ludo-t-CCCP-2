package client.ui;

import java.awt.Color;
import java.awt.FlowLayout;
import java.util.function.LongConsumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import protocol.SessionStatus;

/** Styled simulation controls whose actions remain delegated to the protocol client. */
public final class ControlPanel extends JPanel {

    private final JButton start = button("Start", AppTheme.SUCCESS, new Color(6, 31, 26));
    private final JButton pause = button("Pause", AppTheme.WARNING, new Color(42, 28, 4));
    private final JButton resume = button("Resume", AppTheme.SUCCESS, new Color(6, 31, 26));
    private final JButton step = button("Step once", AppTheme.ACCENT, new Color(5, 24, 38));
    private final JButton stop = button("Stop", AppTheme.DANGER, new Color(40, 8, 8));
    private final JButton leave = button("Leave session", AppTheme.SURFACE_RAISED, AppTheme.TEXT);
    private final JComboBox<String> speed =
            new JComboBox<>(new String[] {"250 ms", "500 ms", "1000 ms"});
    private final PillLabel status = new PillLabel("NO SESSION");

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
        super(new FlowLayout(FlowLayout.LEFT, 9, 9));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        start.setToolTipText("Start the automatic simulation");
        pause.setToolTipText("Pause after the current turn");
        resume.setToolTipText("Resume automatic turns");
        step.setToolTipText("Execute exactly one turn while paused");
        stop.setToolTipText("Stop this session permanently");
        leave.setToolTipText("Return to the session lobby");

        speed.setFont(AppTheme.BODY);
        speed.setBackground(AppTheme.SURFACE_RAISED);
        speed.setForeground(AppTheme.TEXT);
        speed.setBorder(BorderFactory.createEmptyBorder(7, 8, 7, 8));

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

        add(AppTheme.label("SIMULATION", AppTheme.SMALL, AppTheme.TEXT_MUTED));
        add(status);
        add(start);
        add(pause);
        add(resume);
        add(step);
        add(new JLabel("Speed"));
        add(speed);
        add(stop);
        add(leave);
        applyStatus(null);
    }

    private static JButton button(String text, Color background, Color foreground) {
        JButton button = new JButton(text);
        AppTheme.styleButton(button, background, foreground);
        return button;
    }

    public void applyStatus(SessionStatus sessionStatus) {
        start.setEnabled(sessionStatus == SessionStatus.CREATED);
        pause.setEnabled(sessionStatus == SessionStatus.RUNNING);
        resume.setEnabled(sessionStatus == SessionStatus.PAUSED);
        step.setEnabled(sessionStatus == SessionStatus.PAUSED);
        stop.setEnabled(
                sessionStatus == SessionStatus.CREATED
                        || sessionStatus == SessionStatus.RUNNING
                        || sessionStatus == SessionStatus.PAUSED);
        speed.setEnabled(
                sessionStatus == SessionStatus.CREATED
                        || sessionStatus == SessionStatus.RUNNING
                        || sessionStatus == SessionStatus.PAUSED);
        leave.setEnabled(sessionStatus != null);
        String value = sessionStatus == null ? "NO SESSION" : sessionStatus.name();
        status.setPill(value, AppTheme.statusColor(value));
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
