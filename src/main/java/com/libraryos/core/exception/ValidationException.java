package com.libraryos.core.exception;

import java.util.Collections;
import java.util.List;

/**
 * Thrown when form input or business constraints fail validation.
 */
public class ValidationException extends DomainException {

    private final List<String> validationErrors;

    public ValidationException(String message) {
        super("VALIDATION_ERROR", message);
        this.validationErrors = Collections.singletonList(message);
    }

    public ValidationException(List<String> validationErrors) {
        super("VALIDATION_ERROR", String.join("; ", validationErrors));
        this.validationErrors = Collections.unmodifiableList(validationErrors);
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }
}
