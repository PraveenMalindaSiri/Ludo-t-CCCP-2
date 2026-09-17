package piece.state;

/** Default state. No modifiers applied to movement. */
public class NormalState implements IPieceState {
    @Override
    public int calculateMovement(int diceValue) {
        return diceValue;
    }

    @Override
    public boolean canMove() {
        return true;
    }

    @Override
    public IPieceState onRoundPass() {
        return this;
    }

    @Override
    public IPieceState onDiceRoll(int value) {
        return this;
    }

    @Override
    public boolean canJoinBlock() {
        return true;
    }

    @Override
    public boolean shouldTeleportToBase() {
        return false;
    }

    @Override
    public IPieceState onTeleportHandled() {
        return this;
    }

    @Override
    public String toString() {
        return "Normal";
    }
}
