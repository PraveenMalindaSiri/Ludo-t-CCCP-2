package util;

import java.util.ArrayList;
import java.util.List;

public class CyclicIterator<T> {
    private final List<T> items;
    private int currentIndex;

    public CyclicIterator(List<T> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("CyclicIterator requires a non-empty list.");
        }
        this.items = new ArrayList<>(items);
        this.currentIndex = 0;
    }

    public T next() {
        T item = items.get(currentIndex);
        currentIndex = (currentIndex + 1) % items.size();
        return item;
    }

    public T current() {
        return items.get(currentIndex);
    }

    public void reset() {
        currentIndex = 0;
    }

    public void setIndex(int index) {
        if (index < 0 || index >= items.size()) {
            throw new IllegalArgumentException("Index out of bounds: " + index);
        }
        currentIndex = index;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public boolean hasNext() {
        return true;
    }

    public int size() {
        return items.size();
    }
}
