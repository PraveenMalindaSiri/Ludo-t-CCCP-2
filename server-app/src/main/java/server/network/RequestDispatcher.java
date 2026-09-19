package server.network;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import protocol.ErrorCode;
import protocol.MessageKind;
import protocol.RequestMessage;
import protocol.RequestType;
import protocol.ResponseMessage;

/** Handles only the connection-level operations included in Work 02. */
public final class RequestDispatcher {

    public DispatchResult dispatch(RequestMessage request, UUID registeredClientId) {
        if (request.protocolVersion() != RequestMessage.CURRENT_PROTOCOL_VERSION) {
            return failure(
                    request,
                    ErrorCode.UNSUPPORTED_PROTOCOL,
                    "Unsupported protocol version " + request.protocolVersion(),
                    false,
                    registeredClientId);
        }

        if (request.requestType() == RequestType.CONNECT) {
            if (registeredClientId != null && !registeredClientId.equals(request.clientId())) {
                return failure(
                        request,
                        ErrorCode.INVALID_STATE,
                        "This connection is already registered to another client",
                        false,
                        registeredClientId);
            }
            String message = registeredClientId == null ? "Connected" : "Already connected";
            return success(request, message, List.of(), false, request.clientId());
        }

        if (registeredClientId == null) {
            return failure(
                    request,
                    ErrorCode.INVALID_STATE,
                    "CONNECT must be completed first",
                    false,
                    null);
        }
        if (!registeredClientId.equals(request.clientId())) {
            return failure(
                    request,
                    ErrorCode.INVALID_REQUEST,
                    "Request clientId does not match this connection",
                    false,
                    registeredClientId);
        }

        return switch (request.requestType()) {
            case PING -> success(request, "PONG", List.of(), false, registeredClientId);
            case LIST_SESSIONS ->
                    success(
                            request,
                            "No sessions are available yet",
                            List.of(),
                            false,
                            registeredClientId);
            case DISCONNECT ->
                    success(request, "Disconnected", List.of(), true, registeredClientId);
            default ->
                    failure(
                            request,
                            ErrorCode.INVALID_REQUEST,
                            request.requestType() + " is not available in Work 02",
                            false,
                            registeredClientId);
        };
    }

    private DispatchResult success(
            RequestMessage request,
            String message,
            List<protocol.SessionSummaryDto> sessions,
            boolean closeAfterWrite,
            UUID registeredClientId) {
        ResponseMessage response =
                new ResponseMessage(
                        RequestMessage.CURRENT_PROTOCOL_VERSION,
                        MessageKind.RESPONSE,
                        request.requestId(),
                        request.clientId(),
                        request.sessionId(),
                        true,
                        null,
                        message,
                        null,
                        sessions,
                        null,
                        Instant.now());
        return new DispatchResult(response, registeredClientId, closeAfterWrite);
    }

    private DispatchResult failure(
            RequestMessage request,
            ErrorCode errorCode,
            String message,
            boolean closeAfterWrite,
            UUID registeredClientId) {
        ResponseMessage response =
                new ResponseMessage(
                        RequestMessage.CURRENT_PROTOCOL_VERSION,
                        MessageKind.RESPONSE,
                        request.requestId(),
                        request.clientId(),
                        request.sessionId(),
                        false,
                        errorCode,
                        message,
                        null,
                        List.of(),
                        null,
                        Instant.now());
        return new DispatchResult(response, registeredClientId, closeAfterWrite);
    }

    public record DispatchResult(
            ResponseMessage response, UUID registeredClientId, boolean closeAfterWrite) {}
}
