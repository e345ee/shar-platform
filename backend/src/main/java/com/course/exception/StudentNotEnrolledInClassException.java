package com.course.exception;

/**
 * Thrown when a student is not enrolled in a class.
 */
public class StudentNotEnrolledInClassException extends ResourceNotFoundException {
    public StudentNotEnrolledInClassException(String message) {
        super(message);
    }

    public StudentNotEnrolledInClassException(String message, Throwable cause) {
        super(message, cause);
    }
}
