package event;

import java.util.ArrayList;
import java.util.List;
import mystery.MysteryManager;
import piece.Piece;
import player.Player;

/** Immutable, presentation-safe view of the complete game-core state. */
public final class GameSnapshot {
    private final int round;
    private final String currentPlayer;
    private final List<String> finalPlacements;
    private final List<PlayerView> players;
    private final boolean mysteryActive;
    private final int mysteryPosition;
    private final int mysteryRoundsRemaining;

    private GameSnapshot(
            int round,
            String currentPlayer,
            List<String> finalPlacements,
            List<PlayerView> players,
            boolean mysteryActive,
            int mysteryPosition,
            int mysteryRoundsRemaining) {
        this.round = round;
        this.currentPlayer = currentPlayer == null ? "" : currentPlayer;
        this.finalPlacements = List.copyOf(finalPlacements);
        this.players = List.copyOf(players);
        this.mysteryActive = mysteryActive;
        this.mysteryPosition = mysteryPosition;
        this.mysteryRoundsRemaining = mysteryRoundsRemaining;
    }

    public static GameSnapshot from(
            int round, List<Player> players, MysteryManager mysteryManager) {
        return from(round, "", players, mysteryManager, List.of());
    }

    public static GameSnapshot from(
            int round,
            String currentPlayer,
            List<Player> players,
            MysteryManager mysteryManager,
            List<String> finalPlacements) {
        List<PlayerView> views = new ArrayList<>();
        for (Player player : players) views.add(PlayerView.from(player));
        return new GameSnapshot(
                round,
                currentPlayer,
                finalPlacements,
                views,
                mysteryManager.isActive(),
                mysteryManager.getPosition(),
                mysteryManager.getRoundsRemaining());
    }

    public int getRound() {
        return round;
    }

    public String getCurrentPlayer() {
        return currentPlayer;
    }

    public List<String> getFinalPlacements() {
        return finalPlacements;
    }

    public List<PlayerView> getPlayers() {
        return players;
    }

    public boolean isMysteryActive() {
        return mysteryActive;
    }

    public int getMysteryPosition() {
        return mysteryPosition;
    }

    public int getMysteryRoundsRemaining() {
        return mysteryRoundsRemaining;
    }

    public enum PieceArea {
        BASE,
        STANDARD_PATH,
        HOME_STRAIGHT,
        HOME
    }

    public static final class PlayerView {
        private final String color;
        private final int boardCount;
        private final int baseCount;
        private final int homeCount;
        private final List<PieceView> pieces;

        private PlayerView(
                String color,
                int boardCount,
                int baseCount,
                int homeCount,
                List<PieceView> pieces) {
            this.color = color;
            this.boardCount = boardCount;
            this.baseCount = baseCount;
            this.homeCount = homeCount;
            this.pieces = List.copyOf(pieces);
        }

        private static PlayerView from(Player player) {
            List<PieceView> pieces = new ArrayList<>();
            for (Piece piece : player.getPieces()) pieces.add(PieceView.from(piece));
            return new PlayerView(
                    player.getColor(),
                    player.getPiecesOnBoard().size(),
                    player.getPiecesInBase().size(),
                    player.getPiecesAtHome().size(),
                    pieces);
        }

        public String getColor() {
            return color;
        }

        public int getBoardCount() {
            return boardCount;
        }

        public int getBaseCount() {
            return baseCount;
        }

        public int getHomeCount() {
            return homeCount;
        }

        public List<PieceView> getPieces() {
            return pieces;
        }
    }

    public static final class PieceView {
        private final String pieceId;
        private final String name;
        private final String fullName;
        private final String color;
        private final PieceArea area;
        private final int standardPosition;
        private final int homeStraightIndex;
        private final String direction;
        private final String stateLabel;
        private final int stateRoundsRemaining;
        private final boolean inBlock;
        private final String blockId;
        private final String position;

        private PieceView(
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
            this.pieceId = pieceId;
            this.name = name;
            this.fullName = fullName;
            this.color = color;
            this.area = area;
            this.standardPosition = standardPosition;
            this.homeStraightIndex = homeStraightIndex;
            this.direction = direction;
            this.stateLabel = stateLabel;
            this.stateRoundsRemaining = stateRoundsRemaining;
            this.inBlock = inBlock;
            this.blockId = blockId;
            this.position = position;
        }

        private static PieceView from(Piece piece) {
            PieceArea area;
            int standardPosition = -1;
            int homeStraightIndex = -1;
            if (piece.isInBase()) {
                area = PieceArea.BASE;
            } else if (piece.isAtHome()) {
                area = PieceArea.HOME;
            } else if (piece.isInHomeStraight()) {
                area = PieceArea.HOME_STRAIGHT;
                homeStraightIndex = piece.getHomeStraightIndex();
            } else {
                area = PieceArea.STANDARD_PATH;
                standardPosition = piece.getPosition();
            }
            String blockId =
                    piece.isInBlock()
                            ? piece.getColor().toUpperCase() + ":" + piece.positionLabel()
                            : "";
            return new PieceView(
                    piece.getFullName(),
                    piece.getName(),
                    piece.getFullName(),
                    piece.getColor(),
                    area,
                    standardPosition,
                    homeStraightIndex,
                    piece.getDirection(),
                    piece.getState().getDisplayName(),
                    piece.getState().getRoundsRemaining(),
                    piece.isInBlock(),
                    blockId,
                    piece.positionLabel());
        }

        public String getPieceId() {
            return pieceId;
        }

        public String getName() {
            return name;
        }

        public String getFullName() {
            return fullName;
        }

        public String getColor() {
            return color;
        }

        public PieceArea getArea() {
            return area;
        }

        public int getStandardPosition() {
            return standardPosition;
        }

        public int getHomeStraightIndex() {
            return homeStraightIndex;
        }

        public String getDirection() {
            return direction;
        }

        public String getStateLabel() {
            return stateLabel;
        }

        public int getStateRoundsRemaining() {
            return stateRoundsRemaining;
        }

        public boolean isInBlock() {
            return inBlock;
        }

        public String getBlockId() {
            return blockId;
        }

        public String getPosition() {
            return position;
        }
    }
}
