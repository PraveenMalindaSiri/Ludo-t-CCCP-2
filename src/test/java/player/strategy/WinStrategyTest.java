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

class WinStrategyTest {

    @Test
    void choosesPieceThatNeedsCaptureWhenItCanCaptureNow() {
        Board board = TestHelpers.board();
        CaptureHandler captureHandler = mock(CaptureHandler.class);
        BlockHandler blockHandler = mock(BlockHandler.class);
        RuleEngine ruleEngine = mock(RuleEngine.class);
        WinStrategy strategy = new WinStrategy(captureHandler, blockHandler);

        Piece needingCapture = TestHelpers.placePiece(board, "YELLOW", "1", 10);
        Piece alreadyQualified = TestHelpers.placePiece(board, "YELLOW", "2", 20);
        alreadyQualified.incrementCaptureCount();
        Piece target = TestHelpers.placePiece(board, "RED", "1", 14);

        when(ruleEngine.calculateDestination(needingCapture, 4)).thenReturn(14);
        when(captureHandler.getCapturedPieceAt(14, "YELLOW")).thenReturn(target);

        Piece chosen = strategy.choosePieceToMove(List.of(alreadyQualified, needingCapture), 4, board, ruleEngine);

        assertSame(needingCapture, chosen);
    }

    @Test
    void otherwiseChoosesPieceClosestToHomeEntry() {
        Board board = TestHelpers.board();
        CaptureHandler captureHandler = mock(CaptureHandler.class);
        BlockHandler blockHandler = mock(BlockHandler.class);
        RuleEngine ruleEngine = mock(RuleEngine.class);
        WinStrategy strategy = new WinStrategy(captureHandler, blockHandler);

        Piece far = TestHelpers.placePiece(board, "YELLOW", "1", 5);
        far.incrementCaptureCount();
        Piece close = TestHelpers.placePiece(board, "YELLOW", "2", 45);
        close.incrementCaptureCount();
        when(blockHandler.distanceToHomeEntry(far)).thenReturn(40);
        when(blockHandler.distanceToHomeEntry(close)).thenReturn(6);

        Piece chosen = strategy.choosePieceToMove(List.of(far, close), 3, board, ruleEngine);

        assertSame(close, chosen);
    }
}
