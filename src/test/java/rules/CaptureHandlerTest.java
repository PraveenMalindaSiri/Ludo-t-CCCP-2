package rules;

import board.Board;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import piece.Piece;
import piece.state.EnergizedState;
import testutil.TestHelpers;

import static org.junit.jupiter.api.Assertions.*;

class CaptureHandlerTest {
    private Board board;
    private CaptureHandler captureHandler;

    @BeforeEach
    void setUp() {
        board = TestHelpers.board();
        captureHandler = new CaptureHandler(board);
    }

    @Test
    void detectsAndHandlesSingleOpponentCapture() {
        Piece red = TestHelpers.placePiece(board, "RED", "1", 10);
        Piece blue = TestHelpers.placePiece(board, "BLUE", "1", 13, "COUNTERCLOCKWISE");
        blue.incrementCaptureCount();
        blue.setState(new EnergizedState(4));

        assertTrue(captureHandler.isCapturePossible(red, 13));
        assertSame(blue, captureHandler.getCapturedPieceAt(13, "RED"));

        captureHandler.handleCapture(red, blue);

        assertEquals(1, red.getCaptureCount());
        assertTrue(blue.isInBase());
        assertEquals(0, blue.getCaptureCount());
        assertEquals("CLOCKWISE", blue.getDirection());
        assertTrue(board.getBaseCell("BLUE").getPieces().contains(blue));
        assertFalse(board.getCellAt(13).getPieces().contains(blue));
    }

    @Test
    void sameColourPieceIsNotCapturable() {
        Piece red = TestHelpers.placePiece(board, "RED", "1", 10);
        TestHelpers.placePiece(board, "RED", "2", 13);

        assertFalse(captureHandler.isCapturePossible(red, 13));
        assertNull(captureHandler.getCapturedPieceAt(13, "RED"));
    }

    @Test
    void singlePieceCannotCaptureOpponentBlock() {
        Piece red = TestHelpers.placePiece(board, "RED", "1", 10);
        TestHelpers.placePiece(board, "BLUE", "1", 13);
        TestHelpers.placePiece(board, "BLUE", "2", 13);

        assertFalse(captureHandler.isCapturePossible(red, 13));
        assertNull(captureHandler.getCapturedPieceAt(13, "RED"));
    }

    @Test
    void ghostPieceInCellIsIgnoredDuringCaptureCheck() {
        Piece red = TestHelpers.placePiece(board, "RED", "1", 10);
        Piece blue = TestHelpers.placePiece(board, "BLUE", "1", 13);
        blue.moveToBase(); // stale/ghost reference still remains in cell list

        assertFalse(captureHandler.isCapturePossible(red, 13));
        assertNull(captureHandler.getCapturedPieceAt(13, "RED"));
    }

    @Test
    void handlingAlreadyResetPieceDoesNotAwardFalseCapture() {
        Piece red = TestHelpers.placePiece(board, "RED", "1", 10);
        Piece blue = new Piece("1", "BLUE");

        captureHandler.handleCapture(red, blue);

        assertEquals(0, red.getCaptureCount());
        assertTrue(board.getBaseCell("BLUE").getPieces().contains(blue));
    }

    @Test
    void captureResetClearsMovementRuleStateAndBlockState() {
        // Arrange
        Piece red = TestHelpers.placePiece(board, "RED", "1", 10);

        Piece blue = TestHelpers.placePiece(board, "BLUE", "1", 13, "COUNTERCLOCKWISE");
        blue.setOriginalDirection("COUNTERCLOCKWISE");
        blue.incrementCaptureCount();
        blue.setHasPassedApproachOnce(true);
        blue.setInBlock(true);
        blue.setState(new EnergizedState(4));

        // Act
        captureHandler.handleCapture(red, blue);

        // Assert
        assertEquals(1, red.getCaptureCount());

        assertTrue(blue.isInBase());
        assertEquals(Piece.BASE_POSITION, blue.getPosition());
        assertEquals("CLOCKWISE", blue.getDirection());
        assertEquals("CLOCKWISE", blue.getOriginalDirection());
        assertEquals(0, blue.getCaptureCount());
        assertFalse(blue.getHasPassedApproachOnce());
        assertFalse(blue.isInBlock());
        assertTrue(blue.isNormalState());

        assertFalse(board.getCellAt(13).getPieces().contains(blue));
        assertTrue(board.getBaseCell("BLUE").getPieces().contains(blue));
    }

    @Test
    void handleCaptureDoesNothingForSameColourPiece() {
        // Arrange
        Piece red1 = TestHelpers.placePiece(board, "RED", "1", 10);
        Piece red2 = TestHelpers.placePiece(board, "RED", "2", 13);

        // Act
        captureHandler.handleCapture(red1, red2);

        // Assert
        assertEquals(0, red1.getCaptureCount());
        assertFalse(red2.isInBase());
        assertEquals(13, red2.getPosition());
        assertTrue(board.getCellAt(13).getPieces().contains(red2));
    }

    @Test
    void invalidDestinationCannotReturnCapturedPiece() {
        // Arrange
        Piece red = TestHelpers.placePiece(board, "RED", "1", 10);

        // Act + Assert
        assertFalse(captureHandler.isCapturePossible(red, -1));
        assertFalse(captureHandler.isCapturePossible(red, 52));

        assertNull(captureHandler.getCapturedPieceAt(-1, "RED"));
        assertNull(captureHandler.getCapturedPieceAt(52, "RED"));
    }

    @Test
    void captureDoesNotDuplicatePieceInBaseWhenSamePieceHandledAgain() {
        // Arrange
        Piece red = TestHelpers.placePiece(board, "RED", "1", 10);
        Piece blue = TestHelpers.placePiece(board, "BLUE", "1", 13);

        // Act
        captureHandler.handleCapture(red, blue);
        captureHandler.handleCapture(red, blue);

        long blueCountInBase = board.getBaseCell("BLUE").getPieces().stream()
                .filter(piece -> piece == blue)
                .count();

        // Assert
        assertEquals(1, red.getCaptureCount());
        assertEquals(1, blueCountInBase);
        assertTrue(blue.isInBase());
    }
}
