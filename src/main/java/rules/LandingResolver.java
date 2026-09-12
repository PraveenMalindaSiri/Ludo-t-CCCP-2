package rules;

import block.Block;
import board.Board;
import board.Cell;
import engine.command.CaptureCommand;
import engine.command.CommandResult;
import mystery.MysteryManager;
import mystery.MysteryOutcome;
import piece.Piece;

/**
 * Runs the common post-movement landing sequence exactly once per arrival.
 */
public final class LandingResolver {
    private final Board board;
    private final CaptureHandler captureHandler;
    private final BlockHandler blockHandler;
    private final MysteryManager mysteryManager;

    public LandingResolver(Board board,
                           CaptureHandler captureHandler,
                           BlockHandler blockHandler,
                           MysteryManager mysteryManager) {
        this.board = board;
        this.captureHandler = captureHandler;
        this.blockHandler = blockHandler;
        this.mysteryManager = mysteryManager;
    }

    public void resolve(Piece mover, CommandResult result) {
        resolve(mover, result, true);
    }

    private void resolve(Piece mover, CommandResult result,
                         boolean allowMysteryTrigger) {
        if (!isOnStandardPath(mover)) return;

        resolveOccupants(mover, result);
        if (!isOnStandardPath(mover)) return;

        if (allowMysteryTrigger
                && mysteryManager.isOnMysteryCell(mover.getPosition())) {
            breakBlockBeforeTeleport(mover);
            MysteryOutcome outcome = mysteryManager.handleLanding(mover);
            result.setMysteryOutcome(outcome);

            // A teleport destination is resolved, but it cannot recursively
            // trigger the same mystery landing.
            resolve(mover, result, false);
        }
    }

    private void resolveOccupants(Piece mover, CommandResult result) {
        Cell cell = board.getCellAt(mover.getPosition());
        Block existingBlock = blockHandler.findBlockAt(cell);

        if (existingBlock != null && !existingBlock.getPieces().contains(mover)) {
            String blockColor = existingBlock.getPieces().getFirst().getColor();
            if (!blockColor.equalsIgnoreCase(mover.getColor())) {
                board.sendToBase(mover);
                result.markMoverReturnedToBase();
                return;
            }

            if (blockHandler.canBeInBlock(mover)) {
                blockHandler.addToBlock(mover, existingBlock, cell);
            } else {
                board.sendToBase(mover);
                result.markMoverReturnedToBase();
            }
            return;
        }

        Piece captured = captureHandler.getOpponentAt(cell, mover);
        if (captured != null) {
            CommandResult captureResult =
                    new CaptureCommand(mover, captured, captureHandler).execute();
            for (Piece piece : captureResult.getCapturedPieces()) {
                result.addCapturedPiece(
                        piece, captureResult.getCapturePosition(piece));
            }
            return;
        }

        if (hasSameColorCompanion(cell, mover)) {
            if (!blockHandler.canBeInBlock(mover)) {
                board.sendToBase(mover);
                result.markMoverReturnedToBase();
                return;
            }
            blockHandler.formOrJoinBlock(mover, cell);
        }
    }

    private void breakBlockBeforeTeleport(Piece piece) {
        if (!piece.isInBlock()) return;
        Block block = blockHandler.findBlockAt(board.getCellAt(piece.getPosition()));
        if (block != null) {
            blockHandler.breakBlock(piece, block);
        }
    }

    private boolean hasSameColorCompanion(Cell cell, Piece mover) {
        for (Piece other : cell.getPieces()) {
            if (other != mover
                    && other.getColor().equalsIgnoreCase(mover.getColor())) {
                return true;
            }
        }
        return false;
    }

    private boolean isOnStandardPath(Piece piece) {
        return piece.isOnBoard()
                && !piece.isInBase()
                && !piece.isAtHome()
                && !piece.isInHomeStraight()
                && piece.getPosition() >= 0;
    }
}
