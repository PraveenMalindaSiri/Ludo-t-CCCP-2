package client.ui;

import java.awt.Point;
import java.util.List;
import java.util.Locale;
import protocol.GameSnapshotDto;

/** Deterministic mapping from protocol position strings to a 15-by-15 board grid. */
public final class BoardGeometry {

    public static final int GRID_SIZE = 15;

    private static final List<Point> STANDARD_PATH =
            List.of(
                    point(6, 13),
                    point(6, 12),
                    point(6, 11),
                    point(6, 10),
                    point(6, 9),
                    point(5, 8),
                    point(4, 8),
                    point(3, 8),
                    point(2, 8),
                    point(1, 8),
                    point(0, 8),
                    point(0, 7),
                    point(0, 6),
                    point(1, 6),
                    point(2, 6),
                    point(3, 6),
                    point(4, 6),
                    point(5, 6),
                    point(6, 5),
                    point(6, 4),
                    point(6, 3),
                    point(6, 2),
                    point(6, 1),
                    point(6, 0),
                    point(7, 0),
                    point(8, 0),
                    point(8, 1),
                    point(8, 2),
                    point(8, 3),
                    point(8, 4),
                    point(8, 5),
                    point(9, 6),
                    point(10, 6),
                    point(11, 6),
                    point(12, 6),
                    point(13, 6),
                    point(14, 6),
                    point(14, 7),
                    point(14, 8),
                    point(13, 8),
                    point(12, 8),
                    point(11, 8),
                    point(10, 8),
                    point(9, 8),
                    point(8, 9),
                    point(8, 10),
                    point(8, 11),
                    point(8, 12),
                    point(8, 13),
                    point(8, 14),
                    point(7, 14),
                    point(6, 14));

    private BoardGeometry() {}

    public static List<Point> standardPath() {
        return STANDARD_PATH.stream().map(Point::new).toList();
    }

    public static Point pointFor(String color, String position, int pieceIndex) {
        String normalizedColor = normalize(color);
        String normalizedPosition = normalize(position);
        if (normalizedPosition.chars().allMatch(Character::isDigit)
                && !normalizedPosition.isEmpty()) {
            return standardCell(Integer.parseInt(normalizedPosition));
        }
        if (normalizedPosition.startsWith("CELL_")) {
            return standardCell(parseIndex(normalizedPosition, "CELL_"));
        }
        if (normalizedPosition.startsWith("HOME_STRAIGHT_")) {
            return homeStraight(normalizedColor, parseIndex(normalizedPosition, "HOME_STRAIGHT_"));
        }
        String coreHomePathPrefix = normalizedColor + "HOMEPATH";
        if (normalizedPosition.startsWith(coreHomePathPrefix)) {
            return homeStraight(
                    normalizedColor, parseIndex(normalizedPosition, coreHomePathPrefix));
        }
        if (normalizedPosition.equals("HOME")) {
            return home(normalizedColor);
        }
        return base(normalizedColor, pieceIndex);
    }

    public static Point pointFor(GameSnapshotDto.PieceDto piece, int pieceIndex) {
        return switch (piece.area()) {
            case BASE -> base(piece.color(), pieceIndex);
            case STANDARD_PATH -> standardCell(piece.standardPosition());
            case HOME_STRAIGHT -> homeStraight(piece.color(), piece.homeStraightIndex());
            case HOME -> home(piece.color());
        };
    }

    public static Point standardCell(int index) {
        if (index < 0 || index >= STANDARD_PATH.size()) {
            throw new IllegalArgumentException("Standard cell index must be between 0 and 51");
        }
        return new Point(STANDARD_PATH.get(index));
    }

    public static Point base(String color, int pieceIndex) {
        int slot = Math.floorMod(pieceIndex, 4);
        int xOffset = slot % 2 * 2;
        int yOffset = slot / 2 * 2;
        return switch (normalize(color)) {
            case "RED" -> point(2 + xOffset, 2 + yOffset);
            case "GREEN" -> point(10 + xOffset, 2 + yOffset);
            case "YELLOW" -> point(10 + xOffset, 10 + yOffset);
            case "BLUE" -> point(2 + xOffset, 10 + yOffset);
            default -> point(2 + xOffset, 2 + yOffset);
        };
    }

    public static Point homeStraight(String color, int index) {
        int step = Math.max(0, Math.min(4, index));
        return switch (normalize(color)) {
            case "RED" -> point(7, 1 + step);
            case "GREEN" -> point(13 - step, 7);
            case "YELLOW" -> point(7, 13 - step);
            case "BLUE" -> point(1 + step, 7);
            default -> point(7, 1 + step);
        };
    }

    public static Point home(String color) {
        return switch (normalize(color)) {
            case "RED" -> point(7, 6);
            case "GREEN" -> point(8, 7);
            case "YELLOW" -> point(7, 8);
            case "BLUE" -> point(6, 7);
            default -> point(7, 7);
        };
    }

    private static int parseIndex(String value, String prefix) {
        try {
            return Integer.parseInt(value.substring(prefix.length()));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid board position " + value, exception);
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static Point point(int x, int y) {
        return new Point(x, y);
    }
}
