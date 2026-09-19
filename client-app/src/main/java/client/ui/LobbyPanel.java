package client.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import protocol.SessionSummaryDto;

/** Session lobby backed by server-confirmed create, list and join operations. */
public final class LobbyPanel extends JPanel {

    private final DefaultListModel<SessionSummaryDto> sessionsModel = new DefaultListModel<>();
    private final JList<SessionSummaryDto> sessionsList = new JList<>(sessionsModel);
    private final JLabel statusLabel = new JLabel("Lobby");

    public LobbyPanel(
            Runnable refreshAction,
            Runnable pingAction,
            Consumer<String> createAction,
            Consumer<UUID> joinAction,
            Runnable disconnectAction) {
        super(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        add(statusLabel, BorderLayout.NORTH);
        sessionsList.setCellRenderer(
                (list, value, index, selected, focused) -> {
                    JLabel label = new JLabel();
                    label.setOpaque(true);
                    label.setText(
                            value.name()
                                    + " - "
                                    + value.status()
                                    + " - clients: "
                                    + value.connectedClients());
                    label.setBackground(
                            selected ? list.getSelectionBackground() : list.getBackground());
                    label.setForeground(
                            selected ? list.getSelectionForeground() : list.getForeground());
                    return label;
                });
        add(new JScrollPane(sessionsList), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refresh = new JButton("Refresh sessions");
        JButton ping = new JButton("Ping server");
        JButton create = new JButton("Create session");
        JButton join = new JButton("Join selected");
        JButton disconnect = new JButton("Disconnect");
        refresh.addActionListener(ignored -> refreshAction.run());
        ping.addActionListener(ignored -> pingAction.run());
        create.addActionListener(
                ignored -> {
                    String name =
                            JOptionPane.showInputDialog(
                                    this,
                                    "Session name:",
                                    "Create session",
                                    JOptionPane.PLAIN_MESSAGE);
                    if (name != null && !name.isBlank()) {
                        createAction.accept(name);
                    }
                });
        join.addActionListener(
                ignored -> {
                    SessionSummaryDto selected = sessionsList.getSelectedValue();
                    if (selected != null) {
                        joinAction.accept(selected.sessionId());
                    }
                });
        disconnect.addActionListener(ignored -> disconnectAction.run());
        actions.add(refresh);
        actions.add(ping);
        actions.add(create);
        actions.add(join);
        actions.add(disconnect);
        add(actions, BorderLayout.SOUTH);
    }

    public void render(List<SessionSummaryDto> sessions, String lastAction) {
        sessionsModel.clear();
        sessions.forEach(sessionsModel::addElement);
        String defaultText = sessions.isEmpty() ? "Lobby - no sessions available" : "Lobby";
        statusLabel.setText(lastAction == null || lastAction.isBlank() ? defaultText : lastAction);
    }
}
