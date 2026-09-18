package protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JsonLineCodecTest {

    private static final UUID REQUEST_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CLIENT_ID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID SESSION_ID =
            UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID EVENT_ID =
            UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final Instant TIME = Instant.parse("2026-09-18T12:30:45Z");

    private final JsonLineCodec codec = new JsonLineCodec();

    @Test
    void requestRoundTripPreservesIdentifiersEnumsAndParameters() throws Exception {
        RequestMessage request =
                RequestMessage.create(
                        REQUEST_ID,
                        CLIENT_ID,
                        RequestType.SET_SPEED,
                        SESSION_ID,
                        Map.of("turnDelayMillis", "500"),
                        TIME);

        String json = codec.encode(request);
        RequestMessage decoded = codec.decode(json, RequestMessage.class);

        assertEquals(request, decoded);
        assertEquals(MessageKind.REQUEST, decoded.kind());
        assertEquals(REQUEST_ID, decoded.requestId());
        assertEquals(CLIENT_ID, decoded.clientId());
        assertEquals(SESSION_ID, decoded.sessionId());
    }

    @Test
    void responseRoundTripSupportsListsSnapshotAndNullableFields() throws Exception {
        SessionSummaryDto summary = sessionSummary();
        ResponseMessage response =
                new ResponseMessage(
                        1,
                        MessageKind.RESPONSE,
                        REQUEST_ID,
                        CLIENT_ID,
                        SESSION_ID,
                        true,
                        null,
                        null,
                        summary,
                        List.of(summary),
                        snapshot(),
                        TIME);

        ResponseMessage decoded = codec.decode(codec.encode(response), ResponseMessage.class);

        assertEquals(response, decoded);
        assertTrue(decoded.success());
        assertNull(decoded.errorCode());
        assertNull(decoded.message());
    }

    @Test
    void eventWithNestedSnapshotRoundTripsWithoutARequestIdentifier() throws Exception {
        EventMessage event =
                new EventMessage(
                        1,
                        MessageKind.EVENT,
                        EVENT_ID,
                        SESSION_ID,
                        EventType.GAME_EVENT_BATCH,
                        12,
                        "Round completed",
                        List.of("Yellow rolled 6", "Yellow moved Y1"),
                        snapshot(),
                        TIME);

        EventMessage decoded = codec.decode(codec.encode(event), EventMessage.class);

        assertEquals(event, decoded);
        assertEquals(12, decoded.snapshot().version());
        assertEquals("Yellow", decoded.snapshot().players().getFirst().color());
    }

    @Test
    void instantUsesIsoTextRatherThanNumericTimestamp() throws Exception {
        String json =
                codec.encode(
                        RequestMessage.create(
                                REQUEST_ID,
                                CLIENT_ID,
                                RequestType.PING,
                                null,
                                null,
                                TIME));

        assertTrue(json.contains("\"sentAt\":\"2026-09-18T12:30:45Z\""));
        assertEquals(
                TIME, codec.decode(json, RequestMessage.class).sentAt());
    }

    @Test
    void logicalNewlineIsEscapedAndEncodeLineAddsExactlyOnePhysicalLf() throws Exception {
        ResponseMessage response =
                new ResponseMessage(
                        1,
                        MessageKind.RESPONSE,
                        REQUEST_ID,
                        CLIENT_ID,
                        null,
                        false,
                        ErrorCode.INVALID_REQUEST,
                        "first line\nsecond line",
                        null,
                        null,
                        null,
                        TIME);

        String encodedLine = codec.encodeLine(response);
        String json = encodedLine.substring(0, encodedLine.length() - 1);

        assertTrue(encodedLine.endsWith("\n"));
        assertEquals(1, encodedLine.chars().filter(character -> character == '\n').count());
        assertFalse(json.contains("\n"));
        assertFalse(json.contains("\r"));
        assertTrue(json.contains("\\n"));
        assertEquals(response, codec.decode(json, ResponseMessage.class));
    }

    @Test
    void compactOutputContainsNoPrettyPrintLineBreaks() throws Exception {
        String json = codec.encode(snapshot());

        assertFalse(json.contains("\n"));
        assertFalse(json.contains("\r"));
        assertTrue(json.startsWith("{"));
        assertTrue(json.endsWith("}"));
    }

    @Test
    void malformedJsonAndTrailingTokensAreRejectedClearly() {
        assertThrows(
                ProtocolException.class,
                () -> codec.decode("{not-json}", RequestMessage.class));
        assertThrows(
                ProtocolException.class,
                () -> codec.decode("{} {}", RequestMessage.class));
        assertThrows(
                ProtocolException.class,
                () -> codec.decode("", RequestMessage.class));
    }

    @Test
    void unsupportedOrIncorrectMessageKindIsRejected() throws Exception {
        RequestMessage request =
                RequestMessage.create(
                        REQUEST_ID,
                        CLIENT_ID,
                        RequestType.PING,
                        null,
                        Map.of(),
                        TIME);
        String valid = codec.encode(request);

        assertThrows(
                ProtocolException.class,
                () ->
                        codec.decode(
                                valid.replace("\"kind\":\"REQUEST\"", "\"kind\":\"UNKNOWN\""),
                                RequestMessage.class));
        assertThrows(
                ProtocolException.class,
                () ->
                        codec.decode(
                                valid.replace("\"kind\":\"REQUEST\"", "\"kind\":\"RESPONSE\""),
                                RequestMessage.class));
    }

    @Test
    void inputCollectionsAreDefensivelyCopiedAndExposedAsImmutable() {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("name", "Game One");
        RequestMessage request =
                RequestMessage.create(
                        REQUEST_ID,
                        CLIENT_ID,
                        RequestType.CREATE_SESSION,
                        null,
                        parameters,
                        TIME);

        List<GameSnapshotDto.PieceDto> pieces = new ArrayList<>();
        pieces.add(new GameSnapshotDto.PieceDto("Y1", "Yellow piece 1", "BASE"));
        GameSnapshotDto.PlayerDto player =
                new GameSnapshotDto.PlayerDto("Yellow", 0, 4, pieces);

        parameters.put("name", "Changed");
        pieces.add(new GameSnapshotDto.PieceDto("Y2", "Yellow piece 2", "BASE"));

        assertEquals("Game One", request.parameters().get("name"));
        assertEquals(1, player.pieces().size());
        assertThrows(
                UnsupportedOperationException.class,
                () -> request.parameters().put("extra", "value"));
        assertThrows(
                UnsupportedOperationException.class,
                () ->
                        player.pieces()
                                .add(
                                        new GameSnapshotDto.PieceDto(
                                                "Y3", "Yellow piece 3", "BASE")));
    }

    @Test
    void invalidResponseAndOversizedInputAreRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new ResponseMessage(
                                1,
                                MessageKind.RESPONSE,
                                REQUEST_ID,
                                CLIENT_ID,
                                null,
                                false,
                                null,
                                "missing error code",
                                null,
                                null,
                                null,
                                TIME));

        JsonLineCodec smallCodec = new JsonLineCodec(10);
        assertThrows(
                ProtocolException.class,
                () -> smallCodec.decode("12345678901", RequestMessage.class));
    }

    private SessionSummaryDto sessionSummary() {
        return new SessionSummaryDto(
                SESSION_ID, "Demo Game", SessionStatus.RUNNING, 2, 500, 12, TIME);
    }

    private GameSnapshotDto snapshot() {
        GameSnapshotDto.PieceDto piece =
                new GameSnapshotDto.PieceDto("Y1", "Yellow piece 1", "CELL_8");
        GameSnapshotDto.PlayerDto player =
                new GameSnapshotDto.PlayerDto("Yellow", 1, 3, List.of(piece));
        GameSnapshotDto.MysteryDto mystery = new GameSnapshotDto.MysteryDto(true, 20, 3);
        return new GameSnapshotDto(
                SESSION_ID, 12, 4, SessionStatus.RUNNING, List.of(player), mystery);
    }
}
