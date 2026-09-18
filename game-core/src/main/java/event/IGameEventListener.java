package event;

import java.util.List;
import mystery.MysteryOutcome;

public interface IGameEventListener {
    default void onPlayerInfo(String color, List<String> pieceNames) {}

    default void onInitialRoll(String color, int value) {}

    default void onDiceRolled(String color, int value) {}

    default void onFirstPlayer(String color) {}

    default void onTurnOrder(List<String> colors) {}

    default void onPieceEnteredBoard(
            String color, String pieceName, int boardCount, int baseCount) {}

    default void onPieceMoved(
            String color, String pieceName, int from, int to, int value, String direction) {}

    default void onPieceBlocked(
            String color,
            String pieceName,
            int from,
            int to,
            String blockingColor,
            String blockingName) {}

    default void onNoOtherPieces(String color) {}

    default void onMovedBeforeBlock(String color, String pieceName, int stoppedAt) {}

    default void onPieceCaptured(
            String capturerColor,
            String capturerName,
            int cell,
            String capturedColor,
            String capturedName,
            int boardCount,
            int baseCount) {}

    default void onMysteryResolved(String color, String pieceName, MysteryOutcome outcome) {}

    default void onMysteryCellSpawned(int position, int duration) {}

    default void onStateTeleportToBase(String color, String pieceName) {}

    default void onRoundEnd(GameSnapshot snapshot) {}

    default void onGameWon(String color) {}

    default void onFinalPlacements(List<String> finishOrder) {}
}
