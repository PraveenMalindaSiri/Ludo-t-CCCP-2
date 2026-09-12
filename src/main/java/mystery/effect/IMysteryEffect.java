package mystery.effect;

import board.Board;
import piece.Piece;

public interface IMysteryEffect {
    void apply(Piece piece, Board board);
}
