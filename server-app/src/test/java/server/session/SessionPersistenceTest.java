package server.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import protocol.RequestMessage;
import protocol.RequestType;
import protocol.SessionStatus;
import server.application.port.GameRepository;

class SessionPersistenceTest {

    @Test
    void persistsCreationAndLifecycleChanges() throws Exception {
        RecordingRepository repository = new RecordingRepository();
        try (SessionRegistry registry = new SessionRegistry(16, 10_000, repository)) {
            GameSession session = registry.create("Persistent", 77L);
            TestSubscriber subscriber = new TestSubscriber();

            submit(session, subscriber, RequestType.CREATE_SESSION);
            submit(session, subscriber, RequestType.START_GAME);
            submit(session, subscriber, RequestType.PAUSE_GAME);
            submit(session, subscriber, RequestType.STOP_GAME);

            assertEquals(SessionStatus.CREATED, repository.created.status());
            assertEquals(SessionStatus.STOPPED, repository.updates.getLast().status());
        }
    }

    @Test
    void orderlyShutdownPersistsInterruptedStatus() throws Exception {
        RecordingRepository repository = new RecordingRepository();
        SessionRegistry registry = new SessionRegistry(16, 10_000, repository);
        GameSession session = registry.create("Interrupted", 88L);
        TestSubscriber subscriber = new TestSubscriber();
        submit(session, subscriber, RequestType.CREATE_SESSION);
        submit(session, subscriber, RequestType.START_GAME);

        registry.close();

        assertEquals(SessionStatus.INTERRUPTED, repository.updates.getLast().status());
    }

    private static void submit(
            GameSession session, TestSubscriber subscriber, RequestType requestType)
            throws Exception {
        RequestMessage request =
                RequestMessage.create(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        requestType,
                        session.sessionId(),
                        Map.of(),
                        Instant.now());
        assertTrue(session.submit(request, subscriber, System.nanoTime()));
        assertTrue(subscriber.takeResponse().success());
    }

    private static final class RecordingRepository implements GameRepository {

        private final List<SessionRecord> updates = new CopyOnWriteArrayList<>();
        private volatile SessionRecord created;

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public void validateConnection() {}

        @Override
        public void createSession(SessionRecord session) {
            created = session;
        }

        @Override
        public void updateSession(SessionRecord session) {
            updates.add(session);
        }

        @Override
        public void saveCompletedResult(SessionRecord session, GameResultRecord result) {
            updates.add(session);
        }

        @Override
        public int markUnfinishedSessionsInterrupted(Instant interruptedAt) {
            return 0;
        }

        @Override
        public void close() {}
    }
}
