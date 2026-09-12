package engine.command;

import mystery.MysteryOutcome;
import piece.Piece;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Structured result produced by a game command.
 */
public final class CommandResult {
    public enum Type {
        ENTER_BOARD,
        MOVE,
        BLOCK_MOVE,
        HOME_MOVE,
        CAPTURE
    }

    private final Type type;
    private final List<Piece> movedPieces = new ArrayList<>();
    private final List<Piece> capturedPieces = new ArrayList<>();
    private final Map<Piece, Integer> capturePositions = new IdentityHashMap<>();
    private int fromPosition;
    private int toPosition;
    private int movement;
    private String direction;
    private boolean executed;
    private boolean blocked;
    private int blockedAt = -1;
    private boolean movedBeforeBlock;
    private boolean moverReturnedToBase;
    private MysteryOutcome mysteryOutcome;

    public CommandResult(Type type) {
        this.type = type;
    }

    public Type getType() { return type; }
    public List<Piece> getMovedPieces() { return new ArrayList<>(movedPieces); }
    public List<Piece> getCapturedPieces() { return new ArrayList<>(capturedPieces); }
    public int getFromPosition() { return fromPosition; }
    public int getToPosition() { return toPosition; }
    public int getMovement() { return movement; }
    public String getDirection() { return direction; }
    public boolean wasExecuted() { return executed; }
    public boolean wasBlocked() { return blocked; }
    public int getBlockedAt() { return blockedAt; }
    public boolean movedBeforeBlock() { return movedBeforeBlock; }
    public boolean wasMoverReturnedToBase() { return moverReturnedToBase; }
    public MysteryOutcome getMysteryOutcome() { return mysteryOutcome; }
    public boolean hasCaptured() { return !capturedPieces.isEmpty(); }

    public void addMovedPiece(Piece piece) {
        if (piece != null && !movedPieces.contains(piece)) movedPieces.add(piece);
    }

    public void addCapturedPiece(Piece piece) {
        addCapturedPiece(piece, piece != null ? piece.getPosition() : -1);
    }

    public void addCapturedPiece(Piece piece, int capturePosition) {
        if (piece != null && !capturedPieces.contains(piece)) {
            capturedPieces.add(piece);
            capturePositions.put(piece, capturePosition);
        }
    }

    public int getCapturePosition(Piece piece) {
        return capturePositions.getOrDefault(piece, -1);
    }

    public void setMovement(int fromPosition, int toPosition,
                            int movement, String direction) {
        this.fromPosition = fromPosition;
        this.toPosition = toPosition;
        this.movement = movement;
        this.direction = direction;
        this.executed = true;
    }

    public void markBlocked(int position) {
        blocked = true;
        blockedAt = position;
    }

    public void markMovedBeforeBlock() {
        movedBeforeBlock = true;
    }

    public void markMoverReturnedToBase() {
        moverReturnedToBase = true;
    }

    public void setMysteryOutcome(MysteryOutcome mysteryOutcome) {
        this.mysteryOutcome = mysteryOutcome;
    }
}
