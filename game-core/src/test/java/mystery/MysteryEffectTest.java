package mystery;

import static org.junit.jupiter.api.Assertions.*;

import board.Board;
import mystery.effect.AlphaEffect;
import mystery.effect.ApproachEffect;
import mystery.effect.BaseEffect;
import mystery.effect.BetaEffect;
import mystery.effect.GammaEffect;
import mystery.effect.StartEffect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import piece.Piece;
import piece.state.EnergizedState;
import piece.state.FrozenState;
import piece.state.SickState;
import support.TestSupport;

class MysteryEffectTest {
    private Board board;

    @BeforeEach
    void setUp() {
        board = TestSupport.newBoard();
    }

    @Test
    void alphaCanApplyEnergizedOutcome() {
        Piece piece = TestSupport.place(board, "1", "YELLOW", 4, "CLOCKWISE");
        AlphaEffect effect =
                new AlphaEffect(new TestSupport.SequenceRandom(new int[0], new boolean[] {true}));

        MysteryOutcome outcome = effect.apply(piece, board);

        assertEquals(MysteryOutcome.Type.ALPHA_ENERGIZED, outcome.getType());
        assertEquals(6, piece.getPosition());
        assertTrue(piece.getState() instanceof EnergizedState);
    }

    @Test
    void alphaCanApplySickOutcome() {
        Piece piece = TestSupport.place(board, "1", "YELLOW", 4, "CLOCKWISE");
        AlphaEffect effect =
                new AlphaEffect(new TestSupport.SequenceRandom(new int[0], new boolean[] {false}));

        MysteryOutcome outcome = effect.apply(piece, board);

        assertEquals(MysteryOutcome.Type.ALPHA_SICK, outcome.getType());
        assertTrue(piece.getState() instanceof SickState);
    }

    @Test
    void betaTeleportsAndFreezesPiece() {
        Piece piece = TestSupport.place(board, "1", "YELLOW", 4, "CLOCKWISE");

        MysteryOutcome outcome = new BetaEffect().apply(piece, board);

        assertEquals(MysteryOutcome.Type.BETA, outcome.getType());
        assertEquals(24, piece.getPosition());
        assertTrue(piece.getState() instanceof FrozenState);
    }

    @Test
    void clockwiseGammaChangesDirection() {
        Piece piece = TestSupport.place(board, "1", "YELLOW", 4, "CLOCKWISE");

        MysteryOutcome outcome = new GammaEffect(new BetaEffect()).apply(piece, board);

        assertEquals(MysteryOutcome.Type.GAMMA_DIRECTION_CHANGED, outcome.getType());
        assertEquals(43, piece.getPosition());
        assertEquals("CLOCKWISE", outcome.getOldDirection());
        assertEquals("COUNTERCLOCKWISE", outcome.getNewDirection());
        assertEquals("COUNTERCLOCKWISE", piece.getDirection());
    }

    @Test
    void counterclockwiseGammaDelegatesToBetaEffect() {
        Piece piece = TestSupport.place(board, "1", "YELLOW", 4, "COUNTERCLOCKWISE");

        MysteryOutcome outcome = new GammaEffect(new BetaEffect()).apply(piece, board);

        assertEquals(MysteryOutcome.Type.GAMMA_TO_BETA, outcome.getType());
        assertEquals(24, piece.getPosition());
        assertTrue(piece.getState() instanceof FrozenState);
    }

    @Test
    void baseStartAndApproachEffectsUseBoardRelocationMethods() {
        Piece basePiece = TestSupport.place(board, "1", "YELLOW", 7, "CLOCKWISE");
        Piece startPiece = TestSupport.place(board, "2", "BLUE", 7, "CLOCKWISE");
        Piece approachPiece = TestSupport.place(board, "3", "GREEN", 7, "CLOCKWISE");

        MysteryOutcome base = new BaseEffect().apply(basePiece, board);
        MysteryOutcome start = new StartEffect().apply(startPiece, board);
        MysteryOutcome approach = new ApproachEffect().apply(approachPiece, board);

        assertEquals(MysteryOutcome.Type.BASE, base.getType());
        assertTrue(basePiece.isInBase());
        assertEquals(board.getStartingPosition("BLUE"), startPiece.getPosition());
        assertEquals(MysteryOutcome.Type.START, start.getType());
        assertEquals(board.getApproachPosition("GREEN"), approachPiece.getPosition());
        assertEquals(MysteryOutcome.Type.APPROACH, approach.getType());
    }
}
