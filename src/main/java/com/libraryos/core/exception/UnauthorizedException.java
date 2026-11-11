package com.libraryos.core.exception;

/**
 * Thrown when an authenticated user attempts an operation without having the required permission
 * or violates member data isolation boundaries.
 */
public class UnauthorizedException extends DomainException {

    private final String requiredPermission;

    public UnauthorizedException(String message) {
        super("ACCESS_DENIED", message);
        this.requiredPermission = null;
    }

    public UnauthorizedException(String requiredPermission, String message) {
        super("ACCESS_DENIED", message);
        this.requiredPermission = requiredPermission;
    }

    public String getRequiredPermission() {
        return requiredPermission;
    }
}
