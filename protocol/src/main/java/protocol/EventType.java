package protocol;

/** Unsolicited state changes pushed by the server. */
public enum EventType {
    SESSION_UPDATED,
    GAME_EVENT_BATCH,
    SESSION_COMPLETED,
    SESSION_STOPPED,
    SERVER_SHUTTING_DOWN
}
