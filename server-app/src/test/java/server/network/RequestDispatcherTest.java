package server.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import protocol.ErrorCode;
import protocol.RequestMessage;
import protocol.RequestType;
import protocol.ResponseMessage;
import server.application.port.SessionSubscriber;

class RequestDispatcherTest {

    private final RequestDispatcher dispatcher = new RequestDispatcher();
    private final UUID clientId = UUID.randomUUID();
    private final RecordingSubscriber subscriber = new RecordingSubscriber();

    @AfterEach
    void closeDispatcher() {
        dispatcher.close();
    }

    @Test
    void connectionRequestsRemainImmediateAndGameRequestsAreRouted() throws Exception {
        RequestDispatcher.DispatchResult connected =
                dispatcher.dispatch(request(RequestType.CONNECT), null);
        assertTrue(connected.response().success());
        assertEquals(clientId, connected.registeredClientId());

        RequestDispatcher.DispatchResult ping =
                dispatcher.dispatch(request(RequestType.PING), clientId);
        assertEquals("PONG", ping.response().message());

        RequestDispatcher.DispatchResult list =
                dispatcher.dispatch(request(RequestType.LIST_SESSIONS), clientId, subscriber);
        assertTrue(list.response().sessions().isEmpty());

        RequestDispatcher.DispatchResult create =
                dispatcher.dispatch(request(RequestType.CREATE_SESSION), clientId, subscriber);
        assertEquals(null, create.response());
        ResponseMessage created = subscriber.take();
        assertTrue(created.success());
        assertEquals("Game-1", created.session().name());
        assertEquals(1, dispatcher.games().registry().size());

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

    @Test
    void gameWorkIsRejectedAfterShutdownBegins() {
        RequestDispatcher.DispatchResult connected =
                dispatcher.dispatch(request(RequestType.CONNECT), null);
        dispatcher.beginShutdown();

        RequestDispatcher.DispatchResult result =
                dispatcher.dispatch(
                        request(RequestType.LIST_SESSIONS),
                        connected.registeredClientId(),
                        subscriber);

        assertEquals(ErrorCode.SERVER_ERROR, result.response().errorCode());
    }

    private RequestMessage request(RequestType type) {
        return RequestMessage.create(
                UUID.randomUUID(), clientId, type, null, Map.of(), Instant.now());
    }

    private static final class RecordingSubscriber implements SessionSubscriber {

        private final UUID id = UUID.randomUUID();
        private final LinkedBlockingQueue<ResponseMessage> responses = new LinkedBlockingQueue<>();

        @Override
        public UUID connectionId() {
            return id;
        }

        @Override
        public boolean offer(Object message) {
            return responses.offer((ResponseMessage) message);
        }

        private ResponseMessage take() throws InterruptedException {
            ResponseMessage response = responses.poll(3, TimeUnit.SECONDS);
            if (response == null) {
                throw new AssertionError("Timed out waiting for response");
            }
            return response;
        }
    }
}
