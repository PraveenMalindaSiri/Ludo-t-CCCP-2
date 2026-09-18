package protocol;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Immutable, transport-safe view of one complete game session. */
public record GameSnapshotDto(
        UUID sessionId,
        long version,
        int round,
        SessionStatus status,
        List<PlayerDto> players,
        MysteryDto mystery) {

    public GameSnapshotDto {
        Objects.requireNonNull(sessionId, "sessionId");
        if (version < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
        if (round < 0) {
            throw new IllegalArgumentException("round must not be negative");
        }
        Objects.requireNonNull(status, "status");
        players = players == null ? List.of() : List.copyOf(players);
        Objects.requireNonNull(mystery, "mystery");
    }

    /** Immutable player view nested to keep the protocol surface compact. */
    public record PlayerDto(
            String color, int boardCount, int baseCount, List<PieceDto> pieces) {

        public PlayerDto {
            requireText(color, "color");
            if (boardCount < 0 || baseCount < 0) {
                throw new IllegalArgumentException("piece counts must not be negative");
            }
            pieces = pieces == null ? List.of() : List.copyOf(pieces);
        }
    }

    /** Immutable piece view required by the future board UI. */
    public record PieceDto(String name, String fullName, String position) {

        public PieceDto {
            requireText(name, "name");
            requireText(fullName, "fullName");
            requireText(position, "position");
        }
    }

    /** Current mystery-cell state. */
    public record MysteryDto(boolean active, int position, int roundsRemaining) {

        public MysteryDto {
            if (roundsRemaining < 0) {
                throw new IllegalArgumentException("roundsRemaining must not be negative");
            }
        }
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
