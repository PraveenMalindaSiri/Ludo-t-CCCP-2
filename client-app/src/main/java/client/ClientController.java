package client;

import client.model.ClientViewState;
import client.model.ClientViewState.ConnectionStatus;
import client.network.ClientTransport;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.swing.SwingUtilities;
import protocol.EventMessage;
import protocol.GameSnapshotDto;
import protocol.RequestType;
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
        transport
                .connect(host, port)
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
                                                            "Connected as " + response.clientId()));
                                            refreshSessions();
                                        }));
    }

    public void refreshSessions() {
        requireEdt();
        if (state.connectionStatus() != ConnectionStatus.CONNECTED) {
            return;
        }
        transport
                .listSessions()
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
        transport
                .ping()
                .whenComplete(
                        (response, failure) ->
                                onEdt(
                                        () ->
                                                appendEvent(
                                                        failure == null
                                                                ? response.message()
                                                                : failureMessage(failure))));
    }

    public void createSession(String name) {
        requireEdt();
        if (!connected() || name == null || name.isBlank()) {
            return;
        }
        handleSessionFuture(transport.createSession(name.trim()));
    }

    public void joinSession(UUID sessionId) {
        requireEdt();
        if (!connected() || sessionId == null) {
            return;
        }
        handleSessionFuture(transport.joinSession(sessionId));
    }

    public void leaveSession() {
        requireEdt();
        UUID sessionId = activeSessionId();
        if (!connected() || sessionId == null) {
            return;
        }
        transport
                .leaveSession(sessionId)
                .whenComplete(
                        (response, failure) ->
                                onEdt(
                                        () -> {
                                            if (failure != null) {
                                                appendEvent(failureMessage(failure));
                                            } else if (!response.success()) {
                                                appendEvent(response.message());
                                            } else {
                                                setState(state.leaveSession(response.message()));
                                                refreshSessions();
                                            }
                                        }));
    }

    public void startGame() {
        sendControl(RequestType.START_GAME);
    }

    public void pauseGame() {
        sendControl(RequestType.PAUSE_GAME);
    }

    public void resumeGame() {
        sendControl(RequestType.RESUME_GAME);
    }

    public void stepGame() {
        sendControl(RequestType.STEP_GAME);
    }

    public void stopGame() {
        sendControl(RequestType.STOP_GAME);
    }

    public void setSpeed(long delayMillis) {
        requireEdt();
        UUID sessionId = activeSessionId();
        if (!connected() || sessionId == null) {
            return;
        }
        handleSessionFuture(transport.setSpeed(sessionId, delayMillis));
    }

    public void disconnect() {
        requireEdt();
        if (state.connectionStatus() != ConnectionStatus.CONNECTED) {
            return;
        }
        setState(state.withConnection(ConnectionStatus.DISCONNECTING, "Disconnecting..."));
        transport
                .disconnect()
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
                && (state.snapshot() == null || incoming.version() > state.snapshot().version())) {
            setState(state.withSnapshot(incoming, event.message()));
        }
        if (event.message() != null && !event.message().isBlank()) {
            appendEvent(event.message());
        }
        event.gameEvents().forEach(this::appendEvent);
    }

    private void sendControl(RequestType requestType) {
        requireEdt();
        UUID sessionId = activeSessionId();
        if (!connected() || sessionId == null) {
            return;
        }
        handleSessionFuture(transport.control(requestType, sessionId));
    }

    private void handleSessionFuture(
            java.util.concurrent.CompletableFuture<ResponseMessage> future) {
        future.whenComplete(
                (response, failure) ->
                        onEdt(
                                () -> {
                                    if (failure != null) {
                                        appendEvent(failureMessage(failure));
                                        return;
                                    }
                                    if (!response.success()) {
                                        appendEvent(response.message());
                                        return;
                                    }
                                    ClientViewState updated =
                                            state.updateSession(
                                                    response.session(), response.message());
                                    if (response.snapshot() != null) {
                                        updated =
                                                updated.withSnapshot(
                                                        response.snapshot(), response.message());
                                    }
                                    setState(updated);
                                }));
    }

    private boolean connected() {
        return state.connectionStatus() == ConnectionStatus.CONNECTED;
    }

    private UUID activeSessionId() {
        return state.snapshot() == null ? null : state.snapshot().sessionId();
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
        return message == null || message.isBlank() ? cause.getClass().getSimpleName() : message;
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
