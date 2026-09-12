package util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CyclicIteratorTest {

    @Test
    void cyclesThroughItemsRepeatedly() {
        CyclicIterator<String> iterator = new CyclicIterator<>(List.of("R", "G", "Y"));

        assertEquals("R", iterator.next());
        assertEquals("G", iterator.next());
        assertEquals("Y", iterator.next());
        assertEquals("R", iterator.next());
    }

    @Test
    void currentAndSetIndexWorkWithoutAdvancing() {
        CyclicIterator<String> iterator = new CyclicIterator<>(List.of("R", "G", "Y"));

        iterator.setIndex(2);

        assertEquals("Y", iterator.current());
        assertEquals(2, iterator.getCurrentIndex());
        assertEquals(3, iterator.size());
    }

    @Test
    void rejectsEmptyOrInvalidIndex() {
        assertThrows(IllegalArgumentException.class, () -> new CyclicIterator<>(List.of()));
        CyclicIterator<String> iterator = new CyclicIterator<>(List.of("R"));
        assertThrows(IllegalArgumentException.class, () -> iterator.setIndex(1));
    }
}
