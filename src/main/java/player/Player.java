package player;

import board.Board;
import piece.Piece;
import player.strategy.IPlayerStrategy;
import rules.RuleEngine;

import java.util.ArrayList;
import java.util.List;

public abstract class Player {
    protected final String color;
    protected final String name;
    protected final List<Piece> pieces;
    protected final IPlayerStrategy strategy;
    protected int consecutiveSixes;

    protected Player(String color, String name,
                     List<Piece> pieces, IPlayerStrategy strategy) {
        this.color = color;
        this.name = name;
        this.pieces = new ArrayList<>(pieces);
        this.strategy = strategy;
        this.consecutiveSixes = 0;
    }

    public final Piece selectMove(List<Piece> validMoves, int diceValue,
                                  Board board, RuleEngine ruleEngine) {
        if (validMoves.isEmpty()) return null;
        return choosePieceToMove(validMoves, diceValue, board, ruleEngine);
    }

    // children will implement this accordingly
    protected abstract Piece choosePieceToMove(
            List<Piece> pieces, int diceValue, Board board, RuleEngine ruleEngine);

    // each child has different ways to exit the base
    public boolean shouldMoveFromBase(int diceValue, Board board, RuleEngine ruleEngine) {
        return strategy.shouldMoveFromBase(
                new ArrayList<>(pieces), diceValue, board, ruleEngine);
    }

    // Queries ------------------------------------------------------------------------------------------

    public List<Piece> getPieces() {
        return new ArrayList<>(pieces);
    }

    public List<Piece> getPiecesOnBoard() {
        List<Piece> result = new ArrayList<>();
        for (Piece p : pieces) {
            if (p.isOnBoard()) result.add(p);
        }
        return result;
    }

    public List<Piece> getPiecesInBase() {
        List<Piece> result = new ArrayList<>();
        for (Piece p : pieces) {
            if (p.isInBase()) result.add(p);
        }
        return result;
    }

    public List<Piece> getPiecesAtHome() {
        List<Piece> result = new ArrayList<>();
        for (Piece p : pieces) {
            if (p.isAtHome()) result.add(p);
        }
        return result;
    }

    public boolean hasWon() {
        for (Piece p : pieces) {
            if (!p.isAtHome()) return false;
        }
        return true;
    }

    public void incrementConsecutiveSixes() {
        consecutiveSixes++;
    }

    public void resetConsecutiveSixes() {
        consecutiveSixes = 0;
    }

    public int getConsecutiveSixes() {
        return consecutiveSixes;
    }

    public String getColor() {
        return color;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name + " Player";
    }
}
