package engine.command;

import board.*;
import dice.ICoinToss;
import piece.Piece;

/**
 * moving a piece from base to its starting cell (X).
 */
public class EnterBoardCommand implements ICommand {
    private final Piece piece;
    private final StartingCell startingCell;
    private final BaseCell baseCell;
    private final ICoinToss coinToss;
    private String resultDirection;

    public EnterBoardCommand(Piece piece, StartingCell startingCell,
                             BaseCell baseCell, ICoinToss coinToss) {
        this.piece = piece;
        this.startingCell = startingCell;
        this.baseCell = baseCell;
        this.coinToss = coinToss;
    }

    @Override
    public void execute() {
        // Remove from base
        baseCell.removePiece(piece);

        // Place on starting cell
        piece.moveToPosition(startingCell.getPosition());
        startingCell.addPiece(piece);

        // Coin toss determines direction
        String tossResult = coinToss.toss();
        resultDirection = "HEADS".equals(tossResult)
                ? "CLOCKWISE"
                : "COUNTERCLOCKWISE";

        piece.setDirection(resultDirection);
        piece.setOriginalDirection(resultDirection);
    }

    public String getResultDirection() {
        return resultDirection;
    }
}
