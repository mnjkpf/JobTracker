package com.jobtracker.backendJobTracker.application.parsing;

public class ParsingException extends RuntimeException {
 
    public ParsingException(String message) {
        super(message);
    }
 
    public ParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}

