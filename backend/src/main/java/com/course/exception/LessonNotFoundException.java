package com.course.exception;

public class LessonNotFoundException extends ResourceNotFoundException {
    public LessonNotFoundException(String message) {
        super(message);
    }
}
