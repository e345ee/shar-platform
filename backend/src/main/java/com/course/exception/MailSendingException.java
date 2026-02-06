package com.course.exception;

/** Thrown when an SMTP provider rejects the message or sending fails. */
public class MailSendingException extends RuntimeException {
    public MailSendingException(String message, Throwable cause) {
        super(message, cause);
    }
}
