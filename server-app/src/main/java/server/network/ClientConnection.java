package server.network;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import protocol.BoundedLineReader;
import protocol.JsonLineCodec;
import protocol.MessageKind;
import protocol.ProtocolException;
import protocol.RequestMessage;
import server.ServerConfig;
import server.application.port.SessionSubscriber;

/** Owns the reader, bounded output queue and sole writer for one persistent socket. */
public final class ClientConnection implements AutoCloseable, SessionSubscriber {

    private final UUID connectionId = UUID.randomUUID();
    private final Socket socket;
    private final RequestDispatcher dispatcher;
    private final Runnable onClosed;
    private final JsonLineCodec codec;
    private final BoundedLineReader reader;
    private final BufferedWriter writer;
    private final BlockingQueue<OutboundMessage> outbound;
    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();
    private volatile UUID registeredClientId;
    private volatile Thread readerThread;
    private volatile Thread writerThread;

    public ClientConnection(
            Socket socket, ServerConfig config, RequestDispatcher dispatcher, Runnable onClosed)
            throws IOException {
        this.socket = Objects.requireNonNull(socket, "socket");
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
        this.onClosed = Objects.requireNonNull(onClosed, "onClosed");
        this.codec = new JsonLineCodec(config.maxLineLength());
        this.reader =
                new BoundedLineReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8),
                        config.maxLineLength());
        this.writer =
                new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        this.outbound = new ArrayBlockingQueue<>(config.outboundQueueCapacity());
    }

    public void start() {
        if (!started.compareAndSet(false, true)) {
            throw new IllegalStateException("Connection already started");
        }
        writerThread =
                Thread.ofVirtual().name("server-writer-" + connectionId).start(this::writeLoop);
        readerThread =
                Thread.ofVirtual().name("server-reader-" + connectionId).start(this::readLoop);
    }

    public UUID connectionId() {
        return connectionId;
    }

    public UUID registeredClientId() {
        return registeredClientId;
    }

    public boolean networkingThreadsAreVirtual() {
        Thread currentReader = readerThread;
        Thread currentWriter = writerThread;
        return currentReader != null
                && currentWriter != null
                && currentReader.isVirtual()
                && currentWriter.isVirtual();
    }

    @Override
    public boolean offer(Object message) {
        return enqueue(new OutboundMessage(message, false));
    }

    @Override
    public boolean isOpen() {
        return !closed.get();
    }

    /** Compatibility alias retained for the Work 02 writer-queue test. */
    public boolean send(Object message) {
        return offer(message);
    }

    private void readLoop() {
        try {
            while (!closed.get()) {
                String line = reader.readLine();
                if (line == null) {
                    close();
                    return;
                }
                if (codec.decodeKind(line) != MessageKind.REQUEST) {
                    throw new ProtocolException("Server accepts request messages only");
                }
                RequestMessage request = codec.decode(line, RequestMessage.class);
                RequestDispatcher.DispatchResult result =
                        dispatcher.dispatch(request, registeredClientId, this);
                registeredClientId = result.registeredClientId();
                if (result.response() != null) {
                    if (!enqueue(
                            new OutboundMessage(result.response(), result.closeAfterWrite()))) {
                        return;
                    }
                }
                if (result.closeAfterWrite()) {
                    return;
                }
            }
        } catch (IOException | ProtocolException exception) {
            if (!closed.get()) {
                System.err.printf(
                        "Closing connection %s after invalid input or socket failure: %s%n",
                        connectionId, exception.getMessage());
            }
            close();
        }
    }

    private void writeLoop() {
        try {
            while (!closed.get()) {
                OutboundMessage outboundMessage = outbound.take();
                writer.write(codec.encodeLine(outboundMessage.message()));
                writer.flush();
                if (outboundMessage.closeAfterWrite()) {
                    close();
                    return;
                }
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            close();
        } catch (IOException | ProtocolException exception) {
            if (!closed.get()) {
                System.err.printf(
                        "Closing connection %s after write failure: %s%n",
                        connectionId, exception.getMessage());
            }
            close();
        }
    }

    private boolean enqueue(OutboundMessage message) {
        if (closed.get()) {
            return false;
        }
        if (outbound.offer(message)) {
            return true;
        }
        System.err.printf("Closing connection %s because its output queue is full%n", connectionId);
        close();
        return false;
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        try {
            socket.close();
        } catch (IOException ignored) {
            // The connection is already closing.
        }
        interruptOther(readerThread);
        interruptOther(writerThread);
        dispatcher.connectionClosed(this);
        onClosed.run();
    }

    private void interruptOther(Thread thread) {
        if (thread != null && thread != Thread.currentThread()) {
            thread.interrupt();
        }
    }

    private record OutboundMessage(Object message, boolean closeAfterWrite) {

        private OutboundMessage {
            Objects.requireNonNull(message, "message");
        }
    }
}
