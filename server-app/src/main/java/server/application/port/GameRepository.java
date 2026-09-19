package server.application.port;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import protocol.SessionStatus;

/** Application-facing persistence contract; no JDBC type crosses this boundary. */
public interface GameRepository extends AutoCloseable {

    boolean isEnabled();

    void validateConnection();

    void createSession(SessionRecord session);

    void updateSession(SessionRecord session);

    void saveCompletedResult(SessionRecord session, GameResultRecord result);

    int markUnfinishedSessionsInterrupted(Instant interruptedAt);

    @Override
    void close();

    /**
     * Explicitly disabled adapter retained for isolated existing tests and compatibility callers.
     */
    static GameRepository disabled() {
        return DisabledRepository.INSTANCE;
    }

    record SessionRecord(
            UUID sessionId,
            String name,
            long randomSeed,
            SessionStatus status,
            Instant createdAt,
            Instant startedAt,
            Instant completedAt,
            Instant updatedAt) {

        public SessionRecord {
            Objects.requireNonNull(sessionId, "sessionId");
            name = requireText(name, "name");
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(createdAt, "createdAt");
            Objects.requireNonNull(updatedAt, "updatedAt");
        }
    }

    record GameResultRecord(
            UUID sessionId,
            String winner,
            List<String> placements,
            int finalRound,
            Instant completedAt) {

        public GameResultRecord {
            Objects.requireNonNull(sessionId, "sessionId");
            winner = requireText(winner, "winner");
            placements = placements == null ? List.of() : List.copyOf(placements);
            if (placements.isEmpty() || finalRound < 0) {
                throw new IllegalArgumentException(
                        "Completed result requires placements and round");
            }
            Objects.requireNonNull(completedAt, "completedAt");
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    enum DisabledRepository implements GameRepository {
        INSTANCE;

        @Override
        public boolean isEnabled() {
            return false;
        }

        @Override
        public void validateConnection() {}

        @Override
        public void createSession(SessionRecord session) {}

        @Override
        public void updateSession(SessionRecord session) {}

        @Override
        public void saveCompletedResult(SessionRecord session, GameResultRecord result) {}

        @Override
        public int markUnfinishedSessionsInterrupted(Instant interruptedAt) {
            return 0;
        }

        @Override
        public void close() {}
    }
}
