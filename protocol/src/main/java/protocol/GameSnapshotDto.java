package protocol;

import java.util.List;
import java.util.Locale;
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

    /** Compatibility constructor retained for Work 02/03 callers. */
    public GameSnapshotDto(
            UUID sessionId,
            long version,
            int round,
            SessionStatus status,
            List<PlayerDto> players,
            MysteryDto mystery,
            String currentPlayer,
            long turn,
            String lastAction) {
        this(
                sessionId,
                version,
                round,
                status,
                players,
                mystery,
                currentPlayer,
                turn,
                lastAction,
                0,
                null,
                "",
                List.of(),
                0);
    }

    /** Compatibility constructor for snapshots created before turn metadata was available. */
    public GameSnapshotDto(
            UUID sessionId,
            long version,
            int round,
            SessionStatus status,
            List<PlayerDto> players,
            MysteryDto mystery) {
        this(sessionId, version, round, status, players, mystery, "", 0, "");
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

        /** Compatibility constructor retained for Work 02/03 callers. */
        public PlayerDto(String color, int boardCount, int baseCount, List<PieceDto> pieces) {
            this(color, boardCount, baseCount, 0, pieces);
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

        /** Compatibility constructor retained for the earlier string-position DTO. */
        public PieceDto(String name, String fullName, String position) {
            this(
                    name,
                    name,
                    fullName,
                    colorFrom(name, fullName),
                    areaFrom(position),
                    standardPositionFrom(position),
                    homeStraightIndexFrom(position),
                    "",
                    "NORMAL",
                    0,
                    false,
                    "",
                    position);
        }

        private static String colorFrom(String name, String fullName) {
            String candidate =
                    name != null && !name.isBlank() ? name : fullName == null ? "" : fullName;
            if (candidate.isBlank()) return "UNKNOWN";
            return switch (Character.toUpperCase(candidate.charAt(0))) {
                case 'R' -> "RED";
                case 'G' -> "GREEN";
                case 'Y' -> "YELLOW";
                case 'B' -> "BLUE";
                default -> "UNKNOWN";
            };
        }

        private static PieceArea areaFrom(String position) {
            String value = normalize(position);
            if (value.equals("HOME")) return PieceArea.HOME;
            if (value.contains("HOMEPATH") || value.startsWith("HOME_STRAIGHT_")) {
                return PieceArea.HOME_STRAIGHT;
            }
            if (value.matches("\\d+") || value.startsWith("CELL_")) {
                return PieceArea.STANDARD_PATH;
            }
            return PieceArea.BASE;
        }

        private static int standardPositionFrom(String position) {
            String value = normalize(position);
            try {
                if (value.startsWith("CELL_")) return Integer.parseInt(value.substring(5));
                if (value.matches("\\d+")) return Integer.parseInt(value);
            } catch (NumberFormatException ignored) {
                // Compatibility data remains renderable as a base position.
            }
            return -1;
        }

        private static int homeStraightIndexFrom(String position) {
            String value = normalize(position);
            int marker = value.lastIndexOf("HOMEPATH");
            String suffix = marker >= 0 ? value.substring(marker + 8) : "";
            if (value.startsWith("HOME_STRAIGHT_")) suffix = value.substring(14);
            try {
                return suffix.isEmpty() ? -1 : Integer.parseInt(suffix);
            } catch (NumberFormatException ignored) {
                return -1;
            }
        }

        private static String normalize(String value) {
            return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
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
