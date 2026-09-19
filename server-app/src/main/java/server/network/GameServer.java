package server.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import server.ServerConfig;
import server.application.GameService;

/** Accepts independent persistent client connections for the standalone server process. */
public final class GameServer implements AutoCloseable {

    private final ServerConfig config;
    private final RequestDispatcher dispatcher;
    private final Map<UUID, ClientConnection> connections = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean();
    private final CountDownLatch terminated = new CountDownLatch(1);
    private volatile ServerSocket serverSocket;
    private volatile Thread acceptThread;

    public GameServer(ServerConfig config) {
        this(config, new RequestDispatcher(new GameService(config)));
    }

    GameServer(ServerConfig config, RequestDispatcher dispatcher) {
        this.config = config;
        this.dispatcher = dispatcher;
    }

    public synchronized void start() throws IOException {
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("Server already started");
        }
        ServerSocket socket = new ServerSocket();
        socket.setReuseAddress(true);
        socket.bind(new InetSocketAddress(config.host(), config.port()));
        serverSocket = socket;
        acceptThread = Thread.ofVirtual().name("server-accept").start(this::acceptLoop);
    }

    public int port() {
        ServerSocket socket = serverSocket;
        if (socket == null) {
            throw new IllegalStateException("Server has not started");
        }
        return socket.getLocalPort();
    }

    public int connectionCount() {
        return connections.size();
    }

    public boolean acceptThreadIsVirtual() {
        Thread thread = acceptThread;
        return thread != null && thread.isVirtual();
    }

    public boolean allConnectionThreadsAreVirtual() {
        return connections.values().stream()
                .allMatch(ClientConnection::networkingThreadsAreVirtual);
    }

    public void awaitTermination() throws InterruptedException {
        terminated.await();
    }

    private void acceptLoop() {
        try {
            while (running.get()) {
                Socket socket = serverSocket.accept();
                socket.setTcpNoDelay(true);
                ClientConnection[] reference = new ClientConnection[1];
                ClientConnection connection =
                        new ClientConnection(
                                socket,
                                config,
                                dispatcher,
                                () -> connections.remove(reference[0].connectionId()));
                reference[0] = connection;
                connections.put(connection.connectionId(), connection);
                System.out.printf(
                        "Accepted connection %s from %s%n",
                        connection.connectionId(), socket.getRemoteSocketAddress());
                connection.start();
            }
        } catch (SocketException exception) {
            if (running.get()) {
                System.err.println("Server socket failed: " + exception.getMessage());
            }
        } catch (IOException exception) {
            if (running.get()) {
                System.err.println("Unable to accept client: " + exception.getMessage());
            }
        } finally {
            close();
        }
    }

    @Override
    public void close() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        try {
            serverSocket.close();
        } catch (IOException ignored) {
            // The server socket is already closing.
        }
        connections.values().forEach(ClientConnection::close);
        connections.clear();
        dispatcher.close();
        Thread thread = acceptThread;
        if (thread != null && thread != Thread.currentThread()) {
            thread.interrupt();
        }
        terminated.countDown();
    }
}
