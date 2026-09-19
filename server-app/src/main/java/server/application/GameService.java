package server.application;

import java.time.Instant;
import java.util.List;
import protocol.ErrorCode;
import protocol.MessageKind;
import protocol.RequestMessage;
import protocol.RequestType;
import protocol.ResponseMessage;
import server.ServerConfig;
import server.application.port.SessionSubscriber;
import server.session.GameSession;
import server.session.SessionRegistry;

/** Application use cases between network connections and the session registry. */
public final class GameService implements AutoCloseable {

    private final SessionRegistry registry;

    public GameService(ServerConfig config) {
        this(new SessionRegistry(config.sessionQueueCapacity(), config.defaultTurnDelayMillis()));
    }

    public GameService(SessionRegistry registry) {
        this.registry = registry;
    }

    public ResponseMessage handleOrEnqueue(
            RequestMessage request, SessionSubscriber subscriber, long receivedNanos) {
        return switch (request.requestType()) {
            case LIST_SESSIONS -> list(request);
            case CREATE_SESSION -> create(request, subscriber);
            case JOIN_SESSION,
                    LEAVE_SESSION,
                    GET_SNAPSHOT,
                    START_GAME,
                    PAUSE_GAME,
                    RESUME_GAME,
                    STEP_GAME,
                    STOP_GAME,
                    SET_SPEED ->
                    enqueue(request, subscriber, receivedNanos);
            default ->
                    failure(
                            request,
                            ErrorCode.INVALID_REQUEST,
                            request.requestType() + " is not a game service operation");
        };
    }

    public SessionRegistry registry() {
        return registry;
    }

    public void connectionClosed(SessionSubscriber subscriber) {
        registry.removeSubscriber(subscriber);
    }

    private ResponseMessage list(RequestMessage request) {
        return new ResponseMessage(
                RequestMessage.CURRENT_PROTOCOL_VERSION,
                MessageKind.RESPONSE,
                request.requestId(),
                request.clientId(),
                null,
                true,
                null,
                "Sessions listed",
                null,
                registry.list(),
                null,
                Instant.now());
    }

    private ResponseMessage create(RequestMessage request, SessionSubscriber subscriber) {
        String name = request.parameters().get("name");
        long seed;
        try {
            String rawSeed = request.parameters().get("seed");
            seed = rawSeed == null ? System.nanoTime() : Long.parseLong(rawSeed);
        } catch (NumberFormatException exception) {
            return failure(request, ErrorCode.INVALID_REQUEST, "seed must be a long integer");
        }

        GameSession session = registry.create(name, seed);
        RequestMessage join =
                new RequestMessage(
                        request.protocolVersion(),
                        request.kind(),
                        request.requestId(),
                        request.clientId(),
                        RequestType.JOIN_SESSION,
                        session.sessionId(),
                        request.parameters(),
                        request.sentAt());
        ResponseMessage immediate = enqueue(join, subscriber, System.nanoTime());
        if (immediate != null) {
            registry.remove(session.sessionId());
            return immediate;
        }
        return null;
    }

    private ResponseMessage enqueue(
            RequestMessage request, SessionSubscriber subscriber, long receivedNanos) {
        GameSession session = registry.find(request.sessionId());
        if (session == null) {
            return failure(request, ErrorCode.UNKNOWN_SESSION, "Unknown game session");
        }
        if (!session.submit(request, subscriber, receivedNanos)) {
            return failure(request, ErrorCode.QUEUE_FULL, "Session request queue is full");
        }
        return null;
    }

    private ResponseMessage failure(RequestMessage request, ErrorCode code, String message) {
        return new ResponseMessage(
                RequestMessage.CURRENT_PROTOCOL_VERSION,
                MessageKind.RESPONSE,
                request.requestId(),
                request.clientId(),
                request.sessionId(),
                false,
                code,
                message,
                null,
                List.of(),
                null,
                Instant.now());
    }

    @Override
    public void close() {
        registry.close();
    }
}
