package player.strategy;

import board.Board;
import config.GameConfig;
import mystery.MysteryManager;
import piece.Piece;
import rules.RuleEngine;
import util.CyclicIterator;

import java.util.ArrayList;
import java.util.List;

public class RandomStrategy implements IPlayerStrategy {
    private final CyclicIterator<Piece> pieceIterator;
    private final MysteryManager mysteryManager;

    public RandomStrategy(List<Piece> pieces, MysteryManager mysteryManager) {
        this.pieceIterator = new CyclicIterator<>(new ArrayList<>(pieces));
        this.mysteryManager = mysteryManager;
    }

    @Override
    public Piece choosePieceToMove(List<Piece> validMoves, int diceValue,
                                   Board board, RuleEngine ruleEngine) {
        int attempts = pieceIterator.size();

        for (int i = 0; i < attempts; i++) {
            Piece current = pieceIterator.current();

            if (validMoves.contains(current)) {
                pieceIterator.next();

                // Apply mystery preference only for pieces on standard path
                if (!current.isInBase() && !current.isAtHome()
                        && !current.isInHomeStraight()) {
                    return applyMysteryPreference(current, validMoves,
                            diceValue, ruleEngine);
                }
                return current;
            }

            pieceIterator.next();
        }

        return validMoves.getFirst();
    }

    // CW will avoid mystery. CCW seeks mystery.
    private Piece applyMysteryPreference(Piece current, List<Piece> validMoves,
                                         int diceValue, RuleEngine ruleEngine) {
        if (!mysteryManager.isActive()) return current;

        int destination = ruleEngine.calculateDestination(current, diceValue);
        boolean landsMystery = mysteryManager.isOnMysteryCell(destination);

        if ("COUNTERCLOCKWISE".equals(current.getDirection())) {
            // CCW seeks mystery
            if (landsMystery) return current;

            for (Piece piece : validMoves) {
                if (piece.isInBase() || piece.isAtHome()
                        || piece.isInHomeStraight()) continue;
                int dest = ruleEngine.calculateDestination(piece, diceValue);
                if (mysteryManager.isOnMysteryCell(dest)) return piece;
            }

            return current;

        } else {
            // CW avoids mystery — swap only if landing on mystery
            if (landsMystery && validMoves.size() > 1) {
                for (Piece piece : validMoves) {
                    if (piece == current) continue;
                    if (piece.isInBase() || piece.isAtHome()
                            || piece.isInHomeStraight()) continue;
                    int dest = ruleEngine.calculateDestination(piece, diceValue);
                    if (!mysteryManager.isOnMysteryCell(dest)) return piece;
                }
            }
            return current;
        }
    }

    @Override
    public boolean shouldMoveFromBase(List<Piece> pieces, int diceValue,
                                      Board board, RuleEngine ruleEngine) {
        if (diceValue != GameConfig.getInstance().getDiceSides()) return false;

        int attempts = pieceIterator.size();
        for (int i = 0; i < attempts; i++) {
            Piece current = pieceIterator.current();
            if (current.isAtHome() || !current.canMove()) {
                pieceIterator.next();
                continue;
            }
            return current.isInBase();
        }
        return false;
    }
}
