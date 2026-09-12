package board;

import piece.Piece;

/**
 * One of the 5 color-specific cells in a player's Home Straight.
 * Only pieces matching the owner color may enter.
 */
public class HomeStraightCell extends Cell {
    private final String ownerColor;
    private final int index;

    public HomeStraightCell(int position, String ownerColor, int index) {
        super(position);
        this.ownerColor = ownerColor;
        this.index = index;
    }

    @Override
    public boolean canAcceptPiece(Piece piece) {
        return piece.getColor().equalsIgnoreCase(ownerColor);
    }

    public String getOwnerColor() {
        return ownerColor;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public String toString() {
        return ownerColor + "homepath" + index;
    }

}
