package support;

import board.Board;
import dice.ICoinToss;
import dice.IDice;
import factory.BoardFactory;
import java.util.List;
import java.util.Random;
import mystery.MysteryManager;
import mystery.effect.StartEffect;
import piece.Piece;
import player.strategy.IPlayerStrategy;
import rules.BlockHandler;
import rules.CaptureHandler;
import rules.LandingResolver;
import rules.RuleEngine;

public final class TestSupport {
    private TestSupport() {}

    public static Board newBoard() {
        return BoardFactory.createBoard();
    }

    public static Piece place(
            Board board, String name, String color, int position, String direction) {
        Piece piece = new Piece(name, color);
        piece.setDirection(direction);
        piece.setOriginalDirection(direction);
        board.teleportToStandardPath(piece, position);
        return piece;
    }

    public static RuleComponents rulesFor(Board board) {
        CaptureHandler captureHandler = new CaptureHandler(board);
        BlockHandler blockHandler = new BlockHandler(board);
        RuleEngine ruleEngine = new RuleEngine(board, blockHandler);
        MysteryManager mysteryManager =
                new MysteryManager(board, new Random(17), List.of(new StartEffect()));
        LandingResolver landingResolver =
                new LandingResolver(board, captureHandler, blockHandler, mysteryManager);
        return new RuleComponents(
                captureHandler, blockHandler, ruleEngine, mysteryManager, landingResolver);
    }

    public static final class RuleComponents {
        public final CaptureHandler captureHandler;
        public final BlockHandler blockHandler;
        public final RuleEngine ruleEngine;
        public final MysteryManager mysteryManager;
        public final LandingResolver landingResolver;

        private RuleComponents(
                CaptureHandler captureHandler,
                BlockHandler blockHandler,
                RuleEngine ruleEngine,
                MysteryManager mysteryManager,
                LandingResolver landingResolver) {
            this.captureHandler = captureHandler;
            this.blockHandler = blockHandler;
            this.ruleEngine = ruleEngine;
            this.mysteryManager = mysteryManager;
            this.landingResolver = landingResolver;
        }
    }

    public static final class SequenceRandom extends Random {
        private static final long serialVersionUID = 1L;

        private final int[] integers;
        private final boolean[] booleans;
        private int integerIndex;
        private int booleanIndex;

        public SequenceRandom(int... integers) {
            this(integers, new boolean[0]);
        }

        public SequenceRandom(int[] integers, boolean[] booleans) {
            this.integers = integers.clone();
            this.booleans = booleans.clone();
        }

        @Override
        public int nextInt(int bound) {
            if (bound <= 0) throw new IllegalArgumentException("bound must be positive");
            if (integers.length == 0) return 0;
            int value = integers[Math.min(integerIndex, integers.length - 1)];
            integerIndex++;
            return Math.floorMod(value, bound);
        }

        @Override
        public boolean nextBoolean() {
            if (booleans.length == 0) return false;
            boolean value = booleans[Math.min(booleanIndex, booleans.length - 1)];
            booleanIndex++;
            return value;
        }
    }

    public static final class SequenceDice implements IDice {
        private final int[] values;
        private int index;

        public SequenceDice(int... values) {
            if (values.length == 0) throw new IllegalArgumentException("values required");
            this.values = values.clone();
        }

        @Override
        public int roll() {
            int value = values[Math.min(index, values.length - 1)];
            index++;
            return value;
        }
    }

    public static final class FixedCoinToss implements ICoinToss {
        private final String value;

        public FixedCoinToss(String value) {
            this.value = value;
        }

        @Override
        public String toss() {
            return value;
        }
    }

    public static class FirstPieceStrategy implements IPlayerStrategy {
        private final boolean moveFromBase;
        public int chooseCalls;
        public int baseDecisionCalls;

        public FirstPieceStrategy(boolean moveFromBase) {
            this.moveFromBase = moveFromBase;
        }

        @Override
        public Piece choosePieceToMove(
                List<Piece> validPieces, int diceValue, Board board, RuleEngine ruleEngine) {
            chooseCalls++;
            return validPieces.isEmpty() ? null : validPieces.getFirst();
        }

        @Override
        public boolean shouldMoveFromBase(
                List<Piece> pieces, int diceValue, Board board, RuleEngine ruleEngine) {
            baseDecisionCalls++;
            return moveFromBase;
        }
    }
}
