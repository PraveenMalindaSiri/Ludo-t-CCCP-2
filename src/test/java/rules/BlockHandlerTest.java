package rules;

import block.Block;
import board.Board;
import board.Cell;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import piece.Piece;
import piece.state.EnergizedState;
import testutil.TestHelpers;
import player.Player;
import player.strategy.IPlayerStrategy;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BlockHandlerTest {
    private Board board;
    private BlockHandler blockHandler;

    @BeforeEach
    void setUp() {
        board = TestHelpers.board();
        blockHandler = new BlockHandler(board);
    }

    @Test
    void onlyNormalStandardPathPiecesCanBeInBlock() {
        Piece normal = TestHelpers.placePiece(board, "GREEN", "1", 10);
        Piece energized = TestHelpers.placePiece(board, "GREEN", "2", 11);
        energized.setState(new EnergizedState(4));
        Piece base = new Piece("3", "GREEN");
        Piece homeStraight = new Piece("4", "GREEN");
        homeStraight.moveToHomeStraight(0);

        assertTrue(blockHandler.canBeInBlock(normal));
        assertFalse(blockHandler.canBeInBlock(energized));
        assertFalse(blockHandler.canBeInBlock(base));
        assertFalse(blockHandler.canBeInBlock(homeStraight));
    }

    @Test
    void createBlockMarksPiecesAndTracksActiveBlock() {
        Cell cell = board.getCellAt(10);
        Piece p1 = TestHelpers.placePiece(board, "GREEN", "1", 10);
        Piece p2 = TestHelpers.placePiece(board, "GREEN", "2", 10);

        Block block = blockHandler.createBlock(p2, p1, cell);

        assertNotNull(block);
        assertEquals(2, block.getSize());
        assertTrue(p1.isInBlock());
        assertTrue(p2.isInBlock());
        assertSame(block, blockHandler.findBlockAt(cell));
        assertEquals(block, blockHandler.getActiveBlocks().get(10));
    }

    @Test
    void opponentPieceIsBlockedByActiveBlockAtDestination() {
        Cell cell = board.getCellAt(10);
        Piece g1 = TestHelpers.placePiece(board, "GREEN", "1", 10);
        Piece g2 = TestHelpers.placePiece(board, "GREEN", "2", 10);
        blockHandler.createBlock(g2, g1, cell);
        Piece red = TestHelpers.placePiece(board, "RED", "1", 5);

        assertTrue(blockHandler.isBlockedByOpponent(red, 10));
        assertFalse(blockHandler.isBlockedByOpponent(g1, 10));
    }

    @Test
    void maxMoveBeforeBlockReturnsCellImmediatelyBeforeOpponentBlock() {
        Cell blockCell = board.getCellAt(10);
        Piece g1 = TestHelpers.placePiece(board, "GREEN", "1", 10);
        Piece g2 = TestHelpers.placePiece(board, "GREEN", "2", 10);
        blockHandler.createBlock(g2, g1, blockCell);
        Piece red = TestHelpers.placePiece(board, "RED", "1", 5, "CLOCKWISE");

        assertEquals(9, blockHandler.getMaxMoveBeforeBlock(red, 6));
    }

    @Test
    void moveBlockMovesByDiceValueDividedByBlockSize() {
        Cell oldCell = board.getCellAt(10);
        Piece p1 = TestHelpers.placePiece(board, "GREEN", "1", 10, "CLOCKWISE");
        Piece p2 = TestHelpers.placePiece(board, "GREEN", "2", 10, "CLOCKWISE");
        Block block = blockHandler.createBlock(p2, p1, oldCell);

        blockHandler.moveBlock(block, 6);

        assertEquals(13, p1.getPosition());
        assertEquals(13, p2.getPosition());
        assertFalse(oldCell.getPieces().contains(p1));
        assertTrue(board.getCellAt(13).getPieces().contains(p1));
        assertTrue(board.getCellAt(13).getPieces().contains(p2));
        assertSame(block, blockHandler.findBlockAt(board.getCellAt(13)));
    }

    @Test
    void blockCaptureResetsDefendersAndIncrementsAttackers() {
        Cell attackCell = board.getCellAt(10);
        Piece a1 = TestHelpers.placePiece(board, "RED", "1", 10);
        Piece a2 = TestHelpers.placePiece(board, "RED", "2", 10);
        Block attackers = blockHandler.createBlock(a2, a1, attackCell);

        Cell defendCell = board.getCellAt(14);
        Piece d1 = TestHelpers.placePiece(board, "BLUE", "1", 14);
        Piece d2 = TestHelpers.placePiece(board, "BLUE", "2", 14);
        Block defenders = blockHandler.createBlock(d2, d1, defendCell);

        assertTrue(blockHandler.canBlockCaptureBlock(attackers, defenders));
        blockHandler.handleBlockCapture(attackers, defenders);

        assertTrue(d1.isInBase());
        assertTrue(d2.isInBase());
        assertTrue(board.getBaseCell("BLUE").getPieces().contains(d1));
        assertEquals(1, a1.getCaptureCount());
        assertEquals(1, a2.getCaptureCount());
        assertNull(blockHandler.findBlockAt(defendCell));
    }

    @Test
    void breakingBlockRestoresOriginalDirectionOfRemovedPiece() {
        Cell cell = board.getCellAt(10);
        Piece p1 = TestHelpers.placePiece(board, "GREEN", "1", 10, "CLOCKWISE");
        Piece p2 = TestHelpers.placePiece(board, "GREEN", "2", 10, "COUNTERCLOCKWISE");
        p2.setOriginalDirection("COUNTERCLOCKWISE");
        Block block = blockHandler.createBlock(p2, p1, cell);
        p2.setDirection("CLOCKWISE");

        blockHandler.breakBlock(p2, block);

        assertFalse(p2.isInBlock());
        assertEquals("COUNTERCLOCKWISE", p2.getDirection());
    }

    @Test
    void addToBlockAllowsThirdNormalPieceToJoinExistingBlock() {
        // Arrange
        Cell cell = board.getCellAt(10);

        Piece p1 = TestHelpers.placePiece(board, "GREEN", "1", 10);
        Piece p2 = TestHelpers.placePiece(board, "GREEN", "2", 10);
        Piece p3 = TestHelpers.placePiece(board, "GREEN", "3", 10);

        Block block = blockHandler.createBlock(p2, p1, cell);

        // Act
        blockHandler.addToBlock(p3, block, cell);

        // Assert
        assertEquals(3, block.getSize());
        assertTrue(p3.isInBlock());
        assertSame(block, blockHandler.findBlockAt(cell));
    }

    @Test
    void blockCannotMoveWhenDiceValueIsSmallerThanBlockSize() {
        // Arrange
        Cell cell = board.getCellAt(10);

        Piece p1 = TestHelpers.placePiece(board, "GREEN", "1", 10);
        Piece p2 = TestHelpers.placePiece(board, "GREEN", "2", 10);
        Piece p3 = TestHelpers.placePiece(board, "GREEN", "3", 10);

        Block block = blockHandler.createBlock(p2, p1, cell);
        blockHandler.addToBlock(p3, block, cell);

        // Act
        blockHandler.moveBlock(block, 2);

        // Assert
        assertFalse(blockHandler.canBlockMove(block, 2));
        assertEquals(10, p1.getPosition());
        assertEquals(10, p2.getPosition());
        assertEquals(10, p3.getPosition());
        assertSame(block, blockHandler.findBlockAt(cell));
    }

    @Test
    void tripleSixBlockBreakKeepsClosestPieceAndMovesOthersByOriginalDirection() {
        // Arrange
        Cell cell = board.getCellAt(10);

        Piece keepPiece = TestHelpers.placePiece(board, "GREEN", "1", 10, "CLOCKWISE");
        Piece movedPiece = TestHelpers.placePiece(board, "GREEN", "2", 10, "COUNTERCLOCKWISE");
        movedPiece.setOriginalDirection("COUNTERCLOCKWISE");

        blockHandler.createBlock(movedPiece, keepPiece, cell);

        Player player = new DummyPlayer("GREEN", List.of(keepPiece, movedPiece));

        // Act
        List<Piece> movedPieces = blockHandler.handleTripleSixBlockBreak(player);

        // Assert
        assertEquals(List.of(movedPiece), movedPieces);
        assertEquals(10, keepPiece.getPosition());
        assertEquals(4, movedPiece.getPosition());
        assertEquals("COUNTERCLOCKWISE", movedPiece.getDirection());

        assertFalse(keepPiece.isInBlock());
        assertFalse(movedPiece.isInBlock());

        assertTrue(board.getCellAt(4).getPieces().contains(movedPiece));
        assertFalse(board.getCellAt(10).getPieces().contains(movedPiece));
    }

    @Test
    void staleBlockIsCleanedWhenOnePieceLeavesTheCell() {
        // Arrange
        Cell cell = board.getCellAt(10);

        Piece p1 = TestHelpers.placePiece(board, "GREEN", "1", 10);
        Piece p2 = TestHelpers.placePiece(board, "GREEN", "2", 10);
        blockHandler.createBlock(p2, p1, cell);

        Piece red = TestHelpers.placePiece(board, "RED", "1", 5);

        // Simulate a stale block member after capture/reset.
        p2.moveToBase();

        // Act
        boolean blocked = blockHandler.isBlockedByOpponent(red, 10);

        // Assert
        assertFalse(blocked);
        assertNull(blockHandler.findBlockAt(cell));
        assertFalse(p1.isInBlock());
        assertFalse(p2.isInBlock());
    }

    @Test
    void blockDirectionUsesPieceFarthestFromHome() {
        // Arrange
        Cell cell = board.getCellAt(20);

        Piece clockwise = TestHelpers.placePiece(board, "RED", "1", 20, "CLOCKWISE");
        Piece counterClockwise = TestHelpers.placePiece(board, "RED", "2", 20, "COUNTERCLOCKWISE");
        counterClockwise.setHasPassedApproachOnce(false);

        Block block = blockHandler.createBlock(counterClockwise, clockwise, cell);

        // Act + Assert
        assertEquals("COUNTERCLOCKWISE", blockHandler.resolveBlockDirection(block));
    }

    @Test
    void counterClockwisePieceStopsAfterOpponentBlockWhenBlockedAcrossZero() {
        // Arrange
        Cell blockCell = board.getCellAt(50);

        Piece green1 = TestHelpers.placePiece(board, "GREEN", "1", 50);
        Piece green2 = TestHelpers.placePiece(board, "GREEN", "2", 50);
        blockHandler.createBlock(green2, green1, blockCell);

        Piece red = TestHelpers.placePiece(board, "RED", "1", 1, "COUNTERCLOCKWISE");

        // Act
        int firstBlockPosition = blockHandler.getFirstOpponentBlockPosition(red, 4);
        int maxMoveBeforeBlock = blockHandler.getMaxMoveBeforeBlock(red, 4);

        // Assert
        assertEquals(50, firstBlockPosition);
        assertEquals(51, maxMoveBeforeBlock);
    }

    @Test
    void differentSizedBlocksCannotCaptureEachOther() {
        // Arrange
        Cell attackCell = board.getCellAt(10);

        Piece a1 = TestHelpers.placePiece(board, "RED", "1", 10);
        Piece a2 = TestHelpers.placePiece(board, "RED", "2", 10);
        Block attackers = blockHandler.createBlock(a2, a1, attackCell);

        Cell defendCell = board.getCellAt(15);

        Piece d1 = TestHelpers.placePiece(board, "BLUE", "1", 15);
        Piece d2 = TestHelpers.placePiece(board, "BLUE", "2", 15);
        Piece d3 = TestHelpers.placePiece(board, "BLUE", "3", 15);
        Block defenders = blockHandler.createBlock(d2, d1, defendCell);
        blockHandler.addToBlock(d3, defenders, defendCell);

        // Act + Assert
        assertFalse(blockHandler.canBlockCaptureBlock(attackers, defenders));
    }

    @Test
    void breakingTwoPieceBlockRestoresRemainingPieceOriginalDirection() {
        // Arrange
        Cell cell = board.getCellAt(10);

        Piece p1 = TestHelpers.placePiece(board, "GREEN", "1", 10, "CLOCKWISE");
        Piece p2 = TestHelpers.placePiece(board, "GREEN", "2", 10, "COUNTERCLOCKWISE");

        p1.setOriginalDirection("CLOCKWISE");
        p2.setOriginalDirection("COUNTERCLOCKWISE");

        Block block = blockHandler.createBlock(p2, p1, cell);

        // Simulate block movement changing both directions.
        p1.setDirection("COUNTERCLOCKWISE");
        p2.setDirection("COUNTERCLOCKWISE");

        // Act
        blockHandler.breakBlock(p2, block);

        // Assert
        assertFalse(p1.isInBlock());
        assertFalse(p2.isInBlock());

        assertEquals("CLOCKWISE", p1.getDirection());
        assertEquals("COUNTERCLOCKWISE", p2.getDirection());

        assertNull(blockHandler.findBlockAt(cell));
    }

    private static class DummyPlayer extends Player {
        DummyPlayer(String color, List<Piece> pieces) {
            super(color, color, pieces, new NoMoveStrategy());
        }

        @Override
        protected Piece choosePieceToMove(List<Piece> pieces, int diceValue,
                                          Board board, RuleEngine ruleEngine) {
            return pieces.isEmpty() ? null : pieces.getFirst();
        }
    }

    private static class NoMoveStrategy implements IPlayerStrategy {
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
