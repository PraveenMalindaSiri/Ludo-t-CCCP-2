package client.network;

import client.ClientConfig;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import protocol.BoundedLineReader;
import protocol.EventMessage;
import protocol.JsonLineCodec;
import protocol.MessageKind;
import protocol.ProtocolException;
import protocol.RequestMessage;
import protocol.RequestType;
import protocol.ResponseMessage;

/** Persistent virtual-thread TCP transport with UUID request correlation. */
public final class ServerConnection implements ClientTransport {

    private enum State {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }

    private final ClientConfig config;
    private final UUID clientId;
    private final JsonLineCodec codec;
    private final Map<UUID, CompletableFuture<ResponseMessage>> pending = new ConcurrentHashMap<>();
    private final AtomicReference<State> state = new AtomicReference<>(State.DISCONNECTED);
    private volatile Consumer<EventMessage> eventListener = event -> {};
    private volatile Consumer<Throwable> connectionClosedListener = cause -> {};
    private volatile Socket socket;
    private volatile BoundedLineReader reader;
    private volatile BufferedWriter writer;
    private volatile BlockingQueue<RequestMessage> outbound;
    private volatile Thread connectorThread;
    private volatile Thread readerThread;
    private volatile Thread writerThread;

    public ServerConnection(ClientConfig config) {
        this(config, UUID.randomUUID());
    }

    ServerConnection(ClientConfig config, UUID clientId) {
        this.config = Objects.requireNonNull(config, "config");
        this.clientId = Objects.requireNonNull(clientId, "clientId");
        this.codec = new JsonLineCodec(config.maxLineLength());
    }

    @Override
    public UUID clientId() {
        return clientId;
    }

    @Override
    public CompletableFuture<ResponseMessage> connect(String host, int port) {
        if (!state.compareAndSet(State.DISCONNECTED, State.CONNECTING)) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("Connection is already active"));
        }
        CompletableFuture<ResponseMessage> result = new CompletableFuture<>();
        connectorThread =
                Thread.ofVirtual()
                        .name("client-connect-" + clientId)
                        .start(() -> openSocket(host, port, result));
        return result;
    }

    private void openSocket(
            String host, int port, CompletableFuture<ResponseMessage> connectResult) {
        try {
            Socket newSocket = new Socket();
            newSocket.connect(new InetSocketAddress(host, port), config.connectTimeoutMillis());
            newSocket.setTcpNoDelay(true);
            socket = newSocket;
            reader =
                    new BoundedLineReader(
                            new InputStreamReader(
                                    newSocket.getInputStream(), StandardCharsets.UTF_8),
                            config.maxLineLength());
            writer =
                    new BufferedWriter(
                            new OutputStreamWriter(
                                    newSocket.getOutputStream(), StandardCharsets.UTF_8));
            outbound = new ArrayBlockingQueue<>(config.outboundQueueCapacity());
            state.set(State.CONNECTED);
            writerThread =
                    Thread.ofVirtual().name("client-writer-" + clientId).start(this::writeLoop);
            readerThread =
                    Thread.ofVirtual().name("client-reader-" + clientId).start(this::readLoop);
            request(RequestType.CONNECT, null, Map.of())
                    .whenComplete(
                            (response, failure) -> {
                                if (failure != null) {
                                    connectResult.completeExceptionally(failure);
                                    closeWithCause(failure);
                                } else if (!response.success()) {
                                    IllegalStateException rejected =
                                            new IllegalStateException(response.message());
                                    connectResult.completeExceptionally(rejected);
                                    closeWithCause(rejected);
                                } else {
                                    connectResult.complete(response);
                                }
                            });
        } catch (IOException | RuntimeException exception) {
            connectResult.completeExceptionally(exception);
            closeWithCause(exception);
        }
    }

    @Override
    public CompletableFuture<ResponseMessage> request(
            RequestType requestType, UUID sessionId, Map<String, String> parameters) {
        Objects.requireNonNull(requestType, "requestType");
        if (state.get() != State.CONNECTED) {
            return CompletableFuture.failedFuture(new IllegalStateException("Not connected"));
        }

        UUID requestId = UUID.randomUUID();
        RequestMessage request =
                RequestMessage.create(
                        requestId, clientId, requestType, sessionId, parameters, Instant.now());
        CompletableFuture<ResponseMessage> future = new CompletableFuture<>();
        pending.put(requestId, future);
        future.orTimeout(config.requestTimeoutMillis(), TimeUnit.MILLISECONDS);
        future.whenComplete((response, failure) -> pending.remove(requestId, future));

        BlockingQueue<RequestMessage> queue = outbound;
        if (queue == null || !queue.offer(request)) {
            future.completeExceptionally(
                    new IllegalStateException("Client outbound queue is full"));
        }
        return future;
    }

    @Override
    public CompletableFuture<ResponseMessage> disconnect() {
        CompletableFuture<ResponseMessage> response = ClientTransport.super.disconnect();
        response.whenComplete((ignored, failure) -> closeWithCause(failure));
        return response;
    }

    @Override
    public boolean isConnected() {
        return state.get() == State.CONNECTED;
    }

    @Override
    public void setEventListener(Consumer<EventMessage> listener) {
        eventListener = Objects.requireNonNull(listener, "listener");
    }

    @Override
    public void setConnectionClosedListener(Consumer<Throwable> listener) {
        connectionClosedListener = Objects.requireNonNull(listener, "listener");
    }

    public boolean networkingThreadsAreVirtual() {
        Thread currentReader = readerThread;
        Thread currentWriter = writerThread;
        return currentReader != null
                && currentWriter != null
                && currentReader.isVirtual()
                && currentWriter.isVirtual();
    }

    private void readLoop() {
        try {
            while (state.get() == State.CONNECTED) {
                String line = reader.readLine();
                if (line == null) {
                    closeWithCause(new IOException("Server closed the connection"));
                    return;
                }
                MessageKind kind = codec.decodeKind(line);
                if (kind == MessageKind.RESPONSE) {
                    handleResponse(codec.decode(line, ResponseMessage.class));
                } else if (kind == MessageKind.EVENT) {
                    eventListener.accept(codec.decode(line, EventMessage.class));
                } else {
                    throw new ProtocolException("Client cannot receive request messages");
                }
            }
        } catch (IOException | ProtocolException | RuntimeException exception) {
            if (state.get() != State.DISCONNECTED) {
                closeWithCause(exception);
            }
        }
    }

    private void handleResponse(ResponseMessage response) {
        CompletableFuture<ResponseMessage> future = pending.remove(response.requestId());
        if (future == null) {
            System.err.println("Ignoring response with unknown request ID " + response.requestId());
            return;
        }
        future.complete(response);
    }

    private void writeLoop() {
        try {
            while (state.get() == State.CONNECTED) {
                RequestMessage request = outbound.take();
                writer.write(codec.encodeLine(request));
                writer.flush();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (IOException | ProtocolException exception) {
            if (state.get() != State.DISCONNECTED) {
                closeWithCause(exception);
            }
        }
    }

    @Override
    public void close() {
        closeWithCause(null);
    }

    private void closeWithCause(Throwable cause) {
        State previous = state.getAndSet(State.DISCONNECTED);
        if (previous == State.DISCONNECTED) {
            return;
        }
        Socket currentSocket = socket;
        if (currentSocket != null) {
            try {
                currentSocket.close();
            } catch (IOException ignored) {
                // The socket is already closing.
            }
        }
        interruptOther(connectorThread);
        interruptOther(readerThread);
        interruptOther(writerThread);
        Throwable completionCause = cause == null ? new IOException("Connection closed") : cause;
        pending.values().forEach(future -> future.completeExceptionally(completionCause));
        pending.clear();
        connectionClosedListener.accept(cause);
    }

    private void interruptOther(Thread thread) {
        if (thread != null && thread != Thread.currentThread()) {
            thread.interrupt();
        }
    }
}
