package block;

import piece.Piece;

import java.util.List;

public interface ICapturable {
    /**
     * Resets the piece back to base, clearing all state.
     */
    void capture();

    /**
     * Clears direction, capture count, effects, and approach flags.
     */
    void resetState();

    String getColor();

    /** Returns the pieces represented by this capturable component. */
    List<Piece> getPieces();
}
