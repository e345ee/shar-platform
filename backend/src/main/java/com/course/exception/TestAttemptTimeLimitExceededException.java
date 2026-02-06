package com.course.exception;

/**
 * Thrown when a student tries to submit a CONTROL_WORK attempt after its per-attempt time limit.
 */
public class TestAttemptTimeLimitExceededException extends TestAttemptValidationException {
    public TestAttemptTimeLimitExceededException(String message) {
        super(message);
    }
}
