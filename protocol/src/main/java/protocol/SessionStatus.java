package protocol;

/** Lifecycle states exposed across the client-server boundary. */
public enum SessionStatus {
    CREATED,
    RUNNING,
    PAUSED,
    COMPLETED,
    STOPPED,
    INTERRUPTED
}
