package block;

public interface ICapturable {
    /**
     * Resets the piece back to base, clearing all state.
     */
    void capture();

    /**
     * Clears direction, capture count, effects, and approach flags.
     */
    void resetState();

    String getColor();
}
