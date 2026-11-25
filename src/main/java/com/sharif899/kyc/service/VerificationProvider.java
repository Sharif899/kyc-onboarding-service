package com.sharif899.kyc.service;

import com.sharif899.kyc.domain.OnboardingApplication;

/**
 * A verification provider — the external identity-check service (NIMC,
 * a passport registry, a third-party KYC vendor) behind the verify step.
 * Kept as an interface so the mock below can be swapped for a real
 * provider without touching the service or the API.
 */
public interface VerificationProvider {

    VerificationResult verify(OnboardingApplication application);
}
