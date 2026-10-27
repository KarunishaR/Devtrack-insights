package com.tracker.githubtracker.exception;

/**
 * Thrown when GitHub returns 404 for a given username,
 * i.e. the username does not exist on GitHub.
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String username) {
        super("GitHub user not found: " + username);
    }
}
