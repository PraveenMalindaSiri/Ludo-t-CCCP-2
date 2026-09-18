package protocol;

/** Operations a client may request from the server. */
public enum RequestType {
    CONNECT,
    PING,
    LIST_SESSIONS,
    CREATE_SESSION,
    JOIN_SESSION,
    LEAVE_SESSION,
    START_GAME,
    PAUSE_GAME,
    RESUME_GAME,
    STEP_GAME,
    STOP_GAME,
    SET_SPEED,
    GET_SNAPSHOT,
    DISCONNECT
}
