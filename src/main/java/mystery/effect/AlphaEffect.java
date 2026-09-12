package mystery.effect;

import board.Board;
import config.GameConfig;
import piece.Piece;
import piece.state.EnergizedState;
import piece.state.SickState;

import java.util.Random;

public class AlphaEffect implements IMysteryEffect {
    private final Random random;

    public AlphaEffect(Random random) {
        this.random = random;
    }

    // Teleport to Alpha cell and apply Energized or Sick state
    @Override
    public void apply(Piece piece, Board board) {
        int alphaCell = GameConfig.getInstance().getAlphaCell();
        int duration = GameConfig.getInstance().getEffectDuration();

        piece.moveToPosition(alphaCell);
        board.getCellAt(alphaCell).addPiece(piece);

        if (random.nextBoolean()) {
            piece.setState(new EnergizedState(duration));
        } else {
            piece.setState(new SickState(duration));
        }
    }
}
