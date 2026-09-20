package client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Point;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import protocol.GameSnapshotDto;

class BoardGeometryTest {

    @Test
    void mapsCompleteStandardPathBaseHomeStraightAndHome() {
        assertEquals(52, BoardGeometry.standardPath().size());
        assertEquals(52, new HashSet<>(BoardGeometry.standardPath()).size());
        for (int index = 0; index < 52; index++) {
            Point current = BoardGeometry.standardCell(index);
            Point next = BoardGeometry.standardCell((index + 1) % 52);
            int gridDistance = Math.max(Math.abs(current.x - next.x), Math.abs(current.y - next.y));
            assertEquals(1, gridDistance, "Path breaks after standard cell " + index);
        }
        assertNotEquals(BoardGeometry.standardCell(0), BoardGeometry.standardCell(51));
        assertNotEquals(BoardGeometry.base("RED", 0), BoardGeometry.base("RED", 3));
        assertNotEquals(
                BoardGeometry.homeStraight("BLUE", 0), BoardGeometry.homeStraight("BLUE", 5));
        assertNotEquals(BoardGeometry.home("RED"), BoardGeometry.home("GREEN"));
        assertEquals(BoardGeometry.standardCell(8), BoardGeometry.pointFor("Yellow", "8", 0));
        assertEquals(
                BoardGeometry.homeStraight("Yellow", 2),
                BoardGeometry.pointFor("Yellow", "yellowhomepath2", 0));
    }

    @Test
    void mapsEachPlayersColoredStartingCell() {
        assertEquals(BoardGeometry.standardCell(0), BoardGeometry.startingCell("YELLOW"));
        assertEquals(BoardGeometry.standardCell(13), BoardGeometry.startingCell("BLUE"));
        assertEquals(BoardGeometry.standardCell(26), BoardGeometry.startingCell("RED"));
        assertEquals(BoardGeometry.standardCell(39), BoardGeometry.startingCell("GREEN"));
        assertEquals(new Point(8, 1), BoardGeometry.startingCell("YELLOW"));
        assertEquals(new Point(13, 8), BoardGeometry.startingCell("BLUE"));
        assertEquals(new Point(6, 13), BoardGeometry.startingCell("RED"));
        assertEquals(new Point(1, 6), BoardGeometry.startingCell("GREEN"));
    }

    @Test
    void mapsAssignmentOneBaseAndHomeQuadrants() {
        assertEquals(new Point(2, 2), BoardGeometry.base("GREEN", 0));
        assertEquals(new Point(10, 2), BoardGeometry.base("YELLOW", 0));
        assertEquals(new Point(2, 10), BoardGeometry.base("RED", 0));
        assertEquals(new Point(10, 10), BoardGeometry.base("BLUE", 0));

        assertEquals(new Point(7, 1), BoardGeometry.homeStraight("YELLOW", 0));
        assertEquals(new Point(13, 7), BoardGeometry.homeStraight("BLUE", 0));
        assertEquals(new Point(7, 13), BoardGeometry.homeStraight("RED", 0));
        assertEquals(new Point(1, 7), BoardGeometry.homeStraight("GREEN", 0));

        assertEquals(new Point(7, 6), BoardGeometry.home("YELLOW"));
        assertEquals(new Point(8, 7), BoardGeometry.home("BLUE"));
        assertEquals(new Point(7, 8), BoardGeometry.home("RED"));
        assertEquals(new Point(6, 7), BoardGeometry.home("GREEN"));
    }

    @Test
    void mapsAssignmentOneApproachCircles() {
        assertEquals(BoardGeometry.standardCell(50), BoardGeometry.approachCell("YELLOW"));
        assertEquals(BoardGeometry.standardCell(11), BoardGeometry.approachCell("BLUE"));
        assertEquals(BoardGeometry.standardCell(24), BoardGeometry.approachCell("RED"));
        assertEquals(BoardGeometry.standardCell(37), BoardGeometry.approachCell("GREEN"));
        assertEquals(new Point(7, 0), BoardGeometry.approachCell("YELLOW"));
        assertEquals(new Point(14, 7), BoardGeometry.approachCell("BLUE"));
        assertEquals(new Point(7, 14), BoardGeometry.approachCell("RED"));
        assertEquals(new Point(0, 7), BoardGeometry.approachCell("GREEN"));
    }

    @Test
    void mapsFixedMysteryEffectDestinationSymbols() {
        assertEquals(BoardGeometry.standardCell(6), BoardGeometry.mysteryEffectCell("ALPHA"));
        assertEquals(BoardGeometry.standardCell(24), BoardGeometry.mysteryEffectCell("BETA"));
        assertEquals(BoardGeometry.standardCell(43), BoardGeometry.mysteryEffectCell("GAMMA"));
        assertEquals(BoardGeometry.approachCell("RED"), BoardGeometry.mysteryEffectCell("BETA"));
    }

    @Test
    void allSixteenDtoPiecesMapToValidGridCoordinates() {
        Set<Point> positions = new HashSet<>();
        for (String color : List.of("Red", "Green", "Yellow", "Blue")) {
            for (int index = 0; index < 4; index++) {
                GameSnapshotDto.PieceDto piece =
                        new GameSnapshotDto.PieceDto(
                                color.substring(0, 1) + (index + 1),
                                color + " piece " + (index + 1),
                                index == 0 ? "CELL_" + positions.size() : "BASE");
                Point mapped = BoardGeometry.pointFor(color, piece.position(), index);
                assertTrue(mapped.x >= 0 && mapped.x < BoardGeometry.GRID_SIZE);
                assertTrue(mapped.y >= 0 && mapped.y < BoardGeometry.GRID_SIZE);
                positions.add(mapped);
            }
        }
        assertEquals(16, positions.size());
    }
}
