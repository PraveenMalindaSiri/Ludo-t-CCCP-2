package piece.state;

/**
 * Applied when teleported to Alpha and outcome is "sick".
 * Movement halves for effectDuration rounds, then auto-transitions to NormalState.
 */
public class SickState implements IPieceState {
    private int roundsRemaining;

    public SickState(int roundsRemaining) {
        this.roundsRemaining = roundsRemaining;
    }

    @Override
    public int calculateMovement(int diceValue) {
        return diceValue / 2;
    }

    @Override
    public boolean canMove() {
        return true;
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
        return this;
    }

    public int getRoundsRemaining() {
        return roundsRemaining;
    }

    @Override
    public String toString() {
        return "Sick(" + roundsRemaining + " rounds left)";
    }
}
