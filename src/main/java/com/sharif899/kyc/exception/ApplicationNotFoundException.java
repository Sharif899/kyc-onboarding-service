package com.sharif899.kyc.exception;

import java.util.UUID;

/** No application exists with the given id. Mapped to HTTP 404. */
public class ApplicationNotFoundException extends RuntimeException {

    public ApplicationNotFoundException(UUID id) {
        super("No onboarding application found with id " + id);
    }
}
