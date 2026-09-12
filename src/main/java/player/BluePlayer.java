package player;

import board.Board;
import mystery.MysteryManager;
import piece.Piece;
import player.strategy.RandomStrategy;
import rules.RuleEngine;

import java.util.List;

public class BluePlayer extends Player {

    public BluePlayer(List<Piece> pieces, RandomStrategy strategy) {
        super("BLUE", "Blue", pieces, strategy);
    }

    @Override
    protected Piece choosePieceToMove(List<Piece> validMoves, int diceValue,
                                      Board board, RuleEngine ruleEngine) {
        return strategy.choosePieceToMove(validMoves, diceValue, board, ruleEngine);
    }
}
