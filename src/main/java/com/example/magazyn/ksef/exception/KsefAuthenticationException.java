package com.example.magazyn.ksef.exception;

public class KsefAuthenticationException extends RuntimeException {
    public KsefAuthenticationException(String message) {
        super(message);
    }

    public KsefAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
