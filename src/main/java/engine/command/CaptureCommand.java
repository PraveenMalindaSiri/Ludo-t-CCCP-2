package engine.command;

import piece.Piece;
import rules.CaptureHandler;

/**
 * single-piece capture action
 */
public class CaptureCommand implements ICommand {
    private final Piece capturerPiece;
    private final Piece capturedPiece;
    private final CaptureHandler captureHandler;

    public CaptureCommand(Piece capturerPiece, Piece capturedPiece,
                          CaptureHandler captureHandler) {
        this.capturerPiece = capturerPiece;
        this.capturedPiece = capturedPiece;
        this.captureHandler = captureHandler;
    }

    @Override
    public CommandResult execute() {
        CommandResult result = new CommandResult(CommandResult.Type.CAPTURE);
        int fromPosition = capturedPiece.getPosition();
        String direction = capturedPiece.getDirection();
        captureHandler.handleCapture(capturerPiece, capturedPiece);
        result.addCapturedPiece(capturedPiece, fromPosition);
        result.setMovement(fromPosition,
                capturedPiece.getPosition(), 0, direction);
        return result;
    }
}
