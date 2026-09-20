package protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CsvEncoderTest {

    @Test
    void quotesCommasQuotesAndNewlines() {
        assertEquals("plain", CsvEncoder.field("plain"));
        assertEquals("\"a,b\"", CsvEncoder.field("a,b"));
        assertEquals("\"a\"\"b\"", CsvEncoder.field("a\"b"));
        assertEquals("\"a\nb\"", CsvEncoder.field("a\nb"));
    }

    @Test
    void rowHandlesNullAndTerminatesOnce() {
        assertEquals("A,,3" + System.lineSeparator(), CsvEncoder.row("A", null, 3));
    }
}
