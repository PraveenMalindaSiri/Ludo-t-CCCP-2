package player.strategy;

import board.Board;
import org.junit.jupiter.api.Test;
import piece.Piece;
import rules.BlockHandler;
import rules.RuleEngine;
import testutil.TestHelpers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BlockStrategyTest {

    @Test
    void prioritisesPieceAlreadyInHomeStraight() {
        Board board = TestHelpers.board();
        BlockHandler blockHandler = mock(BlockHandler.class);
        RuleEngine ruleEngine = mock(RuleEngine.class);
        BlockStrategy strategy = new BlockStrategy(blockHandler);

        Piece normal = TestHelpers.placePiece(board, "GREEN", "1", 10);
        Piece homeStraight = new Piece("2", "GREEN");
        homeStraight.moveToHomeStraight(3);

        Piece chosen = strategy.choosePieceToMove(List.of(normal, homeStraight), 2, board, ruleEngine);

        assertSame(homeStraight, chosen);
    }

    @Test
    void prioritisesMovingExistingBlockBeforeCreatingNewBlock() {
        Board board = TestHelpers.board();
        BlockHandler blockHandler = mock(BlockHandler.class);
        RuleEngine ruleEngine = mock(RuleEngine.class);
        BlockStrategy strategy = new BlockStrategy(blockHandler);

        Piece blockPiece = TestHelpers.placePiece(board, "GREEN", "1", 10);
        blockPiece.setInBlock(true);
        Piece other = TestHelpers.placePiece(board, "GREEN", "2", 20);

        when(blockHandler.canBeInBlock(blockPiece)).thenReturn(true);

        Piece chosen = strategy.choosePieceToMove(List.of(other, blockPiece), 6, board, ruleEngine);

        assertSame(blockPiece, chosen);
    }

    @Test
    void choosesMoveThatCanFormBlock() {
        Board board = TestHelpers.board();
        BlockHandler blockHandler = mock(BlockHandler.class);
        RuleEngine ruleEngine = mock(RuleEngine.class);
        BlockStrategy strategy = new BlockStrategy(blockHandler);

        Piece p1 = TestHelpers.placePiece(board, "GREEN", "1", 10);
        Piece p2 = TestHelpers.placePiece(board, "GREEN", "2", 20);
        when(ruleEngine.calculateDestination(p1, 3)).thenReturn(13);
        when(ruleEngine.calculateDestination(p2, 3)).thenReturn(23);
        when(ruleEngine.canFormBlock(p1, 13)).thenReturn(false);
        when(ruleEngine.canFormBlock(p2, 23)).thenReturn(true);

        Piece chosen = strategy.choosePieceToMove(List.of(p1, p2), 3, board, ruleEngine);

        assertSame(p2, chosen);
    }

    @Test
    void greenDoesNotMoveFromBaseWhenBoardMoveCanCreateBlock() {
        Board board = TestHelpers.board();
        BlockHandler blockHandler = mock(BlockHandler.class);
        RuleEngine ruleEngine = mock(RuleEngine.class);
        BlockStrategy strategy = new BlockStrategy(blockHandler);

        Piece moving = TestHelpers.placePiece(board, "GREEN", "1", 10);
        TestHelpers.placePiece(board, "GREEN", "2", 16);
        Piece basePiece = new Piece("3", "GREEN");
        when(ruleEngine.calculateDestination(moving, 6)).thenReturn(16);
        when(blockHandler.isSameColorBlock(eq(moving), any())).thenReturn(false);

        assertFalse(strategy.shouldMoveFromBase(List.of(moving, basePiece), 6, board, ruleEngine));
    }
}
