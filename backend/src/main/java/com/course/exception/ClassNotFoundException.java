package com.course.exception;

/**
 * Thrown when a study class is not found.
 */
public class ClassNotFoundException extends ResourceNotFoundException {
    public ClassNotFoundException(String message) {
        super(message);
    }

    public ClassNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
