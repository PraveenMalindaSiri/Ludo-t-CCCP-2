package mystery.effect;

import board.Board;
import piece.Piece;

public class ApproachEffect implements IMysteryEffect {
    // goes to relevant approach cell
    @Override
    public void apply(Piece piece, Board board) {
        int approachPos = board.getApproachPosition(piece.getColor());
        piece.moveToPosition(approachPos);
        board.getApproachCell(piece.getColor()).addPiece(piece);
        piece.setHasPassedApproachOnce(false);
    }
}
