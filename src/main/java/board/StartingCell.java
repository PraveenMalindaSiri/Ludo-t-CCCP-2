package board;

import piece.Piece;

/**
 * The 'X' cell
 **/
public class StartingCell extends Cell {
    private final String ownerColor;

    public StartingCell(int position, String ownerColor) {
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

    @Override
    public String toString() {
        return "StartingCell[" + position + ", owner=" + ownerColor + "]";
    }
}
