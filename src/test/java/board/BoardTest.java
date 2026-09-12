package board;

import factory.BoardFactory;
import org.junit.jupiter.api.Test;
import piece.Piece;

import static org.junit.jupiter.api.Assertions.*;

public class BoardTest {
    @Test
    void homeStraightCellAcceptsOnlyOwnColour() {
        // Arrange
        Board board = BoardFactory.createBoard();

        Piece red = new Piece("1", "RED");
        Piece blue = new Piece("1", "BLUE");

        HomeStraightCell redHomeStraight = board.getHomeStraightCell("RED", 0);

        // Act + Assert
        assertTrue(redHomeStraight.canAcceptPiece(red));
        assertFalse(redHomeStraight.canAcceptPiece(blue));
    }

    @Test
    void baseCellAcceptsOnlyOwnColour() {
        // Arrange
        Board board = BoardFactory.createBoard();

        Piece red = new Piece("1", "RED");
        Piece green = new Piece("1", "GREEN");

        BaseCell redBase = board.getBaseCell("RED");

        // Act + Assert
        assertTrue(redBase.canAcceptPiece(red));
        assertFalse(redBase.canAcceptPiece(green));
    }

    @Test
    void homeCellAcceptsOnlyOwnColour() {
        // Arrange
        Board board = BoardFactory.createBoard();

        Piece yellow = new Piece("1", "YELLOW");
        Piece blue = new Piece("1", "BLUE");

        HomeCell yellowHome = board.getHomeCell("YELLOW");

        // Act + Assert
        assertTrue(yellowHome.canAcceptPiece(yellow));
        assertFalse(yellowHome.canAcceptPiece(blue));
    }

    @Test
    void getHomeStraightCellReturnsCorrectOwnerAndIndex() {
        // Arrange
        Board board = BoardFactory.createBoard();

        // Act
        HomeStraightCell redCell = board.getHomeStraightCell("RED", 0);
        HomeStraightCell greenCell = board.getHomeStraightCell("GREEN", 4);

        // Assert
        assertEquals("RED", redCell.getOwnerColor());
        assertEquals(0, redCell.getIndex());
        assertEquals("REDhomepath0", redCell.toString());

        assertEquals("GREEN", greenCell.getOwnerColor());
        assertEquals(4, greenCell.getIndex());
        assertEquals("GREENhomepath4", greenCell.toString());
    }
}
