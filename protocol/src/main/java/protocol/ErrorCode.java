package protocol;

/** Stable error categories that clients can handle without parsing message text. */
public enum ErrorCode {
    MALFORMED_MESSAGE,
    INVALID_REQUEST,
    INVALID_STATE,
    UNKNOWN_SESSION,
    QUEUE_FULL,
    UNSUPPORTED_PROTOCOL,
    SERVER_ERROR
}
