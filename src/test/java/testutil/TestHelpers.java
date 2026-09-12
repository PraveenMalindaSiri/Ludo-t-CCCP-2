package testutil;

import board.Board;
import board.Cell;
import factory.BoardFactory;
import piece.Piece;
import rules.BlockHandler;
import rules.CaptureHandler;
import rules.RuleEngine;

public final class TestHelpers {
    private TestHelpers() {
    }

    public static Board board() {
        return BoardFactory.createBoard();
    }

    public static BlockHandler blockHandler(Board board) {
        return new BlockHandler(board);
    }

    public static CaptureHandler captureHandler(Board board) {
        return new CaptureHandler(board);
    }

    public static RuleEngine ruleEngine(Board board) {
        BlockHandler blockHandler = new BlockHandler(board);
        CaptureHandler captureHandler = new CaptureHandler(board);
        return new RuleEngine(board, blockHandler, captureHandler);
    }

    public static RuleEngine ruleEngine(Board board, BlockHandler blockHandler, CaptureHandler captureHandler) {
        return new RuleEngine(board, blockHandler, captureHandler);
    }

    public static Piece placePiece(Board board, String color, String name, int position) {
        return placePiece(board, color, name, position, "CLOCKWISE");
    }

    public static Piece placePiece(Board board, String color, String name, int position, String direction) {
        Piece piece = new Piece(name, color);
        piece.moveToPosition(position);
        piece.setDirection(direction);
        piece.setOriginalDirection(direction);
        board.getCellAt(position).addPiece(piece);
        return piece;
    }

    public static Piece basePiece(Board board, String color, String name) {
        Piece piece = new Piece(name, color);
        board.getBaseCell(color).addPiece(piece);
        return piece;
    }

    public static void removeFromCurrentStandardCell(Board board, Piece piece) {
        int position = piece.getPosition();
        if (position >= 0 && position < 52) {
            board.getCellAt(position).removePiece(piece);
        }
    }

    public static boolean cellContains(Cell cell, Piece piece) {
        return cell.getPieces().contains(piece);
    }
}
