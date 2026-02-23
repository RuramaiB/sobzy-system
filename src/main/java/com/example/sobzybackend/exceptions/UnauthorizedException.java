package com.example.sobzybackend.exceptions;

/**
 * Exception thrown for unauthorized access
 */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException() {
        super("Unauthorized access");
    }
}
