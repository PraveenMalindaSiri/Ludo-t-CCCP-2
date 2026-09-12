package mystery.effect;

import board.Board;
import config.GameConfig;
import piece.Piece;
import piece.state.FrozenState;

public class BetaEffect implements IMysteryEffect {

    // Teleport piece to Beta cell, then freeze it
    @Override
    public void apply(Piece piece, Board board) {
        int betaCell = GameConfig.getInstance().getBetaCell();
        int duration = GameConfig.getInstance().getEffectDuration();

        piece.moveToPosition(betaCell);
        board.getCellAt(betaCell).addPiece(piece);

        piece.setState(new FrozenState(duration));
    }
}
