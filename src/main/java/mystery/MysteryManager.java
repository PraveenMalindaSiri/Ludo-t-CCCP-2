package mystery;

import board.Board;
import config.GameConfig;
import mystery.effect.*;
import piece.Piece;
import piece.state.EnergizedState;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MysteryManager {
    private int currentPosition;
    private int roundsRemaining;
    private int previousPosition;
    private boolean isActive;
    private int roundsWithPiecesOnPath;
    private final Board board;
    private final Random random;
    private final List<IMysteryEffect> effects;
    private final GameConfig config;

    private int lastEffectIndex;
    private boolean lastAlphaWasEnergized;
    private boolean lastGammaWasCCWToBeta;

    public MysteryManager(Board board, Random random) {
        this.board = board;
        this.random = random;
        this.config = GameConfig.getInstance();
        this.isActive = false;
        this.roundsWithPiecesOnPath = 0;
        this.previousPosition = -1;
        this.lastEffectIndex = -1;

        // effects list
        BetaEffect betaEffect = new BetaEffect();
        effects = new ArrayList<>();
        effects.add(new AlphaEffect(random));
        effects.add(betaEffect);
        effects.add(new GammaEffect(betaEffect));
        effects.add(new BaseEffect());
        effects.add(new StartEffect());
        effects.add(new ApproachEffect());
    }

    // spawn mystery cell
    public void spawnMysteryCell() {
        List<Integer> candidates = new ArrayList<>();
        for (int i = 0; i < config.getStandardCellCount(); i++) {
            if (!board.getCellAt(i).hasPieces() && i != previousPosition) {
                candidates.add(i);
            }
        }
        if (candidates.isEmpty()) {
            return;
        }
        currentPosition = candidates.get(random.nextInt(candidates.size()));
        previousPosition = currentPosition;
        roundsRemaining = config.getMysteryCellDuration();
        isActive = true;
    }

    public void updateRound(boolean piecesOnStandardPath) {
        if (piecesOnStandardPath) {
            roundsWithPiecesOnPath++;
        }
        if (isActive) {
            roundsRemaining--;
            if (roundsRemaining <= 0) {
                isActive = false;
                spawnMysteryCell();
            }
        } else if (roundsWithPiecesOnPath >= config.getRoundsBeforeMysterySpawn()) {
            spawnMysteryCell();
        }
    }

    // pick on effect, remove piece from cell and apply effect
    public void handleLanding(Piece piece) {
        lastEffectIndex = random.nextInt(effects.size());
        lastGammaWasCCWToBeta = false;

        board.getCellAt(currentPosition).removePiece(piece);
        effects.get(lastEffectIndex).apply(piece, board);

        if (lastEffectIndex == 0) {
            lastAlphaWasEnergized = piece.getState() instanceof EnergizedState;
        }
        if (lastEffectIndex == 2) {
            lastGammaWasCCWToBeta = (piece.getPosition() == config.getBetaCell());
        }
    }

    public boolean isOnMysteryCell(int position) {
        return isActive && position == currentPosition;
    }

    public int getPosition() {
        return currentPosition;
    }

    public int getRoundsRemaining() {
        return roundsRemaining;
    }

    public boolean isActive() {
        return isActive;
    }

    public int getLastEffectIndex() {
        return lastEffectIndex;
    }

    public boolean isLastAlphaEnergized() {
        return lastAlphaWasEnergized;
    }

    public boolean isLastGammaCCWToBeta() {
        return lastGammaWasCCWToBeta;
    }
}
