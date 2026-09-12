package rules;

import block.Block;
import board.Board;
import board.Cell;
import config.GameConfig;
import piece.Piece;
import player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates moves and calculates destinations.
 */
public class RuleEngine {
    private final Board board;
    private final GameConfig config;
    private final BlockHandler blockHandler;
    private final CaptureHandler captureHandler;

    public RuleEngine(Board board,
                      BlockHandler blockHandler,
                      CaptureHandler captureHandler) {
        this.board = board;
        this.blockHandler = blockHandler;
        this.captureHandler = captureHandler;
        this.config = GameConfig.getInstance();
    }

    public boolean canMoveFromBase(int diceValue) {
        return diceValue == config.getDiceSides();
    }

    public boolean isThirdConsecutiveSix(int consecutiveSixCount) {
        return consecutiveSixCount >= config.getMaxConsecutiveSixes();
    }

    // Home straight rules ------------------------------------------------------------------------------------
    // Need at least one capture
    public boolean canEnterHomeStraight(Piece piece) {
        return piece.getCaptureCount() >= 1;
    }

    // CCW has to pass approach 2 times
    public boolean canEnterHomeStraightCCW(Piece piece) {
        return piece.getHasPassedApproachOnce();
    }

    // In home straight, need exact roll to go home
    public boolean needsExactRoll(Piece piece, int diceValue) {
        if (!piece.isInHomeStraight()) return false;
        int effective = piece.getEffectiveMovement(diceValue);
        int stepsToHome = config.getHomePathLength() - piece.getHomeStraightIndex();
        return effective == stepsToHome;
    }

    // Rolled more than need in home straight
    public boolean overshotsHome(Piece piece, int diceValue) {
        if (!piece.isInHomeStraight()) return false;
        int effective = piece.getEffectiveMovement(diceValue); // energized=x2, sick=x/2
        int stepsToHome = config.getHomePathLength() - piece.getHomeStraightIndex();
        return effective > stepsToHome;
    }

    // Check if a standard-path piece's roll would overshoot home
    public boolean overshootsHomeFromStandardPath(Piece piece, int diceValue) {
        if (piece.isInBase() || piece.isAtHome() || piece.isInHomeStraight()) return false;
        if (!canPassApproach(piece, diceValue)) return false;
        if (!canEnterHomeStraight(piece)) return false;

        if ("COUNTERCLOCKWISE".equals(piece.getDirection())
                && !canEnterHomeStraightCCW(piece)) {
            return false;
        }

        int effective = piece.getEffectiveMovement(diceValue);
        int stepsOverApproach = calculateStepsOverApproach(piece, effective);
        int maxSteps = config.getHomePathLength() + 1;
        return stepsOverApproach > maxSteps;
    }

    public int calculateHomeStraightDestination(Piece piece, int diceValue) {
        int effective = piece.getEffectiveMovement(diceValue);
        return piece.getHomeStraightIndex() + effective;
    }

    public int calculateDestination(Piece piece, int diceValue) {
        int effective = piece.getEffectiveMovement(diceValue);
        if ("CLOCKWISE".equals(piece.getDirection())) {
            return (piece.getPosition() + effective) % config.getStandardCellCount();
        } else {
            return (piece.getPosition() - effective + config.getStandardCellCount())
                    % config.getStandardCellCount();
        }
    }

    // Check approach cell ------------------------------------------------------------------------------------

    public boolean canPassApproach(Piece piece, int diceValue) {
        if (piece.isInHomeStraight() || piece.isInBase() || piece.isAtHome()) {
            return false;
        }

        int effective = piece.getEffectiveMovement(diceValue);
        int current = piece.getPosition();
        int cellCount = config.getStandardCellCount();

        if (board.isApproachCell(current, piece.getColor())) {
            return true;
        }

        for (int step = 1; step <= effective; step++) {
            int checkPos = "CLOCKWISE".equals(piece.getDirection())
                    ? (current + step) % cellCount
                    : (current - step + cellCount) % cellCount;

            if (board.isApproachCell(checkPos, piece.getColor())) {
                return true;
            }
        }

        return false;
    }

    // Validate moving forward ------------------------------------------------------------------------------------

    public boolean isValidMove(Piece piece, int diceValue) {
        if (piece.isAtHome()) return false;
        if (!piece.canMove()) return false;
        if (piece.isInBase()) return canMoveFromBase(diceValue);
        if (piece.getEffectiveMovement(diceValue) <= 0) return false; // sick pieces dice value 1 is 0
        if (piece.isInHomeStraight()) return !overshotsHome(piece, diceValue);
        return true;
    }

    public List<Piece> getValidMoves(Player player, int diceValue) {
        List<Piece> valid = new ArrayList<>();

        for (Piece piece : player.getPieces()) {
            if (!isValidMove(piece, diceValue)) continue;

            if (piece.isInBase()) {
                valid.add(piece);
                continue;
            }

            if (piece.isInHomeStraight()) {
                valid.add(piece);
                continue;
            }

            if (piece.isInBlock()) {
                Block block = blockHandler.findBlockAt(
                        board.getCellAt(piece.getPosition()));

                if (block == null || !blockHandler.canBeInBlock(piece)) {
                    if (block != null) {
                        blockHandler.breakBlock(piece, block);
                    } else {
                        blockHandler.removeFromBlockIfNeeded(piece);
                    }
                    continue;
                }

                if (!blockHandler.canBlockMove(block, diceValue)) {
                    continue;
                }
            }

            if (overshootsHomeFromStandardPath(piece, diceValue)) continue;

            if (!piece.isInBlock()) {
                int destination = calculateDestination(piece, diceValue);
                if (wouldCreateInvalidSameColorStack(piece, destination)) {
                    continue;
                }
            }

            valid.add(piece);
        }

        return valid;
    }

    // Same-color check ------------------------------------------------------------------------------------

    public boolean isSameColorAtDestination(Piece piece, int destination) {
        Cell cell = board.getCellAt(destination);
        if (!cell.hasPieces()) return false;
        return cell.getPieces().stream()
                .anyMatch(p -> p.getColor().equalsIgnoreCase(piece.getColor()));
    }

    // if one same-color piece, form a block
    public boolean canFormBlock(Piece piece, int destination) {
        Cell cell = board.getCellAt(destination);
        if (!cell.hasPieces()) return false;
        if (!blockHandler.canBeInBlock(piece)) return false;

        long blockableSameColor = cell.getPieces().stream()
                .filter(p -> p.getColor().equalsIgnoreCase(piece.getColor()))
                .filter(blockHandler::canBeInBlock)
                .count();

        return blockableSameColor == 1;
    }

    // Helpers

    private boolean wouldCreateInvalidSameColorStack(Piece movingPiece, int destination) {
        Cell destinationCell = board.getCellAt(destination);
        if (!destinationCell.hasPieces()) return false;

        boolean hasSameColorPiece = false;

        for (Piece pieceOnCell : destinationCell.getPieces()) {
            if (pieceOnCell.getColor().equalsIgnoreCase(movingPiece.getColor())) {
                hasSameColorPiece = true;

                if (!blockHandler.canBeInBlock(pieceOnCell)) {
                    return true;
                }
            }
        }

        return hasSameColorPiece && !blockHandler.canBeInBlock(movingPiece);
    }


    private int calculateStepsOverApproach(Piece piece, int effective) {
        return effective - blockHandler.distanceFromApproach(piece);
    }
}
