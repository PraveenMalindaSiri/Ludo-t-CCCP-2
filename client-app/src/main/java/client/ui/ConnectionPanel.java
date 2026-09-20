package client.ui;

import client.model.ClientViewState;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.function.BiConsumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

/** Polished host/port screen for starting one persistent server connection. */
public final class ConnectionPanel extends JPanel {

    private final JTextField hostField;
    private final JTextField portField;
    private final JLabel statusText = AppTheme.label("Not connected", AppTheme.BODY, AppTheme.TEXT);
    private final JLabel clientId = AppTheme.label("Client -", AppTheme.SMALL, AppTheme.TEXT_MUTED);
    private final PillLabel connectionBadge = new PillLabel("OFFLINE");
    private final JButton connectButton = new JButton("Connect to server");
    private final JButton disconnectButton = new JButton("Cancel connection");

    public ConnectionPanel(
            String defaultHost,
            int defaultPort,
            BiConsumer<String, Integer> connectAction,
            Runnable disconnectAction) {
        super(new GridBagLayout());
        setBackground(AppTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));

        hostField = new JTextField(defaultHost, 20);
        portField = new JTextField(Integer.toString(defaultPort), 9);
        AppTheme.styleField(hostField);
        AppTheme.styleField(portField);
        AppTheme.styleButton(connectButton, AppTheme.ACCENT, new Color(7, 19, 32));
        AppTheme.styleButton(disconnectButton, AppTheme.SURFACE_RAISED, AppTheme.TEXT_MUTED);
        connectButton.setPreferredSize(new Dimension(210, 44));

        JPanel card = createConnectionCard();
        card.setPreferredSize(new Dimension(560, 500));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.insets = new Insets(30, 30, 30, 30);
        add(card, constraints);

        connectButton.addActionListener(
                ignored -> {
                    try {
                        connectAction.accept(
                                hostField.getText().trim(),
                                Integer.parseInt(portField.getText().trim()));
                    } catch (NumberFormatException exception) {
                        JOptionPane.showMessageDialog(
                                this,
                                "Enter a numeric port between 1 and 65535.",
                                "Invalid server port",
                                JOptionPane.ERROR_MESSAGE);
                    }
                });
        disconnectButton.addActionListener(ignored -> disconnectAction.run());
    }

    private JPanel createConnectionCard() {
        CardPanel card = new CardPanel();
        card.setLayout(new GridBagLayout());
        card.setBorder(BorderFactory.createEmptyBorder(42, 42, 42, 42));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 2;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 0, 6, 0);
        JLabel brand = AppTheme.label("LUDO-T", AppTheme.DISPLAY.deriveFont(44f), AppTheme.TEXT);
        brand.setHorizontalAlignment(JLabel.CENTER);
        card.add(brand, constraints);

        constraints.gridy++;
        constraints.insets = new Insets(0, 0, 32, 0);
        JLabel section = AppTheme.label("SERVER CONNECTION", AppTheme.SMALL, AppTheme.ACCENT);
        section.setHorizontalAlignment(JLabel.CENTER);
        card.add(section, constraints);

        constraints.gridy++;
        constraints.gridwidth = 1;
        constraints.insets = new Insets(0, 0, 7, 10);
        card.add(AppTheme.label("SERVER HOST", AppTheme.SMALL, AppTheme.TEXT_MUTED), constraints);
        constraints.gridx = 1;
        card.add(AppTheme.label("PORT", AppTheme.SMALL, AppTheme.TEXT_MUTED), constraints);

        constraints.gridy++;
        constraints.gridx = 0;
        constraints.weightx = 1;
        constraints.insets = new Insets(0, 0, 22, 10);
        card.add(hostField, constraints);
        constraints.gridx = 1;
        constraints.weightx = 0;
        card.add(portField, constraints);

        constraints.gridy++;
        constraints.gridx = 0;
        constraints.gridwidth = 2;
        constraints.insets = new Insets(0, 0, 10, 0);
        card.add(connectButton, constraints);

        constraints.gridy++;
        card.add(disconnectButton, constraints);

        JPanel status = new JPanel(new BorderLayout(10, 3));
        status.setOpaque(false);
        status.add(connectionBadge, BorderLayout.WEST);
        status.add(statusText, BorderLayout.CENTER);
        status.add(clientId, BorderLayout.SOUTH);
        constraints.gridy++;
        constraints.weighty = 1;
        constraints.anchor = GridBagConstraints.SOUTH;
        constraints.insets = new Insets(35, 0, 0, 0);
        card.add(status, constraints);
        return card;
    }

    public void render(ClientViewState state) {
        statusText.setText(state.statusText());
        clientId.setText("Client ID  " + state.clientId());
        boolean disconnected =
                state.connectionStatus() == ClientViewState.ConnectionStatus.DISCONNECTED;
        boolean connected = state.connectionStatus() == ClientViewState.ConnectionStatus.CONNECTED;
        hostField.setEnabled(disconnected);
        portField.setEnabled(disconnected);
        connectButton.setEnabled(disconnected);
        disconnectButton.setEnabled(!disconnected && !connected);

        switch (state.connectionStatus()) {
            case CONNECTED -> connectionBadge.setPill("ONLINE", AppTheme.SUCCESS);
            case CONNECTING -> connectionBadge.setPill("CONNECTING", AppTheme.WARNING);
            case DISCONNECTING -> connectionBadge.setPill("CLOSING", AppTheme.WARNING);
            case DISCONNECTED -> connectionBadge.setPill("OFFLINE", AppTheme.DANGER);
        }
    }
}
