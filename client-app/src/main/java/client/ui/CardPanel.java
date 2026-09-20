package client.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;

/** Lightweight rounded card used to group related Swing content. */
public class CardPanel extends JPanel {

    private final Color fill;
    private final int radius;

    public CardPanel() {
        this(AppTheme.SURFACE, 18);
    }

    public CardPanel(Color fill, int radius) {
        this.fill = fill;
        this.radius = radius;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(0, 0, 0, 45));
            g.fillRoundRect(
                    3,
                    5,
                    Math.max(0, getWidth() - 6),
                    Math.max(0, getHeight() - 7),
                    radius,
                    radius);
            g.setColor(fill);
            g.fillRoundRect(
                    1,
                    1,
                    Math.max(0, getWidth() - 4),
                    Math.max(0, getHeight() - 5),
                    radius,
                    radius);
            g.setColor(AppTheme.BORDER);
            g.drawRoundRect(
                    1,
                    1,
                    Math.max(0, getWidth() - 4),
                    Math.max(0, getHeight() - 5),
                    radius,
                    radius);
        } finally {
            g.dispose();
        }
        super.paintComponent(graphics);
    }
}
