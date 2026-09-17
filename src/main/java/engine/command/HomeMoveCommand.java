package engine.command;

import board.Board;
import config.GameConfig;
import piece.Piece;

/** Moves a piece within its home straight or into Home. */
public final class HomeMoveCommand implements ICommand {
    private final Piece piece;
    private final Board board;
    private final int destinationIndex;
    private final int movement;

    public HomeMoveCommand(Piece piece, Board board, int destinationIndex, int movement) {
        this.piece = piece;
        this.board = board;
        this.destinationIndex = destinationIndex;
        this.movement = movement;
    }

    @Override
    public CommandResult execute() {
        int fromPosition = piece.getPosition();
        String direction = piece.getDirection();

        if (destinationIndex >= GameConfig.getInstance().getHomePathLength()) {
            board.moveToHome(piece);
        } else {
            board.moveToHomeStraight(piece, destinationIndex);
        }

        CommandResult result = new CommandResult(CommandResult.Type.HOME_MOVE);
        result.addMovedPiece(piece);
        result.setMovement(fromPosition, piece.getPosition(), movement, direction);
        return result;
    }
}
