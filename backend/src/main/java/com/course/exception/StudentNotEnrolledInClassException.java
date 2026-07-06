package com.course.exception;


public class StudentNotEnrolledInClassException extends ResourceNotFoundException {
    public StudentNotEnrolledInClassException(String message) {
        super(message);
    }

    public StudentNotEnrolledInClassException(String message, Throwable cause) {
        super(message, cause);
    }
}
