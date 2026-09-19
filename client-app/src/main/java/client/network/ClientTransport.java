package client.network;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import protocol.EventMessage;
import protocol.RequestType;
import protocol.ResponseMessage;
import protocol.SessionSummaryDto;

/** Protocol-only boundary used by the Swing controller. */
public interface ClientTransport extends AutoCloseable {

    UUID clientId();

    CompletableFuture<ResponseMessage> connect(String host, int port);

    CompletableFuture<ResponseMessage> request(
            RequestType requestType, UUID sessionId, Map<String, String> parameters);

    default CompletableFuture<ResponseMessage> ping() {
        return request(RequestType.PING, null, Map.of());
    }

    default CompletableFuture<List<SessionSummaryDto>> listSessions() {
        return request(RequestType.LIST_SESSIONS, null, Map.of()).thenApply(ResponseMessage::sessions);
    }

    default CompletableFuture<ResponseMessage> disconnect() {
        return request(RequestType.DISCONNECT, null, Map.of());
    }

    boolean isConnected();

    void setEventListener(Consumer<EventMessage> listener);

    void setConnectionClosedListener(Consumer<Throwable> listener);

    @Override
    void close();
}
