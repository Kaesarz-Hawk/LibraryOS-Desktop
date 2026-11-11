package com.libraryos.core.exception;

/**
 * Thrown when database connectivity or low-level JDBC query execution fails.
 */
public class DatabaseException extends DomainException {

    public DatabaseException(String message) {
        super("DATABASE_ERROR", message);
    }

    public DatabaseException(String message, Throwable cause) {
        super("DATABASE_ERROR", message, cause);
    }
}
