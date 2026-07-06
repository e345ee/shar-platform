package com.course.exception;

public class TestQuestionNotFoundException extends ResourceNotFoundException {
    public TestQuestionNotFoundException(String message) {
        super(message);
    }
}
