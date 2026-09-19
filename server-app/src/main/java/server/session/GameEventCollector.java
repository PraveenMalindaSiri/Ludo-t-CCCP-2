package server.session;

import event.GameSnapshot;
import event.IGameEventListener;
import java.util.ArrayList;
import java.util.List;
import mystery.MysteryOutcome;

/** Collects presentation-safe event text on the session worker without performing I/O. */
public final class GameEventCollector implements IGameEventListener {

    private final List<String> events = new ArrayList<>();
    private Integer latestDice;
    private String latestResult = "";

    public synchronized void beginOperation() {
        events.clear();
        latestDice = null;
        latestResult = "";
    }

    public synchronized EventBatch drain() {
        EventBatch result = new EventBatch(events, latestDice, latestResult);
        events.clear();
        latestDice = null;
        latestResult = "";
        return result;
    }

    @Override
    public synchronized void onPlayerInfo(String color, List<String> pieceNames) {
        record(color + " pieces: " + String.join(", ", pieceNames));
    }

    @Override
    public synchronized void onInitialRoll(String color, int value) {
        record(color + " initial roll: " + value);
    }

    @Override
    public synchronized void onDiceRolled(String color, int value) {
        latestDice = value;
        record(color + " rolled " + value);
    }

    @Override
    public synchronized void onFirstPlayer(String color) {
        record(color + " plays first");
    }

    @Override
    public synchronized void onTurnOrder(List<String> colors) {
        record("Turn order: " + String.join(" -> ", colors));
    }

    @Override
    public synchronized void onPieceEnteredBoard(
            String color, String pieceName, int boardCount, int baseCount) {
        record(pieceName + " entered the board");
    }

    @Override
    public synchronized void onPieceMoved(
            String color, String pieceName, int from, int to, int value, String direction) {
        record(pieceName + " moved " + from + " -> " + to + " (" + direction + ")");
    }

    @Override
    public synchronized void onPieceBlocked(
            String color,
            String pieceName,
            int from,
            int to,
            String blockingColor,
            String blockingName) {
        record(pieceName + " was blocked at " + to + " by " + blockingName);
    }

    @Override
    public synchronized void onNoOtherPieces(String color) {
        record(color + " had no other valid piece");
    }

    @Override
    public synchronized void onMovedBeforeBlock(String color, String pieceName, int stoppedAt) {
        record(pieceName + " stopped before a block at " + stoppedAt);
    }

    @Override
    public synchronized void onPieceCaptured(
            String capturerColor,
            String capturerName,
            int cell,
            String capturedColor,
            String capturedName,
            int boardCount,
            int baseCount) {
        record(capturerName + " captured " + capturedName + " at " + cell);
    }

    @Override
    public synchronized void onMysteryResolved(
            String color, String pieceName, MysteryOutcome outcome) {
        record(pieceName + " mystery result: " + outcome.getType());
    }

    @Override
    public synchronized void onMysteryCellSpawned(int position, int duration) {
        record("Mystery cell appeared at " + position + " for " + duration + " rounds");
    }

    @Override
    public synchronized void onStateTeleportToBase(String color, String pieceName) {
        record(pieceName + " returned to base because of its state");
    }

    @Override
    public synchronized void onRoundEnd(GameSnapshot snapshot) {
        record("Round " + snapshot.getRound() + " completed");
    }

    @Override
    public synchronized void onGameWon(String color) {
        record(color + " finished all pieces");
    }

    @Override
    public synchronized void onFinalPlacements(List<String> finishOrder) {
        record("Final placements: " + String.join(", ", finishOrder));
    }

    private void record(String event) {
        events.add(event);
        latestResult = event;
    }

    public record EventBatch(List<String> events, Integer latestDice, String latestResult) {

        public EventBatch {
            events = events == null ? List.of() : List.copyOf(events);
            latestResult = latestResult == null ? "" : latestResult;
        }
    }
}
