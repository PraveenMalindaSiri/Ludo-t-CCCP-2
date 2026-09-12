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

    // capture
    public void handleCapture(Piece capturerPiece, Piece capturedPiece) {
        if (capturerPiece == null || capturedPiece == null) return;
        if (capturedPiece.getColor().equalsIgnoreCase(capturerPiece.getColor())) return;

        boolean wasActuallyOnBoard = capturedPiece.isOnBoard()
                && !capturedPiece.isInBase()
                && !capturedPiece.isAtHome()
                && capturedPiece.getPosition() >= 0
                && capturedPiece.getPosition() < config.getStandardCellCount();

        Cell currentCell = findStandardCellContaining(capturedPiece);
        if (currentCell != null) {
            currentCell.removePiece(capturedPiece);
        }

        Cell baseCell = board.getBaseCell(capturedPiece.getColor());

        if (!wasActuallyOnBoard) {
            if (!baseCell.getPieces().contains(capturedPiece)) {
                baseCell.addPiece(capturedPiece);
            }
            return;
        }

        capturedPiece.capture();

        if (!baseCell.getPieces().contains(capturedPiece)) {
            baseCell.addPiece(capturedPiece);
        }

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

    private Cell findStandardCellContaining(Piece piece) {
        for (int i = 0; i < config.getStandardCellCount(); i++) {
            Cell cell = board.getCellAt(i);

            if (cell.getPieces().contains(piece)) {
                return cell;
            }
        }

        return null;
    }
}