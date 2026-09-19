package client.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import protocol.SessionSummaryDto;

/** Session lobby shell; creation and joining are intentionally deferred to Work 03. */
public final class LobbyPanel extends JPanel {

    private final DefaultListModel<String> sessionsModel = new DefaultListModel<>();
    private final JLabel statusLabel = new JLabel("Lobby");

    public LobbyPanel(Runnable refreshAction, Runnable pingAction, Runnable disconnectAction) {
        super(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        add(statusLabel, BorderLayout.NORTH);
        add(new JScrollPane(new JList<>(sessionsModel)), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refresh = new JButton("Refresh sessions");
        JButton ping = new JButton("Ping server");
        JButton create = new JButton("Create session");
        JButton join = new JButton("Join selected");
        JButton disconnect = new JButton("Disconnect");
        create.setEnabled(false);
        join.setEnabled(false);
        create.setToolTipText("Session creation is added in Work 03");
        join.setToolTipText("Session joining is added in Work 03");
        refresh.addActionListener(ignored -> refreshAction.run());
        ping.addActionListener(ignored -> pingAction.run());
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
        sessions.forEach(
                session ->
                        sessionsModel.addElement(
                                session.name()
                                        + " - "
                                        + session.status()
                                        + " - clients: "
                                        + session.connectedClients()));
        if (sessions.isEmpty()) {
            sessionsModel.addElement("No sessions available");
        }
        statusLabel.setText(lastAction == null || lastAction.isBlank() ? "Lobby" : lastAction);
    }
}
