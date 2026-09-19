package server.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import protocol.ErrorCode;
import protocol.RequestMessage;
import protocol.RequestType;

class RequestDispatcherTest {

    private final RequestDispatcher dispatcher = new RequestDispatcher();
    private final UUID clientId = UUID.randomUUID();

    @Test
    void connectPingListAndDisconnectAreTheOnlySupportedWork02Operations() {
        RequestDispatcher.DispatchResult connected =
                dispatcher.dispatch(request(RequestType.CONNECT), null);
        assertTrue(connected.response().success());
        assertEquals(clientId, connected.registeredClientId());

        RequestDispatcher.DispatchResult ping =
                dispatcher.dispatch(request(RequestType.PING), clientId);
        assertEquals("PONG", ping.response().message());

        RequestDispatcher.DispatchResult list =
                dispatcher.dispatch(request(RequestType.LIST_SESSIONS), clientId);
        assertTrue(list.response().sessions().isEmpty());

        RequestDispatcher.DispatchResult unsupported =
                dispatcher.dispatch(request(RequestType.CREATE_SESSION), clientId);
        assertFalse(unsupported.response().success());
        assertEquals(ErrorCode.INVALID_REQUEST, unsupported.response().errorCode());

        RequestDispatcher.DispatchResult disconnect =
                dispatcher.dispatch(request(RequestType.DISCONNECT), clientId);
        assertTrue(disconnect.closeAfterWrite());
    }

    @Test
    void operationsBeforeConnectAndMismatchedClientIdsAreRejected() {
        RequestDispatcher.DispatchResult beforeConnect =
                dispatcher.dispatch(request(RequestType.PING), null);
        assertEquals(ErrorCode.INVALID_STATE, beforeConnect.response().errorCode());

        RequestDispatcher.DispatchResult mismatch =
                dispatcher.dispatch(request(RequestType.PING), UUID.randomUUID());
        assertEquals(ErrorCode.INVALID_REQUEST, mismatch.response().errorCode());
    }

    private RequestMessage request(RequestType type) {
        return RequestMessage.create(
                UUID.randomUUID(), clientId, type, null, Map.of(), Instant.now());
    }
}
