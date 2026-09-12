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
    public void execute() {
        captureHandler.handleCapture(capturerPiece, capturedPiece);
    }
}
