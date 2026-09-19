package server.network;

import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import protocol.BoundedLineReader;
import protocol.EventMessage;
import protocol.JsonLineCodec;
import protocol.MessageKind;
import protocol.RequestMessage;
import protocol.RequestType;
import protocol.ResponseMessage;

/** Small synchronous test probe that understands interleaved responses and pushed events. */
final class PushTestClient implements AutoCloseable {

    private final UUID clientId = UUID.randomUUID();
    private final JsonLineCodec codec = new JsonLineCodec();
    private final Socket socket;
    private final BufferedWriter writer;
    private final BoundedLineReader reader;

    PushTestClient(int port) throws Exception {
        socket = new Socket("127.0.0.1", port);
        socket.setSoTimeout(3_000);
        writer =
                new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        reader =
                new BoundedLineReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8),
                        JsonLineCodec.DEFAULT_MAX_LINE_LENGTH);
        exchange(request(RequestType.CONNECT, null, Map.of()));
    }

    RequestMessage request(RequestType type, UUID sessionId, Map<String, String> parameters) {
        return RequestMessage.create(
                UUID.randomUUID(), clientId, type, sessionId, parameters, Instant.now());
    }

    ResponseMessage exchange(RequestMessage request) throws Exception {
        writer.write(codec.encodeLine(request));
        writer.flush();
        while (true) {
            String line = reader.readLine();
            if (line == null) throw new AssertionError("Connection closed before response");
            if (codec.decodeKind(line) == MessageKind.RESPONSE) {
                ResponseMessage response = codec.decode(line, ResponseMessage.class);
                if (response.requestId().equals(request.requestId())) return response;
            }
        }
    }

    EventMessage readSnapshotEvent(long minimumVersion) throws Exception {
        while (true) {
            String line = reader.readLine();
            if (line == null) throw new AssertionError("Connection closed before snapshot push");
            if (codec.decodeKind(line) == MessageKind.EVENT) {
                EventMessage event = codec.decode(line, EventMessage.class);
                if (event.snapshot() != null && event.version() >= minimumVersion) return event;
            }
        }
    }

    @Override
    public void close() throws Exception {
        socket.close();
    }
}
