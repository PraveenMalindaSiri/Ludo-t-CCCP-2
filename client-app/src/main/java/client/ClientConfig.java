package client;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

/** Immutable client settings loaded from client.properties. */
public record ClientConfig(
        String host,
        int port,
        int outboundQueueCapacity,
        int maxLineLength,
        int connectTimeoutMillis,
        int requestTimeoutMillis,
        int eventHistoryLimit) {

    private static final String RESOURCE = "/client.properties";

    public ClientConfig {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("host must not be blank");
        }
        if (port < 1 || port > 65_535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }
        if (outboundQueueCapacity <= 0
                || maxLineLength <= 0
                || connectTimeoutMillis <= 0
                || requestTimeoutMillis <= 0
                || eventHistoryLimit <= 0) {
            throw new IllegalArgumentException("Client limits and timeouts must be positive");
        }
    }

    public static ClientConfig load() {
        Properties properties = new Properties();
        try (InputStream input = ClientConfig.class.getResourceAsStream(RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("Missing " + RESOURCE);
            }
            properties.load(input);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load " + RESOURCE, exception);
        }

        return new ClientConfig(
                setting(properties, "client.host"),
                integerSetting(properties, "client.port"),
                integerSetting(properties, "client.outboundQueueCapacity"),
                integerSetting(properties, "client.maxLineLength"),
                integerSetting(properties, "client.connectTimeoutMillis"),
                integerSetting(properties, "client.requestTimeoutMillis"),
                integerSetting(properties, "client.eventHistoryLimit"));
    }

    private static String setting(Properties properties, String key) {
        return Objects.requireNonNullElse(System.getProperty(key), properties.getProperty(key));
    }

    private static int integerSetting(Properties properties, String key) {
        try {
            return Integer.parseInt(setting(properties, key));
        } catch (NumberFormatException exception) {
            throw new IllegalStateException(key + " must be an integer", exception);
        }
    }
}
