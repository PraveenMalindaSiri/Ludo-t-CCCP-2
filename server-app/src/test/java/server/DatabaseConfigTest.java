package server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DatabaseConfigTest {

    @Test
    void validatesUrlAndUser() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new DatabaseConfig("jdbc:postgresql://localhost/ludot", "user", "secret"));
        assertThrows(
                IllegalArgumentException.class,
                () -> new DatabaseConfig("jdbc:mysql://localhost/ludot", " ", "secret"));
    }

    @Test
    void diagnosticsNeverExposePassword() {
        DatabaseConfig config =
                new DatabaseConfig("jdbc:mysql://localhost/ludot", "ludot_app", "top-secret");

        assertTrue(config.toString().contains("<redacted>"));
        assertFalse(config.toString().contains("top-secret"));
    }
}
