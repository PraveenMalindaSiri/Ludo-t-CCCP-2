package mystery.effect;

import board.Board;
import config.GameConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import piece.Piece;
import piece.state.EnergizedState;
import piece.state.FrozenState;
import piece.state.SickState;
import testutil.TestHelpers;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class MysteryEffectsTest {
    private Board board;
    private GameConfig config;

    @BeforeEach
    void setUp() {
        board = TestHelpers.board();
        config = GameConfig.getInstance();
    }

    @Test
    void alphaEffectTeleportsToAlphaAndCanEnergizePiece() {
        Piece piece = new Piece("1", "RED");
        AlphaEffect effect = new AlphaEffect(new FixedBooleanRandom(true));

        effect.apply(piece, board);

        assertEquals(config.getAlphaCell(), piece.getPosition());
        assertTrue(piece.getState() instanceof EnergizedState);
        assertTrue(board.getCellAt(config.getAlphaCell()).getPieces().contains(piece));
    }

    @Test
    void alphaEffectCanMakePieceSick() {
        Piece piece = new Piece("1", "RED");
        AlphaEffect effect = new AlphaEffect(new FixedBooleanRandom(false));

        effect.apply(piece, board);

        assertEquals(config.getAlphaCell(), piece.getPosition());
        assertTrue(piece.getState() instanceof SickState);
    }

    @Test
    void betaEffectTeleportsToBetaAndFreezesPiece() {
        Piece piece = new Piece("1", "BLUE");

        new BetaEffect().apply(piece, board);

        assertEquals(config.getBetaCell(), piece.getPosition());
        assertTrue(piece.getState() instanceof FrozenState);
        assertTrue(board.getCellAt(config.getBetaCell()).getPieces().contains(piece));
    }

    @Test
    void gammaEffectForClockwisePieceChangesDirectionToCounterClockwise() {
        Piece piece = new Piece("1", "GREEN");
        piece.setDirection("CLOCKWISE");

        new GammaEffect(new BetaEffect()).apply(piece, board);

        assertEquals(config.getGammaCell(), piece.getPosition());
        assertEquals("COUNTERCLOCKWISE", piece.getDirection());
    }

    @Test
    void gammaEffectForCounterClockwisePieceRedirectsToBetaAndFreezes() {
        Piece piece = new Piece("1", "GREEN");
        piece.setDirection("COUNTERCLOCKWISE");

        new GammaEffect(new BetaEffect()).apply(piece, board);

        assertEquals(config.getBetaCell(), piece.getPosition());
        assertTrue(piece.getState() instanceof FrozenState);
    }

    @Test
    void baseEffectResetsPieceAndAddsItToBase() {
        Piece piece = TestHelpers.placePiece(board, "YELLOW", "1", 20, "COUNTERCLOCKWISE");
        piece.incrementCaptureCount();

        new BaseEffect().apply(piece, board);

        assertTrue(piece.isInBase());
        assertEquals(0, piece.getCaptureCount());
        assertTrue(board.getBaseCell("YELLOW").getPieces().contains(piece));
    }

    @Test
    void startAndApproachEffectsMoveToOwnColourCellsAndClearApproachPassFlag() {
        Piece red = new Piece("1", "RED");
        red.setHasPassedApproachOnce(true);
        new StartEffect().apply(red, board);
        assertEquals(board.getStartingPosition("RED"), red.getPosition());
        assertFalse(red.getHasPassedApproachOnce());

        Piece blue = new Piece("1", "BLUE");
        blue.setHasPassedApproachOnce(true);
        new ApproachEffect().apply(blue, board);
        assertEquals(board.getApproachPosition("BLUE"), blue.getPosition());
        assertFalse(blue.getHasPassedApproachOnce());
    }

    private static class FixedBooleanRandom extends Random {
        private final boolean value;

        FixedBooleanRandom(boolean value) {
            this.value = value;
        }

        @Override
        public boolean nextBoolean() {
            return value;
        }
    }
}
