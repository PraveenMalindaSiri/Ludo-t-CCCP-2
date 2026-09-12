package config;

public final class GameConfig {
    private static final GameConfig INSTANCE = new GameConfig();

    private final int standardCellCount = 52;
    private final int homePathLength = 5;
    private final int piecesPerPlayer = 4;
    private final int diceSides = 6;
    private final int mysteryCellDuration = 4;
    private final int maxConsecutiveSixes = 3;
    private final int alphaCell = 8;
    private final int betaCell = 26;
    private final int gammaCell = 45;
    private final int effectDuration = 4;
    private final int consecutiveThreesForTeleport = 3;
    private final int roundsBeforeMysterySpawn = 2;
    private final int totalPlayers = 4;

    // Starting positions (X cells) on the standard path
    private final int yellowStart = 0;
    private final int blueStart = 13;
    private final int redStart = 26;
    private final int greenStart = 39;

    // Approach positions on the standard path
    private final int yellowApproach = 51;
    private final int blueApproach = 12;
    private final int redApproach = 25;
    private final int greenApproach = 38;

    private GameConfig() {
    }

    public static GameConfig getInstance() {
        return INSTANCE;
    }

    public int getStandardCellCount() {
        return standardCellCount;
    }

    public int getHomePathLength() {
        return homePathLength;
    }

    public int getPiecesPerPlayer() {
        return piecesPerPlayer;
    }

    public int getDiceSides() {
        return diceSides;
    }

    public int getMysteryCellDuration() {
        return mysteryCellDuration;
    }

    public int getMaxConsecutiveSixes() {
        return maxConsecutiveSixes;
    }

    public int getAlphaCell() {
        return alphaCell;
    }

    public int getBetaCell() {
        return betaCell;
    }

    public int getGammaCell() {
        return gammaCell;
    }

    public int getEffectDuration() {
        return effectDuration;
    }

    public int getConsecutiveThreesForTeleport() {
        return consecutiveThreesForTeleport;
    }

    public int getRoundsBeforeMysterySpawn() {
        return roundsBeforeMysterySpawn;
    }

    public int getTotalPlayers() {
        return totalPlayers;
    }

    public int getYellowStart() {
        return yellowStart;
    }

    public int getBlueStart() {
        return blueStart;
    }

    public int getRedStart() {
        return redStart;
    }

    public int getGreenStart() {
        return greenStart;
    }

    public int getYellowApproach() {
        return yellowApproach;
    }

    public int getBlueApproach() {
        return blueApproach;
    }

    public int getRedApproach() {
        return redApproach;
    }

    public int getGreenApproach() {
        return greenApproach;
    }

}
