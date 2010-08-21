package com.ima.security;

public final class AuthenticationException extends RuntimeException {
    public AuthenticationException() {
        super("valid API credentials are required");
    }
}
