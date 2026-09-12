package board;

import piece.Piece;

/**
 * The final destination for a piece. Position -1 says not on standard path.
 */
public class HomeCell extends Cell {
    private final String ownerColor;

    public HomeCell(String ownerColor) {
        super(-1); // Home is outside the standard path index space
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
        return ownerColor + "Home";
    }
}
