package com.course.exception;

public class LessonValidationException extends IllegalArgumentException {
    public LessonValidationException(String message) {
        super(message);
    }
}
