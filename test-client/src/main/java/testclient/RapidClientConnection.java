package testclient;

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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import protocol.BoundedLineReader;
import protocol.EventMessage;
import protocol.EventType;
import protocol.JsonLineCodec;
import protocol.MessageKind;
import protocol.ProtocolException;
import protocol.RequestMessage;
import protocol.RequestType;
import protocol.ResponseMessage;

/** Persistent NDJSON connection supporting many simultaneously outstanding requests. */
public final class RapidClientConnection implements AutoCloseable {

    private final String host;
    private final int port;
    private final UUID clientId;
    private final JsonLineCodec codec;
    private final long timeoutMillis;
    private final BlockingQueue<RequestMessage> outbound;
    private final Map<UUID, CompletableFuture<ResponseMessage>> pending = new ConcurrentHashMap<>();
    private final AtomicBoolean open = new AtomicBoolean();
    private final AtomicInteger maximumOutstanding = new AtomicInteger();
    private volatile Consumer<UUID> duplicateResponseListener = ignored -> {};
    private volatile Socket socket;
    private volatile BoundedLineReader reader;
    private volatile BufferedWriter writer;
    private volatile Thread readerThread;
    private volatile Thread writerThread;

    public RapidClientConnection(
            String host, int port, int outboundCapacity, int maxLineLength, long timeoutMillis) {
        this.host = requireText(host, "host");
        if (port < 1 || port > 65_535 || outboundCapacity <= 0 || timeoutMillis <= 0) {
            throw new IllegalArgumentException("Port, capacity and timeout must be positive");
        }
        this.port = port;
        this.clientId = UUID.randomUUID();
        this.codec = new JsonLineCodec(maxLineLength);
        this.timeoutMillis = timeoutMillis;
        this.outbound = new ArrayBlockingQueue<>(outboundCapacity);
    }

    public ResponseMessage connect() throws Exception {
        if (!open.compareAndSet(false, true)) {
            throw new IllegalStateException("Connection is already open");
        }
        try {
            Socket connected = new Socket();
            connected.connect(new InetSocketAddress(host, port), Math.toIntExact(timeoutMillis));
            connected.setTcpNoDelay(true);
            socket = connected;
            reader =
                    new BoundedLineReader(
                            new InputStreamReader(
                                    connected.getInputStream(), StandardCharsets.UTF_8),
                            codec.maxLineLength());
            writer =
                    new BufferedWriter(
                            new OutputStreamWriter(
                                    connected.getOutputStream(), StandardCharsets.UTF_8));
            writerThread =
                    Thread.ofVirtual()
                            .name("rapid-client-writer-" + clientId)
                            .start(this::writeLoop);
            readerThread =
                    Thread.ofVirtual()
                            .name("rapid-client-reader-" + clientId)
                            .start(this::readLoop);
            ResponseMessage response =
                    requestAsync(RequestType.CONNECT, null, Map.of())
                            .future()
                            .get(timeoutMillis, TimeUnit.MILLISECONDS);
            if (!response.success()) {
                throw new IllegalStateException(response.message());
            }
            return response;
        } catch (Exception failure) {
            closeWithCause(failure);
            throw failure;
        }
    }

    public PendingRequest requestAsync(
            RequestType type, UUID sessionId, Map<String, String> parameters) {
        Objects.requireNonNull(type, "type");
        if (!open.get()) {
            RequestMessage request =
                    RequestMessage.create(
                            UUID.randomUUID(),
                            clientId,
                            type,
                            sessionId,
                            parameters,
                            Instant.now());
            return new PendingRequest(
                    request,
                    CompletableFuture.failedFuture(new IOException("Connection is closed")));
        }
        RequestMessage request =
                RequestMessage.create(
                        UUID.randomUUID(), clientId, type, sessionId, parameters, Instant.now());
        CompletableFuture<ResponseMessage> future = new CompletableFuture<>();
        pending.put(request.requestId(), future);
        maximumOutstanding.accumulateAndGet(pending.size(), Math::max);
        future.orTimeout(timeoutMillis, TimeUnit.MILLISECONDS);
        future.whenComplete((response, failure) -> pending.remove(request.requestId(), future));
        if (!outbound.offer(request)) {
            future.completeExceptionally(
                    new IllegalStateException("Client outbound queue is full"));
        }
        return new PendingRequest(request, future);
    }

    public UUID clientId() {
        return clientId;
    }

    public int pendingCount() {
        return pending.size();
    }

    public int maximumOutstanding() {
        return maximumOutstanding.get();
    }

    public boolean networkingThreadsAreVirtual() {
        return readerThread != null
                && writerThread != null
                && readerThread.isVirtual()
                && writerThread.isVirtual();
    }

    public void setDuplicateResponseListener(Consumer<UUID> listener) {
        duplicateResponseListener = Objects.requireNonNull(listener, "listener");
    }

    private void readLoop() {
        try {
            while (open.get()) {
                String line = reader.readLine();
                if (line == null) {
                    throw new IOException("Server closed the connection");
                }
                MessageKind kind = codec.decodeKind(line);
                if (kind == MessageKind.RESPONSE) {
                    correlate(codec.decode(line, ResponseMessage.class));
                } else if (kind == MessageKind.EVENT) {
                    EventMessage event = codec.decode(line, EventMessage.class);
                    if (event.eventType() == EventType.SERVER_SHUTTING_DOWN) {
                        throw new IOException(event.message());
                    }
                } else {
                    throw new ProtocolException("Rapid client cannot receive request messages");
                }
            }
        } catch (IOException | ProtocolException | RuntimeException failure) {
            if (open.get()) {
                closeWithCause(failure);
            }
        }
    }

    private void correlate(ResponseMessage response) {
        CompletableFuture<ResponseMessage> future = pending.remove(response.requestId());
        if (future == null || !future.complete(response)) {
            duplicateResponseListener.accept(response.requestId());
        }
    }

    private void writeLoop() {
        try {
            while (open.get()) {
                RequestMessage request = outbound.take();
                writer.write(codec.encodeLine(request));
                writer.flush();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (IOException | ProtocolException failure) {
            if (open.get()) {
                closeWithCause(failure);
            }
        }
    }

    public void disconnect() {
        if (!open.get()) {
            return;
        }
        try {
            requestAsync(RequestType.DISCONNECT, null, Map.of())
                    .future()
                    .get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (Exception ignored) {
            // Closing the socket below is the deterministic fallback.
        } finally {
            close();
        }
    }

    @Override
    public void close() {
        closeWithCause(new IOException("Rapid client closed"));
    }

    private void closeWithCause(Throwable cause) {
        if (!open.compareAndSet(true, false)) {
            return;
        }
        Socket current = socket;
        if (current != null) {
            try {
                current.close();
            } catch (IOException ignored) {
                // Socket is already closing.
            }
        }
        interruptOther(readerThread);
        interruptOther(writerThread);
        pending.values().forEach(future -> future.completeExceptionally(cause));
        pending.clear();
    }

    private void interruptOther(Thread thread) {
        if (thread != null && thread != Thread.currentThread()) {
            thread.interrupt();
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    public record PendingRequest(
            RequestMessage request, CompletableFuture<ResponseMessage> future) {

        public PendingRequest {
            Objects.requireNonNull(request, "request");
            Objects.requireNonNull(future, "future");
        }
    }
}
