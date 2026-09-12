package factory;

import board.*;
import config.GameConfig;
import piece.Piece;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Creates and assembles the complete Board.
 */
public class BoardFactory {
    private BoardFactory() {
    }

    public static Board createBoard() {
        GameConfig config = GameConfig.getInstance();

        List<Cell> standardPath =
                createStandardPath(config);

        Map<String, StartingCell> startingCells =
                createStartingCells(config);

        Map<String, ApproachCell> approachCells =
                createApproachCells(config);

        Map<String, List<HomeStraightCell>> homeStraightCells =
                createHomeStraights(config);

        Map<String, HomeCell> homeCells =
                createHomeCells();

        Map<String, BaseCell> baseCells =
                createBaseCells();

        // putting starting cells at correct places in standard path
        standardPath.set(config.getYellowStart(),
                startingCells.get("YELLOW"));
        standardPath.set(config.getBlueStart(),
                startingCells.get("BLUE"));
        standardPath.set(config.getRedStart(),
                startingCells.get("RED"));
        standardPath.set(config.getGreenStart(),
                startingCells.get("GREEN"));

        // putting approach cells at correct places in standard path
        standardPath.set(config.getYellowApproach(),
                approachCells.get("YELLOW"));
        standardPath.set(config.getBlueApproach(),
                approachCells.get("BLUE"));
        standardPath.set(config.getRedApproach(),
                approachCells.get("RED"));
        standardPath.set(config.getGreenApproach(),
                approachCells.get("GREEN"));

        return new Board(
                standardPath,
                startingCells,
                approachCells,
                homeStraightCells,
                homeCells,
                baseCells
        );
    }

    // Standard path ----------------------------------------------------------------

    private static List<Cell> createStandardPath(GameConfig config) {
        List<Cell> path = new ArrayList<>();
        for (int i = 0; i < config.getStandardCellCount(); i++) {
            path.add(new StandardCell(i));
        }
        return path;
    }

    // Starting cells ----------------------------------------------------------------

    private static Map<String, StartingCell> createStartingCells(
            GameConfig config) {
        Map<String, StartingCell> cells = new HashMap<>();
        cells.put("YELLOW",
                new StartingCell(config.getYellowStart(), "YELLOW"));
        cells.put("BLUE",
                new StartingCell(config.getBlueStart(), "BLUE"));
        cells.put("RED",
                new StartingCell(config.getRedStart(), "RED"));
        cells.put("GREEN",
                new StartingCell(config.getGreenStart(), "GREEN"));
        return cells;
    }

    // Approach cells ----------------------------------------------------------------

    private static Map<String, ApproachCell> createApproachCells(
            GameConfig config) {
        Map<String, ApproachCell> cells = new HashMap<>();
        cells.put("YELLOW",
                new ApproachCell(config.getYellowApproach(), "YELLOW"));
        cells.put("BLUE",
                new ApproachCell(config.getBlueApproach(), "BLUE"));
        cells.put("RED",
                new ApproachCell(config.getRedApproach(), "RED"));
        cells.put("GREEN",
                new ApproachCell(config.getGreenApproach(), "GREEN"));
        return cells;
    }

    // Home straights ----------------------------------------------------------------

    private static Map<String, List<HomeStraightCell>> createHomeStraights(
            GameConfig config) {
        Map<String, List<HomeStraightCell>> straights = new HashMap<>();
        String[] colors = {"YELLOW", "BLUE", "RED", "GREEN"};

        for (String color : colors) {
            List<HomeStraightCell> cells = new ArrayList<>();
            for (int i = 0; i < config.getHomePathLength(); i++) {
                cells.add(new HomeStraightCell(
                        Piece.HOME_STRAIGHT_OFFSET + i,
                        color,
                        i
                ));
            }
            straights.put(color, cells);
        }
        return straights;
    }

    // Home cells ----------------------------------------------------------------

    private static Map<String, HomeCell> createHomeCells() {
        Map<String, HomeCell> cells = new HashMap<>();
        cells.put("YELLOW", new HomeCell("YELLOW"));
        cells.put("BLUE", new HomeCell("BLUE"));
        cells.put("RED", new HomeCell("RED"));
        cells.put("GREEN", new HomeCell("GREEN"));
        return cells;
    }

    // Base cells ----------------------------------------------------------------

    private static Map<String, BaseCell> createBaseCells() {
        Map<String, BaseCell> cells = new HashMap<>();
        cells.put("YELLOW", new BaseCell("YELLOW"));
        cells.put("BLUE", new BaseCell("BLUE"));
        cells.put("RED", new BaseCell("RED"));
        cells.put("GREEN", new BaseCell("GREEN"));
        return cells;
    }

}
