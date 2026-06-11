package com.jobtracker.backendJobTracker.cv.tailored.export;


public class ExportException extends RuntimeException {
 
    public ExportException(String message) {
        super(message);
    }
 
    public ExportException(String message, Throwable cause) {
        super(message, cause);
    }
}

