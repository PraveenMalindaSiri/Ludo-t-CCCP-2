package server;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;
import protocol.JsonLineCodec;

/** Immutable connection-level server settings loaded from server.properties. */
public record ServerConfig(
        String host,
        int port,
        int outboundQueueCapacity,
        int maxLineLength,
        int sessionQueueCapacity,
        long defaultTurnDelayMillis) {

    private static final String RESOURCE = "/server.properties";

    public ServerConfig {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("host must not be blank");
        }
        if (port < 0 || port > 65_535) {
            throw new IllegalArgumentException("port must be between 0 and 65535");
        }
        if (outboundQueueCapacity <= 0
                || maxLineLength <= 0
                || sessionQueueCapacity <= 0
                || defaultTurnDelayMillis <= 0) {
            throw new IllegalArgumentException("Server limits and delays must be positive");
        }
    }

    /** Compatibility constructor retained for connection-focused tests and callers. */
    public ServerConfig(String host, int port, int outboundQueueCapacity, int maxLineLength) {
        this(host, port, outboundQueueCapacity, maxLineLength, 64, 500);
    }

    public static ServerConfig load() {
        Properties properties = new Properties();
        try (InputStream input = ServerConfig.class.getResourceAsStream(RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("Missing " + RESOURCE);
            }
            properties.load(input);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load " + RESOURCE, exception);
        }

        return new ServerConfig(
                setting(properties, "server.host"),
                integerSetting(properties, "server.port"),
                integerSetting(properties, "server.outboundQueueCapacity"),
                integerSetting(properties, "server.maxLineLength"),
                integerSetting(properties, "server.sessionQueueCapacity"),
                longSetting(properties, "server.defaultTurnDelayMillis"));
    }

    public static ServerConfig defaultsForPort(int port) {
        return new ServerConfig(
                "127.0.0.1", port, 256, JsonLineCodec.DEFAULT_MAX_LINE_LENGTH, 64, 500);
    }

    private static String setting(Properties properties, String key) {
        String systemValue = System.getProperty(key);
        return Objects.requireNonNullElse(systemValue, properties.getProperty(key));
    }

    private static int integerSetting(Properties properties, String key) {
        String value = setting(properties, key);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IllegalStateException(key + " must be an integer", exception);
        }
    }

    private static long longSetting(Properties properties, String key) {
        String value = setting(properties, key);
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw new IllegalStateException(key + " must be a long", exception);
        }
    }
}
