package com.example.sobzybackend.exceptions;

/**
 * Exception thrown when password doesn't match
 */
public class PasswordMismatchException extends AuthenticationException {
    public PasswordMismatchException() {
        super("Passwords do not match");
    }
}
