package com.course.exception;

public class AchievementValidationException extends IllegalArgumentException {
    public AchievementValidationException(String message) {
        super(message);
    }
}
