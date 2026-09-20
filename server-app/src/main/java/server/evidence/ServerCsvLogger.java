package server.evidence;

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

/** Bounded multi-producer/single-writer CSV evidence logger. */
public final class ServerCsvLogger implements AutoCloseable {

    private static final String HEADER =
            "request_id,client_id,session_id,request_type,received_at,queued_at,started_at,"
                    + "completed_at,queue_wait_ms,processing_ms,queue_depth,result,thread_name,"
                    + "is_virtual";
    private static final int FLUSH_EVERY = 50;

    private final BlockingQueue<ServerEvidenceRecord> records;
    private final BufferedWriter writer;
    private final AtomicBoolean closing = new AtomicBoolean();
    private final AtomicLong accepted = new AtomicLong();
    private final AtomicLong dropped = new AtomicLong();
    private final Thread writerThread;
    private final boolean enabled;

    private ServerCsvLogger() {
        records = null;
        writer = null;
        writerThread = null;
        enabled = false;
    }

    public ServerCsvLogger(Path file, int capacity) throws IOException {
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
        enabled = true;
        writerThread = Thread.ofVirtual().name("server-csv-writer").start(this::writeLoop);
    }

    public static ServerCsvLogger disabled() {
        return new ServerCsvLogger();
    }

    public boolean record(ServerEvidenceRecord record) {
        Objects.requireNonNull(record, "record");
        if (!enabled || closing.get()) {
            return !enabled;
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
        return writerThread == null || writerThread.isVirtual();
    }

    private void writeLoop() {
        int sinceFlush = 0;
        try {
            while (!closing.get() || !records.isEmpty()) {
                ServerEvidenceRecord record = records.poll(100, TimeUnit.MILLISECONDS);
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
            System.err.println("Server evidence writer failed: " + exception.getMessage());
        } finally {
            try {
                writer.close();
            } catch (IOException exception) {
                System.err.println(
                        "Unable to close server evidence CSV: " + exception.getMessage());
            }
        }
    }

    private void drainAfterInterrupt() {
        try {
            ServerEvidenceRecord record;
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
        if (!enabled || !closing.compareAndSet(false, true)) {
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
        if (dropped.get() > 0) {
            System.err.printf("Server evidence lost %d record(s)%n", dropped.get());
        }
    }

    public record ServerEvidenceRecord(
            UUID requestId,
            UUID clientId,
            UUID sessionId,
            RequestType requestType,
            Instant receivedAt,
            Instant queuedAt,
            Instant startedAt,
            Instant completedAt,
            long queueWaitMillis,
            long processingMillis,
            int queueDepth,
            String result,
            String threadName,
            boolean virtualThread) {

        public ServerEvidenceRecord {
            Objects.requireNonNull(requestId, "requestId");
            Objects.requireNonNull(clientId, "clientId");
            Objects.requireNonNull(requestType, "requestType");
            Objects.requireNonNull(receivedAt, "receivedAt");
            Objects.requireNonNull(queuedAt, "queuedAt");
            Objects.requireNonNull(startedAt, "startedAt");
            Objects.requireNonNull(completedAt, "completedAt");
            result = Objects.requireNonNullElse(result, "");
            threadName = Objects.requireNonNullElse(threadName, "");
            if (queueWaitMillis < 0 || processingMillis < 0 || queueDepth < 0) {
                throw new IllegalArgumentException("Timing and queue values must not be negative");
            }
        }

        String toCsv() {
            return CsvEncoder.row(
                    requestId,
                    clientId,
                    sessionId,
                    requestType,
                    receivedAt,
                    queuedAt,
                    startedAt,
                    completedAt,
                    queueWaitMillis,
                    processingMillis,
                    queueDepth,
                    result,
                    threadName,
                    virtualThread);
        }
    }
}
