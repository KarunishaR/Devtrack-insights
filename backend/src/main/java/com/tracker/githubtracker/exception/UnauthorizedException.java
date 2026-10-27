package com.tracker.githubtracker.exception;

/**
 * Thrown when GitHub (or something intercepting the request, like a
 * corporate proxy) returns 401 Unauthorized.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
