package com.course.exception;


public class TestAttemptTimeLimitExceededException extends TestAttemptValidationException {
    public TestAttemptTimeLimitExceededException(String message) {
        super(message);
    }
}
