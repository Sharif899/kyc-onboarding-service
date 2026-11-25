package com.sharif899.kyc.exception;

/** A request is invalid at the domain level (age, document format, missing reason). Mapped to HTTP 400. */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}
