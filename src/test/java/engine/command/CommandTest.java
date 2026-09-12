package engine.command;

import block.Block;
import board.Board;
import mystery.MysteryManager;
import mystery.MysteryOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import piece.Piece;
import rules.BlockHandler;
import rules.CaptureHandler;
import rules.LandingResolver;
import support.TestSupport;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CommandTest {
    private Board board;
    private CaptureHandler captureHandler;
    private BlockHandler blockHandler;
    private LandingResolver resolver;

    @BeforeEach
    void setUp() {
        board = TestSupport.newBoard();
        captureHandler = new CaptureHandler(board);
        blockHandler = new BlockHandler(board);
        MysteryManager mystery = new MysteryManager(
                board, new TestSupport.SequenceRandom(0),
                List.of((piece, gameBoard) ->
                        new MysteryOutcome(MysteryOutcome.Type.START, "unused")));
        resolver = new LandingResolver(
                board, captureHandler, blockHandler, mystery);
    }

    @Test
    void enterBoardCommandSetsClockwiseDirectionForHeads() {
        Piece piece = new Piece("1", "YELLOW");
        board.initializeInBase(piece);

        CommandResult result = new EnterBoardCommand(
                piece, board, new TestSupport.FixedCoinToss("HEADS"), resolver)
                .execute();

        assertEquals(CommandResult.Type.ENTER_BOARD, result.getType());
        assertTrue(result.wasExecuted());
        assertEquals(Piece.BASE_POSITION, result.getFromPosition());
        assertEquals(board.getStartingPosition("YELLOW"), result.getToPosition());
        assertEquals("CLOCKWISE", piece.getDirection());
        assertEquals(List.of(piece), result.getMovedPieces());
    }

    @Test
    void enterBoardCommandSetsCounterclockwiseDirectionForTails() {
        Piece piece = new Piece("1", "BLUE");
        board.initializeInBase(piece);

        EnterBoardCommand command = new EnterBoardCommand(
                piece, board, new TestSupport.FixedCoinToss("TAILS"), resolver);
        command.execute();

        assertEquals("COUNTERCLOCKWISE", piece.getDirection());
        assertEquals("COUNTERCLOCKWISE", piece.getOriginalDirection());
        assertEquals("COUNTERCLOCKWISE", command.getResultDirection());
    }

    @Test
    void moveCommandProducesStructuredMovementResult() {
        Piece piece = TestSupport.place(
                board, "1", "YELLOW", 10, "CLOCKWISE");

        CommandResult result = new MoveCommand(
                piece, board, 14, 4, resolver).execute();

        assertEquals(CommandResult.Type.MOVE, result.getType());
        assertTrue(result.wasExecuted());
        assertEquals(10, result.getFromPosition());
        assertEquals(14, result.getToPosition());
        assertEquals(4, result.getMovement());
        assertEquals("CLOCKWISE", result.getDirection());
        assertEquals(14, piece.getPosition());
    }

    @Test
    void captureCommandReportsOriginalCapturePosition() {
        Piece capturer = TestSupport.place(
                board, "1", "YELLOW", 8, "CLOCKWISE");
        Piece captured = TestSupport.place(
                board, "1", "RED", 8, "CLOCKWISE");

        CommandResult result = new CaptureCommand(
                capturer, captured, captureHandler).execute();

        assertEquals(CommandResult.Type.CAPTURE, result.getType());
        assertEquals(8, result.getCapturePosition(captured));
        assertTrue(captured.isInBase());
        assertTrue(result.hasCaptured());
    }

    @Test
    void homeMoveCommandMovesWithinStraightThenIntoHome() {
        Piece piece = new Piece("1", "YELLOW");
        board.moveToHomeStraight(piece, 1);

        CommandResult within = new HomeMoveCommand(piece, board, 3, 2).execute();
        assertEquals(CommandResult.Type.HOME_MOVE, within.getType());
        assertEquals(3, piece.getHomeStraightIndex());

        CommandResult home = new HomeMoveCommand(piece, board, 5, 2).execute();
        assertTrue(home.wasExecuted());
        assertTrue(piece.isAtHome());
        assertTrue(board.getHomeCell("YELLOW").getPieces().contains(piece));
    }

    @Test
    void equalSizedBlockCapturesDefendingBlock() {
        Block attacker = createBlock("YELLOW", 0, 2);
        Block defender = createBlock("RED", 3, 2);

        CommandResult result = new BlockMoveCommand(
                attacker, 6, board, blockHandler,
                captureHandler, resolver).execute();

        assertTrue(result.wasExecuted());
        assertFalse(result.wasBlocked());
        assertEquals(3, attacker.getPosition());
        assertEquals(2, result.getCapturedPieces().size());
        assertTrue(defender.getPieces().isEmpty());
        assertTrue(result.getCapturedPieces().stream().allMatch(Piece::isInBase));
    }

    @Test
    void differentlySizedBlockCannotCaptureDefendingBlock() {
        Block attacker = createBlock("YELLOW", 0, 2);
        Block defender = createBlock("RED", 3, 3);

        CommandResult result = new BlockMoveCommand(
                attacker, 6, board, blockHandler,
                captureHandler, resolver).execute();

        assertFalse(result.wasExecuted());
        assertTrue(result.wasBlocked());
        assertEquals(3, result.getBlockedAt());
        assertEquals(0, attacker.getPosition());
        assertEquals(3, defender.getSize());
    }

    @Test
    void commandResultCollectionsCannotMutateRecordedResult() {
        Piece piece = new Piece("1", "YELLOW");
        CommandResult result = new CommandResult(CommandResult.Type.MOVE);
        result.addMovedPiece(piece);
        result.addCapturedPiece(piece, 9);

        result.getMovedPieces().clear();
        result.getCapturedPieces().clear();

        assertEquals(List.of(piece), result.getMovedPieces());
        assertEquals(List.of(piece), result.getCapturedPieces());
        assertEquals(9, result.getCapturePosition(piece));
    }

    private Block createBlock(String color, int position, int size) {
        Piece first = TestSupport.place(
                board, "1", color, position, "CLOCKWISE");
        Piece second = TestSupport.place(
                board, "2", color, position, "CLOCKWISE");
        Block block = blockHandler.createBlock(
                first, second, board.getCellAt(position));
        for (int index = 3; index <= size; index++) {
            Piece extra = TestSupport.place(
                    board, String.valueOf(index), color, position, "CLOCKWISE");
            blockHandler.addToBlock(extra, block, board.getCellAt(position));
        }
        return block;
    }
}
