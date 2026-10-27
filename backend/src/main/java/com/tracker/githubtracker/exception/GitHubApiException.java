package com.tracker.githubtracker.exception;

/**
 * Thrown for any non-404 failure talking to GitHub
 * (rate limit exceeded, network issue, unexpected response, etc.)
 */
public class GitHubApiException extends RuntimeException {

    public GitHubApiException(String message) {
        super(message);
    }

    public GitHubApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
