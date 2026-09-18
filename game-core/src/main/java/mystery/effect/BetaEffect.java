package mystery.effect;

import board.Board;
import config.GameConfig;
import mystery.MysteryOutcome;
import piece.Piece;
import piece.state.FrozenState;

public class BetaEffect implements IMysteryEffect {

    // Teleport piece to Beta cell, then freeze it
    @Override
    public MysteryOutcome apply(Piece piece, Board board) {
        int betaCell = GameConfig.getInstance().getBetaCell();
        int duration = GameConfig.getInstance().getEffectDuration();

        board.teleportToStandardPath(piece, betaCell);

        piece.setState(new FrozenState(duration));
        return new MysteryOutcome(MysteryOutcome.Type.BETA, "Beta");
    }
}
