package factory;

import config.GameConfig;
import mystery.MysteryManager;
import piece.Piece;
import player.*;
import player.strategy.*;
import rules.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates completed player with pieces and strategies.
 */
public class PlayerFactory {
    private PlayerFactory() {
    }

    public static Player createPlayer(String color,
                                      CaptureHandler captureHandler,
                                      BlockHandler blockHandler,
                                      MysteryManager mysteryManager) {
        List<Piece> pieces = createPieces(color);

        switch (color.toUpperCase()) {
            case "RED":
                return new RedPlayer(
                        pieces,
                        new AggressiveStrategy(captureHandler, blockHandler));
            case "GREEN":
                return new GreenPlayer(
                        pieces,
                        new BlockStrategy(blockHandler));
            case "YELLOW":
                return new YellowPlayer(
                        pieces,
                        new WinStrategy(captureHandler, blockHandler));
            case "BLUE":
                return new BluePlayer(
                        pieces,
                        new RandomStrategy(pieces, mysteryManager));
            default:
                throw new IllegalArgumentException(
                        "Unknown player color: " + color);
        }
    }

    // Creates 4 pieces for the given color.
    public static List<Piece> createPieces(String color) {
        List<Piece> pieces = new ArrayList<>();
        int count = GameConfig.getInstance().getPiecesPerPlayer();
        for (int i = 1; i <= count; i++) {
            pieces.add(new Piece(String.valueOf(i), color.toUpperCase()));
        }
        return pieces;
    }
}
