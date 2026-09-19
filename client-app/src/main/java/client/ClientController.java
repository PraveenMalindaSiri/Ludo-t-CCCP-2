package client;

import client.model.ClientViewState;
import client.model.ClientViewState.ConnectionStatus;
import client.network.ClientTransport;
import java.util.List;
import java.util.Objects;
import javax.swing.SwingUtilities;
import protocol.EventMessage;
import protocol.GameSnapshotDto;
import protocol.ResponseMessage;

/** Coordinates asynchronous transport results and immutable Swing presentation state. */
public final class ClientController implements AutoCloseable {

    public interface View {
        void render(ClientViewState state);
    }

    private final ClientTransport transport;
    private final int eventHistoryLimit;
    private ClientViewState state;
    private View view = ignored -> {};

    public ClientController(ClientTransport transport, int eventHistoryLimit) {
        this.transport = Objects.requireNonNull(transport, "transport");
        if (eventHistoryLimit <= 0) {
            throw new IllegalArgumentException("eventHistoryLimit must be positive");
        }
        this.eventHistoryLimit = eventHistoryLimit;
        this.state = ClientViewState.initial(transport.clientId());
        transport.setEventListener(event -> onEdt(() -> acceptEvent(event)));
        transport.setConnectionClosedListener(
                failure -> onEdt(() -> acceptConnectionClosed(failure)));
    }

    public void attachView(View view) {
        requireEdt();
        this.view = Objects.requireNonNull(view, "view");
        view.render(state);
    }

    public void connect(String host, int port) {
        requireEdt();
        if (state.connectionStatus() != ConnectionStatus.DISCONNECTED) {
            return;
        }
        setState(state.withConnection(ConnectionStatus.CONNECTING, "Connecting..."));
        transport.connect(host, port)
                .whenComplete(
                        (response, failure) ->
                                onEdt(
                                        () -> {
                                            if (failure != null) {
                                                setState(
                                                        state.withConnection(
                                                                ConnectionStatus.DISCONNECTED,
                                                                failureMessage(failure)));
                                                return;
                                            }
                                            setState(
                                                    state.connectedTo(
                                                            "Connected as "
                                                                    + response.clientId()));
                                            refreshSessions();
                                        }));
    }

    public void refreshSessions() {
        requireEdt();
        if (state.connectionStatus() != ConnectionStatus.CONNECTED) {
            return;
        }
        transport.listSessions()
                .whenComplete(
                        (sessions, failure) ->
                                onEdt(
                                        () -> {
                                            if (failure != null) {
                                                appendEvent(failureMessage(failure));
                                            } else {
                                                List<protocol.SessionSummaryDto> safeSessions =
                                                        sessions == null ? List.of() : sessions;
                                                setState(
                                                        state.withSessions(
                                                                safeSessions,
                                                                safeSessions.isEmpty()
                                                                        ? "Lobby is empty"
                                                                        : "Lobby refreshed"));
                                            }
                                        }));
    }

    public void ping() {
        requireEdt();
        if (state.connectionStatus() != ConnectionStatus.CONNECTED) {
            return;
        }
        transport.ping()
                .whenComplete(
                        (response, failure) ->
                                onEdt(
                                        () ->
                                                appendEvent(
                                                        failure == null
                                                                ? response.message()
                                                                : failureMessage(failure))));
    }

    public void disconnect() {
        requireEdt();
        if (state.connectionStatus() != ConnectionStatus.CONNECTED) {
            return;
        }
        setState(state.withConnection(ConnectionStatus.DISCONNECTING, "Disconnecting..."));
        transport.disconnect()
                .whenComplete(
                        (response, failure) ->
                                onEdt(
                                        () ->
                                                setState(
                                                        state.withConnection(
                                                                ConnectionStatus.DISCONNECTED,
                                                                failure == null
                                                                        ? "Disconnected"
                                                                        : failureMessage(
                                                                                failure)))));
    }

    private void acceptEvent(EventMessage event) {
        requireEdt();
        GameSnapshotDto incoming = event.snapshot();
        if (incoming != null
                && (state.snapshot() == null
                        || incoming.version() > state.snapshot().version())) {
            setState(state.withSnapshot(incoming, event.message()));
        }
        if (event.message() != null && !event.message().isBlank()) {
            appendEvent(event.message());
        }
        event.gameEvents().forEach(this::appendEvent);
    }

    private void acceptConnectionClosed(Throwable failure) {
        requireEdt();
        String message = failure == null ? "Disconnected" : failureMessage(failure);
        setState(state.withConnection(ConnectionStatus.DISCONNECTED, message));
    }

    private void appendEvent(String message) {
        setState(state.appendEvent(message, eventHistoryLimit));
    }

    private void setState(ClientViewState newState) {
        requireEdt();
        state = newState;
        view.render(state);
    }

    private static String failureMessage(Throwable failure) {
        Throwable cause = failure.getCause() == null ? failure : failure.getCause();
        String message = cause.getMessage();
        return message == null || message.isBlank()
                ? cause.getClass().getSimpleName()
                : message;
    }

    private static void onEdt(Runnable work) {
        if (SwingUtilities.isEventDispatchThread()) {
            work.run();
        } else {
            SwingUtilities.invokeLater(work);
        }
    }

    private static void requireEdt() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Swing controller changes must run on the EDT");
        }
    }

    @Override
    public void close() {
        transport.close();
    }
}
