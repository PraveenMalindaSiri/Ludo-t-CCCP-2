package event;

import config.GameConfig;
import java.util.List;
import mystery.MysteryOutcome;
import piece.Piece;

public class GameLogger implements IGameEventListener {

    // helper
    private String capitalize(String color) {
        if (color == null || color.isEmpty()) return color;
        return color.charAt(0) + color.substring(1).toLowerCase();
    }

    private String formatPosition(int pos, String color) {
        if (pos == Piece.HOME_POSITION) return "Home";
        if (pos == Piece.BASE_POSITION) return "Base";
        if (pos >= Piece.HOME_STRAIGHT_OFFSET
                && pos
                        < Piece.HOME_STRAIGHT_OFFSET
                                + GameConfig.getInstance().getHomePathLength()) {
            return color.toLowerCase() + "homepath" + (pos - Piece.HOME_STRAIGHT_OFFSET);
        }
        return String.valueOf(pos);
    }

    private String formatDirection(String direction) {
        if ("COUNTERCLOCKWISE".equals(direction)) {
            return "counter-clockwise";
        }

        if ("CLOCKWISE".equals(direction)) {
            return "clockwise";
        }

        return direction == null ? "" : direction.toLowerCase();
    }

    private String ordinal(int number) {
        return switch (number) {
            case 1 -> "1st";
            case 2 -> "2nd";
            case 3 -> "3rd";
            default -> number + "th";
        };
    }

    // Player info ----------------------------------------------------------------

    @Override
    public void onPlayerInfo(String color, List<String> pieceNames) {
        StringBuilder sb = new StringBuilder();
        sb.append("The ").append(color.toLowerCase()).append(" player has four (04) pieces named ");
        for (int i = 0; i < pieceNames.size(); i++) {
            sb.append(pieceNames.get(i));
            if (i < pieceNames.size() - 2) sb.append(", ");
            else if (i == pieceNames.size() - 2) sb.append(", and ");
        }
        sb.append(".");
        System.out.println(sb);
    }

    // Dice info ----------------------------------------------------------------

    // first roll info
    @Override
    public void onInitialRoll(String color, int value) {
        System.out.println(capitalize(color) + " rolls " + value);
    }

    // In-game roll
    @Override
    public void onDiceRolled(String color, int value) {
        System.out.println(capitalize(color) + " player rolled " + value + ".");
    }

    // Turn info ----------------------------------------------------------------

    @Override
    public void onFirstPlayer(String color) {
        System.out.println(
                capitalize(color) + " player has the highest roll and will begin the game.");
    }

    @Override
    public void onTurnOrder(List<String> colors) {
        StringBuilder sb = new StringBuilder("The order of a single round is ");
        for (int i = 0; i < colors.size(); i++) {
            sb.append(capitalize(colors.get(i)));
            if (i < colors.size() - 2) sb.append(", ");
            else if (i == colors.size() - 2) sb.append(", and ");
        }
        sb.append(".");
        System.out.println(sb);
    }

    // Movement info ----------------------------------------------------------------

    @Override
    public void onPieceEnteredBoard(String color, String pieceName, int boardCount, int baseCount) {
        System.out.println(
                capitalize(color) + " player moves piece " + pieceName + " to the starting point.");
        System.out.println(
                capitalize(color)
                        + " player now has "
                        + boardCount
                        + "/"
                        + GameConfig.getInstance().getPiecesPerPlayer()
                        + " on pieces on the board and "
                        + baseCount
                        + "/"
                        + GameConfig.getInstance().getPiecesPerPlayer()
                        + " pieces on the base.");
    }

    @Override
    public void onPieceMoved(
            String color, String pieceName, int from, int to, int value, String direction) {
        System.out.println(
                capitalize(color)
                        + " moves piece "
                        + pieceName
                        + " from location "
                        + formatPosition(from, color)
                        + " to "
                        + formatPosition(to, color)
                        + " by "
                        + value
                        + " units in "
                        + formatDirection(direction)
                        + " direction.");
    }

    // Block info ----------------------------------------------------------------

    @Override
    public void onPieceBlocked(
            String color,
            String pieceName,
            int from,
            int to,
            String blockingColor,
            String blockingName) {
        System.out.println(
                capitalize(color)
                        + " piece "
                        + pieceName
                        + " is blocked from moving from "
                        + from
                        + " to "
                        + to
                        + " by "
                        + capitalize(blockingColor)
                        + " piece "
                        + blockingName
                        + ".");
    }

    @Override
    public void onNoOtherPieces(String color) {
        System.out.println(
                capitalize(color)
                        + " does not have other pieces in the board to move"
                        + " instead of the blocked piece."
                        + " Ignoring the throw and moving on to the next player.");
    }

    @Override
    public void onMovedBeforeBlock(String color, String pieceName, int stoppedAt) {
        System.out.println(
                capitalize(color)
                        + " does not have other pieces in the board to move"
                        + " instead of the blocked piece. Moved the piece to square "
                        + stoppedAt
                        + " which is the cell before the block.");
    }

    // Capturing ----------------------------------------------------------------

    @Override
    public void onPieceCaptured(
            String capturerColor,
            String capturerName,
            int cell,
            String capturedColor,
            String capturedName,
            int boardCount,
            int baseCount) {
        System.out.println(
                capitalize(capturerColor)
                        + " piece "
                        + capturerName
                        + " lands on square "
                        + cell
                        + ", captures "
                        + capitalize(capturedColor)
                        + " piece "
                        + capturedName
                        + ", and returns it to the base.");
        System.out.println(
                capitalize(capturedColor)
                        + " player now has "
                        + boardCount
                        + "/"
                        + GameConfig.getInstance().getPiecesPerPlayer()
                        + " on pieces on the board and "
                        + baseCount
                        + "/"
                        + GameConfig.getInstance().getPiecesPerPlayer()
                        + " pieces on the base.");
    }

    // Mystery info ----------------------------------------------------------------

    @Override
    public void onMysteryResolved(String color, String pieceName, MysteryOutcome outcome) {
        System.out.println(
                capitalize(color)
                        + " player lands on a mystery cell and is teleported to "
                        + outcome.getDestination()
                        + ".");
        System.out.println(
                capitalize(color)
                        + " piece "
                        + pieceName
                        + " teleported to "
                        + outcome.getDestination()
                        + ".");

        switch (outcome.getType()) {
            case ALPHA_ENERGIZED ->
                    System.out.println(
                            capitalize(color)
                                    + " piece "
                                    + pieceName
                                    + " feels energized, and movement speed doubles.");
            case ALPHA_SICK ->
                    System.out.println(
                            capitalize(color)
                                    + " piece "
                                    + pieceName
                                    + " feels sick, and movement speed halves.");
            case BETA ->
                    System.out.println(
                            capitalize(color)
                                    + " piece "
                                    + pieceName
                                    + " attends briefing and cannot move for four rounds.");
            case GAMMA_DIRECTION_CHANGED ->
                    System.out.println(
                            "The "
                                    + capitalize(color)
                                    + " piece "
                                    + pieceName
                                    + ", which was moving clockwise,"
                                    + " has changed to moving counterclockwise.");
            case GAMMA_TO_BETA ->
                    System.out.println(
                            "The "
                                    + capitalize(color)
                                    + " piece "
                                    + pieceName
                                    + " is moving in a counterclockwise direction."
                                    + " Teleporting to Beta from Gamma.");
            default -> {}
        }
    }

    @Override
    public void onMysteryCellSpawned(int position, int duration) {
        System.out.println(
                "A mystery cell has spawned in location "
                        + position
                        + " and will be at this location for the next "
                        + duration
                        + " rounds.");
    }

    @Override
    public void onStateTeleportToBase(String color, String pieceName) {
        System.out.println(
                capitalize(color)
                        + " piece "
                        + pieceName
                        + " is movement-restricted and has rolled three consecutively."
                        + " Teleporting piece "
                        + pieceName
                        + " to base.");
    }

    // Round End ----------------------------------------------------------------

    @Override
    public void onRoundEnd(GameSnapshot snapshot) {
        for (GameSnapshot.PlayerView player : snapshot.getPlayers()) {
            int boardCount = player.getBoardCount();
            int baseCount = player.getBaseCount();

            System.out.println(
                    capitalize(player.getColor())
                            + " player now has "
                            + boardCount
                            + "/"
                            + GameConfig.getInstance().getPiecesPerPlayer()
                            + " on pieces on the board and "
                            + baseCount
                            + "/"
                            + GameConfig.getInstance().getPiecesPerPlayer()
                            + " pieces on the base.");

            System.out.println("============================");
            System.out.println("Location of pieces " + capitalize(player.getColor()));
            System.out.println("============================");

            for (GameSnapshot.PieceView piece : player.getPieces()) {
                System.out.println("Piece " + piece.getName() + " -> " + piece.getPosition());
            }
        }

        if (snapshot.isMysteryActive()) {
            System.out.println(
                    "The mystery cell is at "
                            + snapshot.getMysteryPosition()
                            + " and will be at that location for the next "
                            + snapshot.getMysteryRoundsRemaining()
                            + " values.");
        }
    }

    // Win ----------------------------------------------------------------

    @Override
    public void onGameWon(String color) {
        System.out.println(capitalize(color) + " player wins!!!");
    }

    @Override
    public void onFinalPlacements(List<String> finishOrder) {
        System.out.println("============================");
        System.out.println("Final Places");
        System.out.println("============================");

        for (int i = 0; i < finishOrder.size(); i++) {
            System.out.println(
                    ordinal(i + 1) + " place: " + capitalize(finishOrder.get(i)) + " player");
        }
    }
}
