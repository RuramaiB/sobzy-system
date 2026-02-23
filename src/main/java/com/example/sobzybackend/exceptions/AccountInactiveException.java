package com.example.sobzybackend.exceptions;

/**
 * Exception thrown when account is inactive
 */
public class AccountInactiveException extends AuthenticationException {
    public AccountInactiveException() {
        super("Account is inactive. Please contact administrator.");
    }
}
