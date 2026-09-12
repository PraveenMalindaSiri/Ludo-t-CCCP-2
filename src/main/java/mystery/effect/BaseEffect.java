package mystery.effect;

import board.Board;
import mystery.MysteryOutcome;
import piece.Piece;

public class BaseEffect implements IMysteryEffect {
    // Go to base cell
    @Override
    public MysteryOutcome apply(Piece piece, Board board) {
        board.sendToBase(piece);
        return new MysteryOutcome(MysteryOutcome.Type.BASE, "Base");
    }
}
