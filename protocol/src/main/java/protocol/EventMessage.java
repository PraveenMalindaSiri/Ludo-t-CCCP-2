package protocol;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record EventMessage(
        int protocolVersion,
        MessageKind kind,
        UUID eventId,
        UUID sessionId,
        EventType eventType,
        long version,
        String message,
        List<String> gameEvents,
        GameSnapshotDto snapshot,
        Instant occurredAt) {

    public EventMessage {
        if (protocolVersion <= 0) {
            throw new IllegalArgumentException("protocolVersion must be positive");
        }
        if (kind != MessageKind.EVENT) {
            throw new IllegalArgumentException("EventMessage kind must be EVENT");
        }
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(eventType, "eventType");
        if (version < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
        gameEvents = gameEvents == null ? List.of() : List.copyOf(gameEvents);
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
