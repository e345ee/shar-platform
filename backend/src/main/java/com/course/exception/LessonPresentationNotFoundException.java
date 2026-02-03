package com.course.exception;

public class LessonPresentationNotFoundException extends ResourceNotFoundException {
    public LessonPresentationNotFoundException(String message) {
        super(message);
    }
}
