package server.network;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import protocol.ErrorCode;
import protocol.MessageKind;
import protocol.RequestMessage;
import protocol.RequestType;
import protocol.ResponseMessage;
import server.ServerConfig;
import server.application.GameService;
import server.application.port.SessionSubscriber;

/** Validates connection-level rules and routes game operations to the application service. */
public final class RequestDispatcher implements AutoCloseable {

    private final GameService games;

    public RequestDispatcher() {
        this(new GameService(ServerConfig.defaultsForPort(0)));
    }

    public RequestDispatcher(GameService games) {
        this.games = games;
    }

    public DispatchResult dispatch(RequestMessage request, UUID registeredClientId) {
        return dispatch(request, registeredClientId, null);
    }

    public DispatchResult dispatch(
            RequestMessage request, UUID registeredClientId, SessionSubscriber subscriber) {
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
            case DISCONNECT ->
                    success(request, "Disconnected", List.of(), true, registeredClientId);
            default -> dispatchGameRequest(request, registeredClientId, subscriber);
        };
    }

    public void connectionClosed(SessionSubscriber subscriber) {
        games.connectionClosed(subscriber);
    }

    GameService games() {
        return games;
    }

    private DispatchResult dispatchGameRequest(
            RequestMessage request, UUID registeredClientId, SessionSubscriber subscriber) {
        if (subscriber == null) {
            return failure(
                    request,
                    ErrorCode.SERVER_ERROR,
                    "A live connection is required for game operations",
                    false,
                    registeredClientId);
        }
        ResponseMessage response = games.handleOrEnqueue(request, subscriber, System.nanoTime());
        return new DispatchResult(response, registeredClientId, false);
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

    @Override
    public void close() {
        games.close();
    }
}
