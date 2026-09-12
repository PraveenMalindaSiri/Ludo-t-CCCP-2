package block;

import board.Cell;
import config.GameConfig;
import piece.Piece;

import java.util.ArrayList;
import java.util.List;

/**
 * Same color pieces sharing one cell.
 */
public class Block implements IMovable {

    private final List<Piece> pieces;
    private Cell cell;

    public Block(Cell cell) {
        this.pieces = new ArrayList<>();
        this.cell = cell;
    }

    @Override
    public void move(int steps) {
        int movementPerPiece = pieces.isEmpty() ? 0 : steps / pieces.size();
        String blockDirection = getDirection();

        for (Piece piece : pieces) {
            piece.setDirection(blockDirection);
            piece.move(movementPerPiece);
        }
    }

    @Override
    public int getPosition() {
        return pieces.isEmpty() ? -1 : pieces.getFirst().getPosition();
    }

    // get direction of the piece farthest from home.
    @Override
    public String getDirection() {
        if (pieces.isEmpty()) return "CLOCKWISE";

        Piece farthestFromHome = null;
        int longestDistance = -1;

        for (Piece piece : pieces) {
            int distance = distanceToHomeEntry(piece);

            if (distance > longestDistance) {
                longestDistance = distance;
                farthestFromHome = piece;
            }
        }

        return farthestFromHome != null
                ? farthestFromHome.getDirection()
                : "CLOCKWISE";
    }

    private int getApproachPosition(String color) {
        GameConfig config = GameConfig.getInstance();

        if (color == null) return 0;

        return switch (color.toUpperCase()) {
            case "YELLOW" -> config.getYellowApproach();
            case "BLUE" -> config.getBlueApproach();
            case "RED" -> config.getRedApproach();
            case "GREEN" -> config.getGreenApproach();
            default -> 0;
        };
    }

    private int distanceToHomeEntry(Piece piece) {
        if (piece == null) return -1;

        if (piece.isInHomeStraight() || piece.isAtHome()) {
            return 0;
        }

        int current = piece.getPosition();
        int count = GameConfig.getInstance().getStandardCellCount();

        if (current < 0 || current >= count) {
            return -1;
        }

        int approach = getApproachPosition(piece.getColor());

        int distance;
        if ("CLOCKWISE".equals(piece.getDirection())) {
            distance = Math.floorMod(approach - current, count);
        } else {
            distance = Math.floorMod(current - approach, count);
            
            if (!piece.getHasPassedApproachOnce()) {
                distance += count;
            }
        }

        return distance;
    }

    // Piece -------------------------------------------------------------------------------------

    public void addPiece(Piece piece) {
        piece.setInBlock(true);
        pieces.add(piece);
    }

    public void removePiece(Piece piece) {
        pieces.remove(piece);
        piece.setInBlock(false);
        piece.setDirection(piece.getOriginalDirection());
    }

    public List<Piece> getPieces() {
        return new ArrayList<>(pieces);
    }

    public int getSize() {
        return pieces.size();
    }

    public boolean isDissolved() {
        return pieces.size() < 2;
    }

    public void restoreOriginalDirections() {
        for (Piece piece : pieces) {
            piece.setDirection(piece.getOriginalDirection());
        }
    }

    // Cell -------------------------------------------------------------------------------------

    public Cell getCell() {
        return cell;
    }

    public void setCell(Cell cell) {
        this.cell = cell;
    }

    @Override
    public String toString() {
        return "Block" + pieces + "@cell" + (cell != null ? cell.getPosition() : "?");
    }
}
