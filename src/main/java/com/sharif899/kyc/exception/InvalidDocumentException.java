package com.sharif899.kyc.exception;

/** The document number does not match the format for its document type. Mapped to HTTP 400. */
public class InvalidDocumentException extends ValidationException {

    public InvalidDocumentException(String message) {
        super(message);
    }
}
