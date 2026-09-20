package client.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import protocol.GameSnapshotDto;

/** Detailed live-game dashboard built entirely from transport-safe protocol DTOs. */
public final class GamePanel extends JPanel {

    private final BoardPanel board = new BoardPanel();
    private final ControlPanel controls;
    private final EventLogPanel eventLog = new EventLogPanel();
    private final JLabel sessionId =
            AppTheme.label("Session -", AppTheme.SMALL, AppTheme.TEXT_MUTED);
    private final JLabel action =
            AppTheme.label("Waiting for a snapshot", AppTheme.BODY, AppTheme.TEXT);
    private final JLabel placements =
            AppTheme.label("Placements: -", AppTheme.BODY, AppTheme.TEXT_MUTED);
    private final JLabel mystery =
            AppTheme.label("Mystery cell: inactive", AppTheme.SMALL, AppTheme.TEXT_MUTED);
    private final JLabel boardLegend =
            AppTheme.label(
                    "X Start   \u25cf Approach   \u03b1/\u03b2/\u03b3 Teleport   ? Mystery",
                    AppTheme.SMALL,
                    AppTheme.TEXT_MUTED);
    private final PillLabel status = new PillLabel("NO SESSION");
    private final MetricCard currentPlayer = new MetricCard("Current player", AppTheme.ACCENT);
    private final MetricCard round = new MetricCard("Round", AppTheme.PURPLE);
    private final MetricCard turn = new MetricCard("Turn", AppTheme.SUCCESS);
    private final MetricCard version = new MetricCard("Version", AppTheme.TEXT);
    private final MetricCard dice = new MetricCard("Last dice", AppTheme.WARNING);
    private final MetricCard queue = new MetricCard("Queue depth", AppTheme.ACCENT);
    private final JPanel players = new JPanel(new GridLayout(0, 1, 8, 8));
    private final JTextArea pieceDetails = new JTextArea(14, 30);

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
        super(new BorderLayout(16, 16));
        setBackground(AppTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        controls =
                new ControlPanel(
                        startAction,
                        pauseAction,
                        resumeAction,
                        stepAction,
                        stopAction,
                        speedAction,
                        leaveAction);

        add(createHeader(), BorderLayout.NORTH);

        CardPanel boardCard = new CardPanel(AppTheme.SURFACE, 20);
        boardCard.setLayout(new BorderLayout());
        boardCard.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        boardCard.add(board, BorderLayout.CENTER);

        JPanel content = new JPanel(new BorderLayout(16, 0));
        content.setOpaque(false);
        content.add(boardCard, BorderLayout.CENTER);
        content.add(createDashboard(), BorderLayout.EAST);
        add(content, BorderLayout.CENTER);

        CardPanel controlCard = new CardPanel(AppTheme.BACKGROUND_SOFT, 16);
        controlCard.setLayout(new BorderLayout());
        controlCard.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        controlCard.add(controls);
        add(controlCard, BorderLayout.SOUTH);
    }

    private JPanel createHeader() {
        CardPanel header = new CardPanel(AppTheme.BACKGROUND_SOFT, 18);
        header.setLayout(new BorderLayout(16, 0));
        header.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        JPanel title = new JPanel();
        title.setOpaque(false);
        title.setLayout(new BoxLayout(title, BoxLayout.Y_AXIS));
        title.add(AppTheme.label("Live game", AppTheme.TITLE, AppTheme.TEXT));
        title.add(Box.createVerticalStrut(3));
        title.add(sessionId);

        JPanel activity = new JPanel();
        activity.setOpaque(false);
        activity.setLayout(new BoxLayout(activity, BoxLayout.Y_AXIS));
        JLabel caption =
                AppTheme.label("LATEST SERVER ACTION", AppTheme.SMALL, AppTheme.TEXT_MUTED);
        caption.setAlignmentX(RIGHT_ALIGNMENT);
        action.setAlignmentX(RIGHT_ALIGNMENT);
        activity.add(caption);
        activity.add(Box.createVerticalStrut(3));
        activity.add(action);

        header.add(title, BorderLayout.WEST);
        header.add(activity, BorderLayout.CENTER);
        header.add(status, BorderLayout.EAST);
        return header;
    }

    private JPanel createDashboard() {
        JPanel side = new JPanel(new BorderLayout(0, 12));
        side.setOpaque(false);
        side.setPreferredSize(new Dimension(390, 640));

        JPanel metrics = new JPanel(new GridLayout(3, 2, 8, 8));
        metrics.setOpaque(false);
        metrics.add(currentPlayer);
        metrics.add(round);
        metrics.add(turn);
        metrics.add(version);
        metrics.add(dice);
        metrics.add(queue);
        side.add(metrics, BorderLayout.NORTH);

        players.setOpaque(false);
        JPanel overview = new JPanel(new BorderLayout(8, 8));
        overview.setBackground(AppTheme.SURFACE);
        JPanel overviewHeader = new JPanel(new GridLayout(3, 1, 0, 4));
        overviewHeader.setOpaque(false);
        overviewHeader.setBorder(BorderFactory.createEmptyBorder(10, 10, 4, 10));
        overviewHeader.add(placements);
        overviewHeader.add(mystery);
        overviewHeader.add(boardLegend);
        overview.add(overviewHeader, BorderLayout.NORTH);
        JScrollPane playersScroll = new JScrollPane(players);
        playersScroll.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));
        playersScroll.getViewport().setBackground(AppTheme.SURFACE);
        overview.add(playersScroll, BorderLayout.CENTER);

        pieceDetails.setEditable(false);
        pieceDetails.setLineWrap(false);
        pieceDetails.setFont(AppTheme.MONO);
        pieceDetails.setBackground(AppTheme.BACKGROUND_SOFT);
        pieceDetails.setForeground(AppTheme.TEXT);
        pieceDetails.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JScrollPane pieceScroll = new JScrollPane(pieceDetails);
        pieceScroll.setBorder(BorderFactory.createEmptyBorder());
        pieceScroll.getViewport().setBackground(AppTheme.BACKGROUND_SOFT);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(AppTheme.BODY_BOLD);
        tabs.setBackground(AppTheme.SURFACE);
        tabs.setForeground(AppTheme.TEXT);
        tabs.addTab("Players", overview);
        tabs.addTab("Piece details", pieceScroll);
        tabs.addTab("Activity", eventLog);
        side.add(tabs, BorderLayout.CENTER);
        return side;
    }

    public void render(GameSnapshotDto snapshot, List<String> events, String latestAction) {
        board.setSnapshot(snapshot);
        eventLog.setEvents(events);
        if (snapshot == null) {
            controls.applyStatus(null);
            return;
        }

        sessionId.setText("SESSION  " + snapshot.sessionId());
        status.setPill(snapshot.status().name(), AppTheme.statusColor(snapshot.status().name()));
        round.setValue(Integer.toString(snapshot.round()));
        turn.setValue(Long.toString(snapshot.turn()));
        version.setValue("v" + snapshot.version());
        dice.setValue(snapshot.lastDice() == null ? "-" : snapshot.lastDice().toString());
        queue.setValue(Integer.toString(snapshot.queueDepth()));
        String player = snapshot.currentPlayer().isBlank() ? "-" : snapshot.currentPlayer();
        currentPlayer.setValue(player);
        currentPlayer.setValueColor(AppTheme.playerColor(player));
        placements.setText(
                "Placements: "
                        + (snapshot.finalPlacements().isEmpty()
                                ? "Not decided"
                                : String.join("  •  ", snapshot.finalPlacements())));
        mystery.setText(
                snapshot.mystery().active()
                        ? "Mystery cell: position "
                                + snapshot.mystery().position()
                                + " • "
                                + snapshot.mystery().roundsRemaining()
                                + " rounds"
                        : "Mystery cell: inactive");

        String displayedAction =
                latestAction == null || latestAction.isBlank()
                        ? snapshot.lastAction()
                        : latestAction;
        String result = snapshot.lastResult().isBlank() ? displayedAction : snapshot.lastResult();
        action.setText(result == null || result.isBlank() ? "Waiting for next turn" : result);
        rebuildPlayers(snapshot);
        pieceDetails.setText(pieceSummary(snapshot));
        pieceDetails.setCaretPosition(0);
        controls.applyStatus(snapshot.status());
    }

    private void rebuildPlayers(GameSnapshotDto snapshot) {
        players.removeAll();
        snapshot.players()
                .forEach(
                        player -> {
                            Color color = AppTheme.playerColor(player.color());
                            boolean active =
                                    player.color().equalsIgnoreCase(snapshot.currentPlayer());
                            CardPanel card =
                                    new CardPanel(
                                            active
                                                    ? AppTheme.blend(
                                                            AppTheme.SURFACE_RAISED, color, 0.18f)
                                                    : AppTheme.BACKGROUND_SOFT,
                                            14);
                            card.setLayout(new BorderLayout(10, 0));
                            card.setBorder(
                                    BorderFactory.createCompoundBorder(
                                            BorderFactory.createMatteBorder(0, 5, 0, 0, color),
                                            BorderFactory.createEmptyBorder(9, 11, 9, 11)));
                            JLabel name =
                                    AppTheme.label(
                                            player.color() + (active ? "  •  ACTIVE" : ""),
                                            AppTheme.BODY_BOLD,
                                            color);
                            JLabel totals =
                                    AppTheme.label(
                                            "Base "
                                                    + player.baseCount()
                                                    + "   Board "
                                                    + player.boardCount()
                                                    + "   Home "
                                                    + player.homeCount(),
                                            AppTheme.SMALL,
                                            AppTheme.TEXT_MUTED);
                            card.add(name, BorderLayout.NORTH);
                            card.add(totals, BorderLayout.CENTER);
                            players.add(card);
                        });
        players.revalidate();
        players.repaint();
    }

    private String pieceSummary(GameSnapshotDto snapshot) {
        StringBuilder text = new StringBuilder();
        snapshot.players()
                .forEach(
                        player -> {
                            text.append(player.color().toUpperCase())
                                    .append("  |  base ")
                                    .append(player.baseCount())
                                    .append("  board ")
                                    .append(player.boardCount())
                                    .append("  home ")
                                    .append(player.homeCount())
                                    .append(System.lineSeparator());
                            player.pieces()
                                    .forEach(
                                            piece -> {
                                                text.append("  • ")
                                                        .append(piece.pieceId())
                                                        .append("  ")
                                                        .append(piece.area());
                                                if (piece.area()
                                                        == GameSnapshotDto.PieceArea
                                                                .STANDARD_PATH) {
                                                    text.append(" @ ")
                                                            .append(piece.standardPosition());
                                                } else if (piece.area()
                                                        == GameSnapshotDto.PieceArea
                                                                .HOME_STRAIGHT) {
                                                    text.append(" @ ")
                                                            .append(piece.homeStraightIndex());
                                                }
                                                text.append("  •  ").append(piece.stateLabel());
                                                if (piece.stateRoundsRemaining() > 0) {
                                                    text.append(" (")
                                                            .append(piece.stateRoundsRemaining())
                                                            .append(" rounds)");
                                                }
                                                if (piece.inBlock()) {
                                                    text.append("  •  block ")
                                                            .append(piece.blockId());
                                                }
                                                text.append(System.lineSeparator());
                                            });
                            text.append(System.lineSeparator());
                        });
        return text.toString();
    }
}
