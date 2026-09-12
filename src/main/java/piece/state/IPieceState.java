package piece.state;

public interface IPieceState {
    /**
     * Calculates the effective movement
     */
    int calculateMovement(int diceValue);

    /**
     * Check if frozen
     */
    boolean canMove();

    /**
     * Called at the end of each round.
     * Timed states decrement their counter and return NormalState when expired.
     */
    IPieceState onRoundPass();

    /**
     * Called when the dice is rolled while this piece is active.
     * FrozenState uses this to track consecutive 3s.
     */
    IPieceState onDiceRoll(int value);
}
