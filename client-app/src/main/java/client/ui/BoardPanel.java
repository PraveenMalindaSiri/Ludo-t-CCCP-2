package client.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.JPanel;
import protocol.GameSnapshotDto;

/** Paints a complete board from immutable protocol DTOs only. */
public final class BoardPanel extends JPanel {

    private static final Color BOARD_BACKGROUND = AppTheme.BACKGROUND_SOFT;
    private static final Color PATH_COLOR = new Color(232, 238, 248);
    private static final Color GRID_COLOR = new Color(93, 108, 132);

    private GameSnapshotDto snapshot;

    public BoardPanel() {
        setPreferredSize(new Dimension(660, 660));
        setBackground(BOARD_BACKGROUND);
    }

    public void setSnapshot(GameSnapshotDto snapshot) {
        this.snapshot = snapshot;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int boardSize = Math.max(1, Math.min(getWidth(), getHeight()) - 24);
            int cellSize = Math.max(1, boardSize / BoardGeometry.GRID_SIZE);
            int paintedSize = cellSize * BoardGeometry.GRID_SIZE;
            int originX = (getWidth() - paintedSize) / 2;
            int originY = (getHeight() - paintedSize) / 2;

            g.setColor(new Color(0, 0, 0, 80));
            g.fillRoundRect(originX - 8, originY - 4, paintedSize + 16, paintedSize + 18, 24, 24);
            g.setColor(new Color(24, 35, 57));
            g.fillRoundRect(originX - 8, originY - 8, paintedSize + 16, paintedSize + 16, 24, 24);
            paintBases(g, originX, originY, cellSize);
            paintPath(g, originX, originY, cellSize);
            paintApproachCells(g, originX, originY, cellSize);
            paintStartingCells(g, originX, originY, cellSize);
            paintMysteryEffectCells(g, originX, originY, cellSize);
            paintHomeStraights(g, originX, originY, cellSize);
            paintCenter(g, originX, originY, cellSize);
            paintMystery(g, originX, originY, cellSize);
            paintPieces(g, originX, originY, cellSize);
            paintBoardCaption(g, originX, originY, paintedSize);
        } finally {
            g.dispose();
        }
    }

    private void paintBases(Graphics2D g, int x, int y, int cell) {
        fillArea(g, x, y, cell, 0, 0, colorFor("GREEN", 55));
        fillArea(g, x, y, cell, 9, 0, colorFor("YELLOW", 65));
        fillArea(g, x, y, cell, 9, 9, colorFor("BLUE", 55));
        fillArea(g, x, y, cell, 0, 9, colorFor("RED", 55));
        for (String color : List.of("RED", "GREEN", "YELLOW", "BLUE")) {
            for (int index = 0; index < 4; index++) {
                Point point = BoardGeometry.base(color, index);
                paintCell(g, x, y, cell, point, Color.WHITE);
            }
        }
        paintBaseLabel(g, "GREEN", x + cell * 3, y + cell, colorFor("GREEN", 255));
        paintBaseLabel(g, "YELLOW", x + cell * 12, y + cell, colorFor("YELLOW", 255));
        paintBaseLabel(g, "RED", x + cell * 3, y + cell * 14, colorFor("RED", 255));
        paintBaseLabel(g, "BLUE", x + cell * 12, y + cell * 14, colorFor("BLUE", 255));
    }

    private void fillArea(Graphics2D g, int x, int y, int cell, int gridX, int gridY, Color color) {
        g.setColor(color);
        g.fillRoundRect(x + gridX * cell, y + gridY * cell, 6 * cell, 6 * cell, cell, cell);
    }

    private void paintPath(Graphics2D g, int x, int y, int cell) {
        BoardGeometry.standardPath().forEach(point -> paintCell(g, x, y, cell, point, PATH_COLOR));
    }

    private void paintHomeStraights(Graphics2D g, int x, int y, int cell) {
        for (String color : List.of("RED", "GREEN", "YELLOW", "BLUE")) {
            for (int index = 0; index < 5; index++) {
                paintCell(
                        g,
                        x,
                        y,
                        cell,
                        BoardGeometry.homeStraight(color, index),
                        colorFor(color, 95));
            }
        }
    }

    private void paintCenter(Graphics2D g, int x, int y, int cell) {
        for (String color : List.of("RED", "GREEN", "YELLOW", "BLUE")) {
            paintCell(g, x, y, cell, BoardGeometry.home(color), colorFor(color, 105));
        }
        paintCell(g, x, y, cell, new Point(7, 7), new Color(236, 239, 244));
    }

    private void paintCell(
            Graphics2D g, int originX, int originY, int cell, Point point, Color fill) {
        int x = originX + point.x * cell;
        int y = originY + point.y * cell;
        g.setColor(fill);
        g.fillRoundRect(x + 1, y + 1, Math.max(1, cell - 2), Math.max(1, cell - 2), 6, 6);
        g.setColor(GRID_COLOR);
        g.drawRoundRect(x + 1, y + 1, Math.max(1, cell - 2), Math.max(1, cell - 2), 6, 6);
    }

    private void paintMystery(Graphics2D g, int x, int y, int cell) {
        if (snapshot == null || !snapshot.mystery().active()) {
            return;
        }
        int position = snapshot.mystery().position();
        if (position < 0 || position >= 52) {
            return;
        }
        Point point = BoardGeometry.standardCell(position);
        int left = x + point.x * cell;
        int top = y + point.y * cell;
        g.setColor(AppTheme.PURPLE);
        g.setStroke(new BasicStroke(Math.max(2f, cell / 10f)));
        g.drawOval(left + 4, top + 4, Math.max(1, cell - 8), Math.max(1, cell - 8));
        drawCentered(g, "?", left, top, cell, AppTheme.PURPLE);
    }

    private void paintPieces(Graphics2D g, int x, int y, int cell) {
        if (snapshot == null) {
            return;
        }
        Map<Point, List<PieceView>> grouped = new LinkedHashMap<>();
        snapshot.players()
                .forEach(
                        player -> {
                            for (int index = 0; index < player.pieces().size(); index++) {
                                GameSnapshotDto.PieceDto piece = player.pieces().get(index);
                                Point point = BoardGeometry.pointFor(piece, index);
                                grouped.computeIfAbsent(point, ignored -> new ArrayList<>())
                                        .add(
                                                new PieceView(
                                                        player.color(),
                                                        piece.name(),
                                                        piece.inBlock(),
                                                        piece.stateLabel(),
                                                        piece.blockId()));
                            }
                        });

        grouped.forEach(
                (point, pieces) -> {
                    for (int index = 0; index < pieces.size(); index++) {
                        PieceView piece = pieces.get(index);
                        int diameter = Math.max(10, cell / 2 - 2);
                        int offsetX = index % 2 * (cell / 2);
                        int offsetY = index / 2 % 2 * (cell / 2);
                        int left = x + point.x * cell + offsetX + 1;
                        int top = y + point.y * cell + offsetY + 1;
                        Color pieceColor = colorFor(piece.color(), 255);
                        g.setPaint(
                                new GradientPaint(
                                        left,
                                        top,
                                        AppTheme.blend(pieceColor, Color.WHITE, 0.2f),
                                        left + diameter,
                                        top + diameter,
                                        AppTheme.blend(pieceColor, Color.BLACK, 0.22f)));
                        g.fillOval(left, top, diameter, diameter);
                        boolean active = snapshot.currentPlayer().equalsIgnoreCase(piece.color());
                        g.setStroke(new BasicStroke(active ? 2.6f : 1.3f));
                        g.setColor(active ? Color.WHITE : new Color(25, 32, 43));
                        g.drawOval(left, top, diameter, diameter);
                        Color text =
                                piece.color().equalsIgnoreCase("YELLOW")
                                        ? new Color(35, 29, 5)
                                        : Color.WHITE;
                        drawCentered(g, piece.name(), left, top, diameter, text);
                        if (piece.inBlock()) {
                            g.setStroke(new BasicStroke(2f));
                            g.setColor(AppTheme.PURPLE);
                            g.drawRect(left - 1, top - 1, diameter + 2, diameter + 2);
                        }
                    }
                });
    }

    private void drawCentered(Graphics2D g, String text, int x, int y, int size, Color foreground) {
        FontMetrics metrics = g.getFontMetrics();
        int textX = x + (size - metrics.stringWidth(text)) / 2;
        int textY = y + (size - metrics.getHeight()) / 2 + metrics.getAscent();
        g.setColor(foreground);
        g.drawString(text, textX, textY);
    }

    private Color colorFor(String color, int alpha) {
        Color base =
                switch (color.toUpperCase(Locale.ROOT)) {
                    case "RED" -> new Color(231, 76, 60);
                    case "GREEN" -> new Color(46, 170, 90);
                    case "YELLOW" -> new Color(245, 194, 66);
                    case "BLUE" -> new Color(52, 124, 220);
                    default -> new Color(150, 150, 150);
                };
        return new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha);
    }

    private void paintStartingCells(Graphics2D g, int x, int y, int cell) {
        for (String color : List.of("YELLOW", "BLUE", "RED", "GREEN")) {
            Point point = BoardGeometry.startingCell(color);
            int left = x + point.x * cell;
            int top = y + point.y * cell;
            Color playerColor = colorFor(color, 255);
            g.setColor(AppTheme.blend(PATH_COLOR, playerColor, 0.23f));
            g.fillRoundRect(left + 2, top + 2, Math.max(1, cell - 4), Math.max(1, cell - 4), 6, 6);
            g.setFont(AppTheme.HEADING.deriveFont(java.awt.Font.BOLD, Math.max(12f, cell * 0.52f)));
            drawCentered(g, "X", left, top, cell, playerColor);
        }
    }

    private void paintApproachCells(Graphics2D g, int x, int y, int cell) {
        for (String color : List.of("YELLOW", "BLUE", "RED", "GREEN")) {
            Point point = BoardGeometry.approachCell(color);
            int left = x + point.x * cell;
            int top = y + point.y * cell;
            int diameter = Math.max(12, cell / 2);
            int circleLeft = left + (cell - diameter) / 2;
            int circleTop = top + (cell - diameter) / 2;
            Color playerColor = colorFor(color, 255);

            g.setColor(AppTheme.blend(PATH_COLOR, playerColor, 0.16f));
            g.fillRoundRect(left + 2, top + 2, Math.max(1, cell - 4), Math.max(1, cell - 4), 6, 6);
            g.setColor(playerColor);
            g.fillOval(circleLeft, circleTop, diameter, diameter);
            g.setColor(AppTheme.blend(playerColor, Color.BLACK, 0.28f));
            g.setStroke(new BasicStroke(Math.max(1.4f, cell / 20f)));
            g.drawOval(circleLeft, circleTop, diameter, diameter);
        }
    }

    private void paintMysteryEffectCells(Graphics2D g, int x, int y, int cell) {
        paintMysteryEffectCell(g, x, y, cell, "ALPHA", "\u03b1");
        paintMysteryEffectCell(g, x, y, cell, "BETA", "\u03b2");
        paintMysteryEffectCell(g, x, y, cell, "GAMMA", "\u03b3");
    }

    private void paintMysteryEffectCell(
            Graphics2D g, int x, int y, int cell, String effect, String symbol) {
        Point point = BoardGeometry.mysteryEffectCell(effect);
        int left = x + point.x * cell;
        int top = y + point.y * cell;
        boolean sharesApproachCell = point.equals(BoardGeometry.approachCell("RED"));

        g.setColor(new Color(91, 66, 146, 68));
        g.fillRoundRect(left + 3, top + 3, Math.max(1, cell - 6), Math.max(1, cell - 6), 7, 7);
        g.setColor(AppTheme.PURPLE);
        g.setStroke(new BasicStroke(Math.max(1.5f, cell / 18f)));
        g.drawRoundRect(left + 4, top + 4, Math.max(1, cell - 8), Math.max(1, cell - 8), 7, 7);

        if (sharesApproachCell) {
            int badge = Math.max(13, cell / 2);
            g.setColor(new Color(246, 242, 255));
            g.fillOval(left + cell - badge - 2, top + 2, badge, badge);
            g.setFont(AppTheme.SMALL.deriveFont(java.awt.Font.BOLD, Math.max(10f, cell * 0.28f)));
            drawCentered(g, symbol, left + cell - badge - 2, top + 2, badge, AppTheme.PURPLE);
        } else {
            g.setFont(AppTheme.HEADING.deriveFont(java.awt.Font.BOLD, Math.max(12f, cell * 0.48f)));
            drawCentered(g, symbol, left, top, cell, AppTheme.PURPLE);
        }
    }

    private void paintBaseLabel(Graphics2D g, String text, int centerX, int baseline, Color color) {
        g.setFont(AppTheme.SMALL.deriveFont(java.awt.Font.BOLD));
        FontMetrics metrics = g.getFontMetrics();
        g.setColor(color);
        g.drawString(text, centerX - metrics.stringWidth(text) / 2, baseline);
    }

    private void paintBoardCaption(Graphics2D g, int x, int y, int size) {
        if (snapshot == null) return;
        String text =
                snapshot.status().name()
                        + "  •  turn "
                        + snapshot.turn()
                        + "  •  v"
                        + snapshot.version();
        g.setFont(AppTheme.SMALL);
        FontMetrics metrics = g.getFontMetrics();
        int width = metrics.stringWidth(text) + 18;
        int left = x + size - width - 8;
        int top = y + 8;
        g.setColor(new Color(9, 14, 27, 210));
        g.fillRoundRect(left, top, width, 24, 12, 12);
        g.setColor(AppTheme.TEXT);
        g.drawString(text, left + 9, top + 16);
    }

    private record PieceView(
            String color, String name, boolean inBlock, String state, String blockId) {}
}
