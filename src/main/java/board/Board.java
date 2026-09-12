package board;

import block.Block;
import block.ICapturable;
import block.IMovable;
import config.GameConfig;
import piece.Piece;

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
        return new ArrayList<>(cells);
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

    // Controlled piece relocation ---------------------------------------------------------------

    public void initializeInBase(Piece piece) {
        BaseCell base = getBaseCell(piece.getColor());
        if (!base.getPieces().contains(piece)) {
            base.addPiece(piece);
        }
    }

    public void enterBoard(Piece piece) {
        removeFromCurrentCell(piece);
        StartingCell destination = getStartingCell(piece.getColor());
        piece.moveToPosition(destination.getPosition());
        destination.addPiece(piece);
    }

    public void moveOnStandardPath(IMovable movable, int steps,
                                   int expectedDestination) {
        Cell source = requireStandardCell(movable);
        Cell destination = getCellAt(expectedDestination);

        List<Piece> movingPieces = movable.getPieces();
        for (Piece piece : movingPieces) source.removePiece(piece);
        movable.move(steps);

        if (movable.getPosition() != expectedDestination) {
            throw new IllegalStateException("Movement and destination do not match.");
        }
        for (Piece piece : movingPieces) destination.addPiece(piece);
    }

    public void teleportToStandardPath(Piece piece, int destinationPosition) {
        removeFromCurrentCell(piece);
        Cell destination = getCellAt(destinationPosition);
        piece.moveToPosition(destinationPosition);
        destination.addPiece(piece);
    }

    public void moveToHomeStraight(Piece piece, int index) {
        removeFromCurrentCell(piece);
        HomeStraightCell destination = getHomeStraightCell(piece.getColor(), index);
        piece.moveToHomeStraight(index);
        destination.addPiece(piece);
    }

    public void moveToHome(Piece piece) {
        removeFromCurrentCell(piece);
        piece.moveToHome();
        getHomeCell(piece.getColor()).addPiece(piece);
    }

    public void sendToBase(ICapturable capturable) {
        List<Piece> capturedPieces = capturable.getPieces();
        for (Piece piece : capturedPieces) removeFromCurrentCell(piece);
        capturable.capture();
        for (Piece piece : capturedPieces) {
            getBaseCell(piece.getColor()).addPiece(piece);
        }
    }

    public void moveBlock(Block block, int diceValue) {
        if (block == null || block.isDissolved()) return;

        int destinationPosition = block.getPosition();
        int movement = diceValue / block.getSize();
        if ("CLOCKWISE".equals(block.getDirection())) {
            destinationPosition = Math.floorMod(
                    destinationPosition + movement, config.getStandardCellCount());
        } else {
            destinationPosition = Math.floorMod(
                    destinationPosition - movement, config.getStandardCellCount());
        }
        moveOnStandardPath(block, diceValue, destinationPosition);
        Cell destination = getCellAt(destinationPosition);
        block.setCell(destination);
    }

    public Cell getCurrentCell(Piece piece) {
        if (piece.isInBase()) return getBaseCell(piece.getColor());
        if (piece.isAtHome()) return getHomeCell(piece.getColor());
        if (piece.isInHomeStraight()) {
            return getHomeStraightCell(piece.getColor(), piece.getHomeStraightIndex());
        }
        if (piece.getPosition() >= 0
                && piece.getPosition() < config.getStandardCellCount()) {
            return getCellAt(piece.getPosition());
        }
        return null;
    }

    public void removeFromCurrentCell(Piece piece) {
        Cell current = getCurrentCell(piece);
        if (current != null) {
            current.removePiece(piece);
        }
    }

    private Cell requireStandardCell(IMovable movable) {
        if (movable.getPosition() < 0
                || movable.getPosition() >= config.getStandardCellCount()) {
            throw new IllegalStateException("Piece is not on the standard path.");
        }
        return getCellAt(movable.getPosition());
    }
}
