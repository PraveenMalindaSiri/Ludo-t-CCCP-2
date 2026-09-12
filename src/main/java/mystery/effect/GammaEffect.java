package mystery.effect;

import board.Board;
import config.GameConfig;
import piece.Piece;

public class GammaEffect implements IMysteryEffect {
    private final BetaEffect betaEffect;

    public GammaEffect(BetaEffect betaEffect) {
        this.betaEffect = betaEffect;
    }

    // CW will tp to gamma and swap direction. CWW enter to beta effect
    @Override
    public void apply(Piece piece, Board board) {
        if ("CLOCKWISE".equals(piece.getDirection())) {
            int gammaCell = GameConfig.getInstance().getGammaCell();
            piece.moveToPosition(gammaCell);
            board.getCellAt(gammaCell).addPiece(piece);
            piece.setDirection("COUNTERCLOCKWISE");
        } else {
            betaEffect.apply(piece, board);
        }
    }
}
