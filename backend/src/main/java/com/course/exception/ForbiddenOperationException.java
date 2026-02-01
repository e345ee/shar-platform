package com.course.exception;

/**
 * Thrown when a user attempts to perform an operation that is not allowed.
 * Mapped to HTTP 403 FORBIDDEN.
 */
public class ForbiddenOperationException extends RuntimeException {
    public ForbiddenOperationException(String message) {
        super(message);
    }
}
