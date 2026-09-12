package player.strategy;

import board.Board;
import board.Cell;
import config.GameConfig;
import piece.Piece;
import rules.BlockHandler;
import rules.RuleEngine;

import java.util.List;

/**
 * Prioritizes winning through blocking. Always build blocks with 6 before take new pieces.
 * move home straight first, then blocks, otherwise piece closest to approach.
 */
public class BlockStrategy implements IPlayerStrategy {
    private final BlockHandler blockHandler;

    public BlockStrategy(BlockHandler blockHandler) {
        this.blockHandler = blockHandler;
    }

    @Override
    public Piece choosePieceToMove(List<Piece> validMoves, int diceValue,
                                   Board board, RuleEngine ruleEngine) {
        // move piece already in home straight
        Piece homeStraightPiece = getHomeStraightPiece(validMoves);
        if (homeStraightPiece != null) return homeStraightPiece;

        // move a block forward
        Piece blockPiece = getBlockPiece(validMoves);
        if (blockPiece != null) return blockPiece;

        // create a block
        Piece blockFormingPiece = getBlockFormingPiece(validMoves, diceValue, ruleEngine);
        if (blockFormingPiece != null) return blockFormingPiece;

        // move piece closest to approach
        Piece closestToApproach = getClosestToApproach(validMoves);
        if (closestToApproach != null) return closestToApproach;

        return validMoves.getFirst();
    }

    // prioritized for finishing.
    private Piece getHomeStraightPiece(List<Piece> validMoves) {
        Piece best = null;
        int maxIndex = -1;
        for (Piece piece : validMoves) {
            if (piece.isInHomeStraight() && piece.getHomeStraightIndex() > maxIndex) {
                maxIndex = piece.getHomeStraightIndex();
                best = piece;
            }
        }
        return best;
    }

    // block movement
    private Piece getBlockPiece(List<Piece> validMoves) {
        for (Piece piece : validMoves) {
            if (piece.isInBlock() && blockHandler.canBeInBlock(piece)) return piece;
        }
        return null;
    }

    // piece that can form a block
    private Piece getBlockFormingPiece(List<Piece> validMoves, int diceValue,
                                       RuleEngine ruleEngine) {
        for (Piece piece : validMoves) {
            if (piece.isInBase() || piece.isAtHome() || piece.isInHomeStraight()) continue;
            int destination = ruleEngine.calculateDestination(piece, diceValue);
            if (ruleEngine.canFormBlock(piece, destination)) return piece;
        }
        return null;
    }

    // piece closest to the approach cell
    private Piece getClosestToApproach(List<Piece> validMoves) {
        Piece best = null;
        int minDistance = Integer.MAX_VALUE;
        for (Piece piece : validMoves) {
            if (piece.isInBase() || piece.isAtHome()) continue;
            int distance = piece.isInHomeStraight()
                    ? 0
                    : blockHandler.distanceToHomeEntry(piece);
            if (distance < minDistance) {
                minDistance = distance;
                best = piece;
            }
        }
        return best;
    }

    @Override
    public boolean shouldMoveFromBase(List<Piece> pieces, int diceValue,
                                      Board board, RuleEngine ruleEngine) {
        for (Piece piece : pieces) {
            if (!piece.isOnBoard() || piece.isInBase() || piece.isAtHome()) continue;
            if (piece.isInHomeStraight()) continue;

            int destination = ruleEngine.calculateDestination(piece, diceValue);
            Cell destCell = board.getCellAt(destination);

            if (blockHandler.isSameColorBlock(piece, destCell)) return false;

            if (!destCell.getPieces().isEmpty()
                    && destCell.getPieces().size() == 1
                    && destCell.getPieces().getFirst().getColor()
                    .equalsIgnoreCase(piece.getColor())) {
                return false;
            }
        }
        return true;
    }
}
