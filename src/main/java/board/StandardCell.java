package board;

import piece.Piece;

/**
 * One of the 52 cells on the circular standard path.
 */
public class StandardCell extends Cell {

    public StandardCell(int position) {
        super(position);
    }

    @Override
    public boolean canAcceptPiece(Piece piece) {
        return true;
    }

    @Override
    public String toString() {
        return "StandardCell[" + position + "]";
    }
}
