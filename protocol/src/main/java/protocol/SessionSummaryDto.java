package protocol;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Lightweight session information used by the lobby and session responses. */
public record SessionSummaryDto(
        UUID sessionId,
        String name,
        SessionStatus status,
        int connectedClients,
        long turnDelayMillis,
        long version,
        Instant createdAt) {

    public SessionSummaryDto {
        Objects.requireNonNull(sessionId, "sessionId");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        Objects.requireNonNull(status, "status");
        if (connectedClients < 0) {
            throw new IllegalArgumentException("connectedClients must not be negative");
        }
        if (turnDelayMillis < 0) {
            throw new IllegalArgumentException("turnDelayMillis must not be negative");
        }
        if (version < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
        Objects.requireNonNull(createdAt, "createdAt");
    }
}
