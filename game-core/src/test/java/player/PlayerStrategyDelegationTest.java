package player;

import static org.junit.jupiter.api.Assertions.*;

import board.Board;
import java.util.List;
import org.junit.jupiter.api.Test;
import piece.Piece;
import rules.BlockHandler;
import rules.RuleEngine;
import support.TestSupport;

class PlayerStrategyDelegationTest {

    @Test
    void playerUsesInjectedStrategyForMoveSelection() {
        Board board = TestSupport.newBoard();
        RuleEngine rules = new RuleEngine(board, new BlockHandler(board));
        Piece first = TestSupport.place(board, "1", "YELLOW", 4, "CLOCKWISE");
        Piece second = TestSupport.place(board, "2", "YELLOW", 8, "CLOCKWISE");
        TestSupport.FirstPieceStrategy strategy = new TestSupport.FirstPieceStrategy(true);
        YellowPlayer player = new YellowPlayer(List.of(first, second), strategy);

        Piece chosen = player.selectMove(List.of(second, first), 4, board, rules);

        assertSame(second, chosen);
        assertEquals(1, strategy.chooseCalls);
    }

    @Test
    void playerUsesInjectedStrategyForBaseDecision() {
        Board board = TestSupport.newBoard();
        RuleEngine rules = new RuleEngine(board, new BlockHandler(board));
        Piece piece = new Piece("1", "YELLOW");
        TestSupport.FirstPieceStrategy strategy = new TestSupport.FirstPieceStrategy(false);
        YellowPlayer player = new YellowPlayer(List.of(piece), strategy);

        boolean decision = player.shouldMoveFromBase(6, board, rules);

        assertFalse(decision);
        assertEquals(1, strategy.baseDecisionCalls);
    }

    @Test
    void playerDoesNotCallStrategyWhenNoValidMoveExists() {
        Board board = TestSupport.newBoard();
        RuleEngine rules = new RuleEngine(board, new BlockHandler(board));
        TestSupport.FirstPieceStrategy strategy = new TestSupport.FirstPieceStrategy(true);
        YellowPlayer player = new YellowPlayer(List.of(), strategy);

        assertNull(player.selectMove(List.of(), 2, board, rules));
        assertEquals(0, strategy.chooseCalls);
    }
}
