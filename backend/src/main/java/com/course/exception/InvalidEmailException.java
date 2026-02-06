package com.course.exception;

/** Thrown when a user's email is missing or has invalid format. */
public class InvalidEmailException extends RuntimeException {
    public InvalidEmailException(String message) {
        super(message);
    }
}
