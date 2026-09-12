package mystery.effect;

import board.Board;
import config.GameConfig;
import mystery.MysteryOutcome;
import piece.Piece;

public class GammaEffect implements IMysteryEffect {
    private final BetaEffect betaEffect;

    public GammaEffect(BetaEffect betaEffect) {
        this.betaEffect = betaEffect;
    }

    // CW will tp to gamma and swap direction. CWW enter to beta effect
    @Override
    public MysteryOutcome apply(Piece piece, Board board) {
        if ("CLOCKWISE".equals(piece.getDirection())) {
            String oldDirection = piece.getDirection();
            int gammaCell = GameConfig.getInstance().getGammaCell();
            board.teleportToStandardPath(piece, gammaCell);
            piece.setDirection("COUNTERCLOCKWISE");
            return new MysteryOutcome(
                    MysteryOutcome.Type.GAMMA_DIRECTION_CHANGED,
                    "Gamma", oldDirection, piece.getDirection());
        } else {
            betaEffect.apply(piece, board);
            return new MysteryOutcome(
                    MysteryOutcome.Type.GAMMA_TO_BETA,
                    "Beta", "COUNTERCLOCKWISE", "COUNTERCLOCKWISE");
        }
    }
}
