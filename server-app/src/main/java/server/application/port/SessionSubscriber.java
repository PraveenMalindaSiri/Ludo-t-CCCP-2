package server.application.port;

import java.util.UUID;

/** Network-independent destination for session responses and later pushed events. */
public interface SessionSubscriber {

    UUID connectionId();

    boolean offer(Object message);

    default boolean isOpen() {
        return true;
    }
}
