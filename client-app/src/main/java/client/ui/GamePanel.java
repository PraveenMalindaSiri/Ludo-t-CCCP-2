package client.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import protocol.GameSnapshotDto;

/** Complete game screen shell built entirely from protocol DTOs. */
public final class GamePanel extends JPanel {

    private final BoardPanel board = new BoardPanel();
    private final ControlPanel controls;
    private final EventLogPanel eventLog = new EventLogPanel();
    private final JLabel session = new JLabel();
    private final JLabel status = new JLabel();
    private final JLabel round = new JLabel();
    private final JLabel turn = new JLabel();
    private final JLabel version = new JLabel();
    private final JLabel currentPlayer = new JLabel();
    private final JLabel lastAction = new JLabel();

    public GamePanel() {
        this(() -> {}, () -> {}, () -> {}, () -> {}, () -> {}, ignored -> {}, () -> {});
    }

    public GamePanel(
            Runnable startAction,
            Runnable pauseAction,
            Runnable resumeAction,
            Runnable stepAction,
            Runnable stopAction,
            java.util.function.LongConsumer speedAction,
            Runnable leaveAction) {
        super(new BorderLayout(10, 10));
        controls =
                new ControlPanel(
                        startAction,
                        pauseAction,
                        resumeAction,
                        stepAction,
                        stopAction,
                        speedAction,
                        leaveAction);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(board, BorderLayout.CENTER);

        JPanel side = new JPanel(new BorderLayout(8, 8));
        side.setPreferredSize(new Dimension(320, 600));
        JPanel metadata = new JPanel(new GridLayout(0, 1, 3, 3));
        metadata.setBorder(BorderFactory.createTitledBorder("Session"));
        metadata.add(session);
        metadata.add(status);
        metadata.add(round);
        metadata.add(turn);
        metadata.add(version);
        metadata.add(currentPlayer);
        metadata.add(lastAction);
        side.add(metadata, BorderLayout.NORTH);
        side.add(eventLog, BorderLayout.CENTER);
        add(side, BorderLayout.EAST);
        add(controls, BorderLayout.SOUTH);
    }

    public void render(GameSnapshotDto snapshot, List<String> events, String action) {
        board.setSnapshot(snapshot);
        eventLog.setEvents(events);
        if (snapshot == null) {
            controls.applyStatus(null);
            return;
        }
        session.setText("ID: " + snapshot.sessionId());
        status.setText("Status: " + snapshot.status());
        round.setText("Round: " + snapshot.round());
        turn.setText("Turn: " + snapshot.turn());
        version.setText("Version: " + snapshot.version());
        String player = snapshot.currentPlayer().isBlank() ? "-" : snapshot.currentPlayer();
        currentPlayer.setText("Current player: " + player);
        String displayedAction =
                action == null || action.isBlank() ? snapshot.lastAction() : action;
        lastAction.setText("Last action: " + displayedAction);
        controls.applyStatus(snapshot.status());
    }
}
