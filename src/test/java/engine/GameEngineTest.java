package engine;

import board.Board;
import dice.ICoinToss;
import dice.IDice;
import event.IGameEventListener;
import factory.BoardFactory;
import mystery.MysteryManager;
import org.junit.jupiter.api.Test;
import piece.Piece;
import player.Player;
import player.strategy.IPlayerStrategy;
import rules.BlockHandler;
import rules.CaptureHandler;
import rules.RuleEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class GameEngineTest {

    @Test
    void startGameSelectsHighestInitialRollAndPublishesFinalPlacements() {
        // Arrange
        Board board = BoardFactory.createBoard();
        BlockHandler blockHandler = new BlockHandler(board);
        CaptureHandler captureHandler = new CaptureHandler(board);
        RuleEngine ruleEngine = new RuleEngine(board, blockHandler, captureHandler);
        MysteryManager mysteryManager = new MysteryManager(board, new Random(1));

        FakeDice dice = new FakeDice(
                2, 6, 4, 1,
                1, 1, 1, 1
        );

        List<Player> players = List.of(
                new AutoFinishingPlayer("RED"),
                new AutoFinishingPlayer("GREEN"),
                new AutoFinishingPlayer("YELLOW"),
                new AutoFinishingPlayer("BLUE")
        );

        GameEngine engine = createEngine(board, players, ruleEngine,
                captureHandler, blockHandler, mysteryManager, dice);

        RecordingListener listener = new RecordingListener();
        engine.addEventListener(listener);

        // Act
        engine.startGame();

        // Assert
        assertEquals("GREEN", listener.firstPlayer);
        assertEquals(List.of("GREEN", "YELLOW", "BLUE", "RED"), listener.turnOrder);
        assertEquals(List.of("GREEN", "YELLOW", "BLUE", "RED"), listener.winners);
        assertEquals(List.of("GREEN", "YELLOW", "BLUE", "RED"), listener.finalPlacements);
    }

    @Test
    void rollingSixGivesBonusTurn() {
        // Arrange
        Board board = BoardFactory.createBoard();
        BlockHandler blockHandler = new BlockHandler(board);
        CaptureHandler captureHandler = new CaptureHandler(board);
        RuleEngine ruleEngine = new RuleEngine(board, blockHandler, captureHandler);
        MysteryManager mysteryManager = new MysteryManager(board, new Random(1));

        FakeDice dice = new FakeDice(
                6, 5, 4, 3,
                6, 2,
                1, 1, 1
        );

        List<Player> players = List.of(
                new AutoFinishingPlayer("RED"),
                new AutoFinishingPlayer("GREEN"),
                new AutoFinishingPlayer("YELLOW"),
                new AutoFinishingPlayer("BLUE")
        );

        GameEngine engine = createEngine(board, players, ruleEngine,
                captureHandler, blockHandler, mysteryManager, dice);

        RecordingListener listener = new RecordingListener();
        engine.addEventListener(listener);

        // Act
        engine.startGame();

        // Assert
        assertEquals(List.of("RED=6", "RED=2"), listener.diceRolls.subList(0, 2));
        assertEquals(2, listener.countDiceRollsFor("RED"));
    }

    @Test
    void thirdConsecutiveSixIsIgnored() {
        // Arrange
        Board board = BoardFactory.createBoard();
        BlockHandler blockHandler = new BlockHandler(board);
        CaptureHandler captureHandler = new CaptureHandler(board);
        RuleEngine ruleEngine = new RuleEngine(board, blockHandler, captureHandler);
        MysteryManager mysteryManager = new MysteryManager(board, new Random(1));

        AutoFinishingPlayer red = new AutoFinishingPlayer("RED");

        FakeDice dice = new FakeDice(
                6, 5, 4, 3,
                6, 6, 6,
                5, 1, 1
        );

        List<Player> players = List.of(
                red,
                new AutoFinishingPlayer("GREEN"),
                new AutoFinishingPlayer("YELLOW"),
                new AutoFinishingPlayer("BLUE")
        );

        GameEngine engine = createEngine(board, players, ruleEngine,
                captureHandler, blockHandler, mysteryManager, dice);

        RecordingListener listener = new RecordingListener();
        engine.addEventListener(listener);

        // Act
        engine.startGame();

        // Assert
        assertEquals(List.of("RED=6", "RED=6", "RED=6"), listener.diceRolls.subList(0, 3));
        assertEquals("GREEN=5", listener.diceRolls.get(3));
        assertEquals(3, listener.countDiceRollsFor("RED"));
        assertEquals(0, red.getConsecutiveSixes());
    }

    @Test
    void startGamePublishesPlayerInfoAndInitialRollEvents() {
        // Arrange
        Board board = BoardFactory.createBoard();
        BlockHandler blockHandler = new BlockHandler(board);
        CaptureHandler captureHandler = new CaptureHandler(board);
        RuleEngine ruleEngine = new RuleEngine(board, blockHandler, captureHandler);
        MysteryManager mysteryManager = new MysteryManager(board, new Random(1));

        FakeDice dice = new FakeDice(
                1, 2, 3, 4,
                1, 1, 1, 1
        );

        List<Player> players = List.of(
                new AutoFinishingPlayer("RED"),
                new AutoFinishingPlayer("GREEN"),
                new AutoFinishingPlayer("YELLOW"),
                new AutoFinishingPlayer("BLUE")
        );

        GameEngine engine = createEngine(board, players, ruleEngine,
                captureHandler, blockHandler, mysteryManager, dice);

        RecordingListener listener = new RecordingListener();
        engine.addEventListener(listener);

        // Act
        engine.startGame();

        // Assert
        assertEquals(List.of(
                "RED=0",
                "GREEN=0",
                "YELLOW=0",
                "BLUE=0"
        ), listener.playerInfoEvents);

        assertEquals(List.of(
                "RED=1",
                "GREEN=2",
                "YELLOW=3",
                "BLUE=4"
        ), listener.initialRolls);

        assertEquals("BLUE", listener.firstPlayer);
    }

    @Test
    void startGamePublishesDiceRollEventsAndFinalPlacements() {
        // Arrange
        Board board = BoardFactory.createBoard();
        BlockHandler blockHandler = new BlockHandler(board);
        CaptureHandler captureHandler = new CaptureHandler(board);
        RuleEngine ruleEngine = new RuleEngine(board, blockHandler, captureHandler);
        MysteryManager mysteryManager = new MysteryManager(board, new Random(1));

        FakeDice dice = new FakeDice(
                6, 5, 4, 3,
                1, 2, 3, 4
        );

        List<Player> players = List.of(
                new AutoFinishingPlayer("RED"),
                new AutoFinishingPlayer("GREEN"),
                new AutoFinishingPlayer("YELLOW"),
                new NeverFinishingPlayer("BLUE")
        );

        GameEngine engine = createEngine(board, players, ruleEngine,
                captureHandler, blockHandler, mysteryManager, dice);

        RecordingListener listener = new RecordingListener();
        engine.addEventListener(listener);

        // Act
        engine.startGame();

        // Assert
        assertTrue(listener.diceRolls.contains("RED=1"));
        assertTrue(listener.diceRolls.contains("GREEN=2"));
        assertTrue(listener.diceRolls.contains("YELLOW=3"));
        assertTrue(listener.diceRolls.contains("BLUE=4"));

        assertEquals(List.of("RED", "GREEN", "YELLOW"), listener.winners);
        assertEquals(List.of("RED", "GREEN", "YELLOW", "BLUE"), listener.finalPlacements);
        assertEquals(1, listener.roundEndCount);
    }

    private static GameEngine createEngine(Board board,
                                           List<Player> players,
                                           RuleEngine ruleEngine,
                                           CaptureHandler captureHandler,
                                           BlockHandler blockHandler,
                                           MysteryManager mysteryManager,
                                           IDice dice) {
        return new GameEngine(
                board,
                players,
                ruleEngine,
                captureHandler,
                blockHandler,
                mysteryManager,
                dice,
                new FixedCoinToss("HEADS")
        );
    }

    private static class AutoFinishingPlayer extends Player {
        private int hasWonCalls;

        AutoFinishingPlayer(String color) {
            super(color, color, List.of(), new NoMoveStrategy());
        }

        @Override
        protected Piece choosePieceToMove(List<Piece> pieces, int diceValue,
                                          Board board, RuleEngine ruleEngine) {
            return null;
        }

        @Override
        public boolean hasWon() {
            hasWonCalls++;
            return hasWonCalls >= 2;
        }
    }

    private static class NeverFinishingPlayer extends Player {
        NeverFinishingPlayer(String color) {
            super(color, color, List.of(), new NoMoveStrategy());
        }

        @Override
        protected Piece choosePieceToMove(List<Piece> pieces, int diceValue,
                                          Board board, RuleEngine ruleEngine) {
            return null;
        }

        @Override
        public boolean hasWon() {
            return false;
        }
    }

    private static class NoMoveStrategy implements IPlayerStrategy {
        @Override
        public Piece choosePieceToMove(List<Piece> validMoves, int diceValue,
                                       Board board, RuleEngine ruleEngine) {
            return null;
        }

        @Override
        public boolean shouldMoveFromBase(List<Piece> pieces, int diceValue,
                                          Board board, RuleEngine ruleEngine) {
            return false;
        }
    }

    private static class FakeDice implements IDice {
        private final int[] values;
        private int index;

        FakeDice(int... values) {
            this.values = values;
        }

        @Override
        public int roll() {
            if (index >= values.length) {
                return 1;
            }
            return values[index++];
        }
    }

    private static class FixedCoinToss implements ICoinToss {
        private final String result;

        FixedCoinToss(String result) {
            this.result = result;
        }

        @Override
        public String toss() {
            return result;
        }
    }

    private static class RecordingListener implements IGameEventListener {
        private String firstPlayer;
        private List<String> turnOrder = new ArrayList<>();
        private final List<String> winners = new ArrayList<>();
        private List<String> finalPlacements = new ArrayList<>();
        private final List<String> playerInfoEvents = new ArrayList<>();
        private final List<String> initialRolls = new ArrayList<>();
        private final List<String> diceRolls = new ArrayList<>();
        private int roundEndCount;

        int countDiceRollsFor(String color) {
            int count = 0;
            for (String roll : diceRolls) {
                if (roll.startsWith(color + "=")) {
                    count++;
                }
            }
            return count;
        }

        @Override
        public void onPlayerInfo(String color, List<String> pieceNames) {
            playerInfoEvents.add(color + "=" + pieceNames.size());
        }

        @Override
        public void onInitialRoll(String color, int value) {
            initialRolls.add(color + "=" + value);
        }

        @Override
        public void onDiceRolled(String color, int value) {
            diceRolls.add(color + "=" + value);
        }

        @Override
        public void onFirstPlayer(String color) {
            this.firstPlayer = color;
        }

        @Override
        public void onTurnOrder(List<String> colors) {
            this.turnOrder = new ArrayList<>(colors);
        }

        @Override
        public void onPieceEnteredBoard(String color, String pieceName,
                                        int boardCount, int baseCount) {
        }

        @Override
        public void onPieceMoved(String color, String pieceName,
                                 int from, int to, int value, String direction) {
        }

        @Override
        public void onPieceBlocked(String color, String pieceName,
                                   int from, int to,
                                   String blockingColor, String blockingName) {
        }

        @Override
        public void onNoOtherPieces(String color) {
        }

        @Override
        public void onMovedBeforeBlock(String color, String pieceName, int stoppedAt) {
        }

        @Override
        public void onPieceCaptured(String capturerColor, String capturerName,
                                    int cell,
                                    String capturedColor, String capturedName,
                                    int boardCount, int baseCount) {
        }

        @Override
        public void onMysteryLanding(String color, String pieceName, String destination) {
        }

        @Override
        public void onTeleportEffect(String color, String pieceName, String effect) {
        }

        @Override
        public void onDirectionChanged(String color, String pieceName,
                                       String oldDirection, String newDirection) {
        }

        @Override
        public void onMysteryCellSpawned(int position, int duration) {
        }

        @Override
        public void onRoundEnd(List<Player> players) {
            roundEndCount++;
        }

        @Override
        public void onGameWon(String color) {
            winners.add(color);
        }

        @Override
        public void onFinalPlacements(List<Player> finishOrder) {
            finalPlacements = finishOrder.stream()
                    .map(Player::getColor)
                    .toList();
        }
    }
}