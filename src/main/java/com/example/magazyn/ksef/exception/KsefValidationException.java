package com.example.magazyn.ksef.exception;

import java.util.List;

public class KsefValidationException extends RuntimeException {
    private final List<String> errors;

    public KsefValidationException(List<String> errors) {
        super("KSeF validation failed: " + String.join("; ", errors));
        this.errors = errors;
    }

    public List<String> getErrors() {
        return errors;
    }
}
