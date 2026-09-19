package protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import org.junit.jupiter.api.Test;

class BoundedLineReaderTest {

    @Test
    void readsLfAndCrLfDelimitedMessagesSeparately() throws Exception {
        BoundedLineReader reader = new BoundedLineReader(new StringReader("one\ntwo\r\n"), 10);

        assertEquals("one", reader.readLine());
        assertEquals("two", reader.readLine());
        assertNull(reader.readLine());
    }

    @Test
    void reconstructsLineWhenUnderlyingReaderReturnsSmallChunks() throws Exception {
        BoundedLineReader reader =
                new BoundedLineReader(new ChunkedReader("{\"kind\":\"REQUEST\"}\n", 3), 100);

        assertEquals("{\"kind\":\"REQUEST\"}", reader.readLine());
    }

    @Test
    void rejectsOversizedLineBeforeWaitingForItsTerminator() {
        BoundedLineReader reader = new BoundedLineReader(new StringReader("123456"), 5);

        assertThrows(ProtocolException.class, reader::readLine);
    }

    private static final class ChunkedReader extends Reader {

        private final String value;
        private final int chunkSize;
        private int position;
        private int remainingInChunk;

        private ChunkedReader(String value, int chunkSize) {
            this.value = value;
            this.chunkSize = chunkSize;
            this.remainingInChunk = chunkSize;
        }

        @Override
        public int read(char[] target, int offset, int length) {
            if (position == value.length()) {
                return -1;
            }
            int count = Math.min(Math.min(length, remainingInChunk), value.length() - position);
            value.getChars(position, position + count, target, offset);
            position += count;
            remainingInChunk -= count;
            if (remainingInChunk == 0) {
                remainingInChunk = chunkSize;
            }
            return count;
        }

        @Override
        public int read() throws IOException {
            char[] value = new char[1];
            return read(value, 0, 1) == -1 ? -1 : value[0];
        }

        @Override
        public void close() {}
    }
}
