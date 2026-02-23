package com.example.sobzybackend.exceptions;

/**
 * Exception thrown when user already exists
 */
public class UserAlreadyExistsException extends AuthenticationException {
    public UserAlreadyExistsException(String field, String value) {
        super("User already exists with " + field + ": " + value);
    }
}
