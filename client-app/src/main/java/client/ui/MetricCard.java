package client.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import javax.swing.BorderFactory;
import javax.swing.JLabel;

/** Small dashboard card with a muted label and prominent live value. */
public final class MetricCard extends CardPanel {

    private final JLabel value;

    public MetricCard(String name, Color accent) {
        super(AppTheme.BACKGROUND_SOFT, 14);
        setLayout(new BorderLayout(4, 4));
        setBorder(BorderFactory.createEmptyBorder(10, 12, 12, 12));
        JLabel title = AppTheme.label(name.toUpperCase(), AppTheme.SMALL, AppTheme.TEXT_MUTED);
        value = AppTheme.label("-", AppTheme.HEADING, accent);
        add(title, BorderLayout.NORTH);
        add(value, BorderLayout.CENTER);
    }

    public void setValue(String text) {
        value.setText(text == null || text.isBlank() ? "-" : text);
    }

    public void setValueColor(Color color) {
        value.setForeground(color);
    }
}
