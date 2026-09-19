package server.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import server.session.GameEventCollector.EventBatch;

class GameEventCollectorTest {

    @Test
    void collectsStructuredPresentationDataAndDrainsAtomically() {
        GameEventCollector collector = new GameEventCollector();
        collector.beginOperation();
        collector.onDiceRolled("YELLOW", 6);
        collector.onPieceMoved("YELLOW", "Y1", 8, 14, 6, "CLOCKWISE");

        EventBatch batch = collector.drain();

        assertEquals(6, batch.latestDice());
        assertEquals(2, batch.events().size());
        assertTrue(batch.latestResult().contains("Y1"));
        EventBatch empty = collector.drain();
        assertTrue(empty.events().isEmpty());
        assertNull(empty.latestDice());
    }

    @Test
    void inputListsAreCopiedBeforeTheyLeaveTheObserverCallback() {
        GameEventCollector collector = new GameEventCollector();
        collector.beginOperation();
        collector.onTurnOrder(List.of("RED", "BLUE", "GREEN", "YELLOW"));

        EventBatch batch = collector.drain();

        assertEquals(List.of("Turn order: RED -> BLUE -> GREEN -> YELLOW"), batch.events());
    }
}
