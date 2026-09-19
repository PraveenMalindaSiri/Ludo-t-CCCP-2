package server.session;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import protocol.SessionSummaryDto;
import server.application.port.SessionSubscriber;

/** Thread-safe ownership and lookup of independent server game sessions. */
public final class SessionRegistry implements AutoCloseable {

    private final ConcurrentMap<UUID, GameSession> sessions = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();
    private final int queueCapacity;
    private final long defaultTurnDelayMillis;

    public SessionRegistry(int queueCapacity, long defaultTurnDelayMillis) {
        if (queueCapacity <= 0 || defaultTurnDelayMillis <= 0) {
            throw new IllegalArgumentException("Queue capacity and turn delay must be positive");
        }
        this.queueCapacity = queueCapacity;
        this.defaultTurnDelayMillis = defaultTurnDelayMillis;
    }

    public GameSession create(String requestedName, long seed) {
        long number = sequence.incrementAndGet();
        String name =
                requestedName == null || requestedName.isBlank()
                        ? "Game-" + number
                        : requestedName.trim();
        UUID id = UUID.randomUUID();
        GameSession session =
                new GameSession(id, name, seed, queueCapacity, defaultTurnDelayMillis);
        GameSession previous = sessions.putIfAbsent(id, session);
        if (previous != null) {
            session.close();
            throw new IllegalStateException("Generated duplicate session identifier");
        }
        try {
            session.awaitReady();
            return session;
        } catch (RuntimeException failure) {
            sessions.remove(id, session);
            session.close();
            throw failure;
        }
    }

    public GameSession find(UUID sessionId) {
        return sessionId == null ? null : sessions.get(sessionId);
    }

    public List<SessionSummaryDto> list() {
        return sessions.values().stream()
                .map(GameSession::summary)
                .sorted(Comparator.comparing(SessionSummaryDto::createdAt))
                .toList();
    }

    public boolean remove(UUID sessionId) {
        GameSession removed = sessions.remove(sessionId);
        if (removed == null) {
            return false;
        }
        removed.close();
        return true;
    }

    public int size() {
        return sessions.size();
    }

    public void removeSubscriber(SessionSubscriber subscriber) {
        sessions.values().forEach(session -> session.removeSubscriber(subscriber));
    }

    @Override
    public void close() {
        sessions.values().forEach(GameSession::close);
        sessions.clear();
    }
}
