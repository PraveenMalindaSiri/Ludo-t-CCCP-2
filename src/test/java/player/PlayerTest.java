package player;

import board.Board;
import factory.BoardFactory;
import org.junit.jupiter.api.Test;
import piece.Piece;
import player.strategy.IPlayerStrategy;
import rules.BlockHandler;
import rules.CaptureHandler;
import rules.RuleEngine;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PlayerTest {

    @Test
    void playerReturnsPiecesInBase() {
        // Arrange
        Piece basePiece = new Piece("1", "RED");
        Piece boardPiece = new Piece("2", "RED");
        boardPiece.moveToPosition(10);

        TestPlayer player = new TestPlayer("RED", List.of(basePiece, boardPiece));

        // Act
        List<Piece> piecesInBase = player.getPiecesInBase();

        // Assert
        assertEquals(List.of(basePiece), piecesInBase);
    }

    @Test
    void playerReturnsPiecesOnBoard() {
        // Arrange
        Piece basePiece = new Piece("1", "RED");
        Piece boardPiece = new Piece("2", "RED");
        Piece homeStraightPiece = new Piece("3", "RED");

        boardPiece.moveToPosition(10);
        homeStraightPiece.moveToHomeStraight(2);

        TestPlayer player = new TestPlayer("RED", List.of(basePiece, boardPiece, homeStraightPiece));

        // Act
        List<Piece> piecesOnBoard = player.getPiecesOnBoard();

        // Assert
        assertEquals(List.of(boardPiece, homeStraightPiece), piecesOnBoard);
    }

    @Test
    void playerReturnsPiecesAtHome() {
        // Arrange
        Piece homePiece = new Piece("1", "RED");
        Piece basePiece = new Piece("2", "RED");
        homePiece.moveToHome();

        TestPlayer player = new TestPlayer("RED", List.of(homePiece, basePiece));

        // Act
        List<Piece> piecesAtHome = player.getPiecesAtHome();

        // Assert
        assertEquals(List.of(homePiece), piecesAtHome);
    }

    @Test
    void playerHasWonOnlyWhenAllPiecesAreHome() {
        // Arrange
        Piece p1 = new Piece("1", "RED");
        Piece p2 = new Piece("2", "RED");
        p1.moveToHome();

        TestPlayer player = new TestPlayer("RED", List.of(p1, p2));

        // Act + Assert
        assertFalse(player.hasWon());

        p2.moveToHome();
        assertTrue(player.hasWon());
    }

    @Test
    void consecutiveSixesCanIncrementAndReset() {
        // Arrange
        TestPlayer player = new TestPlayer("RED", List.of());

        // Act
        player.incrementConsecutiveSixes();
        player.incrementConsecutiveSixes();

        // Assert
        assertEquals(2, player.getConsecutiveSixes());

        // Act
        player.resetConsecutiveSixes();

        // Assert
        assertEquals(0, player.getConsecutiveSixes());
    }

    @Test
    void selectMoveReturnsNullWhenNoValidMovesExist() {
        // Arrange
        Board board = BoardFactory.createBoard();
        BlockHandler blockHandler = new BlockHandler(board);
        CaptureHandler captureHandler = new CaptureHandler(board);
        RuleEngine ruleEngine = new RuleEngine(board, blockHandler, captureHandler);
        TestPlayer player = new TestPlayer("RED", List.of());

        // Act
        Piece selected = player.selectMove(List.of(), 6, board, ruleEngine);

        // Assert
        assertNull(selected);
    }

    private static class TestPlayer extends Player {
        TestPlayer(String color, List<Piece> pieces) {
            super(color, color, pieces, new FirstMoveStrategy());
        }

        @Override
        protected Piece choosePieceToMove(List<Piece> pieces, int diceValue,
                                          Board board, RuleEngine ruleEngine) {
            return strategy.choosePieceToMove(pieces, diceValue, board, ruleEngine);
        }
    }

    private static class FirstMoveStrategy implements IPlayerStrategy {
        @Override
        public Piece choosePieceToMove(List<Piece> validMoves, int diceValue,
                                       Board board, RuleEngine ruleEngine) {
            return validMoves.isEmpty() ? null : validMoves.getFirst();
        }

        @Override
        public boolean shouldMoveFromBase(List<Piece> pieces, int diceValue,
                                          Board board, RuleEngine ruleEngine) {
            return false;
        }
    }
}