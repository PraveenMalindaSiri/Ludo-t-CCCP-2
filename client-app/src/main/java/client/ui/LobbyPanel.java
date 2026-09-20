package client.ui;

import client.model.ClientViewState;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import protocol.SessionSummaryDto;

/** Session lobby with a dashboard header and detailed session cards. */
public final class LobbyPanel extends JPanel {

    private static final DateTimeFormatter CREATED_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM • HH:mm").withZone(ZoneId.systemDefault());

    private final DefaultListModel<SessionSummaryDto> sessionsModel = new DefaultListModel<>();
    private final JList<SessionSummaryDto> sessionsList = new JList<>(sessionsModel);
    private final JLabel activity =
            AppTheme.label("Lobby ready", AppTheme.BODY, AppTheme.TEXT_MUTED);
    private final JLabel clientId = AppTheme.label("Client -", AppTheme.SMALL, AppTheme.TEXT_MUTED);
    private final JLabel sessionCount =
            AppTheme.label("0 sessions", AppTheme.BODY_BOLD, AppTheme.TEXT);
    private final PillLabel online = new PillLabel("ONLINE");

    public LobbyPanel(
            Runnable refreshAction,
            Runnable pingAction,
            Consumer<String> createAction,
            Consumer<UUID> joinAction,
            Runnable disconnectAction) {
        super(new BorderLayout(18, 18));
        setBackground(AppTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        online.setPill("SERVER ONLINE", AppTheme.SUCCESS);
        add(createHeader(), BorderLayout.NORTH);

        sessionsList.setCellRenderer(new SessionRenderer());
        sessionsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sessionsList.setFixedCellHeight(92);
        sessionsList.setBackground(AppTheme.BACKGROUND_SOFT);
        sessionsList.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JScrollPane scroll = new JScrollPane(sessionsList);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(AppTheme.BACKGROUND_SOFT);

        CardPanel browser = new CardPanel();
        browser.setLayout(new BorderLayout(12, 12));
        browser.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        JPanel browserHeading = new JPanel(new BorderLayout());
        browserHeading.setOpaque(false);
        browserHeading.add(AppTheme.label("Available sessions", AppTheme.HEADING, AppTheme.TEXT));
        browserHeading.add(sessionCount, BorderLayout.EAST);
        browser.add(browserHeading, BorderLayout.NORTH);
        browser.add(scroll, BorderLayout.CENTER);
        add(browser, BorderLayout.CENTER);

        JButton refresh = button("Refresh", AppTheme.SURFACE_RAISED, AppTheme.TEXT);
        JButton ping = button("Ping server", AppTheme.SURFACE_RAISED, AppTheme.TEXT);
        JButton create = button("Create session", AppTheme.ACCENT, new Color(7, 19, 32));
        JButton join = button("Join selected", AppTheme.SUCCESS, new Color(7, 28, 24));
        JButton disconnect = button("Disconnect", AppTheme.DANGER, new Color(35, 8, 8));

        refresh.addActionListener(ignored -> refreshAction.run());
        ping.addActionListener(ignored -> pingAction.run());
        create.addActionListener(
                ignored -> {
                    String name =
                            JOptionPane.showInputDialog(
                                    this,
                                    "Give the new game session a clear name:",
                                    "Create session",
                                    JOptionPane.PLAIN_MESSAGE);
                    if (name != null && !name.isBlank()) createAction.accept(name);
                });
        join.addActionListener(
                ignored -> {
                    SessionSummaryDto selected = sessionsList.getSelectedValue();
                    if (selected != null) {
                        joinAction.accept(selected.sessionId());
                    } else {
                        JOptionPane.showMessageDialog(
                                this,
                                "Select a session card before joining.",
                                "No session selected",
                                JOptionPane.INFORMATION_MESSAGE);
                    }
                });
        disconnect.addActionListener(ignored -> disconnectAction.run());

        JPanel primaryActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        primaryActions.setOpaque(false);
        primaryActions.add(refresh);
        primaryActions.add(ping);
        primaryActions.add(create);
        primaryActions.add(join);
        primaryActions.add(disconnect);

        CardPanel footer = new CardPanel(AppTheme.BACKGROUND_SOFT, 16);
        footer.setLayout(new BorderLayout(12, 0));
        footer.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        footer.add(activity, BorderLayout.CENTER);
        footer.add(primaryActions, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setOpaque(false);
        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.add(AppTheme.label("Session lobby", AppTheme.TITLE, AppTheme.TEXT));
        titles.add(Box.createVerticalStrut(4));
        titles.add(
                AppTheme.label(
                        "Create a game or join another connected client.",
                        AppTheme.BODY,
                        AppTheme.TEXT_MUTED));

        JPanel identity = new JPanel();
        identity.setOpaque(false);
        identity.setLayout(new BoxLayout(identity, BoxLayout.Y_AXIS));
        online.setAlignmentX(RIGHT_ALIGNMENT);
        clientId.setAlignmentX(RIGHT_ALIGNMENT);
        identity.add(online);
        identity.add(Box.createVerticalStrut(7));
        identity.add(clientId);
        header.add(titles, BorderLayout.CENTER);
        header.add(identity, BorderLayout.EAST);
        return header;
    }

    private JButton button(String text, Color background, Color foreground) {
        JButton button = new JButton(text);
        AppTheme.styleButton(button, background, foreground);
        return button;
    }

    public void render(ClientViewState state) {
        sessionsModel.clear();
        state.sessions().forEach(sessionsModel::addElement);
        int count = state.sessions().size();
        sessionCount.setText(count + (count == 1 ? " session" : " sessions"));
        clientId.setText("Client  " + shorten(state.clientId()));
        String defaultText =
                count == 0
                        ? "No sessions are available yet — create the first one."
                        : "Select a session to join it.";
        activity.setText(
                state.lastAction() == null || state.lastAction().isBlank()
                        ? defaultText
                        : state.lastAction());
    }

    private String shorten(UUID id) {
        String value = id.toString();
        return value.substring(0, 8) + "…" + value.substring(value.length() - 4);
    }

    private static final class SessionRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(
                JList<?> list, Object value, int index, boolean selected, boolean focused) {
            SessionSummaryDto session = (SessionSummaryDto) value;
            JPanel card = new JPanel(new BorderLayout(14, 0));
            card.setOpaque(true);
            card.setBackground(selected ? AppTheme.SURFACE_RAISED : AppTheme.BACKGROUND_SOFT);
            card.setBorder(
                    BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(
                                    0,
                                    selected ? 4 : 1,
                                    1,
                                    1,
                                    selected ? AppTheme.ACCENT : AppTheme.BORDER),
                            BorderFactory.createEmptyBorder(12, 14, 12, 14)));

            JLabel marker = new JLabel("●");
            marker.setFont(AppTheme.TITLE);
            marker.setForeground(AppTheme.statusColor(session.status().name()));
            card.add(marker, BorderLayout.WEST);

            JPanel detail = new JPanel(new GridLayout(2, 1, 0, 3));
            detail.setOpaque(false);
            detail.add(AppTheme.label(session.name(), AppTheme.HEADING, AppTheme.TEXT));
            detail.add(
                    AppTheme.label(
                            session.sessionId().toString()
                                    + "  •  created "
                                    + CREATED_FORMAT.format(session.createdAt()),
                            AppTheme.SMALL,
                            AppTheme.TEXT_MUTED));
            card.add(detail, BorderLayout.CENTER);

            JPanel metrics = new JPanel(new GridLayout(2, 1, 0, 3));
            metrics.setOpaque(false);
            JLabel status =
                    AppTheme.label(
                            session.status().name(),
                            AppTheme.BODY_BOLD,
                            AppTheme.statusColor(session.status().name()));
            status.setHorizontalAlignment(JLabel.RIGHT);
            JLabel clients =
                    AppTheme.label(
                            session.connectedClients()
                                    + (session.connectedClients() == 1 ? " client" : " clients")
                                    + "  •  v"
                                    + session.version(),
                            AppTheme.SMALL,
                            AppTheme.TEXT_MUTED);
            clients.setHorizontalAlignment(JLabel.RIGHT);
            metrics.add(status);
            metrics.add(clients);
            metrics.setPreferredSize(new Dimension(130, 52));
            card.add(metrics, BorderLayout.EAST);
            return card;
        }
    }
}
