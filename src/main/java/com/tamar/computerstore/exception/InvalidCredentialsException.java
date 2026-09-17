package com.tamar.computerstore.exception;

import org.springframework.security.core.AuthenticationException;

/**
 * Deliberately carries one message for every failure mode (unknown email, wrong password,
 * disabled account) so the response cannot be used to enumerate registered addresses.
 */
public class InvalidCredentialsException extends AuthenticationException {

    public static final String MESSAGE = "Invalid email or password";

    public InvalidCredentialsException() {
        super(MESSAGE);
    }
}
