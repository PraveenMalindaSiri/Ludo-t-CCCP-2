package testclient;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import protocol.CsvEncoder;
import protocol.RequestType;

/** Bounded client evidence queue consumed by exactly one virtual CSV writer. */
public final class ClientCsvLogger implements AutoCloseable {

    private static final String HEADER =
            "client_id,request_id,session_id,request_type,sent_at,response_at,elapsed_ms,status";
    private static final int FLUSH_EVERY = 50;

    private final BlockingQueue<ClientEvidenceRecord> records;
    private final BufferedWriter writer;
    private final AtomicBoolean closing = new AtomicBoolean();
    private final AtomicLong accepted = new AtomicLong();
    private final AtomicLong dropped = new AtomicLong();
    private final Thread writerThread;

    public ClientCsvLogger(Path file, int capacity) throws IOException {
        Objects.requireNonNull(file, "file");
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        Path absolute = file.toAbsolutePath();
        Path parent = absolute.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        records = new ArrayBlockingQueue<>(capacity);
        writer =
                Files.newBufferedWriter(
                        absolute,
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE);
        writer.write(HEADER);
        writer.newLine();
        writer.flush();
        writerThread = Thread.ofVirtual().name("client-csv-writer").start(this::writeLoop);
    }

    public boolean record(ClientEvidenceRecord record) {
        Objects.requireNonNull(record, "record");
        if (closing.get()) {
            return false;
        }
        try {
            if (records.offer(record, 100, TimeUnit.MILLISECONDS)) {
                accepted.incrementAndGet();
                return true;
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
        dropped.incrementAndGet();
        return false;
    }

    public long acceptedCount() {
        return accepted.get();
    }

    public long droppedCount() {
        return dropped.get();
    }

    public boolean writerIsVirtual() {
        return writerThread.isVirtual();
    }

    private void writeLoop() {
        int sinceFlush = 0;
        try {
            while (!closing.get() || !records.isEmpty()) {
                ClientEvidenceRecord record = records.poll(100, TimeUnit.MILLISECONDS);
                if (record == null) {
                    continue;
                }
                writer.write(record.toCsv());
                sinceFlush++;
                if (sinceFlush >= FLUSH_EVERY) {
                    writer.flush();
                    sinceFlush = 0;
                }
            }
            writer.flush();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            drainAfterInterrupt();
        } catch (IOException exception) {
            dropped.addAndGet(1L + records.size());
            records.clear();
            System.err.println("Client evidence writer failed: " + exception.getMessage());
        } finally {
            try {
                writer.close();
            } catch (IOException exception) {
                System.err.println(
                        "Unable to close client evidence CSV: " + exception.getMessage());
            }
        }
    }

    private void drainAfterInterrupt() {
        try {
            ClientEvidenceRecord record;
            while ((record = records.poll()) != null) {
                writer.write(record.toCsv());
            }
            writer.flush();
        } catch (IOException exception) {
            dropped.addAndGet(1L + records.size());
            records.clear();
        }
    }

    @Override
    public void close() {
        if (!closing.compareAndSet(false, true)) {
            return;
        }
        try {
            writerThread.join(TimeUnit.SECONDS.toMillis(5));
            if (writerThread.isAlive()) {
                writerThread.interrupt();
                writerThread.join(TimeUnit.SECONDS.toMillis(1));
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            writerThread.interrupt();
        }
    }

    public record ClientEvidenceRecord(
            UUID clientId,
            UUID requestId,
            UUID sessionId,
            RequestType requestType,
            Instant sentAt,
            Instant responseAt,
            long elapsedMillis,
            String status) {

        public ClientEvidenceRecord {
            Objects.requireNonNull(clientId, "clientId");
            Objects.requireNonNull(requestId, "requestId");
            Objects.requireNonNull(requestType, "requestType");
            Objects.requireNonNull(sentAt, "sentAt");
            Objects.requireNonNull(responseAt, "responseAt");
            status = Objects.requireNonNullElse(status, "");
            if (elapsedMillis < 0) {
                throw new IllegalArgumentException("elapsedMillis must not be negative");
            }
        }

        String toCsv() {
            return CsvEncoder.row(
                    clientId,
                    requestId,
                    sessionId,
                    requestType,
                    sentAt,
                    responseAt,
                    elapsedMillis,
                    status);
        }
    }
}
