package event;

import mystery.MysteryOutcome;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameEventPublisherTest {

    @Test
    void registeredListenerReceivesStructuredEvents() {
        GameEventPublisher publisher = new GameEventPublisher();
        RecordingListener listener = new RecordingListener();
        MysteryOutcome outcome = new MysteryOutcome(
                MysteryOutcome.Type.BETA, "Beta");
        publisher.addListener(listener);

        publisher.diceRolled("YELLOW", 6);
        publisher.mysteryResolved("YELLOW", "Y1", outcome);
        publisher.gameWon("YELLOW");

        assertEquals(List.of("YELLOW:6"), listener.diceEvents);
        assertSame(outcome, listener.mysteryOutcome);
        assertEquals("YELLOW", listener.winner);
    }

    @Test
    void duplicateRegistrationDoesNotDuplicateNotification() {
        GameEventPublisher publisher = new GameEventPublisher();
        RecordingListener listener = new RecordingListener();

        publisher.addListener(listener);
        publisher.addListener(listener);
        publisher.diceRolled("BLUE", 3);

        assertEquals(1, listener.diceEvents.size());
    }

    @Test
    void removedListenerReceivesNoFurtherNotification() {
        GameEventPublisher publisher = new GameEventPublisher();
        RecordingListener listener = new RecordingListener();
        publisher.addListener(listener);

        publisher.removeListener(listener);
        publisher.gameWon("RED");

        assertNull(listener.winner);
    }

    @Test
    void listPayloadsAreImmutableCopies() {
        GameEventPublisher publisher = new GameEventPublisher();
        RecordingListener listener = new RecordingListener();
        publisher.addListener(listener);
        List<String> original = new ArrayList<>(List.of("YELLOW", "BLUE"));

        publisher.turnOrder(original);
        original.add("RED");

        assertEquals(List.of("YELLOW", "BLUE"), listener.turnOrder);
        assertThrows(UnsupportedOperationException.class,
                () -> listener.turnOrder.add("GREEN"));
    }

    @Test
    void listenerCanRemoveItselfDuringNotification() {
        GameEventPublisher publisher = new GameEventPublisher();
        int[] calls = {0};
        IGameEventListener selfRemoving = new IGameEventListener() {
            @Override
            public void onGameWon(String color) {
                calls[0]++;
                publisher.removeListener(this);
            }
        };
        publisher.addListener(selfRemoving);

        publisher.gameWon("GREEN");
        publisher.gameWon("GREEN");

        assertEquals(1, calls[0]);
    }

    private static final class RecordingListener implements IGameEventListener {
        private final List<String> diceEvents = new ArrayList<>();
        private MysteryOutcome mysteryOutcome;
        private String winner;
        private List<String> turnOrder;

        @Override
        public void onDiceRolled(String color, int value) {
            diceEvents.add(color + ":" + value);
        }

        @Override
        public void onMysteryResolved(String color, String pieceName,
                                      MysteryOutcome outcome) {
            mysteryOutcome = outcome;
        }

        @Override
        public void onGameWon(String color) {
            winner = color;
        }

        @Override
        public void onTurnOrder(List<String> colors) {
            turnOrder = colors;
        }
    }
}
