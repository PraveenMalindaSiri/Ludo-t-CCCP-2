package client;

import client.network.ServerConnection;
import client.ui.AppTheme;
import client.ui.MainFrame;
import javax.swing.SwingUtilities;

/** Starts the standalone Swing thin-client process on the event-dispatch thread. */
public final class ClientMain {

    private ClientMain() {}

    public static void main(String[] args) {
        AppTheme.install();
        SwingUtilities.invokeLater(
                () -> {
                    ClientConfig config = ClientConfig.load();
                    ClientController controller =
                            new ClientController(
                                    new ServerConnection(config), config.eventHistoryLimit());
                    MainFrame frame = new MainFrame(controller, config.host(), config.port());
                    controller.attachView(frame);
                    frame.setVisible(true);
                });
    }
}
