package piece.state;

import config.GameConfig;

/**
 * Applied when teleported to Beta. The piece cannot move for effectDuration rounds.
 * If the player rolls three consecutive 3s while frozen, the piece teleports to base.
 */
public class FrozenState implements IPieceState {
    private int roundsRemaining;
    private int consecutiveThrees;
    private boolean shouldTeleportToBase;

    private final int requiredConsecutiveThrees;

    public FrozenState(int roundsRemaining) {
        this.roundsRemaining = roundsRemaining;
        this.consecutiveThrees = 0;
        this.shouldTeleportToBase = false;
        this.requiredConsecutiveThrees =
                GameConfig.getInstance().getConsecutiveThreesForTeleport();
    }

    @Override
    public int calculateMovement(int diceValue) {
        return 0;
    }

    @Override
    public boolean canMove() {
        return false;
    }

    @Override
    public IPieceState onRoundPass() {
        roundsRemaining--;
        if (roundsRemaining <= 0) {
            return new NormalState();
        }
        return this;
    }

    @Override
    public IPieceState onDiceRoll(int value) {
        if (value == 3) {
            consecutiveThrees++;
            if (consecutiveThrees >= requiredConsecutiveThrees) {
                shouldTeleportToBase = true;
            }
        } else {
            consecutiveThrees = 0; // streak broken
        }
        return this;
    }

    public boolean shouldTeleportToBase() {
        return shouldTeleportToBase;
    }

    public void resetTeleportFlag() {
        shouldTeleportToBase = false;
        consecutiveThrees = 0;
    }

    public int getRoundsRemaining() {
        return roundsRemaining;
    }

    public int getConsecutiveThrees() {
        return consecutiveThrees;
    }

    @Override
    public String toString() {
        return "Frozen(" + roundsRemaining + " rounds left, " + consecutiveThrees + " threes)";
    }
}
