package edu.eia.racing.exception;

/**
 * Thrown when login or token refresh fails. Mapped to HTTP 401 Unauthorized.
 * The message must stay generic so it cannot be used to enumerate valid usernames.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
