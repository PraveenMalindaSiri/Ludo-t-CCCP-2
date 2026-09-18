package player.strategy;

import board.Board;
import java.util.List;
import piece.Piece;
import rules.RuleEngine;

public interface IPlayerStrategy {
    /**
     * Chooses which piece to move from the list of valid movable pieces. Returns null only when no
     * valid move exists.
     */
    Piece choosePieceToMove(
            List<Piece> validPieces, int diceValue, Board board, RuleEngine ruleEngine);

    boolean shouldMoveFromBase(
            List<Piece> pieces, int diceValue, Board board, RuleEngine ruleEngine);
}
