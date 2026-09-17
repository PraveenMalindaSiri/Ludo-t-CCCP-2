package util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CyclicIteratorTest {

    @Test
    void iteratorCyclesInStableOrder() {
        CyclicIterator<String> iterator = new CyclicIterator<>(List.of("A", "B", "C"));

        assertEquals("A", iterator.next());
        assertEquals("B", iterator.next());
        assertEquals("C", iterator.next());
        assertEquals("A", iterator.next());
    }

    @Test
    void iteratorCopiesInputAndSupportsIndexControl() {
        List<String> input = new ArrayList<>(List.of("A", "B"));
        CyclicIterator<String> iterator = new CyclicIterator<>(input);
        input.clear();

        iterator.setIndex(1);
        assertEquals("B", iterator.current());
        iterator.reset();
        assertEquals("A", iterator.current());
        assertEquals(2, iterator.size());
        assertTrue(iterator.hasNext());
    }

    @Test
    void iteratorRejectsMissingItemsAndInvalidIndex() {
        assertThrows(IllegalArgumentException.class, () -> new CyclicIterator<>(List.of()));
        assertThrows(IllegalArgumentException.class, () -> new CyclicIterator<>(null));

        CyclicIterator<String> iterator = new CyclicIterator<>(List.of("A"));
        assertThrows(IllegalArgumentException.class, () -> iterator.setIndex(-1));
        assertThrows(IllegalArgumentException.class, () -> iterator.setIndex(1));
    }
}
