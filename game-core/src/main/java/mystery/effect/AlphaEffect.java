package mystery.effect;

import board.Board;
import config.GameConfig;
import java.util.Random;
import mystery.MysteryOutcome;
import piece.Piece;
import piece.state.EnergizedState;
import piece.state.SickState;

public class AlphaEffect implements IMysteryEffect {
    private final Random random;

    public AlphaEffect(Random random) {
        this.random = random;
    }

    // Teleport to Alpha cell and apply Energized or Sick state
    @Override
    public MysteryOutcome apply(Piece piece, Board board) {
        int alphaCell = GameConfig.getInstance().getAlphaCell();
        int duration = GameConfig.getInstance().getEffectDuration();

        board.teleportToStandardPath(piece, alphaCell);

        if (random.nextBoolean()) {
            piece.setState(new EnergizedState(duration));
            return new MysteryOutcome(MysteryOutcome.Type.ALPHA_ENERGIZED, "Alpha");
        } else {
            piece.setState(new SickState(duration));
            return new MysteryOutcome(MysteryOutcome.Type.ALPHA_SICK, "Alpha");
        }
    }
}
