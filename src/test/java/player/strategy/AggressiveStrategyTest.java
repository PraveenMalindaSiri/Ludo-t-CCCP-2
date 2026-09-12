package player.strategy;

import board.Board;
import org.junit.jupiter.api.Test;
import piece.Piece;
import rules.BlockHandler;
import rules.CaptureHandler;
import rules.RuleEngine;
import testutil.TestHelpers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AggressiveStrategyTest {

    @Test
    void choosesCapturingMoveWithOpponentClosestToHome() {
        Board board = TestHelpers.board();
        CaptureHandler captureHandler = mock(CaptureHandler.class);
        BlockHandler blockHandler = mock(BlockHandler.class);
        RuleEngine ruleEngine = mock(RuleEngine.class);
        AggressiveStrategy strategy = new AggressiveStrategy(captureHandler, blockHandler);

        Piece red1 = TestHelpers.placePiece(board, "RED", "1", 5);
        Piece red2 = TestHelpers.placePiece(board, "RED", "2", 15);
        Piece blueFar = TestHelpers.placePiece(board, "BLUE", "1", 9);
        Piece greenClose = TestHelpers.placePiece(board, "GREEN", "1", 19);

        when(ruleEngine.calculateDestination(red1, 4)).thenReturn(9);
        when(ruleEngine.calculateDestination(red2, 4)).thenReturn(19);
        when(captureHandler.getCapturedPieceAt(9, "RED")).thenReturn(blueFar);
        when(captureHandler.getCapturedPieceAt(19, "RED")).thenReturn(greenClose);
        when(blockHandler.distanceToHomeEntry(blueFar)).thenReturn(20);
        when(blockHandler.distanceToHomeEntry(greenClose)).thenReturn(3);

        Piece chosen = strategy.choosePieceToMove(List.of(red1, red2), 4, board, ruleEngine);

        assertSame(red2, chosen);
    }

    @Test
    void avoidsCreatingSameColourBlockWhenNonBlockingMoveExists() {
        Board board = TestHelpers.board();
        CaptureHandler captureHandler = mock(CaptureHandler.class);
        BlockHandler blockHandler = mock(BlockHandler.class);
        RuleEngine ruleEngine = mock(RuleEngine.class);
        AggressiveStrategy strategy = new AggressiveStrategy(captureHandler, blockHandler);

        Piece red1 = TestHelpers.placePiece(board, "RED", "1", 5);
        Piece red2 = TestHelpers.placePiece(board, "RED", "2", 8);
        when(ruleEngine.calculateDestination(red1, 2)).thenReturn(7);
        when(ruleEngine.calculateDestination(red2, 2)).thenReturn(10);
        when(ruleEngine.isSameColorAtDestination(red1, 7)).thenReturn(true);
        when(ruleEngine.isSameColorAtDestination(red2, 10)).thenReturn(false);

        Piece chosen = strategy.choosePieceToMove(List.of(red1, red2), 2, board, ruleEngine);

        assertSame(red2, chosen);
    }

    @Test
    void redMovesFromBaseOnlyWhenNoBoardPieceCanCapture() {
        Board board = TestHelpers.board();
        CaptureHandler captureHandler = mock(CaptureHandler.class);
        BlockHandler blockHandler = mock(BlockHandler.class);
        RuleEngine ruleEngine = mock(RuleEngine.class);
        AggressiveStrategy strategy = new AggressiveStrategy(captureHandler, blockHandler);

        Piece basePiece = new Piece("1", "RED");
        Piece boardPiece = TestHelpers.placePiece(board, "RED", "2", 10);
        Piece target = TestHelpers.placePiece(board, "BLUE", "1", 16);

        when(ruleEngine.calculateDestination(boardPiece, 6)).thenReturn(16);
        when(captureHandler.getCapturedPieceAt(16, "RED")).thenReturn(target);

        assertFalse(strategy.shouldMoveFromBase(List.of(basePiece, boardPiece), 6, board, ruleEngine));
    }

    @Test
    void redMovesFromBaseWhenNoPieceIsAlreadyOnStandardPath() {
        Board board = TestHelpers.board();
        AggressiveStrategy strategy = new AggressiveStrategy(mock(CaptureHandler.class), mock(BlockHandler.class));

        assertTrue(strategy.shouldMoveFromBase(List.of(new Piece("1", "RED")), 6, board, mock(RuleEngine.class)));
    }
}
