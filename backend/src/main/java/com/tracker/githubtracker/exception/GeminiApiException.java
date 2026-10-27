package com.tracker.githubtracker.exception;

/** Thrown when the Gemini AI call fails, or isn't configured. */
public class GeminiApiException extends RuntimeException {

    public GeminiApiException(String message) {
        super(message);
    }

    public GeminiApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
