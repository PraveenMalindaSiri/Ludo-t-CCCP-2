package dice;

import java.util.Random;

public class CoinToss implements ICoinToss {
    private final Random random;

    public CoinToss(Random random) {
        this.random = random;
    }

    @Override
    public String toss() {
        return random.nextBoolean() ? "HEADS" : "TAILS";
    }
}
