package mystery.effect;

import board.Board;
import mystery.MysteryOutcome;
import piece.Piece;

public interface IMysteryEffect {
    MysteryOutcome apply(Piece piece, Board board);
}
