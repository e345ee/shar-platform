package com.course.exception;


public class TeacherDeletionConflictException extends DuplicateResourceException {
    public TeacherDeletionConflictException(String message) {
        super(message);
    }
}
