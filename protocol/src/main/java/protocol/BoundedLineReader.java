package protocol;

import java.io.IOException;
import java.io.Reader;
import java.util.Objects;

/**
 * Reads one protocol line without allowing an unbounded line to accumulate in memory.
 *
 * <p>The terminating LF is removed. A preceding CR is accepted as part of a CRLF terminator.
 * End-of-stream returns {@code null} only when no characters were read.
 */
public final class BoundedLineReader {

    private final Reader reader;
    private final int maxLineLength;

    public BoundedLineReader(Reader reader, int maxLineLength) {
        this.reader = Objects.requireNonNull(reader, "reader");
        if (maxLineLength <= 0) {
            throw new IllegalArgumentException("maxLineLength must be positive");
        }
        this.maxLineLength = maxLineLength;
    }

    public String readLine() throws IOException, ProtocolException {
        StringBuilder line = new StringBuilder(Math.min(maxLineLength, 256));
        while (true) {
            int value = reader.read();
            if (value == -1) {
                return line.isEmpty() ? null : line.toString();
            }
            if (value == '\n') {
                int length = line.length();
                if (length > 0 && line.charAt(length - 1) == '\r') {
                    line.setLength(length - 1);
                }
                return line.toString();
            }
            if (line.length() == maxLineLength) {
                throw new ProtocolException("Protocol line exceeds maximum line length");
            }
            line.append((char) value);
        }
    }
}
