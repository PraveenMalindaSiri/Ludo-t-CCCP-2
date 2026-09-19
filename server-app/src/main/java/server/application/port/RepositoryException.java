package server.application.port;

/** Reports a persistence failure without exposing JDBC types to the application layer. */
public final class RepositoryException extends RuntimeException {

    public RepositoryException(String message) {
        super(message);
    }

    public RepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
