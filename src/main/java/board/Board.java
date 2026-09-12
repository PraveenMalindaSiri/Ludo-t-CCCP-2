package board;

import config.GameConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Holds all cells on the board.
 */
public class Board {
    private final List<Cell> standardPath;
    private final Map<String, StartingCell> startingCells;
    private final Map<String, ApproachCell> approachCells;
    private final Map<String, List<HomeStraightCell>> homeStraightCells;
    private final Map<String, HomeCell> homeCells;
    private final Map<String, BaseCell> baseCells;

    private final GameConfig config;

    public Board(
            List<Cell> standardPath,
            Map<String, StartingCell> startingCells,
            Map<String, ApproachCell> approachCells,
            Map<String, List<HomeStraightCell>> homeStraightCells,
            Map<String, HomeCell> homeCells,
            Map<String, BaseCell> baseCells
    ) {
        this.standardPath = standardPath;
        this.startingCells = startingCells;
        this.approachCells = approachCells;
        this.homeStraightCells = homeStraightCells;
        this.homeCells = homeCells;
        this.baseCells = baseCells;
        this.config = GameConfig.getInstance();
    }

    // Normal cell -------------------------------------------------------------------------------------

    public Cell getCellAt(int position) {
        if (position < 0 || position >= config.getStandardCellCount()) {
            throw new IllegalArgumentException("Position out of range: " + position);
        }
        return standardPath.get(position);
    }

    public List<Cell> getStandardPath() {
        return new ArrayList<>(standardPath);
    }

    // Special cell -------------------------------------------------------------------------------------

    public StartingCell getStartingCell(String color) {
        StartingCell cell = startingCells.get(color.toUpperCase());
        if (cell == null) {
            throw new IllegalArgumentException("No StartingCell for color: " + color);
        }
        return cell;
    }

    public ApproachCell getApproachCell(String color) {
        ApproachCell cell = approachCells.get(color.toUpperCase());
        if (cell == null) {
            throw new IllegalArgumentException("No ApproachCell for color: " + color);
        }
        return cell;
    }

    public List<HomeStraightCell> getHomeStraight(String color) {
        List<HomeStraightCell> cells = homeStraightCells.get(color.toUpperCase());
        if (cells == null) {
            throw new IllegalArgumentException("No home straight for color: " + color);
        }
        return cells;
    }

    public HomeStraightCell getHomeStraightCell(String color, int index) {
        return getHomeStraight(color).get(index);
    }

    public HomeCell getHomeCell(String color) {
        HomeCell cell = homeCells.get(color.toUpperCase());
        if (cell == null) {
            throw new IllegalArgumentException("No home cell for color: " + color);
        }
        return cell;
    }

    public BaseCell getBaseCell(String color) {
        BaseCell cell = baseCells.get(color.toUpperCase());
        if (cell == null) {
            throw new IllegalArgumentException("No base cell for color: " + color);
        }
        return cell;
    }

    // Position info -------------------------------------------------------------------------------------

    public int getStartingPosition(String color) {
        return getStartingCell(color).getPosition();
    }

    public int getApproachPosition(String color) {
        return getApproachCell(color).getPosition();
    }

    public boolean isApproachCell(int position, String color) {
        if (position < 0 || position >= config.getStandardCellCount()) return false;

        Cell cell = getCellAt(position);

        return cell instanceof ApproachCell
                && ((ApproachCell) cell).isApproachFor(color);
    }

    public boolean isStartingCell(int position, String color) {
        if (position < 0 || position >= config.getStandardCellCount()) return false;

        Cell cell = getCellAt(position);

        return cell instanceof StartingCell
                && ((StartingCell) cell).getOwnerColor().equalsIgnoreCase(color);
    }
}
