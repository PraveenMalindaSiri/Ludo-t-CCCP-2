package event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import engine.GameEngine;
import factory.GameFactory;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameSnapshotCompletenessTest {

    @Test
    void completeSnapshotContainsAllPlayersAndAllSixteenStructuredPieces() {
        GameEngine engine = GameFactory.createGame(42L);
        engine.initializeGame();

        GameSnapshot snapshot = engine.getSnapshot();
        List<GameSnapshot.PieceView> pieces =
                snapshot.getPlayers().stream()
                        .flatMap(player -> player.getPieces().stream())
                        .toList();

        assertEquals(4, snapshot.getPlayers().size());
        assertEquals(16, pieces.size());
        assertEquals(
                16, pieces.stream().map(GameSnapshot.PieceView::getPieceId).distinct().count());
        assertFalse(snapshot.getCurrentPlayer().isBlank());
        pieces.forEach(
                piece -> {
                    assertEquals(GameSnapshot.PieceArea.BASE, piece.getArea());
                    assertEquals(-1, piece.getStandardPosition());
                    assertEquals(-1, piece.getHomeStraightIndex());
                    assertFalse(piece.getColor().isBlank());
                    assertFalse(piece.getDirection().isBlank());
                    assertFalse(piece.getStateLabel().isBlank());
                });
        snapshot.getPlayers()
                .forEach(
                        player -> {
                            assertEquals(4, player.getBaseCount());
                            assertEquals(0, player.getBoardCount());
                            assertEquals(0, player.getHomeCount());
                        });
    }

    @Test
    void snapshotCollectionsAndValuesRemainStableAfterLaterEngineChanges() {
        GameEngine engine = GameFactory.createGame(77L);
        engine.initializeGame();
        GameSnapshot before = engine.getSnapshot();
        String currentBefore = before.getCurrentPlayer();
        List<String> positionsBefore =
                before.getPlayers().stream()
                        .flatMap(player -> player.getPieces().stream())
                        .map(GameSnapshot.PieceView::getPosition)
                        .toList();

        engine.advanceOneTurn();

        assertEquals(currentBefore, before.getCurrentPlayer());
        assertEquals(
                positionsBefore,
                before.getPlayers().stream()
                        .flatMap(player -> player.getPieces().stream())
                        .map(GameSnapshot.PieceView::getPosition)
                        .toList());
        assertThrows(UnsupportedOperationException.class, () -> before.getPlayers().clear());
        assertThrows(
                UnsupportedOperationException.class,
                () -> before.getPlayers().getFirst().getPieces().clear());
        assertNotEquals(before, engine.getSnapshot());
    }
}
