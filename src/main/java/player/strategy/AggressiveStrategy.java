package player.strategy;

import board.Board;
import config.GameConfig;
import piece.Piece;
import rules.*;

import java.util.List;


/**
 * Prioritizes capturing opponents
 * Captures opponent closest to its own home, Keep one piece.
 * Only get new pieces if one can capture any with 6, avoid block
 */
public class AggressiveStrategy implements IPlayerStrategy {
    private final CaptureHandler captureHandler;
    private final BlockHandler blockHandler;

    public AggressiveStrategy(CaptureHandler captureHandler, BlockHandler blockHandler) {
        this.captureHandler = captureHandler;
        this.blockHandler = blockHandler;
    }

    @Override
    public Piece choosePieceToMove(List<Piece> validMoves, int diceValue,
                                   Board board, RuleEngine ruleEngine) {
        // capture opponent closest to its home
        Piece capturingPiece = findBestCapture(validMoves, diceValue, board, ruleEngine);
        if (capturingPiece != null) return capturingPiece;

        for (Piece piece : validMoves) {
            if (piece.isInBase() || piece.isAtHome() || piece.isInHomeStraight()) continue;
            int destination = ruleEngine.calculateDestination(piece, diceValue);
            if (!ruleEngine.isSameColorAtDestination(piece, destination)) {
                return piece;
            }
        }

        // move the single piece already on the board
        for (Piece piece : validMoves) {
            if (piece.isOnBoard() && !piece.isInBase()) return piece;
        }

        return validMoves.getFirst();
    }

    // find the piece that can capture opponent close to their home
    private Piece findBestCapture(List<Piece> validMoves, int diceValue,
                                  Board board, RuleEngine ruleEngine) {
        Piece bestPiece = null;
        int minDistance = Integer.MAX_VALUE;

        for (Piece piece : validMoves) {
            if (piece.isInBase() || piece.isAtHome() || piece.isInHomeStraight()) continue;

            int destination = ruleEngine.calculateDestination(piece, diceValue);
            Piece target = captureHandler.getCapturedPieceAt(
                    destination, piece.getColor());

            if (target != null) {
                int distance = blockHandler.distanceToHomeEntry(target);
                if (distance < minDistance) {
                    minDistance = distance;
                    bestPiece = piece;
                }
            }
        }
        return bestPiece;
    }

    @Override
    public boolean shouldMoveFromBase(List<Piece> pieces, int diceValue,
                                      Board board, RuleEngine ruleEngine) {
        boolean hasPieceOnBoard = false;
        for (Piece p : pieces) {
            if (p.isOnBoard() && !p.isInBase()
                    && !p.isAtHome() && !p.isInHomeStraight()) {
                hasPieceOnBoard = true;
                break;
            }
        }
        if (!hasPieceOnBoard) return true;

        for (Piece piece : pieces) {
            if (!piece.isOnBoard() || piece.isInBase()
                    || piece.isAtHome() || piece.isInHomeStraight()) continue;

            int destination = ruleEngine.calculateDestination(piece, diceValue);
            Piece target = captureHandler.getCapturedPieceAt(
                    destination, piece.getColor());
            if (target != null) return false;
        }
        return true;
    }
}
