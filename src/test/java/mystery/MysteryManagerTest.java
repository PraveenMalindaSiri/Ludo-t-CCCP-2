package mystery;

import board.Board;
import config.GameConfig;
import mystery.effect.IMysteryEffect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import piece.Piece;
import support.TestSupport;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MysteryManagerTest {
    private Board board;
    private IMysteryEffect effect;

    @BeforeEach
    void setUp() {
        board = TestSupport.newBoard();
        effect = (piece, gameBoard) ->
                new MysteryOutcome(MysteryOutcome.Type.START, "test");
    }

    @Test
    void managerRequiresAtLeastOneEffect() {
        assertThrows(IllegalArgumentException.class,
                () -> new MysteryManager(
                        board, new TestSupport.SequenceRandom(0), List.of()));
    }

    @Test
    void spawnAvoidsOccupiedCells() {
        TestSupport.place(board, "1", "YELLOW", 0, "CLOCKWISE");
        MysteryManager manager = new MysteryManager(
                board, new TestSupport.SequenceRandom(0), List.of(effect));

        manager.spawnMysteryCell();

        assertTrue(manager.isActive());
        assertEquals(1, manager.getPosition());
        assertEquals(GameConfig.getInstance().getMysteryCellDuration(),
                manager.getRoundsRemaining());
    }

    @Test
    void inactiveManagerSpawnsAfterConfiguredRoundsWithPiecesOnPath() {
        MysteryManager manager = new MysteryManager(
                board, new TestSupport.SequenceRandom(6), List.of(effect));

        manager.updateRound(true);
        assertFalse(manager.isActive());
        manager.updateRound(true);

        assertTrue(manager.isActive());
        assertEquals(6, manager.getPosition());
    }

    @Test
    void expiredMysteryRespawnsAtDifferentPosition() {
        MysteryManager manager = new MysteryManager(
                board, new TestSupport.SequenceRandom(5, 5), List.of(effect));
        manager.spawnMysteryCell();
        int first = manager.getPosition();

        for (int i = 0; i < GameConfig.getInstance().getMysteryCellDuration(); i++) {
            manager.updateRound(false);
        }

        assertTrue(manager.isActive());
        assertNotEquals(first, manager.getPosition());
    }

    @Test
    void handleLandingReturnsStructuredEffectOutcome() {
        Piece piece = TestSupport.place(
                board, "1", "YELLOW", 4, "CLOCKWISE");
        MysteryManager manager = new MysteryManager(
                board, new TestSupport.SequenceRandom(0), List.of(effect));

        MysteryOutcome outcome = manager.handleLanding(piece);

        assertEquals(MysteryOutcome.Type.START, outcome.getType());
        assertEquals("test", outcome.getDestination());
    }
}
