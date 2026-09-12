package player.strategy;

import board.Board;
import piece.Piece;
import rules.*;

import java.util.List;

/**
 * Prioritizes winning. Empty the base ASAP. only capture needed amount. moves the piece closest to home
 */
public class WinStrategy implements IPlayerStrategy {
    private final CaptureHandler captureHandler;
    private final BlockHandler blockHandler;

    public WinStrategy(CaptureHandler captureHandler, BlockHandler blockHandler) {
        this.captureHandler = captureHandler;
        this.blockHandler = blockHandler;
    }

    @Override
    public Piece choosePieceToMove(List<Piece> validMoves, int diceValue,
                                   Board board, RuleEngine ruleEngine) {
        // piece needing capture that can capture right now
        Piece needsCapturePiece = findCaptureForNeedyPiece(validMoves, diceValue, ruleEngine);
        if (needsCapturePiece != null) return needsCapturePiece;

        // piece closest to home
        Piece closest = getClosestToApproach(validMoves);
        if (closest != null) return closest;

        return validMoves.getFirst();
    }

    // pieces with no captures
    private Piece findCaptureForNeedyPiece(List<Piece> validMoves, int diceValue,
                                           RuleEngine ruleEngine) {
        for (Piece piece : validMoves) {
            if (piece.isInBase() || piece.isAtHome() || piece.isInHomeStraight()) continue;
            if (piece.getCaptureCount() > 0) continue;
            int destination = ruleEngine.calculateDestination(piece, diceValue);
            Piece target = captureHandler.getCapturedPieceAt(destination, piece.getColor());

            if (target != null) return piece;
        }
        return null;
    }

    // pieces closest to approach
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
    public boolean shouldMoveFromBase(List<Piece> pieces, int diceValue, Board board, RuleEngine ruleEngine) {
        return true;
    }
}
