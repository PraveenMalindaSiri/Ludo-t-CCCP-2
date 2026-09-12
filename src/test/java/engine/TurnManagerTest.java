package engine;

import board.Board;
import org.junit.jupiter.api.Test;
import piece.Piece;
import player.Player;
import player.strategy.IPlayerStrategy;
import rules.RuleEngine;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class TurnManagerTest {

    @Test
    void cyclesThroughPlayersInOrder() {
        Player red = new DummyPlayer("RED");
        Player green = new DummyPlayer("GREEN");
        Player yellow = new DummyPlayer("YELLOW");
        TurnManager turnManager = new TurnManager(List.of(red, green, yellow));

        assertSame(red, turnManager.getNextPlayer());
        assertSame(green, turnManager.getNextPlayer());
        assertSame(yellow, turnManager.getNextPlayer());
        assertSame(red, turnManager.getNextPlayer());
    }

    @Test
    void setPlayerOrderChangesCurrentIteratorPosition() {
        Player red = new DummyPlayer("RED");
        Player green = new DummyPlayer("GREEN");
        Player yellow = new DummyPlayer("YELLOW");
        TurnManager turnManager = new TurnManager(List.of(red, green, yellow));

        turnManager.setPlayerOrder(1);

        assertSame(green, turnManager.getCurrentPlayer());
        assertSame(green, turnManager.getNextPlayer());
    }

    @Test
    void tracksRoundCountAndPlayerIndex() {
        Player red = new DummyPlayer("RED");
        Player green = new DummyPlayer("GREEN");
        TurnManager turnManager = new TurnManager(List.of(red, green));

        turnManager.incrementRound();
        turnManager.incrementRound();

        assertEquals(2, turnManager.getRoundCount());
        assertEquals(1, turnManager.getIndexOf(green));
        assertEquals(List.of(red, green), turnManager.getPlayers());
    }

    private static class DummyPlayer extends Player {
        DummyPlayer(String color) {
            super(color, color, List.of(new Piece("1", color)), mock(IPlayerStrategy.class));
        }

        @Override
        protected Piece choosePieceToMove(List<Piece> pieces, int diceValue, Board board, RuleEngine ruleEngine) {
            return pieces.get(0);
        }
    }
}
