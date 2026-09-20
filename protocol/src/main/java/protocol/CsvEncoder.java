package protocol;

import java.util.Arrays;

/** Small RFC 4180-style encoder shared by the evidence writers. */
public final class CsvEncoder {

    private CsvEncoder() {}

    public static String row(Object... values) {
        return Arrays.stream(values).map(CsvEncoder::field).reduce((a, b) -> a + "," + b).orElse("")
                + System.lineSeparator();
    }

    public static String field(Object value) {
        String text = value == null ? "" : value.toString();
        if (text.indexOf(',') < 0
                && text.indexOf('"') < 0
                && text.indexOf('\n') < 0
                && text.indexOf('\r') < 0) {
            return text;
        }
        return '"' + text.replace("\"", "\"\"") + '"';
    }
}
