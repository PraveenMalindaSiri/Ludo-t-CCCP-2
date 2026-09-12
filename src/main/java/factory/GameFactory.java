package factory;

import board.Board;
import dice.CoinToss;
import dice.Dice;
import engine.GameEngine;
import mystery.MysteryManager;
import mystery.effect.AlphaEffect;
import mystery.effect.ApproachEffect;
import mystery.effect.BaseEffect;
import mystery.effect.BetaEffect;
import mystery.effect.GammaEffect;
import mystery.effect.IMysteryEffect;
import mystery.effect.StartEffect;
import player.Player;
import rules.BlockHandler;
import rules.CaptureHandler;
import rules.RuleEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Creates one complete, isolated game-session object graph. */
public final class GameFactory {
    private GameFactory() { }

    public static GameEngine createGame(long randomSeed) {
        Random random = new Random(randomSeed);
        Board board = BoardFactory.createBoard();
        CaptureHandler captureHandler = new CaptureHandler(board);
        BlockHandler blockHandler = new BlockHandler(board);
        RuleEngine ruleEngine = new RuleEngine(board, blockHandler);

        BetaEffect betaEffect = new BetaEffect();
        List<IMysteryEffect> effects = new ArrayList<>();
        effects.add(new AlphaEffect(random));
        effects.add(betaEffect);
        effects.add(new GammaEffect(betaEffect));
        effects.add(new BaseEffect());
        effects.add(new StartEffect());
        effects.add(new ApproachEffect());
        MysteryManager mysteryManager =
                new MysteryManager(board, random, effects);

        List<Player> players = new ArrayList<>();
        players.add(PlayerFactory.createPlayer(
                "YELLOW", captureHandler, blockHandler, mysteryManager));
        players.add(PlayerFactory.createPlayer(
                "BLUE", captureHandler, blockHandler, mysteryManager));
        players.add(PlayerFactory.createPlayer(
                "RED", captureHandler, blockHandler, mysteryManager));
        players.add(PlayerFactory.createPlayer(
                "GREEN", captureHandler, blockHandler, mysteryManager));

        return new GameEngine(board, players, ruleEngine,
                captureHandler, blockHandler, mysteryManager,
                new Dice(random), new CoinToss(random));
    }
}
