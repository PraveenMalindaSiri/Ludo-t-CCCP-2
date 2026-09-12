package engine;

import player.Player;
import util.CyclicIterator;

import java.util.List;

/**
 * Manages turn order and round counting.
 */
public class TurnManager {
    private final List<Player> players;
    private final CyclicIterator<Player> playerIterator;
    private int roundCount;

    public TurnManager(List<Player> players) {
        this.players = players;
        this.playerIterator = new CyclicIterator<>(players);
        this.roundCount = 0;
    }

    // Turn controlling --------------------------------------------------------------------------------------------------

    public Player getNextPlayer() {
        return playerIterator.next();
    }

    public Player getCurrentPlayer() {
        return playerIterator.current();
    }

    public void setPlayerOrder(int index) {
        playerIterator.setIndex(index);
    }

    // Round controlling --------------------------------------------------------------------------------------------------

    public void incrementRound() {
        roundCount++;
    }

    public int getRoundCount() {
        return roundCount;
    }

    // Player info --------------------------------------------------------------------------------------------------

    public List<Player> getPlayers() {
        return players;
    }

    public int getIndexOf(Player player) {
        return players.indexOf(player);
    }

}
