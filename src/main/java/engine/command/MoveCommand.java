package engine.command;

import board.Cell;
import piece.Piece;

/**
 * standard path movement action.
 */
public class MoveCommand implements ICommand {
    private final Piece piece;
    private final Cell fromCell;
    private final Cell toCell;
    private final int steps;

    public MoveCommand(Piece piece, Cell fromCell, Cell toCell, int steps) {
        this.piece = piece;
        this.fromCell = fromCell;
        this.toCell = toCell;
        this.steps = steps;
    }

    @Override
    public void execute() {
        fromCell.removePiece(piece);
        piece.move(steps);
        toCell.addPiece(piece);
    }
}
