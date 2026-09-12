package player;

import board.Board;
import piece.Piece;
import player.strategy.BlockStrategy;
import rules.RuleEngine;

import java.util.List;

public class GreenPlayer extends Player {

    public GreenPlayer(List<Piece> pieces, BlockStrategy strategy) {
        super("GREEN", "Green", pieces, strategy);
    }

    @Override
    protected Piece choosePieceToMove(List<Piece> validMoves, int diceValue,
                                      Board board, RuleEngine ruleEngine) {
        return strategy.choosePieceToMove(validMoves, diceValue, board, ruleEngine);
    }
}
