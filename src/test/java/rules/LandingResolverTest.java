package rules;

import static org.junit.jupiter.api.Assertions.*;

import block.Block;
import board.Board;
import engine.command.CommandResult;
import engine.command.MoveCommand;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import mystery.MysteryManager;
import mystery.MysteryOutcome;
import mystery.effect.IMysteryEffect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import piece.Piece;
import piece.state.SickState;
import support.TestSupport;

class LandingResolverTest {
    private Board board;
    private CaptureHandler captureHandler;
    private BlockHandler blockHandler;

    @BeforeEach
    void setUp() {
        board = TestSupport.newBoard();
        captureHandler = new CaptureHandler(board);
        blockHandler = new BlockHandler(board);
    }

    @Test
    void landingOnSingleOpponentCapturesItAndRecordsResult() {
        Piece mover = TestSupport.place(board, "1", "YELLOW", 4, "CLOCKWISE");
        Piece opponent = TestSupport.place(board, "1", "RED", 8, "CLOCKWISE");
        LandingResolver resolver = resolverWithInactiveMystery();

        CommandResult result = new MoveCommand(mover, board, 8, 4, resolver).execute();

        assertEquals(8, mover.getPosition());
        assertTrue(opponent.isInBase());
        assertEquals(List.of(opponent), result.getCapturedPieces());
        assertEquals(8, result.getCapturePosition(opponent));
        assertEquals(1, mover.getCaptureCount());
    }

    @Test
    void landingWithTeammateFormsOneRegisteredBlock() {
        Piece mover = TestSupport.place(board, "1", "GREEN", 4, "CLOCKWISE");
        Piece teammate = TestSupport.place(board, "2", "GREEN", 8, "CLOCKWISE");

        new MoveCommand(mover, board, 8, 4, resolverWithInactiveMystery()).execute();

        Block block = blockHandler.findBlockAt(board.getCellAt(8));
        assertNotNull(block);
        assertEquals(2, block.getSize());
        assertTrue(block.getPieces().containsAll(List.of(mover, teammate)));
        assertTrue(mover.isInBlock());
        assertTrue(teammate.isInBlock());
    }

    @Test
    void landingOnEnemyBlockReturnsMoverToBase() {
        Piece defenderOne = TestSupport.place(board, "1", "RED", 8, "CLOCKWISE");
        Piece defenderTwo = TestSupport.place(board, "2", "RED", 8, "CLOCKWISE");
        Block enemyBlock = blockHandler.createBlock(defenderOne, defenderTwo, board.getCellAt(8));
        Piece mover = TestSupport.place(board, "1", "YELLOW", 4, "CLOCKWISE");

        CommandResult result =
                new MoveCommand(mover, board, 8, 4, resolverWithInactiveMystery()).execute();

        assertTrue(mover.isInBase());
        assertTrue(result.wasMoverReturnedToBase());
        assertSame(enemyBlock, blockHandler.findBlockAt(board.getCellAt(8)));
        assertEquals(2, enemyBlock.getSize());
    }

    @Test
    void nonNormalPieceCannotJoinOwnBlockAndReturnsToBase() {
        Piece first = TestSupport.place(board, "1", "YELLOW", 8, "CLOCKWISE");
        Piece second = TestSupport.place(board, "2", "YELLOW", 8, "CLOCKWISE");
        blockHandler.createBlock(first, second, board.getCellAt(8));
        Piece mover = TestSupport.place(board, "3", "YELLOW", 4, "CLOCKWISE");
        mover.setState(new SickState(4));

        CommandResult result =
                new MoveCommand(mover, board, 8, 4, resolverWithInactiveMystery()).execute();

        assertTrue(mover.isInBase());
        assertTrue(result.wasMoverReturnedToBase());
    }

    @Test
    void mysteryEffectRunsOnceForOneLandingEvenWhenDestinationIsSameCell() {
        AtomicInteger applications = new AtomicInteger();
        IMysteryEffect effect =
                (piece, gameBoard) -> {
                    applications.incrementAndGet();
                    gameBoard.teleportToStandardPath(piece, 5);
                    return new MysteryOutcome(MysteryOutcome.Type.START, "same cell");
                };
        MysteryManager mystery =
                new MysteryManager(board, new TestSupport.SequenceRandom(5, 0), List.of(effect));
        mystery.spawnMysteryCell();
        LandingResolver resolver =
                new LandingResolver(board, captureHandler, blockHandler, mystery);
        Piece mover = TestSupport.place(board, "1", "BLUE", 1, "CLOCKWISE");

        CommandResult result = new MoveCommand(mover, board, 5, 4, resolver).execute();

        assertEquals(1, applications.get());
        assertNotNull(result.getMysteryOutcome());
        assertEquals(5, mover.getPosition());
    }

    @Test
    void captureAndMysteryAreBothResolvedBySameLandingSequence() {
        IMysteryEffect effect =
                (piece, gameBoard) -> {
                    gameBoard.teleportToStandardPath(piece, 10);
                    return new MysteryOutcome(MysteryOutcome.Type.START, "10");
                };
        MysteryManager mystery =
                new MysteryManager(board, new TestSupport.SequenceRandom(5, 0), List.of(effect));
        mystery.spawnMysteryCell();
        Piece opponent = TestSupport.place(board, "1", "RED", 5, "CLOCKWISE");
        LandingResolver resolver =
                new LandingResolver(board, captureHandler, blockHandler, mystery);
        Piece mover = TestSupport.place(board, "1", "YELLOW", 1, "CLOCKWISE");

        CommandResult result = new MoveCommand(mover, board, 5, 4, resolver).execute();

        assertTrue(opponent.isInBase());
        assertTrue(result.hasCaptured());
        assertNotNull(result.getMysteryOutcome());
        assertEquals(10, mover.getPosition());
    }

    private LandingResolver resolverWithInactiveMystery() {
        MysteryManager mystery =
                new MysteryManager(
                        board,
                        new TestSupport.SequenceRandom(0),
                        List.of(
                                (piece, gameBoard) ->
                                        new MysteryOutcome(MysteryOutcome.Type.START, "unused")));
        return new LandingResolver(board, captureHandler, blockHandler, mystery);
    }
}
