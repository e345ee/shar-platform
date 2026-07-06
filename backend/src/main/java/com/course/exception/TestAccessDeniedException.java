package com.course.exception;

public class TestAccessDeniedException extends ForbiddenOperationException {
    public TestAccessDeniedException(String message) {
        super(message);
    }
}
