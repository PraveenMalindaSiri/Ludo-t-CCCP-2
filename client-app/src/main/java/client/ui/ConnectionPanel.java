package client.ui;

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
import client.model.ClientViewState;

/** Host/port form for starting and ending one persistent connection. */
public final class ConnectionPanel extends JPanel {

    private final JTextField hostField;
    private final JTextField portField;
    private final JLabel statusLabel = new JLabel();
    private final JButton connectButton = new JButton("Connect");
    private final JButton disconnectButton = new JButton("Disconnect");

    public ConnectionPanel(
            String defaultHost,
            int defaultPort,
            BiConsumer<String, Integer> connectAction,
            Runnable disconnectAction) {
        super(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));
        hostField = new JTextField(defaultHost, 18);
        portField = new JTextField(Integer.toString(defaultPort), 8);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(7, 7, 7, 7);
        constraints.anchor = GridBagConstraints.WEST;
        constraints.gridx = 0;
        constraints.gridy = 0;
        add(new JLabel("Server host"), constraints);
        constraints.gridx = 1;
        add(hostField, constraints);
        constraints.gridx = 0;
        constraints.gridy = 1;
        add(new JLabel("Server port"), constraints);
        constraints.gridx = 1;
        add(portField, constraints);
        constraints.gridx = 0;
        constraints.gridy = 2;
        add(connectButton, constraints);
        constraints.gridx = 1;
        add(disconnectButton, constraints);
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.gridwidth = 2;
        add(statusLabel, constraints);

        connectButton.addActionListener(
                ignored -> {
                    try {
                        connectAction.accept(
                                hostField.getText().trim(),
                                Integer.parseInt(portField.getText().trim()));
                    } catch (NumberFormatException exception) {
                        JOptionPane.showMessageDialog(
                                this, "Port must be a number", "Invalid port", JOptionPane.ERROR_MESSAGE);
                    }
                });
        disconnectButton.addActionListener(ignored -> disconnectAction.run());
    }

    public void render(ClientViewState state) {
        statusLabel.setText(state.statusText());
        boolean disconnected =
                state.connectionStatus() == ClientViewState.ConnectionStatus.DISCONNECTED;
        boolean connected =
                state.connectionStatus() == ClientViewState.ConnectionStatus.CONNECTED;
        hostField.setEnabled(disconnected);
        portField.setEnabled(disconnected);
        connectButton.setEnabled(disconnected);
        disconnectButton.setEnabled(connected);
    }
}
