package com.course.exception;

public class AchievementAlreadyAwardedException extends DuplicateResourceException {
    public AchievementAlreadyAwardedException(String message) {
        super(message);
    }
}
