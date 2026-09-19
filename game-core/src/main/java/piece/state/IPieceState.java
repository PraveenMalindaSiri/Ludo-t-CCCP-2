package piece.state;

public interface IPieceState {
    /** Calculates the effective movement */
    int calculateMovement(int diceValue);

    /** Check if frozen */
    boolean canMove();

    /**
     * Called at the end of each round. Timed states decrement their counter and return NormalState
     * when expired.
     */
    IPieceState onRoundPass();

    /**
     * Called when the dice is rolled while this piece is active. FrozenState uses this to track
     * consecutive 3s.
     */
    IPieceState onDiceRoll(int value);

    /** State-specific block eligibility. Only a normal piece may join a block. */
    boolean canJoinBlock();

    /** Signals that this state has requested a move back to base. */
    boolean shouldTeleportToBase();

    /** Called after a pending teleport request has been handled. */
    IPieceState onTeleportHandled();

    /** Stable, presentation-safe state name for immutable snapshots. */
    default String getDisplayName() {
        return getClass().getSimpleName().replace("State", "").toUpperCase();
    }

    /** Remaining duration for timed effects; normal states report zero. */
    default int getRoundsRemaining() {
        return 0;
    }
}
