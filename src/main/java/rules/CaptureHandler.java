package rules;

import board.Board;
import board.Cell;
import config.GameConfig;
import piece.Piece;

import java.util.List;

/**
 * Handles all capture logic.
 */
public class CaptureHandler {
    private final Board board;
    private final GameConfig config;

    public CaptureHandler(Board board) {
        this.board = board;
        this.config = GameConfig.getInstance();
    }

    // single piece capture ------------------------------------------------------------------------------------------

    // Check if landing is capturing
    public boolean isCapturePossible(Piece movingPiece, int destination) {
        if (movingPiece == null) return false;
        return getCapturedPieceAt(destination, movingPiece.getColor()) != null;
    }

    public Piece getCapturedPieceAt(int position, String capturerColor) {
        if (position < 0 || position >= config.getStandardCellCount()) return null;

        Cell cell = board.getCellAt(position);
        List<Piece> piecesOnCell = cell.getPieces();

        Piece opponent = null;
        int validPiecesOnCell = 0;

        for (Piece target : piecesOnCell) {
            if (!isRealStandardPathPieceAt(target, position)) continue;

            validPiecesOnCell++;

            if (!target.getColor().equalsIgnoreCase(capturerColor)) {
                opponent = target;
            }
        }

        if (validPiecesOnCell != 1) return null;
        return opponent;
    }

    public Piece getOpponentAt(Cell cell, Piece mover) {
        if (cell == null || mover == null) return null;

        Piece opponent = null;
        int opponentCount = 0;
        for (Piece target : cell.getPieces()) {
            if (target == mover) continue;
            if (!isRealStandardPathPieceAt(target, cell.getPosition())) continue;
            if (!target.getColor().equalsIgnoreCase(mover.getColor())) {
                opponent = target;
                opponentCount++;
            }
        }
        return opponentCount == 1 ? opponent : null;
    }

    // capture
    public void handleCapture(Piece capturerPiece, Piece capturedPiece) {
        if (capturerPiece == null || capturedPiece == null) return;
        if (capturedPiece.getColor().equalsIgnoreCase(capturerPiece.getColor())) return;

        boolean wasActuallyOnBoard = capturedPiece.isOnBoard()
                && !capturedPiece.isInBase()
                && !capturedPiece.isAtHome()
                && capturedPiece.getPosition() >= 0
                && capturedPiece.getPosition() < config.getStandardCellCount();

        if (!wasActuallyOnBoard) {
            if (!board.getBaseCell(capturedPiece.getColor())
                    .getPieces().contains(capturedPiece)) {
                board.initializeInBase(capturedPiece);
            }
            return;
        }

        board.sendToBase(capturedPiece);
        capturerPiece.incrementCaptureCount();
    }

    private boolean isRealStandardPathPieceAt(Piece piece, int position) {
        if (piece == null) return false;

        return piece.isOnBoard()
                && !piece.isInBase()
                && !piece.isAtHome()
                && !piece.isInHomeStraight()
                && piece.getPosition() == position;
    }

}
