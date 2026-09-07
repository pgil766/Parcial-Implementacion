package edu.eia.racing.exception;

/**
 * Thrown when a business uniqueness rule is violated (duplicate username, email,
 * nickname, team name...). Mapped to HTTP 409 Conflict.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
