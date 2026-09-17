package event;

import java.util.ArrayList;
import java.util.List;
import mystery.MysteryManager;
import piece.Piece;
import player.Player;

/** Immutable view of game state for observers, GUI and server responses. */
public final class GameSnapshot {
    private final int round;
    private final List<PlayerView> players;
    private final boolean mysteryActive;
    private final int mysteryPosition;
    private final int mysteryRoundsRemaining;

    private GameSnapshot(
            int round,
            List<PlayerView> players,
            boolean mysteryActive,
            int mysteryPosition,
            int mysteryRoundsRemaining) {
        this.round = round;
        this.players = List.copyOf(players);
        this.mysteryActive = mysteryActive;
        this.mysteryPosition = mysteryPosition;
        this.mysteryRoundsRemaining = mysteryRoundsRemaining;
    }

    public static GameSnapshot from(
            int round, List<Player> players, MysteryManager mysteryManager) {
        List<PlayerView> views = new ArrayList<>();
        for (Player player : players) views.add(PlayerView.from(player));
        return new GameSnapshot(
                round,
                views,
                mysteryManager.isActive(),
                mysteryManager.getPosition(),
                mysteryManager.getRoundsRemaining());
    }

    public int getRound() {
        return round;
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

    public static final class PlayerView {
        private final String color;
        private final int boardCount;
        private final int baseCount;
        private final List<PieceView> pieces;

        private PlayerView(String color, int boardCount, int baseCount, List<PieceView> pieces) {
            this.color = color;
            this.boardCount = boardCount;
            this.baseCount = baseCount;
            this.pieces = List.copyOf(pieces);
        }

        private static PlayerView from(Player player) {
            List<PieceView> pieces = new ArrayList<>();
            for (Piece piece : player.getPieces()) {
                pieces.add(
                        new PieceView(piece.getName(), piece.getFullName(), piece.positionLabel()));
            }
            return new PlayerView(
                    player.getColor(),
                    player.getPiecesOnBoard().size(),
                    player.getPiecesInBase().size(),
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

        public List<PieceView> getPieces() {
            return pieces;
        }
    }

    public static final class PieceView {
        private final String name;
        private final String fullName;
        private final String position;

        private PieceView(String name, String fullName, String position) {
            this.name = name;
            this.fullName = fullName;
            this.position = position;
        }

        public String getName() {
            return name;
        }

        public String getFullName() {
            return fullName;
        }

        public String getPosition() {
            return position;
        }
    }
}
