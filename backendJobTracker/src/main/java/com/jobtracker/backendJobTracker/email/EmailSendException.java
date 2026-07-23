package com.jobtracker.backendJobTracker.email;

public class EmailSendException extends RuntimeException {
    public EmailSendException(String message, Throwable cause) {
        super(message, cause);
    }

}
