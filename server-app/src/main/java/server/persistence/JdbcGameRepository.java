package server.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import protocol.JsonLineCodec;
import protocol.ProtocolException;
import protocol.SessionStatus;
import server.application.port.GameRepository;
import server.application.port.RepositoryException;

/** Plain JDBC MySQL adapter for durable LUDO-T history and results. */
public final class JdbcGameRepository implements GameRepository {

    private static final String INSERT_SESSION =
            """
            INSERT INTO game_sessions
                (session_id, session_name, random_seed, status, created_at, started_at,
                 completed_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String UPDATE_SESSION =
            """
            UPDATE game_sessions
            SET session_name = ?, status = ?, started_at = ?, completed_at = ?, updated_at = ?
            WHERE session_id = ?
            """;

    private static final String INSERT_RESULT =
            """
            INSERT INTO game_results
                (session_id, winner, placements_json, final_round, completed_at)
            VALUES (?, ?, ?, ?, ?)
            """;

    private static final String INTERRUPT_UNFINISHED =
            """
            UPDATE game_sessions
            SET status = 'INTERRUPTED', completed_at = ?, updated_at = ?
            WHERE status IN ('CREATED', 'RUNNING', 'PAUSED')
            """;

    private final JdbcConnectionFactory connections;
    private final JsonLineCodec json = new JsonLineCodec();

    public JdbcGameRepository(JdbcConnectionFactory connections) {
        this.connections = Objects.requireNonNull(connections, "connections");
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void validateConnection() {
        try (Connection connection = connections.open();
                PreparedStatement statement = connection.prepareStatement("SELECT 1");
                ResultSet result = statement.executeQuery()) {
            if (!result.next() || result.getInt(1) != 1) {
                throw new RepositoryException("MySQL validation query returned no result");
            }
        } catch (SQLException exception) {
            throw failure("Unable to validate MySQL connection", exception);
        }
    }

    @Override
    public void createSession(SessionRecord session) {
        Objects.requireNonNull(session, "session");
        try (Connection connection = connections.open();
                PreparedStatement statement = connection.prepareStatement(INSERT_SESSION)) {
            statement.setString(1, session.sessionId().toString());
            statement.setString(2, session.name());
            statement.setLong(3, session.randomSeed());
            statement.setString(4, statusValue(session.status()));
            setTimestamp(statement, 5, session.createdAt());
            setTimestamp(statement, 6, session.startedAt());
            setTimestamp(statement, 7, session.completedAt());
            setTimestamp(statement, 8, session.updatedAt());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw failure("Unable to create session " + session.sessionId(), exception);
        }
    }

    @Override
    public void updateSession(SessionRecord session) {
        Objects.requireNonNull(session, "session");
        try (Connection connection = connections.open()) {
            requireUpdated(updateSession(connection, session), session.sessionId());
        } catch (SQLException exception) {
            throw failure("Unable to update session " + session.sessionId(), exception);
        }
    }

    @Override
    public void saveCompletedResult(SessionRecord session, GameResultRecord result) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(result, "result");
        if (session.status() != SessionStatus.COMPLETED
                || !session.sessionId().equals(result.sessionId())) {
            throw new IllegalArgumentException(
                    "Completed session and result must have the same identifier");
        }

        try (Connection connection = connections.open()) {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                requireUpdated(updateSession(connection, session), session.sessionId());
                try (PreparedStatement statement = connection.prepareStatement(INSERT_RESULT)) {
                    statement.setString(1, result.sessionId().toString());
                    statement.setString(2, winnerValue(result));
                    statement.setString(3, placementsJson(result.placements()));
                    statement.setInt(4, result.finalRound());
                    setTimestamp(statement, 5, result.completedAt());
                    statement.executeUpdate();
                }
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException exception) {
            throw failure("Unable to save completed result " + result.sessionId(), exception);
        }
    }

    @Override
    public int markUnfinishedSessionsInterrupted(Instant interruptedAt) {
        Objects.requireNonNull(interruptedAt, "interruptedAt");
        try (Connection connection = connections.open();
                PreparedStatement statement = connection.prepareStatement(INTERRUPT_UNFINISHED)) {
            setTimestamp(statement, 1, interruptedAt);
            setTimestamp(statement, 2, interruptedAt);
            return statement.executeUpdate();
        } catch (SQLException exception) {
            throw failure("Unable to mark unfinished sessions interrupted", exception);
        }
    }

    static String statusValue(SessionStatus status) {
        return Objects.requireNonNull(status, "status").name();
    }

    static String winnerValue(GameResultRecord result) {
        Objects.requireNonNull(result, "result");
        if (!result.winner().equals(result.placements().getFirst())) {
            throw new IllegalArgumentException("winner must be the first final placement");
        }
        return result.winner();
    }

    private int updateSession(Connection connection, SessionRecord session) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_SESSION)) {
            statement.setString(1, session.name());
            statement.setString(2, statusValue(session.status()));
            setTimestamp(statement, 3, session.startedAt());
            setTimestamp(statement, 4, session.completedAt());
            setTimestamp(statement, 5, session.updatedAt());
            statement.setString(6, session.sessionId().toString());
            return statement.executeUpdate();
        }
    }

    private String placementsJson(List<String> placements) {
        try {
            return json.encode(Map.of("placements", placements));
        } catch (ProtocolException exception) {
            throw new RepositoryException("Unable to encode final placements", exception);
        }
    }

    private static void requireUpdated(int count, UUID sessionId) {
        if (count != 1) {
            throw new RepositoryException("Session row not found: " + sessionId);
        }
    }

    private static void setTimestamp(PreparedStatement statement, int index, Instant value)
            throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.TIMESTAMP);
        } else {
            statement.setTimestamp(index, Timestamp.from(value));
        }
    }

    private static void rollback(Connection connection, Throwable original) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            original.addSuppressed(rollbackFailure);
        }
    }

    private static RepositoryException failure(String message, SQLException cause) {
        return new RepositoryException(message + ": " + cause.getMessage(), cause);
    }

    @Override
    public void close() {
        // Every operation owns and closes its JDBC connection; there is no pool to shut down.
    }
}
