package board;

import piece.Piece;

/**
 * Where pieces wait before entering the board.
 * Position -2 is for indicating not on standard path, not at home.
 */
public class BaseCell extends Cell {
    private final String ownerColor;

    public BaseCell(String ownerColor) {
        super(-2);
        this.ownerColor = ownerColor;
    }

    @Override
    public boolean canAcceptPiece(Piece piece) {
        return piece.getColor().equalsIgnoreCase(ownerColor);
    }

    public String getOwnerColor() {
        return ownerColor;
    }

    @Override
    public String toString() {
        return ownerColor + "Base";
    }
}
