package rules;

import board.Board;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import piece.Piece;
import piece.state.FrozenState;
import piece.state.SickState;
import testutil.TestHelpers;
import piece.state.EnergizedState;
import player.Player;
import player.strategy.IPlayerStrategy;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RuleEngineTest {
    private Board board;
    private RuleEngine ruleEngine;

    @BeforeEach
    void setUp() {
        board = TestHelpers.board();
        BlockHandler blockHandler = new BlockHandler(board);
        CaptureHandler captureHandler = new CaptureHandler(board);
        ruleEngine = new RuleEngine(board, blockHandler, captureHandler);
    }

    @Test
    void baseEntryRequiresSix() {
        assertTrue(ruleEngine.canMoveFromBase(6));
        assertFalse(ruleEngine.canMoveFromBase(5));
        assertFalse(ruleEngine.canMoveFromBase(1));
    }

    @Test
    void thirdConsecutiveSixIsDetected() {
        assertFalse(ruleEngine.isThirdConsecutiveSix(1));
        assertFalse(ruleEngine.isThirdConsecutiveSix(2));
        assertTrue(ruleEngine.isThirdConsecutiveSix(3));
    }

    @Test
    void calculatesClockwiseAndCounterClockwiseDestinationWithWrapping() {
        Piece clockwise = TestHelpers.placePiece(board, "RED", "1", 50, "CLOCKWISE");
        Piece counter = TestHelpers.placePiece(board, "BLUE", "1", 1, "COUNTERCLOCKWISE");

        assertEquals(2, ruleEngine.calculateDestination(clockwise, 4));
        assertEquals(49, ruleEngine.calculateDestination(counter, 4));
    }

    @Test
    void pieceCanEnterHomeStraightOnlyAfterCapture() {
        Piece piece = new Piece("1", "RED");

        assertFalse(ruleEngine.canEnterHomeStraight(piece));
        piece.incrementCaptureCount();
        assertTrue(ruleEngine.canEnterHomeStraight(piece));
    }

    @Test
    void counterClockwiseHomeStraightEntryNeedsSecondApproachPass() {
        Piece piece = new Piece("1", "RED");
        piece.setDirection("COUNTERCLOCKWISE");

        assertFalse(ruleEngine.canEnterHomeStraightCCW(piece));
        piece.setHasPassedApproachOnce(true);
        assertTrue(ruleEngine.canEnterHomeStraightCCW(piece));
    }

    @Test
    void homeStraightRequiresExactRollAndRejectsOvershoot() {
        Piece piece = new Piece("1", "RED");
        piece.moveToHomeStraight(3);

        assertTrue(ruleEngine.needsExactRoll(piece, 2));
        assertFalse(ruleEngine.needsExactRoll(piece, 1));
        assertFalse(ruleEngine.overshotsHome(piece, 2));
        assertTrue(ruleEngine.overshotsHome(piece, 3));
    }

    @Test
    void validMoveRejectsFrozenPiecesAndSickZeroMovement() {
        Piece frozen = TestHelpers.placePiece(board, "RED", "1", 10);
        frozen.setState(new FrozenState(4));

        Piece sick = TestHelpers.placePiece(board, "BLUE", "1", 20);
        sick.setState(new SickState(4));

        assertFalse(ruleEngine.isValidMove(frozen, 6));
        assertFalse(ruleEngine.isValidMove(sick, 1));
        assertTrue(ruleEngine.isValidMove(sick, 2));
    }

    @Test
    void canPassApproachDetectsRollThatCrossesApproachCell() {
        Piece red = TestHelpers.placePiece(board, "RED", "1", 23, "CLOCKWISE");

        assertFalse(ruleEngine.canPassApproach(red, 1));
        assertTrue(ruleEngine.canPassApproach(red, 2));
    }

    @Test
    void getValidMovesRejectsStandardPathMoveThatOvershootsHome() {
        // Arrange
        Piece piece = TestHelpers.placePiece(board, "RED", "1", board.getApproachPosition("RED"));
        piece.incrementCaptureCount();
        piece.setState(new EnergizedState(4));

        Player player = new DummyPlayer("RED", List.of(piece));

        // Act
        var validMoves = ruleEngine.getValidMoves(player, 4);

        // Assert
        assertTrue(ruleEngine.overshootsHomeFromStandardPath(piece, 4));
        assertFalse(validMoves.contains(piece));
    }

    @Test
    void getValidMovesRejectsSameColourStackWhenDestinationPieceCannotBeInBlock() {
        // Arrange
        Piece moving = TestHelpers.placePiece(board, "RED", "1", 10);

        Piece invalidBlockMember = TestHelpers.placePiece(board, "RED", "2", 12);
        invalidBlockMember.setState(new EnergizedState(4));

        Player player = new DummyPlayer("RED", List.of(moving, invalidBlockMember));

        // Act
        var validMoves = ruleEngine.getValidMoves(player, 2);

        // Assert
        assertFalse(ruleEngine.canFormBlock(moving, 12));
        assertFalse(validMoves.contains(moving));
    }

    @Test
    void canFormBlockReturnsTrueOnlyForOneValidSameColourPieceAtDestination() {
        // Arrange
        Piece moving = TestHelpers.placePiece(board, "GREEN", "1", 10);
        TestHelpers.placePiece(board, "GREEN", "2", 13);

        // Act + Assert
        assertTrue(ruleEngine.canFormBlock(moving, 13));
    }

    @Test
    void energizedHomeStraightExactRollIsNotDoubleAppliedAgain() {
        // Arrange
        Piece piece = new Piece("1", "RED");
        piece.moveToHomeStraight(1); // needs 4 steps to reach home
        piece.setState(new EnergizedState(4)); // dice 2 becomes 4

        Player player = new DummyPlayer("RED", List.of(piece));

        // Act
        var validMoves = ruleEngine.getValidMoves(player, 2);

        // Assert
        assertTrue(ruleEngine.needsExactRoll(piece, 2));
        assertFalse(ruleEngine.overshotsHome(piece, 2));
        assertTrue(validMoves.contains(piece));
    }

    @Test
    void atHomePieceIsRejectedFromValidMoves() {
        // Arrange
        Piece homePiece = new Piece("1", "RED");
        homePiece.moveToHome();

        Player player = new DummyPlayer("RED", List.of(homePiece));

        // Act
        var validMoves = ruleEngine.getValidMoves(player, 6);

        // Assert
        assertFalse(ruleEngine.isValidMove(homePiece, 6));
        assertFalse(validMoves.contains(homePiece));
    }

    @Test
    void basePieceIsValidOnlyWhenDiceIsSix() {
        // Arrange
        Piece basePiece = new Piece("1", "RED");
        Player player = new DummyPlayer("RED", List.of(basePiece));

        // Act
        var validMovesForFive = ruleEngine.getValidMoves(player, 5);
        var validMovesForSix = ruleEngine.getValidMoves(player, 6);

        // Assert
        assertFalse(validMovesForFive.contains(basePiece));
        assertTrue(validMovesForSix.contains(basePiece));
    }

    @Test
    void counterClockwiseFirstApproachPassDoesNotTriggerStandardPathHomeOvershoot() {
        // Arrange
        Piece piece = TestHelpers.placePiece(board, "RED", "1", 26, "COUNTERCLOCKWISE");
        piece.incrementCaptureCount();
        piece.setHasPassedApproachOnce(false);

        // Act + Assert
        assertTrue(ruleEngine.canPassApproach(piece, 1));
        assertFalse(ruleEngine.overshootsHomeFromStandardPath(piece, 6));
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
