package piece.state;

import org.junit.jupiter.api.Test;
import piece.Piece;

import static org.junit.jupiter.api.Assertions.*;

class PieceStateTest {

    @Test
    void normalStateUsesRawDiceValueAndCanMove() {
        NormalState state = new NormalState();

        assertEquals(5, state.calculateMovement(5));
        assertTrue(state.canMove());
        assertSame(state, state.onRoundPass());
        assertSame(state, state.onDiceRoll(3));
    }

    @Test
    void energizedStateDoublesMovementAndExpiresAfterDuration() {
        IPieceState state = new EnergizedState(2);

        assertEquals(8, state.calculateMovement(4));
        assertTrue(state.canMove());
        state = state.onRoundPass();
        assertTrue(state instanceof EnergizedState);
        state = state.onRoundPass();
        assertTrue(state instanceof NormalState);
    }

    @Test
    void sickStateHalvesMovementAndCanProduceZeroMovement() {
        IPieceState state = new SickState(1);

        assertEquals(2, state.calculateMovement(5));
        assertEquals(0, state.calculateMovement(1));
        assertTrue(state.canMove());
        state = state.onRoundPass();
        assertTrue(state instanceof NormalState);
    }

    @Test
    void frozenStateCannotMoveAndTracksThreeConsecutiveThrees() {
        FrozenState frozen = new FrozenState(4);

        assertFalse(frozen.canMove());
        assertEquals(0, frozen.calculateMovement(6));

        frozen.onDiceRoll(3);
        frozen.onDiceRoll(2);
        assertEquals(0, frozen.getConsecutiveThrees());
        assertFalse(frozen.shouldTeleportToBase());

        frozen.onDiceRoll(3);
        frozen.onDiceRoll(3);
        frozen.onDiceRoll(3);
        assertTrue(frozen.shouldTeleportToBase());

        frozen.resetTeleportFlag();
        assertFalse(frozen.shouldTeleportToBase());
        assertEquals(0, frozen.getConsecutiveThrees());
    }

    @Test
    void pieceDelegatesMovementAndDiceRollsToCurrentState() {
        Piece piece = new Piece("1", "BLUE");
        piece.setState(new EnergizedState(4));
        assertEquals(12, piece.getEffectiveMovement(6));

        piece.setState(new FrozenState(4));
        piece.notifyDiceRoll(3);
        piece.notifyDiceRoll(3);
        piece.notifyDiceRoll(3);

        FrozenState frozen = (FrozenState) piece.getState();
        assertTrue(frozen.shouldTeleportToBase());
        assertFalse(piece.canMove());
    }
}
