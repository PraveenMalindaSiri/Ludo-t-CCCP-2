package player;

import board.Board;
import piece.Piece;
import player.strategy.WinStrategy;
import rules.RuleEngine;

import java.util.List;

public class YellowPlayer extends Player {

    public YellowPlayer(List<Piece> pieces, WinStrategy strategy) {
        super("YELLOW", "Yellow", pieces, strategy);
    }

    @Override
    protected Piece choosePieceToMove(List<Piece> validMoves, int diceValue,
                                      Board board, RuleEngine ruleEngine) {
        return strategy.choosePieceToMove(validMoves, diceValue, board, ruleEngine);
    }
}
