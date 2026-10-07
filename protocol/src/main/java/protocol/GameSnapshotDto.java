package protocol;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Immutable, transport-safe view from which a client can rebuild one complete game session. */
public record GameSnapshotDto(
        UUID sessionId,
        long version,
        int round,
        SessionStatus status,
        List<PlayerDto> players,
        MysteryDto mystery,
        String currentPlayer,
        long turn,
        String lastAction,
        long turnDelayMillis,
        Integer lastDice,
        String lastResult,
        List<String> finalPlacements,
        int queueDepth) {

    public GameSnapshotDto {
        Objects.requireNonNull(sessionId, "sessionId");
        if (version < 0 || round < 0 || turn < 0 || turnDelayMillis < 0 || queueDepth < 0) {
            throw new IllegalArgumentException("Snapshot counters must not be negative");
        }
        Objects.requireNonNull(status, "status");
        players = players == null ? List.of() : List.copyOf(players);
        Objects.requireNonNull(mystery, "mystery");
        currentPlayer = currentPlayer == null ? "" : currentPlayer;
        lastAction = lastAction == null ? "" : lastAction;
        lastResult = lastResult == null ? "" : lastResult;
        finalPlacements = finalPlacements == null ? List.of() : List.copyOf(finalPlacements);
    }

    /** Immutable player view with complete piece counts. */
    public record PlayerDto(
            String color, int boardCount, int baseCount, int homeCount, List<PieceDto> pieces) {

        public PlayerDto {
            requireText(color, "color");
            if (boardCount < 0 || baseCount < 0 || homeCount < 0) {
                throw new IllegalArgumentException("Piece counts must not be negative");
            }
            pieces = pieces == null ? List.of() : List.copyOf(pieces);
        }
    }

    /** The board area occupied by a piece. */
    public enum PieceArea {
        BASE,
        STANDARD_PATH,
        HOME_STRAIGHT,
        HOME
    }

    /** Immutable piece view; no mutable game-core object crosses the protocol boundary. */
    public record PieceDto(
            String pieceId,
            String name,
            String fullName,
            String color,
            PieceArea area,
            int standardPosition,
            int homeStraightIndex,
            String direction,
            String stateLabel,
            int stateRoundsRemaining,
            boolean inBlock,
            String blockId,
            String position) {

        public PieceDto {
            requireText(pieceId, "pieceId");
            requireText(name, "name");
            requireText(fullName, "fullName");
            requireText(color, "color");
            Objects.requireNonNull(area, "area");
            if (standardPosition < -1 || homeStraightIndex < -1 || stateRoundsRemaining < 0) {
                throw new IllegalArgumentException("Piece indexes and duration are invalid");
            }
            direction = direction == null ? "" : direction;
            stateLabel = stateLabel == null || stateLabel.isBlank() ? "NORMAL" : stateLabel;
            blockId = blockId == null ? "" : blockId;
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
