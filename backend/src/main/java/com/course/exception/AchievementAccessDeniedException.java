package com.course.exception;

public class AchievementAccessDeniedException extends ForbiddenOperationException {
    public AchievementAccessDeniedException(String message) {
        super(message);
    }
}
