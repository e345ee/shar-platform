package com.course.exception;

public class TestAttemptAccessDeniedException extends ForbiddenOperationException {
    public TestAttemptAccessDeniedException(String message) {
        super(message);
    }
}
