package com.course.exception;

/**
 * Thrown when a user tries to perform an operation on class members
 * without sufficient permissions.
 */
public class ClassStudentAccessDeniedException extends ForbiddenOperationException {
    public ClassStudentAccessDeniedException(String message) {
        super(message);
    }
}
