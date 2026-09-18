package protocol;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** One correlated operation sent by a client. */
public record RequestMessage(
        int protocolVersion,
        MessageKind kind,
        UUID requestId,
        UUID clientId,
        RequestType requestType,
        UUID sessionId,
        Map<String, String> parameters,
        Instant sentAt) {

    public static final int CURRENT_PROTOCOL_VERSION = 1;

    public RequestMessage {
        if (protocolVersion <= 0) {
            throw new IllegalArgumentException("protocolVersion must be positive");
        }
        if (kind != MessageKind.REQUEST) {
            throw new IllegalArgumentException("RequestMessage kind must be REQUEST");
        }
        Objects.requireNonNull(requestId, "requestId");
        Objects.requireNonNull(clientId, "clientId");
        Objects.requireNonNull(requestType, "requestType");
        Objects.requireNonNull(sentAt, "sentAt");
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
    }

    public static RequestMessage create(
            UUID requestId,
            UUID clientId,
            RequestType requestType,
            UUID sessionId,
            Map<String, String> parameters,
            Instant sentAt) {
        return new RequestMessage(
                CURRENT_PROTOCOL_VERSION,
                MessageKind.REQUEST,
                requestId,
                clientId,
                requestType,
                sessionId,
                parameters,
                sentAt);
    }
}
