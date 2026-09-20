package config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GameConfigTest {

    private final GameConfig config = GameConfig.getInstance();

    @Test
    void usesAssignmentOneGlobalStartingAndApproachPositions() {
        assertEquals(0, config.getYellowStart());
        assertEquals(13, config.getBlueStart());
        assertEquals(26, config.getRedStart());
        assertEquals(39, config.getGreenStart());

        assertEquals(50, config.getYellowApproach());
        assertEquals(11, config.getBlueApproach());
        assertEquals(24, config.getRedApproach());
        assertEquals(37, config.getGreenApproach());
    }

    @Test
    void convertsRuleT11LocalOffsetsToGlobalPathPositions() {
        assertEquals(6, config.getAlphaCell());
        assertEquals(24, config.getBetaCell());
        assertEquals(43, config.getGammaCell());
    }
}
