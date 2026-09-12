import engine.GameEngine;
import event.GameLogger;
import factory.GameFactory;

public class Main {
    public static void main(String[] args) {
        GameEngine engine = GameFactory.createGame(42L);
        engine.addEventListener(new GameLogger());
        engine.startGame();
    }
}
