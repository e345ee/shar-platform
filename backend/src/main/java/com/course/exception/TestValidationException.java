package com.course.exception;

public class TestValidationException extends IllegalArgumentException {
    public TestValidationException(String message) {
        super(message);
    }
}
