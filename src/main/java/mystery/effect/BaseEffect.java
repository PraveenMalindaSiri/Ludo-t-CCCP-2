package mystery.effect;

import board.Board;
import piece.Piece;

public class BaseEffect implements IMysteryEffect {
    // Go to base cell
    @Override
    public void apply(Piece piece, Board board) {
        piece.capture();
        board.getBaseCell(piece.getColor()).addPiece(piece);
    }
}
