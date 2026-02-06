package com.course.exception;


public class MailNotConfiguredException extends RuntimeException {
    public MailNotConfiguredException(String message) {
        super(message);
    }
}
