package com.jobtracker.backendJobTracker.exception;

public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }

}
