package com.libraryos.core.exception;

/**
 * Thrown when credentials fail verification, or user account is locked/suspended.
 */
public class AuthenticationException extends DomainException {

    public AuthenticationException(String message) {
        super("AUTH_FAILED", message);
    }

    public AuthenticationException(String errorCode, String message) {
        super(errorCode, message);
    }
}
