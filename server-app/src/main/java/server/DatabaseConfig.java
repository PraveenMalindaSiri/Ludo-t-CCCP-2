package server;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

/** Immutable MySQL settings with environment overrides and redacted diagnostics. */
public record DatabaseConfig(String url, String user, String password) {

    private static final String RESOURCE = "/server.properties";

    public DatabaseConfig {
        if (url == null || !url.startsWith("jdbc:mysql://")) {
            throw new IllegalArgumentException("database URL must start with jdbc:mysql://");
        }
        if (user == null || user.isBlank()) {
            throw new IllegalArgumentException("database user must not be blank");
        }
        password = Objects.requireNonNull(password, "database password must not be null");
    }

    public static DatabaseConfig load() {
        Properties properties = new Properties();
        try (InputStream input = DatabaseConfig.class.getResourceAsStream(RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("Missing " + RESOURCE);
            }
            properties.load(input);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load " + RESOURCE, exception);
        }

        return new DatabaseConfig(
                setting(properties, "database.url", "LUDOT_DB_URL"),
                setting(properties, "database.user", "LUDOT_DB_USER"),
                setting(properties, "database.password", "LUDOT_DB_PASSWORD"));
    }

    private static String setting(Properties properties, String key, String environmentKey) {
        String systemValue = System.getProperty(key);
        if (systemValue != null) {
            return systemValue;
        }
        String environmentValue = System.getenv(environmentKey);
        return environmentValue != null ? environmentValue : properties.getProperty(key);
    }

    @Override
    public String toString() {
        return "DatabaseConfig[url=" + url + ", user=" + user + ", password=<redacted>]";
    }
}
