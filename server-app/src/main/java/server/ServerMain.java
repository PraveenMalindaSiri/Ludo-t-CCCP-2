package server;

import java.time.Instant;
import server.application.port.GameRepository;
import server.network.GameServer;
import server.persistence.JdbcConnectionFactory;
import server.persistence.JdbcGameRepository;

/** Starts the standalone TCP server process. */
public final class ServerMain {

    private ServerMain() {}

    public static void main(String[] args) throws Exception {
        ServerConfig config = ServerConfig.load();
        DatabaseConfig databaseConfig = DatabaseConfig.load();
        GameRepository repository =
                new JdbcGameRepository(new JdbcConnectionFactory(databaseConfig));
        repository.validateConnection();
        int interrupted = repository.markUnfinishedSessionsInterrupted(Instant.now());
        System.out.printf("MySQL ready: %s%n", databaseConfig);
        if (interrupted > 0) {
            System.out.printf("Marked %d unfinished session(s) INTERRUPTED%n", interrupted);
        }

        GameServer server = new GameServer(config, repository);
        Runtime.getRuntime().addShutdownHook(new Thread(server::close, "server-shutdown"));
        try {
            server.start();
            System.out.printf("LUDO-T server listening on %s:%d%n", config.host(), server.port());
            server.awaitTermination();
        } finally {
            server.close();
        }
    }
}
