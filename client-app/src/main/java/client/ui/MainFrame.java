package client.ui;

import client.ClientController;
import client.model.ClientViewState;
import java.awt.CardLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/** Top-level three-screen Swing window. */
public final class MainFrame extends JFrame implements ClientController.View {

    private static final String CONNECTION = "connection";
    private static final String LOBBY = "lobby";
    private static final String GAME = "game";

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final ConnectionPanel connectionPanel;
    private final LobbyPanel lobbyPanel;
    private final GamePanel gamePanel;

    public MainFrame(ClientController controller, String defaultHost, int defaultPort) {
        super("LUDO-T | Concurrent Multiplayer Simulation");
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("MainFrame must be created on the EDT");
        }
        connectionPanel =
                new ConnectionPanel(
                        defaultHost, defaultPort, controller::connect, controller::disconnect);
        lobbyPanel =
                new LobbyPanel(
                        controller::refreshSessions,
                        controller::ping,
                        controller::createSession,
                        controller::joinSession,
                        controller::disconnect);
        gamePanel =
                new GamePanel(
                        controller::startGame,
                        controller::pauseGame,
                        controller::resumeGame,
                        controller::stepGame,
                        controller::stopGame,
                        controller::setSpeed,
                        controller::leaveSession);
        content.setBackground(AppTheme.BACKGROUND);
        content.add(connectionPanel, CONNECTION);
        content.add(lobbyPanel, LOBBY);
        content.add(gamePanel, GAME);
        setContentPane(content);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new java.awt.Dimension(1100, 760));
        setSize(1320, 880);
        setLocationRelativeTo(null);
        addWindowListener(
                new WindowAdapter() {
                    @Override
                    public void windowClosed(WindowEvent event) {
                        controller.close();
                    }
                });
    }

    @Override
    public void render(ClientViewState state) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("MainFrame rendering must run on the EDT");
        }
        connectionPanel.render(state);
        lobbyPanel.render(state);
        gamePanel.render(state.snapshot(), state.events(), state.lastAction());

        if (state.connectionStatus() != ClientViewState.ConnectionStatus.CONNECTED) {
            cards.show(content, CONNECTION);
        } else if (state.snapshot() == null) {
            cards.show(content, LOBBY);
        } else {
            cards.show(content, GAME);
        }
    }
}
