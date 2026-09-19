package server;

import server.network.GameServer;

/** Starts the standalone TCP server process. */
public final class ServerMain {

    private ServerMain() {}

    public static void main(String[] args) throws Exception {
        ServerConfig config = ServerConfig.load();
        GameServer server = new GameServer(config);
        Runtime.getRuntime().addShutdownHook(new Thread(server::close, "server-shutdown"));
        server.start();
        System.out.printf("LUDO-T server listening on %s:%d%n", config.host(), server.port());
        server.awaitTermination();
    }
}
