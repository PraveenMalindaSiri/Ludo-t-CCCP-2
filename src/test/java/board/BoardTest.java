package board;

import config.GameConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import piece.Piece;

import static org.junit.jupiter.api.Assertions.*;

class BoardTest {
    private Board board;

    @BeforeEach
    void setUp() {
        board = support.TestSupport.newBoard();
    }

    @Test
    void factoryBuildsCompleteBoardWithSpecialCells() {
        GameConfig config = GameConfig.getInstance();

        assertEquals(52, board.getStandardPath().size());
        assertSame(board.getStartingCell("yellow"),
                board.getCellAt(config.getYellowStart()));
        assertSame(board.getApproachCell("YELLOW"),
                board.getCellAt(config.getYellowApproach()));
        assertEquals(5, board.getHomeStraight("YELLOW").size());
        assertEquals(Piece.BASE_POSITION, board.getBaseCell("YELLOW").getPosition());
        assertEquals(Piece.HOME_POSITION, board.getHomeCell("YELLOW").getPosition());
    }

    @Test
    void controlledRelocationKeepsCellAndPieceStateSynchronized() {
        Piece piece = new Piece("1", "YELLOW");

        board.initializeInBase(piece);
        assertTrue(board.getBaseCell("YELLOW").getPieces().contains(piece));

        board.enterBoard(piece);
        int start = board.getStartingPosition("YELLOW");
        assertFalse(board.getBaseCell("YELLOW").getPieces().contains(piece));
        assertTrue(board.getCellAt(start).getPieces().contains(piece));

        board.moveOnStandardPath(piece, 4, start + 4);
        assertFalse(board.getCellAt(start).getPieces().contains(piece));
        assertTrue(board.getCellAt(start + 4).getPieces().contains(piece));

        board.moveToHomeStraight(piece, 2);
        assertTrue(piece.isInHomeStraight());
        assertTrue(board.getHomeStraightCell("YELLOW", 2).getPieces().contains(piece));

        board.moveToHome(piece);
        assertTrue(piece.isAtHome());
        assertTrue(board.getHomeCell("YELLOW").getPieces().contains(piece));

        board.sendToBase(piece);
        assertTrue(piece.isInBase());
        assertTrue(board.getBaseCell("YELLOW").getPieces().contains(piece));
        assertFalse(board.getHomeCell("YELLOW").getPieces().contains(piece));
    }

    @Test
    void specialCellsRejectPiecesOwnedByAnotherColor() {
        Piece red = new Piece("1", "RED");

        assertThrows(IllegalArgumentException.class,
                () -> board.getBaseCell("YELLOW").addPiece(red));
        assertThrows(IllegalArgumentException.class,
                () -> board.getHomeStraightCell("YELLOW", 0).addPiece(red));
        assertThrows(IllegalArgumentException.class,
                () -> board.getHomeCell("YELLOW").addPiece(red));
    }

    @Test
    void cellAndBoardQueriesReturnDefensiveCopies() {
        Piece piece = support.TestSupport.place(
                board, "1", "YELLOW", 7, "CLOCKWISE");

        board.getCellAt(7).getPieces().clear();
        board.getStandardPath().clear();

        assertTrue(board.getCellAt(7).getPieces().contains(piece));
        assertEquals(52, board.getStandardPath().size());
    }

    @Test
    void invalidBoardRequestsFailClearly() {
        assertThrows(IllegalArgumentException.class, () -> board.getCellAt(-1));
        assertThrows(IllegalArgumentException.class, () -> board.getCellAt(52));
        assertThrows(IllegalArgumentException.class,
                () -> board.getStartingCell("PURPLE"));
        assertThrows(IllegalArgumentException.class,
                () -> board.getBaseCell("PURPLE"));
    }
}
