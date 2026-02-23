package com.example.sobzybackend.exceptions;

/**
 * Exception thrown when current password is incorrect
 */
public class IncorrectPasswordException extends AuthenticationException {
    public IncorrectPasswordException() {
        super("Current password is incorrect");
    }
}
