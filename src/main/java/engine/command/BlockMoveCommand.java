package engine.command;

import block.Block;
import board.Board;
import piece.Piece;
import rules.BlockHandler;
import rules.CaptureHandler;
import rules.LandingResolver;

import java.util.ArrayList;
import java.util.List;

/**
 * Executes one complete block movement through the existing block component.
 */
public final class BlockMoveCommand implements ICommand {
    private final Block block;
    private final int diceValue;
    private final Board board;
    private final BlockHandler blockHandler;
    private final CaptureHandler captureHandler;
    private final LandingResolver landingResolver;

    public BlockMoveCommand(Block block, int diceValue, Board board,
                            BlockHandler blockHandler,
                            CaptureHandler captureHandler,
                            LandingResolver landingResolver) {
        this.block = block;
        this.diceValue = diceValue;
        this.board = board;
        this.blockHandler = blockHandler;
        this.captureHandler = captureHandler;
        this.landingResolver = landingResolver;
    }

    @Override
    public CommandResult execute() {
        CommandResult result = new CommandResult(CommandResult.Type.BLOCK_MOVE);
        if (block == null || !blockHandler.canBlockMove(block, diceValue)) {
            return result;
        }

        int oldPosition = block.getPosition();
        int destination = blockHandler.calculateBlockDestination(block, diceValue);
        int firstOpponentBlock =
                blockHandler.getFirstOpponentBlockPositionForBlock(block, diceValue);
        Piece representative = block.getPieces().getFirst();
        Piece singleTarget = null;

        if (firstOpponentBlock != -1) {
            if (firstOpponentBlock != destination) {
                result.markBlocked(firstOpponentBlock);
                return result;
            }

            Block defender = blockHandler.findBlockAt(board.getCellAt(destination));
            if (!blockHandler.canBlockCaptureBlock(block, defender)) {
                result.markBlocked(destination);
                return result;
            }

            List<Piece> defenders = new ArrayList<>(defender.getPieces());
            blockHandler.handleBlockCapture(block, defender);
            for (Piece captured : defenders) {
                result.addCapturedPiece(captured, destination);
            }
        } else {
            singleTarget = captureHandler.getCapturedPieceAt(
                    destination, representative.getColor());
        }

        int movement = blockHandler.getBlockMovementAmount(block, diceValue);
        String direction = block.getDirection();
        List<Piece> movingPieces = new ArrayList<>(block.getPieces());
        blockHandler.moveBlock(block, diceValue);

        for (Piece moved : movingPieces) result.addMovedPiece(moved);
        result.setMovement(oldPosition, block.getPosition(), movement, direction);

        if (singleTarget != null) {
            CommandResult capture =
                    new CaptureCommand(representative, singleTarget, captureHandler).execute();
            for (Piece captured : capture.getCapturedPieces()) {
                result.addCapturedPiece(
                        captured, capture.getCapturePosition(captured));
            }
            for (Piece member : block.getPieces()) {
                if (member != representative) member.incrementCaptureCount();
            }
        } else if (!result.hasCaptured()) {
            blockHandler.absorbSameColorPieces(block);
        }

        landingResolver.resolve(representative, result);
        return result;
    }
}
