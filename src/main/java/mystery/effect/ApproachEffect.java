package mystery.effect;

import board.Board;
import mystery.MysteryOutcome;
import piece.Piece;

public class ApproachEffect implements IMysteryEffect {
    // goes to relevant approach cell
    @Override
    public MysteryOutcome apply(Piece piece, Board board) {
        int approachPos = board.getApproachPosition(piece.getColor());
        board.teleportToStandardPath(piece, approachPos);
        piece.setHasPassedApproachOnce(false);
        return new MysteryOutcome(MysteryOutcome.Type.APPROACH, "Approach");
    }
}
