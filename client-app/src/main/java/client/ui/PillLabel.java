package client.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JLabel;

/** Compact rounded status indicator. */
public final class PillLabel extends JLabel {

    private Color fill = AppTheme.SURFACE_RAISED;

    public PillLabel(String text) {
        super(text);
        setFont(AppTheme.SMALL.deriveFont(java.awt.Font.BOLD));
        setForeground(AppTheme.TEXT);
        setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        setOpaque(false);
    }

    public void setPill(String text, Color color) {
        setText(text);
        fill = color;
        setForeground(contrastingText(color));
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(fill);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
        } finally {
            g.dispose();
        }
        super.paintComponent(graphics);
    }

    private Color contrastingText(Color background) {
        double brightness =
                background.getRed() * 0.299
                        + background.getGreen() * 0.587
                        + background.getBlue() * 0.114;
        return brightness > 150 ? new Color(12, 20, 33) : Color.WHITE;
    }
}
