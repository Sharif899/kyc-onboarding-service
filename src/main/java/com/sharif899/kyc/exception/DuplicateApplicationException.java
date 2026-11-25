package com.sharif899.kyc.exception;

import java.util.UUID;

/**
 * An application already exists for the submitted document number.
 * Mapped to HTTP 409. Carries the existing application's id so the caller
 * can fetch it instead of creating a duplicate.
 */
public class DuplicateApplicationException extends RuntimeException {

    private final UUID existingApplicationId;

    public DuplicateApplicationException(UUID existingApplicationId) {
        super("An application already exists for this document number");
        this.existingApplicationId = existingApplicationId;
    }

    public UUID getExistingApplicationId() {
        return existingApplicationId;
    }
}
