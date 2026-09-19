package server.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;
import server.DatabaseConfig;
import server.application.port.RepositoryException;

/** Opens short-lived MySQL connections; callers own them through try-with-resources. */
public final class JdbcConnectionFactory {

    private final DatabaseConfig config;

    public JdbcConnectionFactory(DatabaseConfig config) {
        this.config = Objects.requireNonNull(config, "config");
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException exception) {
            throw new RepositoryException("MySQL Connector/J is not available", exception);
        }
    }

    Connection open() throws SQLException {
        return DriverManager.getConnection(config.url(), config.user(), config.password());
    }
}
