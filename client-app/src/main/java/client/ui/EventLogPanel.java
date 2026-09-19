package client.ui;

import java.awt.BorderLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/** Read-only bounded event history supplied by ClientViewState. */
public final class EventLogPanel extends JPanel {

    private final JTextArea events = new JTextArea(10, 28);

    public EventLogPanel() {
        super(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Events"));
        events.setEditable(false);
        events.setLineWrap(true);
        events.setWrapStyleWord(true);
        add(new JScrollPane(events), BorderLayout.CENTER);
    }

    public void setEvents(List<String> messages) {
        events.setText(String.join(System.lineSeparator(), messages));
        events.setCaretPosition(events.getDocument().getLength());
    }
}
