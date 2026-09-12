package player.strategy;

import board.Board;
import mystery.MysteryManager;
import org.junit.jupiter.api.Test;
import piece.Piece;
import rules.RuleEngine;
import testutil.TestHelpers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RandomStrategyTest {

    @Test
    void cyclesThroughPiecesWhenChoosingMoves() {
        Board board = TestHelpers.board();
        MysteryManager mysteryManager = mock(MysteryManager.class);
        RuleEngine ruleEngine = mock(RuleEngine.class);
        Piece p1 = TestHelpers.placePiece(board, "BLUE", "1", 10);
        Piece p2 = TestHelpers.placePiece(board, "BLUE", "2", 20);
        RandomStrategy strategy = new RandomStrategy(List.of(p1, p2), mysteryManager);

        assertSame(p1, strategy.choosePieceToMove(List.of(p1, p2), 2, board, ruleEngine));
        assertSame(p2, strategy.choosePieceToMove(List.of(p1, p2), 2, board, ruleEngine));
        assertSame(p1, strategy.choosePieceToMove(List.of(p1, p2), 2, board, ruleEngine));
    }

    @Test
    void counterClockwisePieceSeeksMysteryCell() {
        Board board = TestHelpers.board();
        MysteryManager mysteryManager = mock(MysteryManager.class);
        RuleEngine ruleEngine = mock(RuleEngine.class);
        Piece p1 = TestHelpers.placePiece(board, "BLUE", "1", 10, "COUNTERCLOCKWISE");
        Piece p2 = TestHelpers.placePiece(board, "BLUE", "2", 20, "CLOCKWISE");
        RandomStrategy strategy = new RandomStrategy(List.of(p1, p2), mysteryManager);

        when(mysteryManager.isActive()).thenReturn(true);
        when(ruleEngine.calculateDestination(p1, 3)).thenReturn(7);
        when(ruleEngine.calculateDestination(p2, 3)).thenReturn(23);
        when(mysteryManager.isOnMysteryCell(7)).thenReturn(false);
        when(mysteryManager.isOnMysteryCell(23)).thenReturn(true);

        Piece chosen = strategy.choosePieceToMove(List.of(p1, p2), 3, board, ruleEngine);

        assertSame(p2, chosen);
    }

    @Test
    void clockwisePieceAvoidsMysteryCellWhenAlternativeExists() {
        Board board = TestHelpers.board();
        MysteryManager mysteryManager = mock(MysteryManager.class);
        RuleEngine ruleEngine = mock(RuleEngine.class);
        Piece p1 = TestHelpers.placePiece(board, "BLUE", "1", 10, "CLOCKWISE");
        Piece p2 = TestHelpers.placePiece(board, "BLUE", "2", 20, "CLOCKWISE");
        RandomStrategy strategy = new RandomStrategy(List.of(p1, p2), mysteryManager);

        when(mysteryManager.isActive()).thenReturn(true);
        when(ruleEngine.calculateDestination(p1, 4)).thenReturn(14);
        when(ruleEngine.calculateDestination(p2, 4)).thenReturn(24);
        when(mysteryManager.isOnMysteryCell(14)).thenReturn(true);
        when(mysteryManager.isOnMysteryCell(24)).thenReturn(false);

        Piece chosen = strategy.choosePieceToMove(List.of(p1, p2), 4, board, ruleEngine);

        assertSame(p2, chosen);
    }

    @Test
    void shouldMoveFromBaseFollowsCurrentCyclicPieceAndRequiresSix() {
        Board board = TestHelpers.board();
        MysteryManager mysteryManager = mock(MysteryManager.class);
        RuleEngine ruleEngine = mock(RuleEngine.class);
        Piece base = new Piece("1", "BLUE");
        Piece boardPiece = TestHelpers.placePiece(board, "BLUE", "2", 20);
        RandomStrategy strategy = new RandomStrategy(List.of(base, boardPiece), mysteryManager);

        assertFalse(strategy.shouldMoveFromBase(List.of(base, boardPiece), 5, board, ruleEngine));
        assertTrue(strategy.shouldMoveFromBase(List.of(base, boardPiece), 6, board, ruleEngine));
    }
}
