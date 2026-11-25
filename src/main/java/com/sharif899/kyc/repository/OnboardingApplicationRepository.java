package com.sharif899.kyc.repository;

import com.sharif899.kyc.domain.ApplicationStatus;
import com.sharif899.kyc.domain.OnboardingApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OnboardingApplicationRepository extends JpaRepository<OnboardingApplication, UUID> {

    /** Duplicate detection: one document number may only ever have one application. */
    Optional<OnboardingApplication> findByDocumentDocumentNumber(String documentNumber);

    /** Idempotency: a replayed Idempotency-Key returns the original application. */
    Optional<OnboardingApplication> findByIdempotencyKey(String idempotencyKey);

    List<OnboardingApplication> findByStatusOrderByCreatedAtDesc(ApplicationStatus status);
}
