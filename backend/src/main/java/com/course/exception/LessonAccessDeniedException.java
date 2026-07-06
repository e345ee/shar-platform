package com.course.exception;

public class LessonAccessDeniedException extends ForbiddenOperationException {
    public LessonAccessDeniedException(String message) {
        super(message);
    }
}
