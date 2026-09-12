package event;

import mystery.MysteryManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import player.Player;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GameLoggerTest {
    private PrintStream originalOut;
    private ByteArrayOutputStream out;
    private GameLogger logger;

    @BeforeEach
    void setUp() {
        originalOut = System.out;
        out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));
        logger = new GameLogger(mock(MysteryManager.class));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    @Test
    void printsRequiredPlayerInfoMessage() {
        logger.onPlayerInfo("RED", List.of("R1", "R2", "R3", "R4"));

        assertTrue(output().contains("The red player has four (04) pieces named R1, R2, R3, and R4."));
    }

    @Test
    void printsDiceRollAndMovementMessages() {
        logger.onDiceRolled("BLUE", 6);
        logger.onPieceMoved("BLUE", "B1", 13, 19, 6, "CLOCKWISE");

        String output = output();
        assertTrue(output.contains("Blue player rolled 6."));
        assertTrue(output.contains("Blue moves piece B1 from location 13 to 19 by 6 units in clockwise direction."));
    }

    @Test
    void printsCaptureMessageAndCapturedPlayerCounts() {
        logger.onPieceCaptured("RED", "R1", 14,
                "BLUE", "B2", 2, 2);

        String output = output();
        assertTrue(output.contains("Red piece R1 lands on square 14, captures Blue piece B2, and returns it to the base."));
        assertTrue(output.contains("Blue player now has 2/4 on pieces on the board and 2/4 pieces on the base."));
    }

    @Test
    void printsMysteryAndWinMessages() {
        logger.onMysteryLanding("GREEN", "G1", "Alpha");
        logger.onTeleportEffect("GREEN", "G1", "feels energized, and movement speed doubles.");
        logger.onGameWon("GREEN");

        String output = output();
        assertTrue(output.contains("Green player lands on a mystery cell and is teleported to Alpha."));
        assertTrue(output.contains("Green piece G1 teleported to Alpha."));
        assertTrue(output.contains("Green piece G1 feels energized, and movement speed doubles."));
        assertTrue(output.contains("Green player wins!!!"));
    }

    @Test
    void printsInitialRollFirstPlayerAndTurnOrderMessages() {
        // Act
        logger.onInitialRoll("RED", 5);
        logger.onFirstPlayer("BLUE");
        logger.onTurnOrder(List.of("BLUE", "RED", "GREEN", "YELLOW"));

        // Assert
        String output = output();
        assertTrue(output.contains("Red rolls 5"));
        assertTrue(output.contains("Blue player has the highest roll and will begin the game."));
        assertTrue(output.contains("The order of a single round is Blue, Red, Green, and Yellow."));
    }

    @Test
    void printsBaseEntryAndBlockedMoveMessages() {
        // Act
        logger.onPieceEnteredBoard("YELLOW", "Y1", 1, 3);
        logger.onPieceBlocked("YELLOW", "Y1", 0, 4, "RED", "R1");
        logger.onNoOtherPieces("YELLOW");
        logger.onMovedBeforeBlock("YELLOW", "Y1", 3);

        // Assert
        String output = output();

        assertTrue(output.contains("Yellow player moves piece Y1 to the starting point."));
        assertTrue(output.contains("Yellow player now has 1/4 on pieces on the board and 3/4 pieces on the base."));

        assertTrue(output.contains("Yellow piece Y1 is blocked from moving from 0 to 4 by Red piece R1."));
        assertTrue(output.contains("Ignoring the throw and moving on to the next player."));
        assertTrue(output.contains("Moved the piece to square 3 which is the cell before the block."));
    }

    @Test
    void printsDirectionChangeAndMysterySpawnMessages() {
        // Act
        logger.onDirectionChanged("RED", "R1", "CLOCKWISE", "COUNTERCLOCKWISE");
        logger.onDirectionChanged("BLUE", "B1", "COUNTERCLOCKWISE", "CLOCKWISE");
        logger.onMysteryCellSpawned(27, 4);

        // Assert
        String output = output();

        assertTrue(output.contains(
                "The Red piece R1, which was moving clockwise, has changed to moving counterclockwise."
        ));

        assertTrue(output.contains(
                "The Blue piece B1 is moving in a counterclockwise direction. Teleporting to Beta from Gamma."
        ));

        assertTrue(output.contains(
                "A mystery cell has spawned in location 27 and will be at this location for the next 4 rounds."
        ));
    }

    @Test
    void printsFinalPlacementMessages() {
        // Arrange
        Player green = mock(Player.class);
        Player red = mock(Player.class);
        Player blue = mock(Player.class);
        Player yellow = mock(Player.class);

        when(green.getColor()).thenReturn("GREEN");
        when(red.getColor()).thenReturn("RED");
        when(blue.getColor()).thenReturn("BLUE");
        when(yellow.getColor()).thenReturn("YELLOW");

        // Act
        logger.onFinalPlacements(List.of(green, red, blue, yellow));

        // Assert
        String output = output();

        assertTrue(output.contains("Final Places"));
        assertTrue(output.contains("1st place: Green player"));
        assertTrue(output.contains("2nd place: Red player"));
        assertTrue(output.contains("3rd place: Blue player"));
        assertTrue(output.contains("4th place: Yellow player"));
    }

    private String output() {
        return out.toString();
    }
}
