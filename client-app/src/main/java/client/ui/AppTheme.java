package client.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.Border;

/** Shared visual language for the Swing thin client. */
public final class AppTheme {

    public static final Color BACKGROUND = new Color(9, 14, 27);
    public static final Color BACKGROUND_SOFT = new Color(14, 22, 39);
    public static final Color SURFACE = new Color(21, 31, 51);
    public static final Color SURFACE_RAISED = new Color(29, 42, 67);
    public static final Color BORDER = new Color(50, 67, 94);
    public static final Color TEXT = new Color(239, 245, 255);
    public static final Color TEXT_MUTED = new Color(151, 166, 190);
    public static final Color ACCENT = new Color(55, 189, 248);
    public static final Color ACCENT_DARK = new Color(3, 105, 161);
    public static final Color SUCCESS = new Color(52, 211, 153);
    public static final Color WARNING = new Color(251, 191, 36);
    public static final Color DANGER = new Color(248, 113, 113);
    public static final Color PURPLE = new Color(167, 139, 250);

    public static final Font DISPLAY = new Font("Segoe UI", Font.BOLD, 32);
    public static final Font TITLE = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font HEADING = new Font("Segoe UI", Font.BOLD, 16);
    public static final Font BODY = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font BODY_BOLD = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font SMALL = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font MONO = new Font("Consolas", Font.PLAIN, 12);

    private AppTheme() {}

    /** Installs a consistent palette before any Swing component is created. */
    public static void install() {
        UIManager.put("Panel.background", BACKGROUND);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("Label.font", BODY);
        UIManager.put("Button.font", BODY_BOLD);
        UIManager.put("Button.background", SURFACE_RAISED);
        UIManager.put("Button.foreground", TEXT);
        UIManager.put("Button.select", ACCENT_DARK);
        UIManager.put("TextField.font", BODY);
        UIManager.put("TextField.background", BACKGROUND_SOFT);
        UIManager.put("TextField.foreground", TEXT);
        UIManager.put("TextField.caretForeground", TEXT);
        UIManager.put("TextArea.font", BODY);
        UIManager.put("TextArea.background", BACKGROUND_SOFT);
        UIManager.put("TextArea.foreground", TEXT);
        UIManager.put("List.font", BODY);
        UIManager.put("List.background", BACKGROUND_SOFT);
        UIManager.put("List.foreground", TEXT);
        UIManager.put("List.selectionBackground", SURFACE_RAISED);
        UIManager.put("List.selectionForeground", TEXT);
        UIManager.put("ComboBox.font", BODY);
        UIManager.put("ComboBox.background", SURFACE_RAISED);
        UIManager.put("ComboBox.foreground", TEXT);
        UIManager.put("ScrollPane.background", BACKGROUND_SOFT);
        UIManager.put("Viewport.background", BACKGROUND_SOFT);
        UIManager.put("TabbedPane.background", SURFACE);
        UIManager.put("TabbedPane.foreground", TEXT);
        UIManager.put("TabbedPane.selected", SURFACE_RAISED);
        UIManager.put("OptionPane.background", SURFACE);
        UIManager.put("OptionPane.messageForeground", TEXT);
        UIManager.put("OptionPane.messageFont", BODY);
    }

    public static JLabel label(String text, Font font, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(color);
        return label;
    }

    public static void styleField(JTextField field) {
        field.setFont(BODY);
        field.setForeground(TEXT);
        field.setBackground(BACKGROUND_SOFT);
        field.setCaretColor(TEXT);
        field.setSelectionColor(ACCENT_DARK);
        field.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER),
                        BorderFactory.createEmptyBorder(10, 12, 10, 12)));
    }

    public static void styleButton(JButton button, Color background, Color foreground) {
        button.setFont(BODY_BOLD);
        button.setForeground(foreground);
        button.setBackground(background);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
    }

    public static Border sectionBorder(String title) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(BORDER), title, 0, 0, HEADING, TEXT),
                BorderFactory.createEmptyBorder(8, 8, 8, 8));
    }

    public static void removeFocusDecoration(JComponent component) {
        component.setFocusable(false);
    }

    public static Color playerColor(String color) {
        if (color == null) return TEXT_MUTED;
        return switch (color.toUpperCase(Locale.ROOT)) {
            case "RED" -> new Color(248, 92, 92);
            case "GREEN" -> new Color(57, 211, 139);
            case "YELLOW" -> new Color(250, 204, 21);
            case "BLUE" -> new Color(75, 148, 255);
            default -> TEXT_MUTED;
        };
    }

    public static Color statusColor(String status) {
        if (status == null) return TEXT_MUTED;
        return switch (status.toUpperCase(Locale.ROOT)) {
            case "RUNNING" -> SUCCESS;
            case "PAUSED" -> WARNING;
            case "COMPLETED" -> PURPLE;
            case "STOPPED" -> DANGER;
            case "INTERRUPTED" -> TEXT_MUTED;
            default -> ACCENT;
        };
    }

    public static Color blend(Color first, Color second, float ratio) {
        float bounded = Math.max(0f, Math.min(1f, ratio));
        int red = Math.round(first.getRed() * (1f - bounded) + second.getRed() * bounded);
        int green = Math.round(first.getGreen() * (1f - bounded) + second.getGreen() * bounded);
        int blue = Math.round(first.getBlue() * (1f - bounded) + second.getBlue() * bounded);
        return new Color(red, green, blue);
    }
}
