package com.course.exception;

public class AchievementNotFoundException extends ResourceNotFoundException {
    public AchievementNotFoundException(String message) {
        super(message);
    }
}
