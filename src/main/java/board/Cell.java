package board;

import java.util.ArrayList;
import java.util.List;
import piece.Piece;

public abstract class Cell {
    protected final int position;
    protected final List<Piece> pieces;

    protected Cell(int position) {
        this.position = position;
        this.pieces = new ArrayList<>();
    }

    public abstract boolean canAcceptPiece(Piece piece);

    public void addPiece(Piece piece) {
        if (piece == null) {
            throw new IllegalArgumentException("Piece cannot be null.");
        }
        if (!canAcceptPiece(piece)) {
            throw new IllegalArgumentException(
                    getClass().getSimpleName() + " cannot accept " + piece.getFullName());
        }
        if (!pieces.contains(piece)) {
            pieces.add(piece);
        }
    }

    public void removePiece(Piece piece) {
        pieces.remove(piece);
    }

    public boolean hasPieces() {
        return !pieces.isEmpty();
    }

    public List<Piece> getPieces() {
        return new ArrayList<>(pieces);
    }

    public int getPosition() {
        return position;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + position + "]";
    }
}
