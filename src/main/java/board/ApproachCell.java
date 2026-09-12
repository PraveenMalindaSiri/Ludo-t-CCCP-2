package board;

import piece.Piece;


/**
 * The last standard-path cell before a color's Home Straight.
 */
public class ApproachCell extends Cell {
    private final String ownerColor;

    public ApproachCell(int position, String ownerColor) {
        super(position);
        this.ownerColor = ownerColor;
    }

    @Override
    public boolean canAcceptPiece(Piece piece) {
        return true;
    }

    public String getOwnerColor() {
        return ownerColor;
    }

    public boolean isApproachFor(String color) {
        return ownerColor.equalsIgnoreCase(color);
    }

    @Override
    public String toString() {
        return "ApproachCell[" + position + ", owner=" + ownerColor + "]";
    }
}
