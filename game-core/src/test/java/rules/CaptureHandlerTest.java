package rules;

import static org.junit.jupiter.api.Assertions.*;

import board.Board;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import piece.Piece;

class CaptureHandlerTest {
    private Board board;
    private CaptureHandler handler;

    @BeforeEach
    void setUp() {
        board = support.TestSupport.newBoard();
        handler = new CaptureHandler(board);
    }

    @Test
    void detectsSingleOpponentAtDestination() {
        Piece mover = support.TestSupport.place(board, "1", "YELLOW", 4, "CLOCKWISE");
        Piece enemy = support.TestSupport.place(board, "1", "RED", 8, "CLOCKWISE");

        assertTrue(handler.isCapturePossible(mover, 8));
        assertSame(enemy, handler.getCapturedPieceAt(8, "YELLOW"));
    }

    @Test
    void captureSendsOpponentToBaseAndCreditsCapturer() {
        Piece mover = support.TestSupport.place(board, "1", "YELLOW", 8, "CLOCKWISE");
        Piece enemy = support.TestSupport.place(board, "1", "RED", 8, "CLOCKWISE");

        handler.handleCapture(mover, enemy);

        assertTrue(enemy.isInBase());
        assertTrue(board.getBaseCell("RED").getPieces().contains(enemy));
        assertFalse(board.getCellAt(8).getPieces().contains(enemy));
        assertEquals(1, mover.getCaptureCount());
    }

    @Test
    void sameColorPieceIsNeverCaptured() {
        Piece mover = support.TestSupport.place(board, "1", "YELLOW", 8, "CLOCKWISE");
        Piece teammate = support.TestSupport.place(board, "2", "YELLOW", 8, "CLOCKWISE");

        handler.handleCapture(mover, teammate);

        assertFalse(teammate.isInBase());
        assertEquals(0, mover.getCaptureCount());
        assertFalse(handler.isCapturePossible(mover, 8));
    }

    @Test
    void multipleOccupantsAreNotTreatedAsSinglePieceCapture() {
        Piece mover = support.TestSupport.place(board, "1", "YELLOW", 4, "CLOCKWISE");
        support.TestSupport.place(board, "1", "RED", 8, "CLOCKWISE");
        support.TestSupport.place(board, "2", "RED", 8, "CLOCKWISE");

        assertFalse(handler.isCapturePossible(mover, 8));
        assertNull(handler.getCapturedPieceAt(8, "YELLOW"));
    }
}
