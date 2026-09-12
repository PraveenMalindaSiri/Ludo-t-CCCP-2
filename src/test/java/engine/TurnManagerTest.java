package engine;

import org.junit.jupiter.api.Test;
import piece.Piece;
import player.BluePlayer;
import player.Player;
import player.YellowPlayer;
import support.TestSupport;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TurnManagerTest {

    @Test
    void managerCyclesPlayersFromSelectedStartingIndex() {
        Player yellow = new YellowPlayer(
                List.of(new Piece("1", "YELLOW")),
                new TestSupport.FirstPieceStrategy(true));
        Player blue = new BluePlayer(
                List.of(new Piece("1", "BLUE")),
                new TestSupport.FirstPieceStrategy(true));
        TurnManager manager = new TurnManager(List.of(yellow, blue));

        manager.setPlayerOrder(1);

        assertSame(blue, manager.getNextPlayer());
        assertSame(yellow, manager.getNextPlayer());
        assertEquals(0, manager.getRoundCount());
        manager.incrementRound();
        assertEquals(1, manager.getRoundCount());
    }

    @Test
    void managerReturnsDefensivePlayerList() {
        Player yellow = new YellowPlayer(
                List.of(new Piece("1", "YELLOW")),
                new TestSupport.FirstPieceStrategy(true));
        TurnManager manager = new TurnManager(List.of(yellow));

        manager.getPlayers().clear();

        assertEquals(1, manager.getPlayers().size());
        assertEquals(0, manager.getIndexOf(yellow));
    }
}
