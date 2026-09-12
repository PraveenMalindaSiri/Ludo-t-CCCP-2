package mystery;

import board.Board;
import config.GameConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import piece.Piece;
import testutil.TestHelpers;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class MysteryManagerTest {
    private Board board;

    @BeforeEach
    void setUp() {
        board = TestHelpers.board();
    }

    @Test
    void mysteryCellSpawnsOnlyAfterTwoRoundsWithPiecesOnStandardPath() {
        MysteryManager manager = new MysteryManager(board, new Random(1));

        manager.updateRound(true);
        assertFalse(manager.isActive());

        manager.updateRound(true);
        assertTrue(manager.isActive());
        assertEquals(GameConfig.getInstance().getMysteryCellDuration(), manager.getRoundsRemaining());
        assertTrue(manager.getPosition() >= 0 && manager.getPosition() < 52);
    }

    @Test
    void mysteryCellDoesNotSpawnOnOccupiedCell() {
        int onlyEmptyPosition = 7;
        for (int i = 0; i < 52; i++) {
            if (i == onlyEmptyPosition) continue;
            TestHelpers.placePiece(board, "RED", "P" + i, i);
        }
        MysteryManager manager = new MysteryManager(board, new FixedIntRandom(0));

        manager.spawnMysteryCell();

        assertTrue(manager.isActive());
        assertEquals(onlyEmptyPosition, manager.getPosition());
    }

    @Test
    void activeMysteryCellCountsDownAndRespawnsAfterDuration() {
        MysteryManager manager = new MysteryManager(board, new Random(2));
        manager.spawnMysteryCell();
        int firstPosition = manager.getPosition();

        for (int i = 0; i < GameConfig.getInstance().getMysteryCellDuration(); i++) {
            manager.updateRound(true);
        }

        assertTrue(manager.isActive());
        assertNotEquals(firstPosition, manager.getPosition(),
                "Mystery cell should not respawn in the same location consecutively.");
    }

    @Test
    void handleLandingRemovesPieceFromMysteryCellAndAppliesSelectedEffect() {
        int mysteryPosition = 10;
        for (int i = 0; i < 52; i++) {
            if (i == mysteryPosition) continue;
            TestHelpers.placePiece(board, "GREEN", "P" + i, i);
        }

        // First nextInt is used by spawnMysteryCell(candidates.size() == 1), second by handleLanding(effects.size()).
        // Effect index 3 = BaseEffect.
        MysteryManager manager = new MysteryManager(board, new SequenceRandom(0, 3));
        manager.spawnMysteryCell();
        Piece piece = TestHelpers.placePiece(board, "BLUE", "1", mysteryPosition);

        manager.handleLanding(piece);

        assertFalse(board.getCellAt(mysteryPosition).getPieces().contains(piece));
        assertTrue(piece.isInBase());
        assertTrue(board.getBaseCell("BLUE").getPieces().contains(piece));
        assertEquals(3, manager.getLastEffectIndex());
    }

    @Test
    void mysteryCellDoesNotSpawnWhenNoPiecesAreOnStandardPath() {
        // Arrange
        MysteryManager manager = new MysteryManager(board, new Random(1));

        // Act
        manager.updateRound(false);
        manager.updateRound(false);
        manager.updateRound(false);

        // Assert
        assertFalse(manager.isActive());
    }

    @Test
    void spawnMysteryCellDoesNothingWhenNoEmptyCandidateExists() {
        // Arrange
        for (int i = 0; i < 52; i++) {
            TestHelpers.placePiece(board, "RED", "P" + i, i);
        }

        MysteryManager manager = new MysteryManager(board, new FixedIntRandom(0));

        // Act
        manager.spawnMysteryCell();

        // Assert
        assertFalse(manager.isActive());
    }

    @Test
    void alphaTeleportSetsLastAlphaFlagAndTemporaryState() {
        // Arrange
        int mysteryPosition = 10;

        for (int i = 0; i < 52; i++) {
            if (i == mysteryPosition) continue;
            TestHelpers.placePiece(board, "GREEN", "P" + i, i);
        }

        // first nextInt = spawn candidate, second nextInt = Alpha effect, nextBoolean = Energized
        MysteryManager manager = new MysteryManager(board, new EffectRandom(true, 0, 0));
        manager.spawnMysteryCell();

        Piece piece = TestHelpers.placePiece(board, "BLUE", "1", mysteryPosition);

        // Act
        manager.handleLanding(piece);

        // Assert
        assertEquals(0, manager.getLastEffectIndex());
        assertEquals(GameConfig.getInstance().getAlphaCell(), piece.getPosition());
        assertTrue(piece.isEnergized());
        assertTrue(manager.isLastAlphaEnergized());
    }

    @Test
    void gammaCounterClockwisePieceTeleportsToBetaAndSetsFlag() {
        // Arrange
        int mysteryPosition = 10;

        for (int i = 0; i < 52; i++) {
            if (i == mysteryPosition) continue;
            TestHelpers.placePiece(board, "GREEN", "P" + i, i);
        }

        // first nextInt = spawn candidate, second nextInt = Gamma effect
        MysteryManager manager = new MysteryManager(board, new EffectRandom(true, 0, 2));
        manager.spawnMysteryCell();

        Piece piece = TestHelpers.placePiece(board, "BLUE", "1", mysteryPosition, "COUNTERCLOCKWISE");

        // Act
        manager.handleLanding(piece);

        // Assert
        assertEquals(2, manager.getLastEffectIndex());
        assertEquals(GameConfig.getInstance().getBetaCell(), piece.getPosition());
        assertTrue(piece.isFrozen());
        assertTrue(manager.isLastGammaCCWToBeta());
    }

    @Test
    void startEffectMovesPieceToOwnStartingCellAndResetsApproachPassFlag() {
        // Arrange
        int mysteryPosition = 10;

        for (int i = 0; i < 52; i++) {
            if (i == mysteryPosition) continue;
            TestHelpers.placePiece(board, "GREEN", "P" + i, i);
        }

        // first nextInt = spawn candidate, second nextInt = StartEffect
        MysteryManager manager = new MysteryManager(board, new EffectRandom(true, 0, 4));
        manager.spawnMysteryCell();

        Piece piece = TestHelpers.placePiece(board, "BLUE", "1", mysteryPosition);
        piece.setHasPassedApproachOnce(true);

        // Act
        manager.handleLanding(piece);

        // Assert
        assertEquals(4, manager.getLastEffectIndex());
        assertEquals(board.getStartingPosition("BLUE"), piece.getPosition());
        assertFalse(piece.getHasPassedApproachOnce());
        assertTrue(board.getStartingCell("BLUE").getPieces().contains(piece));
    }

    @Test
    void approachEffectMovesPieceToOwnApproachCellAndResetsApproachPassFlag() {
        // Arrange
        int mysteryPosition = 10;

        for (int i = 0; i < 52; i++) {
            if (i == mysteryPosition) continue;
            TestHelpers.placePiece(board, "GREEN", "P" + i, i);
        }

        // first nextInt = spawn candidate, second nextInt = ApproachEffect
        MysteryManager manager = new MysteryManager(board, new EffectRandom(true, 0, 5));
        manager.spawnMysteryCell();

        Piece piece = TestHelpers.placePiece(board, "RED", "1", mysteryPosition);
        piece.setHasPassedApproachOnce(true);

        // Act
        manager.handleLanding(piece);

        // Assert
        assertEquals(5, manager.getLastEffectIndex());
        assertEquals(board.getApproachPosition("RED"), piece.getPosition());
        assertFalse(piece.getHasPassedApproachOnce());
        assertTrue(board.getApproachCell("RED").getPieces().contains(piece));
    }

    private static class FixedIntRandom extends Random {
        private final int value;

        FixedIntRandom(int value) {
            this.value = value;
        }

        @Override
        public int nextInt(int bound) {
            return value;
        }
    }

    private static class SequenceRandom extends Random {
        private final int[] values;
        private int index;

        SequenceRandom(int... values) {
            this.values = values;
        }

        @Override
        public int nextInt(int bound) {
            int value = values[index++];
            return Math.floorMod(value, bound);
        }
    }

    private static class EffectRandom extends Random {
        private final boolean booleanValue;
        private final int[] values;
        private int index;

        EffectRandom(boolean booleanValue, int... values) {
            this.booleanValue = booleanValue;
            this.values = values;
        }

        @Override
        public int nextInt(int bound) {
            int value = values[index++];
            return Math.floorMod(value, bound);
        }

        @Override
        public boolean nextBoolean() {
            return booleanValue;
        }
    }
}
