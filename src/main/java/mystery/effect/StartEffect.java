package mystery.effect;

import board.Board;
import piece.Piece;

public class StartEffect implements IMysteryEffect {
    // goes to relevant X cell
    @Override
    public void apply(Piece piece, Board board) {
        int startPos = board.getStartingPosition(piece.getColor());
        piece.moveToPosition(startPos);
        board.getStartingCell(piece.getColor()).addPiece(piece);
        piece.setHasPassedApproachOnce(false);
    }
}
