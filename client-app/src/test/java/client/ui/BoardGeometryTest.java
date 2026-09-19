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
        assertNotEquals(BoardGeometry.standardCell(0), BoardGeometry.standardCell(51));
        assertNotEquals(
                BoardGeometry.base("RED", 0), BoardGeometry.base("RED", 3));
        assertNotEquals(
                BoardGeometry.homeStraight("BLUE", 0),
                BoardGeometry.homeStraight("BLUE", 5));
        assertNotEquals(BoardGeometry.home("RED"), BoardGeometry.home("GREEN"));
        assertEquals(BoardGeometry.standardCell(8), BoardGeometry.pointFor("Yellow", "8", 0));
        assertEquals(
                BoardGeometry.homeStraight("Yellow", 2),
                BoardGeometry.pointFor("Yellow", "yellowhomepath2", 0));
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
