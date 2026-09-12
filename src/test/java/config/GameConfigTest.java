package config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameConfigTest {

    @Test
    void sharedAssignmentConstantsHaveExpectedValues() {
        GameConfig config = GameConfig.getInstance();

        assertEquals(52, config.getStandardCellCount());
        assertEquals(6, config.getDiceSides());
        assertEquals(4, config.getPiecesPerPlayer());
        assertEquals(5, config.getHomePathLength());
        assertEquals(8, config.getAlphaCell());
        assertEquals(26, config.getBetaCell());
        assertEquals(45, config.getGammaCell());
        assertEquals(4, config.getMysteryCellDuration());
        assertEquals(3, config.getMaxConsecutiveSixes());
    }

    @Test
    void singletonReturnsSameInstance() {
        assertSame(GameConfig.getInstance(), GameConfig.getInstance());
    }
}
