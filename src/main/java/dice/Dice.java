package dice;

import config.GameConfig;

import java.util.Random;

public class Dice implements IDice {
    private final Random random;
    private final int sides;

    public Dice(Random random) {
        this.random = random;
        this.sides = GameConfig.getInstance().getDiceSides();
    }

    @Override
    public int roll() {
        return random.nextInt(sides) + 1;
    }
}
