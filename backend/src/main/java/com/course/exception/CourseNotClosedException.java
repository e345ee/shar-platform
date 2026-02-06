package com.course.exception;


public class CourseNotClosedException extends RuntimeException {
    public CourseNotClosedException(String message) {
        super(message);
    }
}
