package client;

import client.network.ServerConnection;
import client.ui.MainFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Starts the standalone Swing thin-client process on the event-dispatch thread. */
public final class ClientMain {

    private ClientMain() {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(
                () -> {
                    UIManager.put("swing.boldMetal", Boolean.FALSE);
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
