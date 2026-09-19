package client.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
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
    private final JLabel speed = new JLabel();
    private final JLabel dice = new JLabel();
    private final JLabel result = new JLabel();
    private final JLabel placements = new JLabel();
    private final JTextArea pieceDetails = new JTextArea(10, 28);

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
        metadata.add(speed);
        metadata.add(dice);
        metadata.add(result);
        metadata.add(placements);
        metadata.add(lastAction);
        side.add(metadata, BorderLayout.NORTH);
        pieceDetails.setEditable(false);
        pieceDetails.setLineWrap(true);
        pieceDetails.setWrapStyleWord(true);
        JPanel detailPanel = new JPanel(new BorderLayout());
        detailPanel.setBorder(BorderFactory.createTitledBorder("Players and pieces"));
        detailPanel.add(new JScrollPane(pieceDetails), BorderLayout.CENTER);
        JPanel activity = new JPanel(new GridLayout(2, 1, 4, 4));
        activity.add(detailPanel);
        activity.add(eventLog);
        side.add(activity, BorderLayout.CENTER);
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
        speed.setText("Turn delay: " + snapshot.turnDelayMillis() + " ms");
        dice.setText("Last dice: " + (snapshot.lastDice() == null ? "-" : snapshot.lastDice()));
        result.setText(
                "Last result: " + (snapshot.lastResult().isBlank() ? "-" : snapshot.lastResult()));
        placements.setText(
                "Placements: "
                        + (snapshot.finalPlacements().isEmpty()
                                ? "-"
                                : String.join(", ", snapshot.finalPlacements())));
        String displayedAction =
                action == null || action.isBlank() ? snapshot.lastAction() : action;
        lastAction.setText("Last action: " + displayedAction);
        pieceDetails.setText(pieceSummary(snapshot));
        pieceDetails.setCaretPosition(0);
        controls.applyStatus(snapshot.status());
    }

    private String pieceSummary(GameSnapshotDto snapshot) {
        StringBuilder text = new StringBuilder();
        snapshot.players()
                .forEach(
                        player -> {
                            text.append(player.color())
                                    .append(": base ")
                                    .append(player.baseCount())
                                    .append(", board ")
                                    .append(player.boardCount())
                                    .append(", home ")
                                    .append(player.homeCount())
                                    .append(System.lineSeparator());
                            player.pieces()
                                    .forEach(
                                            piece -> {
                                                text.append("  ")
                                                        .append(piece.pieceId())
                                                        .append(" - ")
                                                        .append(piece.area());
                                                if (piece.area()
                                                        == GameSnapshotDto.PieceArea
                                                                .STANDARD_PATH) {
                                                    text.append(' ')
                                                            .append(piece.standardPosition());
                                                } else if (piece.area()
                                                        == GameSnapshotDto.PieceArea
                                                                .HOME_STRAIGHT) {
                                                    text.append(' ')
                                                            .append(piece.homeStraightIndex());
                                                }
                                                text.append(", ").append(piece.stateLabel());
                                                if (piece.stateRoundsRemaining() > 0) {
                                                    text.append(" (")
                                                            .append(piece.stateRoundsRemaining())
                                                            .append(" rounds)");
                                                }
                                                if (piece.inBlock()) {
                                                    text.append(", block ").append(piece.blockId());
                                                }
                                                text.append(System.lineSeparator());
                                            });
                        });
        return text.toString();
    }
}
