package event;

import board.Board;
import mystery.MysteryManager;
import mystery.MysteryOutcome;
import org.junit.jupiter.api.Test;
import piece.Piece;
import player.YellowPlayer;
import support.TestSupport;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameSnapshotTest {

    @Test
    void snapshotContainsPresentationSafePlayerAndPieceViews() {
        Board board = TestSupport.newBoard();
        Piece first = TestSupport.place(
                board, "1", "YELLOW", 7, "CLOCKWISE");
        Piece second = new Piece("2", "YELLOW");
        board.initializeInBase(second);
        YellowPlayer player = new YellowPlayer(
                List.of(first, second), new TestSupport.FirstPieceStrategy(true));
        MysteryManager mystery = new MysteryManager(
                board, new TestSupport.SequenceRandom(0),
                List.of((piece, gameBoard) ->
                        new MysteryOutcome(MysteryOutcome.Type.START, "unused")));

        GameSnapshot snapshot = GameSnapshot.from(3, List.of(player), mystery);
        GameSnapshot.PlayerView view = snapshot.getPlayers().getFirst();

        assertEquals(3, snapshot.getRound());
        assertEquals("YELLOW", view.getColor());
        assertEquals(1, view.getBoardCount());
        assertEquals(1, view.getBaseCount());
        assertEquals("Y1", view.getPieces().getFirst().getFullName());
        assertEquals("7", view.getPieces().getFirst().getPosition());
    }

    @Test
    void snapshotDoesNotChangeWhenDomainObjectsLaterChange() {
        Board board = TestSupport.newBoard();
        Piece piece = TestSupport.place(
                board, "1", "YELLOW", 7, "CLOCKWISE");
        YellowPlayer player = new YellowPlayer(
                List.of(piece), new TestSupport.FirstPieceStrategy(true));
        MysteryManager mystery = new MysteryManager(
                board, new TestSupport.SequenceRandom(0),
                List.of((p, b) ->
                        new MysteryOutcome(MysteryOutcome.Type.START, "unused")));
        GameSnapshot snapshot = GameSnapshot.from(0, List.of(player), mystery);

        board.teleportToStandardPath(piece, 20);

        assertEquals("7", snapshot.getPlayers().getFirst()
                .getPieces().getFirst().getPosition());
        assertEquals("20", piece.positionLabel());
    }

    @Test
    void snapshotCollectionsAreUnmodifiable() {
        Board board = TestSupport.newBoard();
        Piece piece = new Piece("1", "YELLOW");
        YellowPlayer player = new YellowPlayer(
                List.of(piece), new TestSupport.FirstPieceStrategy(true));
        MysteryManager mystery = new MysteryManager(
                board, new TestSupport.SequenceRandom(0),
                List.of((p, b) ->
                        new MysteryOutcome(MysteryOutcome.Type.START, "unused")));
        GameSnapshot snapshot = GameSnapshot.from(0, List.of(player), mystery);

        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.getPlayers().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.getPlayers().getFirst().getPieces().clear());
    }
}
