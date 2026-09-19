package server;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;
import protocol.JsonLineCodec;

/** Immutable connection-level server settings loaded from server.properties. */
public record ServerConfig(
        String host, int port, int outboundQueueCapacity, int maxLineLength) {

    private static final String RESOURCE = "/server.properties";

    public ServerConfig {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("host must not be blank");
        }
        if (port < 0 || port > 65_535) {
            throw new IllegalArgumentException("port must be between 0 and 65535");
        }
        if (outboundQueueCapacity <= 0) {
            throw new IllegalArgumentException("outboundQueueCapacity must be positive");
        }
        if (maxLineLength <= 0) {
            throw new IllegalArgumentException("maxLineLength must be positive");
        }
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
                integerSetting(properties, "server.maxLineLength"));
    }

    public static ServerConfig defaultsForPort(int port) {
        return new ServerConfig(
                "127.0.0.1", port, 256, JsonLineCodec.DEFAULT_MAX_LINE_LENGTH);
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
}
