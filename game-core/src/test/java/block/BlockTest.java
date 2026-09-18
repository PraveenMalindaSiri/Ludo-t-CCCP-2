package block;

import static org.junit.jupiter.api.Assertions.*;

import board.Board;
import board.Cell;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import piece.Piece;
import rules.BlockHandler;

class BlockTest {
    private Board board;
    private BlockHandler handler;

    @BeforeEach
    void setUp() {
        board = support.TestSupport.newBoard();
        handler = new BlockHandler(board);
    }

    @Test
    void createsCompositeFromSameColorPiecesOnSameCell() {
        Piece first = support.TestSupport.place(board, "1", "YELLOW", 10, "CLOCKWISE");
        Piece second = support.TestSupport.place(board, "2", "YELLOW", 10, "CLOCKWISE");

        Block block = handler.createBlock(first, second, board.getCellAt(10));

        assertNotNull(block);
        assertEquals(2, block.getSize());
        assertTrue(first.isInBlock());
        assertTrue(second.isInBlock());
        assertSame(block, handler.findBlockAt(board.getCellAt(10)));
    }

    @Test
    void blockRejectsDifferentColorOrPosition() {
        Cell cell = board.getCellAt(10);
        Piece yellow = support.TestSupport.place(board, "1", "YELLOW", 10, "CLOCKWISE");
        Piece red = support.TestSupport.place(board, "1", "RED", 10, "CLOCKWISE");
        Piece elsewhere = support.TestSupport.place(board, "2", "YELLOW", 11, "CLOCKWISE");
        Block block = new Block(cell);
        block.addPiece(yellow);

        assertThrows(IllegalArgumentException.class, () -> block.addPiece(red));
        assertThrows(IllegalArgumentException.class, () -> block.addPiece(elsewhere));
    }

    @Test
    void compositeMovementMovesEveryMemberAndUpdatesRegistry() {
        Piece first = support.TestSupport.place(board, "1", "YELLOW", 10, "CLOCKWISE");
        Piece second = support.TestSupport.place(board, "2", "YELLOW", 10, "CLOCKWISE");
        Block block = handler.createBlock(first, second, board.getCellAt(10));

        handler.moveBlock(block, 6);

        assertEquals(13, first.getPosition());
        assertEquals(13, second.getPosition());
        assertTrue(board.getCellAt(10).getPieces().isEmpty());
        assertTrue(board.getCellAt(13).getPieces().containsAll(block.getPieces()));
        assertNull(handler.findBlockAt(board.getCellAt(10)));
        assertSame(block, handler.findBlockAt(board.getCellAt(13)));
    }

    @Test
    void breakingCompositeRestoresIndependentPieces() {
        Piece first = support.TestSupport.place(board, "1", "YELLOW", 10, "CLOCKWISE");
        Piece second = support.TestSupport.place(board, "2", "YELLOW", 10, "COUNTERCLOCKWISE");
        Block block = handler.createBlock(first, second, board.getCellAt(10));

        handler.breakBlock(second, block);

        assertFalse(first.isInBlock());
        assertFalse(second.isInBlock());
        assertEquals("CLOCKWISE", first.getDirection());
        assertEquals("COUNTERCLOCKWISE", second.getDirection());
        assertNull(handler.findBlockAt(board.getCellAt(10)));
    }

    @Test
    void capturingCompositeSendsAllLeafPiecesToBase() {
        Piece first = support.TestSupport.place(board, "1", "RED", 20, "CLOCKWISE");
        Piece second = support.TestSupport.place(board, "2", "RED", 20, "CLOCKWISE");
        Block block = handler.createBlock(first, second, board.getCellAt(20));

        board.sendToBase(block);

        assertTrue(first.isInBase());
        assertTrue(second.isInBase());
        assertTrue(
                board.getBaseCell("RED").getPieces().containsAll(java.util.List.of(first, second)));
        assertEquals(0, block.getSize());
    }

    @Test
    void blockQueriesDoNotExposeMutableRegistryOrMemberList() {
        Piece first = support.TestSupport.place(board, "1", "GREEN", 15, "CLOCKWISE");
        Piece second = support.TestSupport.place(board, "2", "GREEN", 15, "CLOCKWISE");
        Block block = handler.createBlock(first, second, board.getCellAt(15));

        handler.getActiveBlocks().clear();
        block.getPieces().clear();

        assertSame(block, handler.findBlockAt(board.getCellAt(15)));
        assertEquals(2, block.getSize());
    }
}
