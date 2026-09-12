package piece;

import org.junit.jupiter.api.Test;
import piece.state.EnergizedState;
import piece.state.FrozenState;
import piece.state.NormalState;
import piece.state.SickState;

import static org.junit.jupiter.api.Assertions.*;

class PieceStateTest {

    @Test
    void normalStateUsesUnmodifiedMovementAndCanJoinBlock() {
        Piece piece = new Piece("1", "YELLOW");

        assertEquals(5, piece.getEffectiveMovement(5));
        assertTrue(piece.canMove());
        assertTrue(piece.canJoinBlock());
        assertFalse(piece.shouldTeleportToBase());
    }

    @Test
    void energizedStateDoublesMovementThenReturnsToNormal() {
        Piece piece = new Piece("1", "YELLOW");
        piece.setState(new EnergizedState(2));

        assertEquals(8, piece.getEffectiveMovement(4));
        assertFalse(piece.canJoinBlock());

        piece.updateState();
        assertTrue(piece.getState() instanceof EnergizedState);
        piece.updateState();
        assertTrue(piece.getState() instanceof NormalState);
        assertEquals(4, piece.getEffectiveMovement(4));
    }

    @Test
    void sickStateHalvesMovementThenReturnsToNormal() {
        Piece piece = new Piece("1", "YELLOW");
        piece.setState(new SickState(1));

        assertEquals(2, piece.getEffectiveMovement(5));
        assertEquals(0, piece.getEffectiveMovement(1));
        assertFalse(piece.canJoinBlock());

        piece.updateState();
        assertTrue(piece.getState() instanceof NormalState);
    }

    @Test
    void frozenStateRequestsTeleportAfterThreeConsecutiveThrees() {
        Piece piece = new Piece("1", "BLUE");
        piece.setState(new FrozenState(4));

        assertFalse(piece.canMove());
        assertEquals(0, piece.getEffectiveMovement(6));

        piece.notifyDiceRoll(3);
        piece.notifyDiceRoll(3);
        assertFalse(piece.shouldTeleportToBase());
        piece.notifyDiceRoll(3);
        assertTrue(piece.shouldTeleportToBase());

        piece.markTeleportHandled();
        assertFalse(piece.shouldTeleportToBase());
        assertEquals(0, ((FrozenState) piece.getState()).getConsecutiveThrees());
    }

    @Test
    void nonThreeBreaksFrozenStateStreak() {
        Piece piece = new Piece("1", "BLUE");
        piece.setState(new FrozenState(4));

        piece.notifyDiceRoll(3);
        piece.notifyDiceRoll(3);
        piece.notifyDiceRoll(2);
        piece.notifyDiceRoll(3);

        assertFalse(piece.shouldTeleportToBase());
        assertEquals(1, ((FrozenState) piece.getState()).getConsecutiveThrees());
    }

    @Test
    void pieceMovementWrapsInBothDirections() {
        Piece clockwise = new Piece("1", "YELLOW");
        clockwise.moveToPosition(50);
        clockwise.setDirection("CLOCKWISE");
        clockwise.move(4);

        Piece counterclockwise = new Piece("2", "YELLOW");
        counterclockwise.moveToPosition(1);
        counterclockwise.setDirection("COUNTERCLOCKWISE");
        counterclockwise.move(4);

        assertEquals(2, clockwise.getPosition());
        assertEquals(49, counterclockwise.getPosition());
    }
}
