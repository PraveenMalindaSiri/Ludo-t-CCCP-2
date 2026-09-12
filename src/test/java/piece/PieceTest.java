package piece;

import org.junit.jupiter.api.Test;
import piece.state.EnergizedState;
import piece.state.NormalState;

import static org.junit.jupiter.api.Assertions.*;

class PieceTest {

    @Test
    void newPieceStartsInBaseWithDefaultClockwiseDirection() {
        Piece piece = new Piece("1", "RED");

        assertTrue(piece.isInBase());
        assertFalse(piece.isOnBoard());
        assertFalse(piece.isAtHome());
        assertEquals(Piece.BASE_POSITION, piece.getPosition());
        assertEquals("CLOCKWISE", piece.getDirection());
        assertEquals("CLOCKWISE", piece.getOriginalDirection());
        assertEquals(0, piece.getCaptureCount());
        assertTrue(piece.getState() instanceof NormalState);
    }

    @Test
    void clockwiseMoveWrapsAroundStandardPath() {
        Piece piece = new Piece("1", "RED");
        piece.moveToPosition(50);
        piece.setDirection("CLOCKWISE");

        piece.move(4);

        assertEquals(2, piece.getPosition());
    }

    @Test
    void counterClockwiseMoveWrapsAroundStandardPath() {
        Piece piece = new Piece("1", "RED");
        piece.moveToPosition(1);
        piece.setDirection("COUNTERCLOCKWISE");

        piece.move(4);

        assertEquals(49, piece.getPosition());
    }

    @Test
    void moveToHomeStraightClearsTemporaryStateAndStoresHomeStraightIndex() {
        Piece piece = new Piece("1", "RED");
        piece.setState(new EnergizedState(4));

        piece.moveToHomeStraight(3);

        assertTrue(piece.isOnBoard());
        assertTrue(piece.isInHomeStraight());
        assertEquals(3, piece.getHomeStraightIndex());
        assertEquals(Piece.HOME_STRAIGHT_OFFSET + 3, piece.getPosition());
        assertTrue(piece.getState() instanceof NormalState);
        assertEquals("redhomepath3", piece.positionLabel());
    }

    @Test
    void captureResetsAllImportantPieceInformation() {
        Piece piece = new Piece("1", "RED");
        piece.moveToPosition(12);
        piece.setDirection("COUNTERCLOCKWISE");
        piece.setOriginalDirection("COUNTERCLOCKWISE");
        piece.incrementCaptureCount();
        piece.setHasPassedApproachOnce(true);
        piece.setInBlock(true);
        piece.setState(new EnergizedState(4));

        piece.capture();

        assertTrue(piece.isInBase());
        assertFalse(piece.isOnBoard());
        assertFalse(piece.isAtHome());
        assertEquals(Piece.BASE_POSITION, piece.getPosition());
        assertEquals("CLOCKWISE", piece.getDirection());
        assertEquals("CLOCKWISE", piece.getOriginalDirection());
        assertEquals(0, piece.getCaptureCount());
        assertFalse(piece.getHasPassedApproachOnce());
        assertFalse(piece.isInBlock());
        assertTrue(piece.getState() instanceof NormalState);
    }

    @Test
    void moveToHomeMarksPieceAsAtHome() {
        Piece piece = new Piece("1", "RED");
        piece.moveToPosition(25);

        piece.moveToHome();

        assertTrue(piece.isAtHome());
        assertFalse(piece.isInBase());
        assertFalse(piece.isOnBoard());
        assertEquals("Home", piece.positionLabel());
    }
}
