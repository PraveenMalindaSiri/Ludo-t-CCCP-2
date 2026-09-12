package engine.command;

import board.Board;
import block.IMovable;
import piece.Piece;
import rules.LandingResolver;

import java.util.List;

/**
 * standard path movement action.
 */
public class MoveCommand implements ICommand {
    private final IMovable movable;
    private final Board board;
    private final int destination;
    private final int steps;
    private final LandingResolver landingResolver;

    public MoveCommand(IMovable movable, Board board, int destination,
                       int steps, LandingResolver landingResolver) {
        this.movable = movable;
        this.board = board;
        this.destination = destination;
        this.steps = steps;
        this.landingResolver = landingResolver;
    }

    @Override
    public CommandResult execute() {
        int fromPosition = movable.getPosition();
        String direction = movable.getDirection();
        List<Piece> movedPieces = movable.getPieces();
        board.moveOnStandardPath(movable, steps, destination);

        CommandResult result = new CommandResult(CommandResult.Type.MOVE);
        for (Piece piece : movedPieces) result.addMovedPiece(piece);
        result.setMovement(fromPosition, destination, steps, direction);
        if (movedPieces.size() == 1) {
            landingResolver.resolve(movedPieces.getFirst(), result);
        }
        return result;
    }
}
