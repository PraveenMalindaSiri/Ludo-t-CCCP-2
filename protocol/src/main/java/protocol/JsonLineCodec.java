package protocol;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.Objects;

/** Converts protocol DTOs to and from one compact JSON object per physical line. */
public final class JsonLineCodec {

    public static final int DEFAULT_MAX_LINE_LENGTH = 1_048_576;

    private final ObjectMapper mapper;
    private final int maxLineLength;

    public JsonLineCodec() {
        this(DEFAULT_MAX_LINE_LENGTH);
    }

    public JsonLineCodec(int maxLineLength) {
        if (maxLineLength <= 0) {
            throw new IllegalArgumentException("maxLineLength must be positive");
        }
        this.maxLineLength = maxLineLength;
        this.mapper =
                JsonMapper.builder()
                        .addModule(new JavaTimeModule())
                        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                        .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                        .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                        .build();
    }

    /** Returns compact JSON without a line terminator. */
    public String encode(Object message) throws ProtocolException {
        Objects.requireNonNull(message, "message");
        try {
            String json = mapper.writeValueAsString(message);
            validateEncodedLine(json);
            return json;
        } catch (JsonProcessingException exception) {
            throw new ProtocolException("Unable to encode protocol message", exception);
        }
    }

    /** Returns compact JSON followed by exactly one LF character. */
    public String encodeLine(Object message) throws ProtocolException {
        return encode(message) + '\n';
    }

    /** Decodes one line after a BufferedReader/readLine-style boundary has removed the LF. */
    public <T> T decode(String line, Class<T> messageType) throws ProtocolException {
        Objects.requireNonNull(messageType, "messageType");
        validateInputLine(line);
        try {
            return mapper.readValue(line, messageType);
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            throw new ProtocolException(
                    "Malformed or invalid " + messageType.getSimpleName(), exception);
        }
    }

    public int maxLineLength() {
        return maxLineLength;
    }

    private void validateEncodedLine(String json) throws ProtocolException {
        if (json.length() > maxLineLength) {
            throw new ProtocolException("Encoded message exceeds maximum line length");
        }
        if (json.indexOf('\n') >= 0 || json.indexOf('\r') >= 0) {
            throw new ProtocolException("Encoded JSON must occupy one physical line");
        }
    }

    private void validateInputLine(String line) throws ProtocolException {
        if (line == null) {
            throw new ProtocolException("Protocol line must not be null");
        }
        if (line.isBlank()) {
            throw new ProtocolException("Protocol line must not be blank");
        }
        if (line.length() > maxLineLength) {
            throw new ProtocolException("Protocol line exceeds maximum line length");
        }
        if (line.indexOf('\n') >= 0 || line.indexOf('\r') >= 0) {
            throw new ProtocolException("Decode requires exactly one physical line");
        }
    }
}
