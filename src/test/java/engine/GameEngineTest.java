package engine;

import board.Board;
import event.GameSnapshot;
import event.IGameEventListener;
import mystery.MysteryManager;
import mystery.MysteryOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import piece.Piece;
import player.BluePlayer;
import player.Player;
import player.YellowPlayer;
import rules.BlockHandler;
import rules.CaptureHandler;
import rules.RuleEngine;
import support.TestSupport;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameEngineTest {
    private Board board;
    private Piece yellowPiece;
    private Piece bluePiece;
    private GameEngine engine;
    private RecordingListener listener;

    @BeforeEach
    void setUp() {
        board = TestSupport.newBoard();
        yellowPiece = new Piece("1", "YELLOW");
        bluePiece = new Piece("1", "BLUE");

        Player yellow = new YellowPlayer(
                List.of(yellowPiece), new TestSupport.FirstPieceStrategy(true));
        Player blue = new BluePlayer(
                List.of(bluePiece), new TestSupport.FirstPieceStrategy(true));

        CaptureHandler capture = new CaptureHandler(board);
        BlockHandler block = new BlockHandler(board);
        RuleEngine rules = new RuleEngine(board, block);
        MysteryManager mystery = new MysteryManager(
                board, new TestSupport.SequenceRandom(0),
                List.of((piece, gameBoard) ->
                        new MysteryOutcome(MysteryOutcome.Type.START, "unused")));

        // Initial rolls: Yellow 6, Blue 1. Yellow's turn: 6 then 1.
        // Blue's first turn: 1.
        engine = new GameEngine(board, List.of(yellow, blue), rules,
                capture, block, mystery,
                new TestSupport.SequenceDice(6, 1, 6, 1, 1),
                new TestSupport.FixedCoinToss("HEADS"));
        listener = new RecordingListener();
        engine.addEventListener(listener);
    }

    @Test
    void initializationIsIdempotentAndPublishesStartingOrder() {
        engine.initializeGame();
        engine.initializeGame();

        assertTrue(engine.isInitialized());
        assertEquals(2, listener.initialRolls.size());
        assertEquals("YELLOW", listener.firstPlayer);
        assertEquals(List.of("YELLOW", "BLUE"), listener.turnOrder);
        assertTrue(board.getBaseCell("YELLOW").getPieces().contains(yellowPiece));
        assertTrue(board.getBaseCell("BLUE").getPieces().contains(bluePiece));
    }

    @Test
    void oneTurnEntryAndMoveAreDrivenThroughCommands() {
        boolean gameContinues = engine.advanceOneTurn();

        assertTrue(gameContinues);
        assertEquals(1, yellowPiece.getPosition());
        assertEquals("CLOCKWISE", yellowPiece.getDirection());
        assertFalse(yellowPiece.isInBase());
        assertEquals(1, listener.entryEvents);
        assertEquals(List.of("YELLOW:0->1"), listener.moveEvents);
        assertEquals(0, engine.getRoundCount());
    }

    @Test
    void engineAdvancesExactlyOneRoundAfterEveryPlayerTurn() {
        engine.advanceOneTurn();
        engine.advanceOneTurn();

        assertEquals(1, engine.getRoundCount());
        assertEquals(1, listener.roundSnapshots.size());
        assertEquals(1, listener.roundSnapshots.getFirst().getRound());
        assertFalse(engine.isCompleted());
    }

    @Test
    void removedObserverNoLongerReceivesEngineEvents() {
        engine.removeEventListener(listener);

        engine.initializeGame();

        assertTrue(listener.initialRolls.isEmpty());
        assertNull(listener.firstPlayer);
    }

    @Test
    void snapshotCanBeRequestedWithoutExposingDomainPlayers() {
        engine.initializeGame();

        GameSnapshot snapshot = engine.getSnapshot();

        assertEquals(2, snapshot.getPlayers().size());
        assertEquals("YELLOW", snapshot.getPlayers().getFirst().getColor());
        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.getPlayers().clear());
    }

    private static final class RecordingListener implements IGameEventListener {
        private final List<String> initialRolls = new ArrayList<>();
        private final List<String> turnOrder = new ArrayList<>();
        private final List<String> moveEvents = new ArrayList<>();
        private final List<GameSnapshot> roundSnapshots = new ArrayList<>();
        private String firstPlayer;
        private int entryEvents;

        @Override
        public void onInitialRoll(String color, int value) {
            initialRolls.add(color + ":" + value);
        }

        @Override
        public void onFirstPlayer(String color) {
            firstPlayer = color;
        }

        @Override
        public void onTurnOrder(List<String> colors) {
            turnOrder.clear();
            turnOrder.addAll(colors);
        }

        @Override
        public void onPieceEnteredBoard(String color, String pieceName,
                                        int boardCount, int baseCount) {
            entryEvents++;
        }

        @Override
        public void onPieceMoved(String color, String pieceName,
                                 int from, int to, int value,
                                 String direction) {
            moveEvents.add(color + ":" + from + "->" + to);
        }

        @Override
        public void onRoundEnd(GameSnapshot snapshot) {
            roundSnapshots.add(snapshot);
        }
    }
}
