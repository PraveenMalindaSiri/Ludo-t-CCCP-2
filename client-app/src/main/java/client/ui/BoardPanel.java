package client.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
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

    private static final Color BOARD_BACKGROUND = new Color(248, 249, 252);
    private static final Color PATH_COLOR = Color.WHITE;
    private static final Color GRID_COLOR = new Color(116, 124, 138);

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

            paintBases(g, originX, originY, cellSize);
            paintPath(g, originX, originY, cellSize);
            paintHomeStraights(g, originX, originY, cellSize);
            paintCenter(g, originX, originY, cellSize);
            paintMystery(g, originX, originY, cellSize);
            paintPieces(g, originX, originY, cellSize);
        } finally {
            g.dispose();
        }
    }

    private void paintBases(Graphics2D g, int x, int y, int cell) {
        fillArea(g, x, y, cell, 0, 0, colorFor("RED", 55));
        fillArea(g, x, y, cell, 9, 0, colorFor("GREEN", 55));
        fillArea(g, x, y, cell, 9, 9, colorFor("YELLOW", 65));
        fillArea(g, x, y, cell, 0, 9, colorFor("BLUE", 55));
        for (String color : List.of("RED", "GREEN", "YELLOW", "BLUE")) {
            for (int index = 0; index < 4; index++) {
                Point point = BoardGeometry.base(color, index);
                paintCell(g, x, y, cell, point, Color.WHITE);
            }
        }
    }

    private void fillArea(
            Graphics2D g, int x, int y, int cell, int gridX, int gridY, Color color) {
        g.setColor(color);
        g.fillRoundRect(x + gridX * cell, y + gridY * cell, 6 * cell, 6 * cell, cell, cell);
    }

    private void paintPath(Graphics2D g, int x, int y, int cell) {
        BoardGeometry.standardPath()
                .forEach(point -> paintCell(g, x, y, cell, point, PATH_COLOR));
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
        g.fillRect(x, y, cell, cell);
        g.setColor(GRID_COLOR);
        g.drawRect(x, y, cell, cell);
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
        g.setColor(new Color(111, 45, 189));
        g.setStroke(new BasicStroke(Math.max(2f, cell / 10f)));
        g.drawOval(left + 4, top + 4, Math.max(1, cell - 8), Math.max(1, cell - 8));
        drawCentered(g, "?", left, top, cell, Color.BLACK);
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
                                Point point =
                                        BoardGeometry.pointFor(
                                                player.color(), piece.position(), index);
                                grouped.computeIfAbsent(point, ignored -> new ArrayList<>())
                                        .add(new PieceView(player.color(), piece.name()));
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
                        g.setColor(colorFor(piece.color(), 150));
                        g.fillOval(left, top, diameter, diameter);
                        g.setColor(Color.DARK_GRAY);
                        g.drawOval(left, top, diameter, diameter);
                        drawCentered(g, piece.name(), left, top, diameter, Color.BLACK);
                    }
                });
    }

    private void drawCentered(
            Graphics2D g, String text, int x, int y, int size, Color foreground) {
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

    private record PieceView(String color, String name) {}
}
