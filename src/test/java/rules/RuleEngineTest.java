package rules;

import static org.junit.jupiter.api.Assertions.*;

import block.Block;
import board.Board;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import piece.Piece;
import piece.state.FrozenState;
import piece.state.SickState;
import player.YellowPlayer;
import support.TestSupport;

class RuleEngineTest {
    private Board board;
    private BlockHandler blockHandler;
    private RuleEngine rules;

    @BeforeEach
    void setUp() {
        board = TestSupport.newBoard();
        blockHandler = new BlockHandler(board);
        rules = new RuleEngine(board, blockHandler);
    }

    @Test
    void baseEntryRequiresSix() {
        Piece piece = new Piece("1", "YELLOW");

        assertFalse(rules.isValidMove(piece, 5));
        assertTrue(rules.isValidMove(piece, 6));
    }

    @Test
    void destinationUsesStateAndDirectionWithoutMutatingPiece() {
        Piece piece = TestSupport.place(board, "1", "YELLOW", 10, "COUNTERCLOCKWISE");
        piece.setState(new SickState(4));

        int destination = rules.calculateDestination(piece, 5);

        assertEquals(8, destination);
        assertEquals(10, piece.getPosition());
        assertSame(piece, board.getCellAt(10).getPieces().getFirst());
    }

    @Test
    void frozenAndHomePiecesAreNotValidMoves() {
        Piece frozen = TestSupport.place(board, "1", "YELLOW", 10, "CLOCKWISE");
        frozen.setState(new FrozenState(4));
        Piece home = new Piece("2", "YELLOW");
        board.moveToHome(home);

        assertFalse(rules.isValidMove(frozen, 6));
        assertFalse(rules.isValidMove(home, 6));
    }

    @Test
    void homeStraightRejectsOvershootButAcceptsExactRoll() {
        Piece piece = new Piece("1", "YELLOW");
        board.moveToHomeStraight(piece, 3);

        assertTrue(rules.needsExactRoll(piece, 2));
        assertTrue(rules.isValidMove(piece, 2));
        assertTrue(rules.overshotsHome(piece, 3));
        assertFalse(rules.isValidMove(piece, 3));
    }

    @Test
    void clearAlternativeRemovesOnlyPathBlockedChoice() {
        Piece blocked = TestSupport.place(board, "1", "YELLOW", 0, "CLOCKWISE");
        Piece clear = TestSupport.place(board, "2", "YELLOW", 20, "CLOCKWISE");
        Piece defenderOne = TestSupport.place(board, "1", "RED", 3, "CLOCKWISE");
        Piece defenderTwo = TestSupport.place(board, "2", "RED", 3, "CLOCKWISE");
        blockHandler.createBlock(defenderOne, defenderTwo, board.getCellAt(3));
        YellowPlayer player =
                new YellowPlayer(List.of(blocked, clear), new TestSupport.FirstPieceStrategy(true));

        List<Piece> valid = rules.getValidMoves(player, 4);

        assertFalse(valid.contains(blocked));
        assertTrue(valid.contains(clear));
    }

    @Test
    void partiallyBlockedPieceRemainsOptionWhenNoClearMoveExists() {
        Piece blocked = TestSupport.place(board, "1", "YELLOW", 0, "CLOCKWISE");
        Piece defenderOne = TestSupport.place(board, "1", "RED", 3, "CLOCKWISE");
        Piece defenderTwo = TestSupport.place(board, "2", "RED", 3, "CLOCKWISE");
        blockHandler.createBlock(defenderOne, defenderTwo, board.getCellAt(3));
        YellowPlayer player =
                new YellowPlayer(List.of(blocked), new TestSupport.FirstPieceStrategy(true));

        assertEquals(List.of(blocked), rules.getValidMoves(player, 4));
    }

    @Test
    void nonBlockableSameColorLandingIsRejected() {
        Piece mover = TestSupport.place(board, "1", "YELLOW", 0, "CLOCKWISE");
        Piece teammate = TestSupport.place(board, "2", "YELLOW", 4, "CLOCKWISE");
        teammate.setState(new SickState(4));
        YellowPlayer player =
                new YellowPlayer(
                        List.of(mover, teammate), new TestSupport.FirstPieceStrategy(true));

        List<Piece> valid = rules.getValidMoves(player, 4);

        assertFalse(valid.contains(mover));
    }

    @Test
    void repeatedQueriesDoNotMovePiecesOrChangeBlockRegistry() {
        Piece first = TestSupport.place(board, "1", "YELLOW", 10, "CLOCKWISE");
        Piece second = TestSupport.place(board, "2", "YELLOW", 10, "CLOCKWISE");
        Block block = blockHandler.createBlock(first, second, board.getCellAt(10));
        YellowPlayer player =
                new YellowPlayer(List.of(first, second), new TestSupport.FirstPieceStrategy(true));

        for (int i = 0; i < 10; i++) rules.getValidMoves(player, 6);

        assertEquals(10, first.getPosition());
        assertEquals(10, second.getPosition());
        assertEquals(1, blockHandler.getActiveBlocks().size());
        assertSame(block, blockHandler.findBlockAt(board.getCellAt(10)));
    }
}
