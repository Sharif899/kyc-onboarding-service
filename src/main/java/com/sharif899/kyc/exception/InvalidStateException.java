package com.sharif899.kyc.exception;

/** The requested state transition is not allowed by the state machine. Mapped to HTTP 409. */
public class InvalidStateException extends RuntimeException {

    public InvalidStateException(String message) {
        super(message);
    }
}
