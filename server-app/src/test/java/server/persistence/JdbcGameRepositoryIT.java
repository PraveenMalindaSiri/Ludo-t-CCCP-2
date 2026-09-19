package server.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import protocol.SessionStatus;
import server.DatabaseConfig;
import server.application.port.GameRepository.GameResultRecord;
import server.application.port.GameRepository.SessionRecord;
import server.application.port.RepositoryException;

@EnabledIfSystemProperty(named = "mysql.tests.enabled", matches = "true")
class JdbcGameRepositoryIT {

    private static JdbcConnectionFactory connections;
    private static JdbcGameRepository repository;
    private final Set<UUID> sessions = ConcurrentHashMap.newKeySet();

    @BeforeAll
    static void connectToTestSchema() {
        String url = requiredEnvironment("LUDOT_TEST_DB_URL");
        if (!url.matches("(?i).*[/\\:]ludot_test(?:[?;].*)?")) {
            throw new IllegalStateException("LUDOT_TEST_DB_URL must target ludot_test");
        }
        DatabaseConfig config =
                new DatabaseConfig(
                        url,
                        requiredEnvironment("LUDOT_TEST_DB_USER"),
                        System.getenv().getOrDefault("LUDOT_TEST_DB_PASSWORD", ""));
        connections = new JdbcConnectionFactory(config);
        repository = new JdbcGameRepository(connections);
        repository.validateConnection();
    }

    @AfterEach
    void cleanOwnRows() throws SQLException {
        try (Connection connection = connections.open()) {
            for (UUID sessionId : sessions) {
                try (PreparedStatement statement =
                        connection.prepareStatement(
                                "DELETE FROM game_sessions WHERE session_id = ?")) {
                    statement.setString(1, sessionId.toString());
                    statement.executeUpdate();
                }
            }
        }
    }

    @Test
    void insertsSessionAndRejectsDuplicateIdentifier() {
        UUID id = newSessionId();
        SessionRecord created = session(id, "O'Hara's Game", SessionStatus.CREATED);

        repository.createSession(created);

        assertEquals("O'Hara's Game", sessionName(id));
        assertThrows(RepositoryException.class, () -> repository.createSession(created));
    }

    @Test
    void updatesLifecycleStatusAndTimestamps() {
        UUID id = newSessionId();
        repository.createSession(session(id, "Lifecycle", SessionStatus.CREATED));
        Instant started = Instant.parse("2026-09-19T10:01:00Z");
        repository.updateSession(
                new SessionRecord(
                        id,
                        "Lifecycle",
                        17L,
                        SessionStatus.PAUSED,
                        createdAt(),
                        started,
                        null,
                        started.plusSeconds(1)));

        assertEquals(
                "PAUSED",
                scalarString("SELECT status FROM game_sessions WHERE session_id = ?", id));
        assertEquals(
                Timestamp.from(started),
                scalarTimestamp("SELECT started_at FROM game_sessions WHERE session_id = ?", id));
    }

    @Test
    void savesCompletedResultAndStatusInOneTransaction() {
        UUID id = newSessionId();
        repository.createSession(session(id, "Complete", SessionStatus.RUNNING));
        Instant completed = Instant.parse("2026-09-19T10:03:00Z");
        SessionRecord finalSession =
                new SessionRecord(
                        id,
                        "Complete",
                        17L,
                        SessionStatus.COMPLETED,
                        createdAt(),
                        createdAt().plusSeconds(1),
                        completed,
                        completed);

        repository.saveCompletedResult(
                finalSession,
                new GameResultRecord(
                        id, "RED", List.of("RED", "BLUE", "GREEN", "YELLOW"), 42, completed));

        assertEquals(
                "COMPLETED",
                scalarString("SELECT status FROM game_sessions WHERE session_id = ?", id));
        assertEquals(
                "RED", scalarString("SELECT winner FROM game_results WHERE session_id = ?", id));
    }

    @Test
    void rollsBackStatusWhenResultInsertFails() throws SQLException {
        UUID id = newSessionId();
        repository.createSession(session(id, "Rollback", SessionStatus.RUNNING));
        insertExistingResult(id);
        Instant completed = Instant.parse("2026-09-19T10:04:00Z");
        SessionRecord finalSession =
                new SessionRecord(
                        id,
                        "Rollback",
                        17L,
                        SessionStatus.COMPLETED,
                        createdAt(),
                        createdAt().plusSeconds(1),
                        completed,
                        completed);

        assertThrows(
                RepositoryException.class,
                () ->
                        repository.saveCompletedResult(
                                finalSession,
                                new GameResultRecord(id, "RED", List.of("RED"), 8, completed)));
        assertEquals(
                "RUNNING",
                scalarString("SELECT status FROM game_sessions WHERE session_id = ?", id));
    }

    @Test
    void marksAllUnfinishedRowsInterrupted() {
        UUID running = newSessionId();
        UUID paused = newSessionId();
        repository.createSession(session(running, "Running", SessionStatus.RUNNING));
        repository.createSession(session(paused, "Paused", SessionStatus.PAUSED));

        int changed = repository.markUnfinishedSessionsInterrupted(Instant.now());

        assertTrue(changed >= 2);
        assertEquals(
                "INTERRUPTED",
                scalarString("SELECT status FROM game_sessions WHERE session_id = ?", running));
        assertEquals(
                "INTERRUPTED",
                scalarString("SELECT status FROM game_sessions WHERE session_id = ?", paused));
    }

    @Test
    void differentSessionsCanBeWrittenConcurrently() throws Exception {
        UUID first = newSessionId();
        UUID second = newSessionId();
        Thread firstWriter =
                Thread.ofVirtual()
                        .start(
                                () ->
                                        repository.createSession(
                                                session(first, "First", SessionStatus.CREATED)));
        Thread secondWriter =
                Thread.ofVirtual()
                        .start(
                                () ->
                                        repository.createSession(
                                                session(second, "Second", SessionStatus.CREATED)));

        firstWriter.join();
        secondWriter.join();

        assertEquals("First", sessionName(first));
        assertEquals("Second", sessionName(second));
    }

    private UUID newSessionId() {
        UUID id = UUID.randomUUID();
        sessions.add(id);
        return id;
    }

    private static SessionRecord session(UUID id, String name, SessionStatus status) {
        Instant started = status == SessionStatus.CREATED ? null : createdAt().plusSeconds(1);
        return new SessionRecord(id, name, 17L, status, createdAt(), started, null, createdAt());
    }

    private static Instant createdAt() {
        return Instant.parse("2026-09-19T10:00:00Z");
    }

    private static String sessionName(UUID id) {
        return scalarString("SELECT session_name FROM game_sessions WHERE session_id = ?", id);
    }

    private static String scalarString(String sql, UUID id) {
        try (Connection connection = connections.open();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id.toString());
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                return result.getString(1);
            }
        } catch (SQLException exception) {
            throw new AssertionError(exception);
        }
    }

    private static Timestamp scalarTimestamp(String sql, UUID id) {
        try (Connection connection = connections.open();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id.toString());
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                return result.getTimestamp(1);
            }
        } catch (SQLException exception) {
            throw new AssertionError(exception);
        }
    }

    private static void insertExistingResult(UUID id) throws SQLException {
        try (Connection connection = connections.open();
                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                INSERT INTO game_results
                                    (session_id, winner, placements_json, final_round, completed_at)
                                VALUES (
                                    ?, 'BLUE', JSON_OBJECT('placements', JSON_ARRAY('BLUE')), 1, ?
                                )
                                """)) {
            statement.setString(1, id.toString());
            statement.setTimestamp(2, Timestamp.from(createdAt()));
            statement.executeUpdate();
        }
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required by the mysql-it profile");
        }
        return value;
    }
}
