package factory;

import board.Board;
import mystery.MysteryManager;
import org.junit.jupiter.api.Test;
import player.*;
import rules.BlockHandler;
import rules.CaptureHandler;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class PlayerFactoryTest {

    @Test
    void createsFourPiecesWithCorrectColourAndNames() {
        var pieces = PlayerFactory.createPieces("RED");

        assertEquals(4, pieces.size());
        assertEquals("RED", pieces.getFirst().getColor());
        assertEquals("1", pieces.getFirst().getName());
        assertEquals("R1", pieces.getFirst().getFullName());
        assertTrue(pieces.stream().allMatch(piece -> piece.isInBase()));
    }

    @Test
    void createsCorrectPlayerSubclassForEachColour() {
        Board board = BoardFactory.createBoard();
        CaptureHandler captureHandler = new CaptureHandler(board);
        BlockHandler blockHandler = new BlockHandler(board);
        MysteryManager mysteryManager = new MysteryManager(board, new Random(1));

        assertInstanceOf(RedPlayer.class, PlayerFactory.createPlayer("RED", captureHandler, blockHandler, mysteryManager));
        assertInstanceOf(GreenPlayer.class, PlayerFactory.createPlayer("GREEN", captureHandler, blockHandler, mysteryManager));
        assertInstanceOf(YellowPlayer.class, PlayerFactory.createPlayer("YELLOW", captureHandler, blockHandler, mysteryManager));
        assertInstanceOf(BluePlayer.class, PlayerFactory.createPlayer("BLUE", captureHandler, blockHandler, mysteryManager));
    }

    @Test
    void rejectsUnknownPlayerColour() {
        Board board = BoardFactory.createBoard();
        CaptureHandler captureHandler = new CaptureHandler(board);
        BlockHandler blockHandler = new BlockHandler(board);
        MysteryManager mysteryManager = new MysteryManager(board, new Random(1));

        assertThrows(IllegalArgumentException.class,
                () -> PlayerFactory.createPlayer("PURPLE", captureHandler, blockHandler, mysteryManager));
    }
}
