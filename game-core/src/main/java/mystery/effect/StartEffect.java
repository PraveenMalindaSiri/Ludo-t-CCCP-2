package mystery.effect;

import board.Board;
import mystery.MysteryOutcome;
import piece.Piece;

public class StartEffect implements IMysteryEffect {
    // goes to relevant X cell
    @Override
    public MysteryOutcome apply(Piece piece, Board board) {
        int startPos = board.getStartingPosition(piece.getColor());
        board.teleportToStandardPath(piece, startPos);
        piece.setHasPassedApproachOnce(false);
        return new MysteryOutcome(MysteryOutcome.Type.START, "X");
    }
}
