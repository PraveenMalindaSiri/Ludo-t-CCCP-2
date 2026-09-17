package player;

import board.Board;
import java.util.List;
import piece.Piece;
import player.strategy.IPlayerStrategy;
import rules.RuleEngine;

public class RedPlayer extends Player {

    public RedPlayer(List<Piece> pieces, IPlayerStrategy strategy) {
        super("RED", "Red", pieces, strategy);
    }

    @Override
    protected Piece choosePieceToMove(
            List<Piece> validMoves, int diceValue, Board board, RuleEngine ruleEngine) {
        return strategy.choosePieceToMove(validMoves, diceValue, board, ruleEngine);
    }
}
