package com.example.sobzybackend.exceptions;


/**
 * Base exception for authentication-related errors
 */
public class AuthenticationException extends RuntimeException {
    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}

/**
 * Exception thrown when credentials are invalid
 */
class InvalidCredentialsException extends AuthenticationException {
    public InvalidCredentialsException() {
        super("Invalid username or password");
    }

    public InvalidCredentialsException(String message) {
        super(message);
    }
}

/**
 * Exception thrown when account is locked
 */
class AccountLockedException extends AuthenticationException {
    public AccountLockedException() {
        super("Account is locked. Please contact administrator.");
    }

    public AccountLockedException(String message) {
        super(message);
    }
}

/**
 * Exception thrown for bad request errors
 */
class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}

