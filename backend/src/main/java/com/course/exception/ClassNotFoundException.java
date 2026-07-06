package com.course.exception;


public class ClassNotFoundException extends ResourceNotFoundException {
    public ClassNotFoundException(String message) {
        super(message);
    }

    public ClassNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
