package protocol;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Exactly one response correlated to a client request. */
public record ResponseMessage(
        int protocolVersion,
        MessageKind kind,
        UUID requestId,
        UUID clientId,
        UUID sessionId,
        boolean success,
        ErrorCode errorCode,
        String message,
        SessionSummaryDto session,
        List<SessionSummaryDto> sessions,
        GameSnapshotDto snapshot,
        Instant sentAt) {

    public ResponseMessage {
        if (protocolVersion <= 0) {
            throw new IllegalArgumentException("protocolVersion must be positive");
        }
        if (kind != MessageKind.RESPONSE) {
            throw new IllegalArgumentException("ResponseMessage kind must be RESPONSE");
        }
        Objects.requireNonNull(requestId, "requestId");
        Objects.requireNonNull(clientId, "clientId");
        Objects.requireNonNull(sentAt, "sentAt");
        if (success && errorCode != null) {
            throw new IllegalArgumentException("Successful response cannot contain an errorCode");
        }
        if (!success && errorCode == null) {
            throw new IllegalArgumentException("Failed response requires an errorCode");
        }
        sessions = sessions == null ? List.of() : List.copyOf(sessions);
    }
}
