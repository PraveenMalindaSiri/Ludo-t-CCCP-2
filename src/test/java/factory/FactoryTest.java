package factory;

import board.Board;
import engine.GameEngine;
import mystery.MysteryManager;
import org.junit.jupiter.api.Test;
import piece.Piece;
import player.BluePlayer;
import player.GreenPlayer;
import player.Player;
import player.RedPlayer;
import player.YellowPlayer;
import rules.BlockHandler;
import rules.CaptureHandler;
import support.TestSupport;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FactoryTest {

    @Test
    void playerFactoryCreatesExpectedParticipantAndFourPieces() {
        Board board = BoardFactory.createBoard();
        CaptureHandler capture = new CaptureHandler(board);
        BlockHandler block = new BlockHandler(board);
        MysteryManager mystery = new MysteryManager(
                board, new TestSupport.SequenceRandom(0),
                List.of((piece, gameBoard) -> null));

        Player yellow = PlayerFactory.createPlayer(
                "yellow", capture, block, mystery);
        Player blue = PlayerFactory.createPlayer(
                "BLUE", capture, block, mystery);
        Player red = PlayerFactory.createPlayer(
                "RED", capture, block, mystery);
        Player green = PlayerFactory.createPlayer(
                "GREEN", capture, block, mystery);

        assertTrue(yellow instanceof YellowPlayer);
        assertTrue(blue instanceof BluePlayer);
        assertTrue(red instanceof RedPlayer);
        assertTrue(green instanceof GreenPlayer);
        assertEquals(4, yellow.getPieces().size());
        assertTrue(yellow.getPieces().stream()
                .allMatch(piece -> piece.getColor().equals("YELLOW")));
    }

    @Test
    void playerFactoryRejectsUnknownColor() {
        Board board = BoardFactory.createBoard();
        CaptureHandler capture = new CaptureHandler(board);
        BlockHandler block = new BlockHandler(board);
        MysteryManager mystery = new MysteryManager(
                board, new TestSupport.SequenceRandom(0),
                List.of((piece, gameBoard) -> null));

        assertThrows(IllegalArgumentException.class,
                () -> PlayerFactory.createPlayer(
                        "PURPLE", capture, block, mystery));
    }

    @Test
    void gameFactoryCreatesIndependentSessions() {
        GameEngine first = GameFactory.createGame(100L);
        GameEngine second = GameFactory.createGame(100L);
        first.initializeGame();
        second.initializeGame();

        for (int turn = 0; turn < 4; turn++) first.advanceOneTurn();

        assertEquals(1, first.getRoundCount());
        assertEquals(0, second.getRoundCount());
        assertNotSame(first.getSnapshot(), second.getSnapshot());
        assertEquals(4, second.getSnapshot().getPlayers().size());
        assertTrue(second.getSnapshot().getPlayers().stream()
                .flatMap(player -> player.getPieces().stream())
                .allMatch(piece -> piece.getPosition().equals("Base")));
    }

    @Test
    void createPiecesReturnsIndependentPieceLists() {
        List<Piece> first = PlayerFactory.createPieces("RED");
        List<Piece> second = PlayerFactory.createPieces("RED");

        first.getFirst().moveToPosition(10);

        assertTrue(second.getFirst().isInBase());
        assertNotSame(first.getFirst(), second.getFirst());
    }
}
