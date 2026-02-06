package com.course.exception;

/**
 * Thrown when a student tries to perform an action that is allowed only after
 * teacher/methodist has explicitly closed the course for the student.
 */
public class CourseNotClosedException extends RuntimeException {
    public CourseNotClosedException(String message) {
        super(message);
    }
}
