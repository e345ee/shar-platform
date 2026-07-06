package com.course.exception;


public class ClassStudentAccessDeniedException extends ForbiddenOperationException {
    public ClassStudentAccessDeniedException(String message) {
        super(message);
    }
}
