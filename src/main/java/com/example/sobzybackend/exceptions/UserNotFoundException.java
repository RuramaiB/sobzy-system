package com.example.sobzybackend.exceptions;

/**
 * Exception thrown when user is not found
 */
public class UserNotFoundException extends AuthenticationException {
    public UserNotFoundException(String username) {
        super("User not found: " + username);
    }
}
