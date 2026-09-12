package piece;

import block.ICapturable;
import block.IMovable;
import config.GameConfig;
import piece.state.*;

/**
 * Represents one game piece belonging to a player.
 */
public class Piece implements IMovable, ICapturable {
    public static final int HOME_STRAIGHT_OFFSET = 100;
    public static final int BASE_POSITION = -2;
    public static final int HOME_POSITION = -1;

    private final String name;
    private final String color;

    private int position;
    private boolean onBoard;
    private boolean atHome;
    private boolean inBase;

    private String direction;         // "CLOCKWISE" or "COUNTERCLOCKWISE"
    private String originalDirection; // set when enters X

    private int captureCount;
    private boolean hasPassedApproachOnce; // for CCW pieces second pass
    private boolean inBlock;
    private IPieceState currentState;

    public Piece(String name, String color) {
        this.name = name;
        this.color = color;
        this.currentState = new NormalState();
        resetToBase();
    }

    @Override
    public void move(int steps) {
        int count = GameConfig.getInstance().getStandardCellCount();
        if ("CLOCKWISE".equals(direction)) {
            position = (position + steps) % count;
        } else {
            position = (position - steps + count) % count;
        }
    }

    @Override
    public int getPosition() {
        return position;
    }

    @Override
    public String getDirection() {
        return direction;
    }

    @Override
    public void capture() {
        resetState();
    }

    @Override
    public void resetState() {
        resetToBase();
    }

    @Override
    public String getColor() {
        return color;
    }

    // state -------------------------------------------------------------------------------------------
    public int getEffectiveMovement(int diceValue) {
        return currentState.calculateMovement(diceValue);
    }

    public boolean canMove() {
        return currentState.canMove();
    }

    public void updateState() {
        currentState = currentState.onRoundPass();
    }

    public void notifyDiceRoll(int diceValue) {
        currentState = currentState.onDiceRoll(diceValue);
    }

    public void setState(IPieceState state) {
        this.currentState = state;
    }

    public IPieceState getState() {
        return currentState;
    }

    // Position helpers -------------------------------------------------------------------------------------------

    public void moveToBase() {
        resetToBase();
    }

    public void moveToHome() {
        clearTemporaryState();
        this.position = HOME_POSITION;
        this.onBoard = false;
        this.atHome = true;
        this.inBase = false;
    }

    public void moveToPosition(int position) {
        this.position = position;
        this.onBoard = true;
        this.atHome = false;
        this.inBase = false;
    }

    public void moveToHomeStraight(int index) {
        clearTemporaryState();
        this.position = HOME_STRAIGHT_OFFSET + index;
        this.onBoard = true;
        this.atHome = false;
        this.inBase = false;
    }

    public int getHomeStraightIndex() {
        if (isInHomeStraight()) {
            return position - HOME_STRAIGHT_OFFSET;
        }
        return -1;
    }

    public boolean isInHomeStraight() {
        return position >= HOME_STRAIGHT_OFFSET
                && position < HOME_STRAIGHT_OFFSET
                + GameConfig.getInstance().getHomePathLength();
    }

    // Status -------------------------------------------------------------------------------------------

    public boolean isOnBoard() {
        return onBoard;
    }

    public boolean isAtHome() {
        return atHome;
    }

    public boolean isInBase() {
        return inBase;
    }

    public boolean isNormalState() {
        return currentState instanceof NormalState;
    }

    public boolean isFrozen() {
        return currentState instanceof FrozenState;
    }

    public boolean isSick() {
        return currentState instanceof SickState;
    }

    public boolean isEnergized() {
        return currentState instanceof EnergizedState;
    }

    public void clearTemporaryState() {
        this.currentState = new NormalState();
    }

    // Directions -------------------------------------------------------------------------------------------

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getOriginalDirection() {
        return originalDirection;
    }

    public void setOriginalDirection(String originalDirection) {
        this.originalDirection = originalDirection;
    }

    // Capture info -------------------------------------------------------------------------------------------

    public void incrementCaptureCount() {
        captureCount++;
    }

    public int getCaptureCount() {
        return captureCount;
    }

    // Approach info -------------------------------------------------------------------------------------------

    public boolean getHasPassedApproachOnce() {
        return hasPassedApproachOnce;
    }

    public void setHasPassedApproachOnce(boolean value) {
        this.hasPassedApproachOnce = value;
    }

    // Block info -------------------------------------------------------------------------------------------

    public boolean isInBlock() {
        return inBlock;
    }

    public void setInBlock(boolean inBlock) {
        this.inBlock = inBlock;
    }

    // others -------------------------------------------------------------------------------------------

    public String getName() {
        return name;
    }

    public String getFullName() {
        return String.valueOf(color.charAt(0)) + name;
    }

    private void resetToBase() {
        this.position = BASE_POSITION;
        this.onBoard = false;
        this.atHome = false;
        this.inBase = true;
        this.direction = "CLOCKWISE";
        this.originalDirection = "CLOCKWISE";
        this.captureCount = 0;
        this.hasPassedApproachOnce = false;
        this.inBlock = false;
        this.currentState = new NormalState();
    }

    @Override
    public String toString() {
        return getFullName() + "@" + positionLabel();
    }

    public String positionLabel() {
        if (inBase) return "Base";
        if (atHome) return "Home";
        if (isInHomeStraight()) return color.toLowerCase() + "homepath" + getHomeStraightIndex();
        return String.valueOf(position);
    }
}
