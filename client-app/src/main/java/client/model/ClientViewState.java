package client.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import protocol.GameSnapshotDto;
import protocol.SessionSummaryDto;

/** Immutable presentation state; it contains protocol DTOs but no domain/game objects. */
public record ClientViewState(
        ConnectionStatus connectionStatus,
        String statusText,
        UUID clientId,
        List<SessionSummaryDto> sessions,
        GameSnapshotDto snapshot,
        List<String> events,
        String lastAction) {

    public enum ConnectionStatus {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        DISCONNECTING
    }

    public ClientViewState {
        Objects.requireNonNull(connectionStatus, "connectionStatus");
        Objects.requireNonNull(statusText, "statusText");
        Objects.requireNonNull(clientId, "clientId");
        sessions = sessions == null ? List.of() : List.copyOf(sessions);
        events = events == null ? List.of() : List.copyOf(events);
        lastAction = lastAction == null ? "" : lastAction;
    }

    public static ClientViewState initial(UUID clientId) {
        return new ClientViewState(
                ConnectionStatus.DISCONNECTED,
                "Not connected",
                clientId,
                List.of(),
                null,
                List.of(),
                "");
    }

    public ClientViewState withConnection(ConnectionStatus status, String text) {
        return new ClientViewState(
                status, text, clientId, sessions, snapshot, events, lastAction);
    }

    public ClientViewState connectedTo(String text) {
        return new ClientViewState(
                ConnectionStatus.CONNECTED,
                text,
                clientId,
                List.of(),
                null,
                events,
                "");
    }

    public ClientViewState withSessions(List<SessionSummaryDto> newSessions, String action) {
        return new ClientViewState(
                connectionStatus,
                statusText,
                clientId,
                newSessions,
                snapshot,
                events,
                action);
    }

    public ClientViewState withSnapshot(GameSnapshotDto newSnapshot, String action) {
        return new ClientViewState(
                connectionStatus,
                statusText,
                clientId,
                sessions,
                newSnapshot,
                events,
                action);
    }

    public ClientViewState appendEvent(String event, int maximumEvents) {
        Objects.requireNonNull(event, "event");
        if (maximumEvents <= 0) {
            throw new IllegalArgumentException("maximumEvents must be positive");
        }
        List<String> updated = new ArrayList<>(events);
        updated.add(event);
        if (updated.size() > maximumEvents) {
            updated = new ArrayList<>(updated.subList(updated.size() - maximumEvents, updated.size()));
        }
        return new ClientViewState(
                connectionStatus,
                statusText,
                clientId,
                sessions,
                snapshot,
                updated,
                event);
    }
}
