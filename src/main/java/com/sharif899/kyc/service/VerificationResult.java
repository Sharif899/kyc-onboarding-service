package com.sharif899.kyc.service;

import com.sharif899.kyc.domain.OnboardingApplication;

/**
 * The outcome of asking a verification provider to check an application.
 * The provider never writes to the database — it only returns a decision
 * and a reason, and the service applies the state transition.
 */
public record VerificationResult(Decision decision, String reason) {

    public enum Decision {
        /** The document matched the issuing authority's record. */
        VERIFIED,
        /** The provider could not decide automatically; a human must review. */
        NEEDS_REVIEW,
        /** The document failed verification. */
        REJECTED
    }
}
