package player;

import board.Board;
import java.util.List;
import piece.Piece;
import player.strategy.IPlayerStrategy;
import rules.RuleEngine;

public class YellowPlayer extends Player {

    public YellowPlayer(List<Piece> pieces, IPlayerStrategy strategy) {
        super("YELLOW", "Yellow", pieces, strategy);
    }

    @Override
    protected Piece choosePieceToMove(
            List<Piece> validMoves, int diceValue, Board board, RuleEngine ruleEngine) {
        return strategy.choosePieceToMove(validMoves, diceValue, board, ruleEngine);
    }
}
