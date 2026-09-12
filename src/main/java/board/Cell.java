package board;

import piece.Piece;

import java.util.ArrayList;
import java.util.List;

public abstract class Cell {
    protected final int position;
    protected final List<Piece> pieces;

    protected Cell(int position) {
        this.position = position;
        this.pieces = new ArrayList<>();
    }

    public abstract boolean canAcceptPiece(Piece piece);

    public void addPiece(Piece piece) {
        pieces.add(piece);
    }

    public void removePiece(Piece piece) {
        pieces.remove(piece);
    }

    public boolean hasPieces() {
        return !pieces.isEmpty();
    }

    public List<Piece> getPieces() {
        return new ArrayList<Piece>(pieces);
    }

    public int getPosition() {
        return position;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + position + "]";
    }
}
