package client.ui;

import java.awt.BorderLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/** Read-only bounded event history supplied by ClientViewState. */
public final class EventLogPanel extends JPanel {

    private final JTextArea events = new JTextArea(10, 28);
    private final JLabel count = AppTheme.label("0 events", AppTheme.SMALL, AppTheme.TEXT_MUTED);

    public EventLogPanel() {
        super(new BorderLayout(8, 8));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(AppTheme.label("Live activity", AppTheme.HEADING, AppTheme.TEXT));
        header.add(count, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        events.setEditable(false);
        events.setLineWrap(true);
        events.setWrapStyleWord(true);
        events.setFont(AppTheme.MONO);
        events.setForeground(AppTheme.TEXT);
        events.setBackground(AppTheme.BACKGROUND_SOFT);
        events.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        JScrollPane scroll = new JScrollPane(events);
        scroll.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER));
        scroll.getViewport().setBackground(AppTheme.BACKGROUND_SOFT);
        add(scroll, BorderLayout.CENTER);
    }

    public void setEvents(List<String> messages) {
        events.setText(String.join(System.lineSeparator() + System.lineSeparator(), messages));
        events.setCaretPosition(events.getDocument().getLength());
        int size = messages.size();
        count.setText(size + (size == 1 ? " event" : " events"));
    }
}
