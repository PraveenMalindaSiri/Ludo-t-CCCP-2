import board.Board;
import dice.*;
import engine.GameEngine;
import event.GameLogger;
import factory.*;
import mystery.MysteryManager;
import player.Player;
import rules.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Main {

    public static void main(String[] args) {

        Random random = new Random(42);

        // build the board, dice and coin toss
        Board board = BoardFactory.createBoard();
        Dice dice = new Dice(random);
        CoinToss coinToss = new CoinToss(random);

        // build handlers
        CaptureHandler captureHandler = new CaptureHandler(board);
        BlockHandler blockHandler = new BlockHandler(board);
        RuleEngine ruleEngine = new RuleEngine(board, blockHandler, captureHandler);

        // build mystery manager
        MysteryManager mysteryManager = new MysteryManager(board, random);

        // build players
        List<Player> players = new ArrayList<>();
        players.add(PlayerFactory.createPlayer("YELLOW", captureHandler,
                blockHandler, mysteryManager));
        players.add(PlayerFactory.createPlayer("BLUE", captureHandler,
                blockHandler, mysteryManager));
        players.add(PlayerFactory.createPlayer("RED", captureHandler,
                blockHandler, mysteryManager));
        players.add(PlayerFactory.createPlayer("GREEN", captureHandler,
                blockHandler, mysteryManager));


        // build engine
        GameEngine engine = new GameEngine(
                board, players, ruleEngine,
                captureHandler, blockHandler, mysteryManager,
                dice, coinToss
        );

        // register logger
        GameLogger logger = new GameLogger(mysteryManager);
        engine.addEventListener(logger);

        // run game
        engine.startGame();
    }

}