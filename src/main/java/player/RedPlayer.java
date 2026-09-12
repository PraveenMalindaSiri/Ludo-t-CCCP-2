package player;

import board.Board;
import piece.Piece;
import player.strategy.AggressiveStrategy;
import rules.RuleEngine;

import java.util.List;

public class RedPlayer extends Player {

    public RedPlayer(List<Piece> pieces, AggressiveStrategy strategy) {
        super("RED", "Red", pieces, strategy);
    }

    @Override
    protected Piece choosePieceToMove(List<Piece> validMoves, int diceValue,
                                      Board board, RuleEngine ruleEngine) {
        return strategy.choosePieceToMove(validMoves, diceValue, board, ruleEngine);
    }
}
