package event;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import mystery.MysteryOutcome;

/** Subject/publisher participant for the existing Observer pattern. */
public final class GameEventPublisher {
    private final List<IGameEventListener> listeners = new CopyOnWriteArrayList<>();

    public void addListener(IGameEventListener listener) {
        if (listener != null && !listeners.contains(listener)) listeners.add(listener);
    }

    public void removeListener(IGameEventListener listener) {
        listeners.remove(listener);
    }

    public void playerInfo(String color, List<String> names) {
        List<String> copy = List.copyOf(names);
        for (IGameEventListener listener : listeners) listener.onPlayerInfo(color, copy);
    }

    public void initialRoll(String color, int value) {
        for (IGameEventListener listener : listeners) listener.onInitialRoll(color, value);
    }

    public void diceRolled(String color, int value) {
        for (IGameEventListener listener : listeners) listener.onDiceRolled(color, value);
    }

    public void firstPlayer(String color) {
        for (IGameEventListener listener : listeners) listener.onFirstPlayer(color);
    }

    public void turnOrder(List<String> colors) {
        List<String> copy = List.copyOf(colors);
        for (IGameEventListener listener : listeners) listener.onTurnOrder(copy);
    }

    public void pieceEnteredBoard(String color, String name, int boardCount, int baseCount) {
        for (IGameEventListener listener : listeners) {
            listener.onPieceEnteredBoard(color, name, boardCount, baseCount);
        }
    }

    public void pieceMoved(
            String color, String name, int from, int to, int value, String direction) {
        for (IGameEventListener listener : listeners) {
            listener.onPieceMoved(color, name, from, to, value, direction);
        }
    }

    public void pieceBlocked(
            String color,
            String name,
            int from,
            int to,
            String blockingColor,
            String blockingName) {
        for (IGameEventListener listener : listeners) {
            listener.onPieceBlocked(color, name, from, to, blockingColor, blockingName);
        }
    }

    public void noOtherPieces(String color) {
        for (IGameEventListener listener : listeners) listener.onNoOtherPieces(color);
    }

    public void movedBeforeBlock(String color, String name, int stoppedAt) {
        for (IGameEventListener listener : listeners) {
            listener.onMovedBeforeBlock(color, name, stoppedAt);
        }
    }

    public void pieceCaptured(
            String capturerColor,
            String capturerName,
            int cell,
            String capturedColor,
            String capturedName,
            int boardCount,
            int baseCount) {
        for (IGameEventListener listener : listeners) {
            listener.onPieceCaptured(
                    capturerColor,
                    capturerName,
                    cell,
                    capturedColor,
                    capturedName,
                    boardCount,
                    baseCount);
        }
    }

    public void mysteryResolved(String color, String name, MysteryOutcome outcome) {
        for (IGameEventListener listener : listeners) {
            listener.onMysteryResolved(color, name, outcome);
        }
    }

    public void mysteryCellSpawned(int position, int duration) {
        for (IGameEventListener listener : listeners) {
            listener.onMysteryCellSpawned(position, duration);
        }
    }

    public void stateTeleportToBase(String color, String name) {
        for (IGameEventListener listener : listeners) {
            listener.onStateTeleportToBase(color, name);
        }
    }

    public void roundEnd(GameSnapshot snapshot) {
        for (IGameEventListener listener : listeners) listener.onRoundEnd(snapshot);
    }

    public void gameWon(String color) {
        for (IGameEventListener listener : listeners) listener.onGameWon(color);
    }

    public void finalPlacements(List<String> order) {
        List<String> copy = List.copyOf(order);
        for (IGameEventListener listener : listeners) {
            listener.onFinalPlacements(copy);
        }
    }
}
