package client;

import static org.junit.jupiter.api.Assertions.assertTrue;

import client.model.ClientViewState;
import client.network.ClientTransport;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import protocol.EventMessage;
import protocol.EventType;
import protocol.MessageKind;
import protocol.RequestType;
import protocol.ResponseMessage;

class ClientControllerTest {

    @Test
    void asynchronousTransportCallbacksAlwaysRenderOnTheEdt() throws Exception {
        FakeTransport transport = new FakeTransport();
        ClientController controller = new ClientController(transport, 10);
        AtomicBoolean everyRenderOnEdt = new AtomicBoolean(true);
        CountDownLatch lobbyRendered = new CountDownLatch(1);
        CountDownLatch eventRendered = new CountDownLatch(1);

        SwingUtilities.invokeAndWait(
                () -> {
                    controller.attachView(
                            state -> {
                                everyRenderOnEdt.compareAndSet(
                                        true, SwingUtilities.isEventDispatchThread());
                                if ("Lobby is empty".equals(state.lastAction())) {
                                    lobbyRendered.countDown();
                                }
                                if (state.events().contains("background event")) {
                                    eventRendered.countDown();
                                }
                            });
                    controller.connect("127.0.0.1", 5050);
                });

        Thread.ofVirtual().start(transport::completeConnect);
        assertTrue(lobbyRendered.await(3, TimeUnit.SECONDS));
        Thread.ofVirtual().start(transport::pushEvent);
        assertTrue(eventRendered.await(3, TimeUnit.SECONDS));
        assertTrue(everyRenderOnEdt.get());
        controller.close();
    }

    private static final class FakeTransport implements ClientTransport {

        private final UUID clientId = UUID.randomUUID();
        private final CompletableFuture<ResponseMessage> connect = new CompletableFuture<>();
        private Consumer<EventMessage> events = ignored -> {};
        private Consumer<Throwable> closed = ignored -> {};
        private volatile boolean connected;

        @Override
        public UUID clientId() {
            return clientId;
        }

        @Override
        public CompletableFuture<ResponseMessage> connect(String host, int port) {
            return connect;
        }

        @Override
        public CompletableFuture<ResponseMessage> request(
                RequestType requestType, UUID sessionId, Map<String, String> parameters) {
            return CompletableFuture.completedFuture(response(requestType.name()));
        }

        @Override
        public boolean isConnected() {
            return connected;
        }

        @Override
        public void setEventListener(Consumer<EventMessage> listener) {
            events = listener;
        }

        @Override
        public void setConnectionClosedListener(Consumer<Throwable> listener) {
            closed = listener;
        }

        @Override
        public void close() {
            connected = false;
            closed.accept(null);
        }

        private void completeConnect() {
            connected = true;
            connect.complete(response("Connected"));
        }

        private void pushEvent() {
            events.accept(
                    new EventMessage(
                            1,
                            MessageKind.EVENT,
                            UUID.randomUUID(),
                            null,
                            EventType.SESSION_UPDATED,
                            0,
                            "background event",
                            List.of(),
                            null,
                            Instant.now()));
        }

        private ResponseMessage response(String message) {
            return new ResponseMessage(
                    1,
                    MessageKind.RESPONSE,
                    UUID.randomUUID(),
                    clientId,
                    null,
                    true,
                    null,
                    message,
                    null,
                    List.of(),
                    null,
                    Instant.now());
        }
    }
}
