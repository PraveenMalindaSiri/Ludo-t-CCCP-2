package mystery;

import board.Board;
import config.GameConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import mystery.effect.*;
import piece.Piece;

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

    public MysteryManager(Board board, Random random) {
        this(board, random, createDefaultEffects(random));
    }

    public MysteryManager(Board board, Random random, List<IMysteryEffect> effects) {
        this.board = board;
        this.random = random;
        this.config = GameConfig.getInstance();
        if (effects == null || effects.isEmpty()) {
            throw new IllegalArgumentException("Mystery effects cannot be empty.");
        }
        this.effects = new ArrayList<>(effects);
        this.isActive = false;
        this.roundsWithPiecesOnPath = 0;
        this.previousPosition = -1;
    }

    private static List<IMysteryEffect> createDefaultEffects(Random random) {
        BetaEffect betaEffect = new BetaEffect();
        List<IMysteryEffect> defaultEffects = new ArrayList<>();
        defaultEffects.add(new AlphaEffect(random));
        defaultEffects.add(betaEffect);
        defaultEffects.add(new GammaEffect(betaEffect));
        defaultEffects.add(new BaseEffect());
        defaultEffects.add(new StartEffect());
        defaultEffects.add(new ApproachEffect());
        return defaultEffects;
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
    public MysteryOutcome handleLanding(Piece piece) {
        int effectIndex = random.nextInt(effects.size());
        return effects.get(effectIndex).apply(piece, board);
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
}
