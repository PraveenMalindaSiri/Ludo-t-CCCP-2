package client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ClientViewStateTest {

    @Test
    void longEventsArePreservedWhileVisibleHistoryRemainsBounded() {
        ClientViewState state = ClientViewState.initial(UUID.randomUUID());
        String longEvent = "x".repeat(20_000);

        state = state.appendEvent("old", 2);
        state = state.appendEvent(longEvent, 2);
        state = state.appendEvent("new", 2);

        assertEquals(2, state.events().size());
        assertTrue(state.events().getFirst().length() >= 20_000);
        assertEquals("new", state.events().getLast());
    }
}
