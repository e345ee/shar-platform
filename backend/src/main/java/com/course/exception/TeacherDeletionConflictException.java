package com.course.exception;

/**
 * SRS 3.1.2 alternative flow: a teacher cannot be deleted while still assigned
 * to existing (active) classes. The UI should propose selecting another teacher.
 */
public class TeacherDeletionConflictException extends DuplicateResourceException {
    public TeacherDeletionConflictException(String message) {
        super(message);
    }
}
