package engine.command;

import board.Board;
import board.Cell;
import dice.ICoinToss;
import org.junit.jupiter.api.Test;
import piece.Piece;
import rules.CaptureHandler;
import testutil.TestHelpers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CommandTest {

    @Test
    void moveCommandMovesPieceBetweenCellsAndUpdatesPosition() {
        Board board = TestHelpers.board();
        Piece piece = TestHelpers.placePiece(board, "RED", "1", 10, "CLOCKWISE");
        Cell from = board.getCellAt(10);
        Cell to = board.getCellAt(14);

        new MoveCommand(piece, from, to, 4).execute();

        assertFalse(from.getPieces().contains(piece));
        assertTrue(to.getPieces().contains(piece));
        assertEquals(14, piece.getPosition());
    }

    @Test
    void enterBoardCommandMovesPieceFromBaseToStartingCellAndSetsDirectionFromCoinToss() {
        Board board = TestHelpers.board();
        Piece piece = TestHelpers.basePiece(board, "RED", "1");
        ICoinToss coinToss = mock(ICoinToss.class);
        when(coinToss.toss()).thenReturn("HEADS");

        EnterBoardCommand command = new EnterBoardCommand(
                piece,
                board.getStartingCell("RED"),
                board.getBaseCell("RED"),
                coinToss);
        command.execute();

        assertFalse(board.getBaseCell("RED").getPieces().contains(piece));
        assertTrue(board.getStartingCell("RED").getPieces().contains(piece));
        assertEquals(board.getStartingPosition("RED"), piece.getPosition());
        assertEquals("CLOCKWISE", piece.getDirection());
        assertEquals("CLOCKWISE", piece.getOriginalDirection());
        assertEquals("CLOCKWISE", command.getResultDirection());
    }

    @Test
    void enterBoardCommandSetsCounterClockwiseDirectionForTails() {
        Board board = TestHelpers.board();
        Piece piece = TestHelpers.basePiece(board, "BLUE", "1");
        ICoinToss coinToss = mock(ICoinToss.class);
        when(coinToss.toss()).thenReturn("TAILS");

        EnterBoardCommand command = new EnterBoardCommand(
                piece,
                board.getStartingCell("BLUE"),
                board.getBaseCell("BLUE"),
                coinToss);
        command.execute();

        assertEquals("COUNTERCLOCKWISE", piece.getDirection());
        assertEquals("COUNTERCLOCKWISE", piece.getOriginalDirection());
    }

    @Test
    void captureCommandDelegatesToCaptureHandler() {
        CaptureHandler captureHandler = mock(CaptureHandler.class);
        Piece capturer = new Piece("1", "RED");
        Piece captured = new Piece("1", "BLUE");

        new CaptureCommand(capturer, captured, captureHandler).execute();

        verify(captureHandler).handleCapture(capturer, captured);
    }
}
