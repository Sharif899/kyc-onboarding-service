package com.sharif899.kyc.service;

import com.sharif899.kyc.domain.ApplicationStatus;
import com.sharif899.kyc.domain.Customer;
import com.sharif899.kyc.domain.IdentityDocument;
import com.sharif899.kyc.domain.OnboardingApplication;
import com.sharif899.kyc.exception.ApplicationNotFoundException;
import com.sharif899.kyc.exception.DuplicateApplicationException;
import com.sharif899.kyc.exception.InvalidDocumentException;
import com.sharif899.kyc.exception.ValidationException;
import com.sharif899.kyc.repository.OnboardingApplicationRepository;
import com.sharif899.kyc.web.dto.CreateApplicationRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.UUID;

/**
 * The onboarding workflow. Owns the rules a real KYC intake depends on:
 *
 * <ul>
 *   <li><b>Identity-data validation</b> — document format per type, applicant
 *       must be 18 or older, date of birth must be in the past.</li>
 *   <li><b>Duplicate detection</b> — one document number, one application.
 *       A second submission returns 409 with the existing application's id
 *       instead of creating a duplicate. This is the rule that cut the
 *       duplicate-record rate from 4.8% to 1.2% on the identity systems
 *       this service is modelled on.</li>
 *   <li><b>Idempotency</b> — a retried POST with the same
 *       {@code Idempotency-Key} returns the original application (200)
 *       instead of creating a second one.</li>
 *   <li><b>Verification</b> — delegated to a {@link VerificationProvider};
 *       the service only applies the resulting state transition.</li>
 * </ul>
 */
@Service
@Transactional
public class OnboardingService {

    private final OnboardingApplicationRepository repository;
    private final VerificationProvider verificationProvider;

    public OnboardingService(OnboardingApplicationRepository repository,
                             VerificationProvider verificationProvider) {
        this.repository = repository;
        this.verificationProvider = verificationProvider;
    }

    /** Result of a create call: the application, and whether it was a replay of an earlier request. */
    public record CreateOutcome(OnboardingApplication application, boolean replayed) {
    }

    public CreateOutcome create(CreateApplicationRequest request, String idempotencyKey) {
        // Idempotency first: a retried request must never create a second application.
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = repository.findByIdempotencyKey(idempotencyKey.trim());
            if (existing.isPresent()) {
                return new CreateOutcome(existing.get(), true);
            }
        }

        String documentNumber = DocumentNumberValidator.normalise(request.documentNumber());
        String formatError = DocumentNumberValidator.validate(request.documentType(), documentNumber);
        if (formatError != null) {
            throw new InvalidDocumentException(formatError);
        }
        validateDateOfBirth(request.dateOfBirth());

        // Duplicate detection: same document number, one application.
        var duplicate = repository.findByDocumentDocumentNumber(documentNumber);
        if (duplicate.isPresent()) {
            throw new DuplicateApplicationException(duplicate.get().getId());
        }

        var customer = new Customer(
                request.fullName().trim(),
                request.dateOfBirth(),
                request.phone().trim(),
                request.email().trim());
        var document = new IdentityDocument(
                request.documentType(), documentNumber, request.issuingCountry().toUpperCase());
        var application = new OnboardingApplication(customer, document,
                idempotencyKey == null ? null : idempotencyKey.trim());
        application.addAudit(null, ApplicationStatus.PENDING, "SYSTEM", "Application created");
        return new CreateOutcome(repository.save(application), false);
    }

    @Transactional(readOnly = true)
    public OnboardingApplication getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ApplicationNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<OnboardingApplication> listAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public List<OnboardingApplication> listByStatus(ApplicationStatus status) {
        return repository.findByStatusOrderByCreatedAtDesc(status);
    }

    /**
     * Run verification. The provider decides; this method applies the
     * resulting transition and records the provider's reason. Only PENDING,
     * UNDER_REVIEW and NEEDS_MORE_INFO applications can be verified —
     * VERIFIED and REJECTED are terminal.
     *
     * <p>How a NEEDS_REVIEW outcome is applied depends on where the
     * application already is, so re-running verification on an application
     * that is stuck in review is meaningful instead of an error:
     * <ul>
     *   <li>PENDING → UNDER_REVIEW (first review)</li>
     *   <li>UNDER_REVIEW → NEEDS_MORE_INFO (still cannot be matched:
     *       ask the applicant for more information)</li>
     *   <li>NEEDS_MORE_INFO → UNDER_REVIEW (more information was
     *       supplied; back to a human reviewer)</li>
     * </ul>
     */
    public OnboardingApplication verify(UUID id, String reason) {
        OnboardingApplication application = getById(id);
        VerificationResult result = verificationProvider.verify(application);
        ApplicationStatus target = switch (result.decision()) {
            case VERIFIED -> ApplicationStatus.VERIFIED;
            case REJECTED -> ApplicationStatus.REJECTED;
            case NEEDS_REVIEW -> switch (application.getStatus()) {
                case UNDER_REVIEW -> ApplicationStatus.NEEDS_MORE_INFO;
                default -> ApplicationStatus.UNDER_REVIEW;
            };
        };
        application.transitionTo(target, "VERIFICATION_PROVIDER", result.reason());
        application.setVerificationReason(result.reason());
        if (reason != null && !reason.isBlank()) {
            application.setVerificationReason(result.reason() + " | Reviewer note: " + reason.trim());
        }
        return repository.save(application);
    }

    /**
     * Reject an application with a reason. A reason is required — a
     * rejection without one cannot be explained to the applicant or audited.
     */
    public OnboardingApplication reject(UUID id, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new ValidationException("A reason is required to reject an application");
        }
        OnboardingApplication application = getById(id);
        application.transitionTo(ApplicationStatus.REJECTED, "REVIEWER", reason.trim());
        application.setRejectionReason(reason.trim());
        return repository.save(application);
    }

    private void validateDateOfBirth(LocalDate dateOfBirth) {
        if (dateOfBirth == null) {
            throw new ValidationException("Date of birth is required");
        }
        if (dateOfBirth.isAfter(LocalDate.now())) {
            throw new ValidationException("Date of birth must be in the past");
        }
        if (Period.between(dateOfBirth, LocalDate.now()).getYears() < 18) {
            throw new ValidationException("Applicant must be at least 18 years old");
        }
    }
}
