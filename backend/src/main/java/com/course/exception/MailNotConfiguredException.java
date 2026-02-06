package com.course.exception;

/** Thrown when email sending is disabled or not configured. */
public class MailNotConfiguredException extends RuntimeException {
    public MailNotConfiguredException(String message) {
        super(message);
    }
}
