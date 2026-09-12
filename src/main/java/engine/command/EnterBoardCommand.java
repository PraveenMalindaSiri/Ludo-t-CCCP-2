package engine.command;

import board.Board;
import dice.ICoinToss;
import piece.Piece;
import rules.LandingResolver;

/**
 * moving a piece from base to its starting cell (X).
 */
public class EnterBoardCommand implements ICommand {
    private final Piece piece;
    private final Board board;
    private final ICoinToss coinToss;
    private final LandingResolver landingResolver;
    private String resultDirection;

    public EnterBoardCommand(Piece piece, Board board,
                             ICoinToss coinToss,
                             LandingResolver landingResolver) {
        this.piece = piece;
        this.board = board;
        this.coinToss = coinToss;
        this.landingResolver = landingResolver;
    }

    @Override
    public CommandResult execute() {
        int fromPosition = piece.getPosition();
        board.enterBoard(piece);

        // Coin toss determines direction
        String tossResult = coinToss.toss();
        resultDirection = "HEADS".equals(tossResult)
                ? "CLOCKWISE"
                : "COUNTERCLOCKWISE";

        piece.setDirection(resultDirection);
        piece.setOriginalDirection(resultDirection);

        CommandResult result = new CommandResult(CommandResult.Type.ENTER_BOARD);
        result.addMovedPiece(piece);
        result.setMovement(fromPosition, piece.getPosition(), 0, resultDirection);
        landingResolver.resolve(piece, result);
        return result;
    }

    public String getResultDirection() {
        return resultDirection;
    }
}
