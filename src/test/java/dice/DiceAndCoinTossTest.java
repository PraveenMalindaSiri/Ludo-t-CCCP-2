package dice;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class DiceAndCoinTossTest {

    @Test
    void diceConvertsZeroBasedRandomValueToOneBasedDiceRoll() {
        Dice dice = new Dice(new FixedRandom(0, true));

        assertEquals(1, dice.roll());
    }

    @Test
    void diceNeverReturnsZeroWithHighestRandomValue() {
        Dice dice = new Dice(new FixedRandom(5, true));

        assertEquals(6, dice.roll());
    }

    @Test
    void coinTossMapsBooleanToHeadsOrTails() {
        assertEquals("HEADS", new CoinToss(new FixedRandom(0, true)).toss());
        assertEquals("TAILS", new CoinToss(new FixedRandom(0, false)).toss());
    }

    private static class FixedRandom extends Random {
        private final int nextIntValue;
        private final boolean nextBooleanValue;

        FixedRandom(int nextIntValue, boolean nextBooleanValue) {
            this.nextIntValue = nextIntValue;
            this.nextBooleanValue = nextBooleanValue;
        }

        @Override
        public int nextInt(int bound) {
            return nextIntValue;
        }

        @Override
        public boolean nextBoolean() {
            return nextBooleanValue;
        }
    }
}
