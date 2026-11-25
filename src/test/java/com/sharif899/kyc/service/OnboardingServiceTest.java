package com.sharif899.kyc.service;

import com.sharif899.kyc.domain.ApplicationStatus;
import com.sharif899.kyc.domain.DocumentType;
import com.sharif899.kyc.domain.OnboardingApplication;
import com.sharif899.kyc.exception.ApplicationNotFoundException;
import com.sharif899.kyc.exception.DuplicateApplicationException;
import com.sharif899.kyc.exception.InvalidDocumentException;
import com.sharif899.kyc.exception.InvalidStateException;
import com.sharif899.kyc.exception.ValidationException;
import com.sharif899.kyc.repository.OnboardingApplicationRepository;
import com.sharif899.kyc.web.dto.CreateApplicationRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Service tests against the real JPA repositories on the in-memory H2
 * database. Each test runs in a transaction that is rolled back, so the
 * tests are independent of each other.
 */
@SpringBootTest
@Transactional
class OnboardingServiceTest {

    @Autowired
    private OnboardingService service;

    @Autowired
    private OnboardingApplicationRepository repository;

    private static CreateApplicationRequest request(String documentNumber) {
        return request(documentNumber, DocumentType.NIN);
    }

    private static CreateApplicationRequest request(String documentNumber, DocumentType type) {
        return new CreateApplicationRequest(
                "Adaeze Okonkwo",
                LocalDate.of(1994, 3, 14),
                "+2348066661895",
                "adaeze@example.com",
                type,
                documentNumber,
                "NG");
    }

    @Test
    void createReturnsPendingApplicationWithCreationAuditEntry() {
        var outcome = service.create(request("12345678901"), null);

        assertFalse(outcome.replayed());
        OnboardingApplication app = outcome.application();
        assertNotNull(app.getId());
        assertEquals(ApplicationStatus.PENDING, app.getStatus());
        assertEquals(1, app.getAuditTrail().size());
        assertEquals(ApplicationStatus.PENDING, app.getAuditTrail().get(0).getToStatus());
    }

    @Test
    void duplicateDocumentNumberReturns409WithExistingId() {
        var first = service.create(request("12345678901"), null);

        DuplicateApplicationException ex = assertThrows(DuplicateApplicationException.class,
                () -> service.create(request("12345678901"), null));
        assertEquals(first.application().getId(), ex.getExistingApplicationId());
        assertEquals(1, repository.count());
    }

    @Test
    void duplicateCheckIsCaseInsensitive() {
        service.create(request("A12345678", DocumentType.PASSPORT), null);

        assertThrows(DuplicateApplicationException.class,
                () -> service.create(request("a12345678", DocumentType.PASSPORT), null));
    }

    @Test
    void idempotentReplayReturnsOriginalWithoutCreatingAnother() {
        var first = service.create(request("12345678901"), "key-1");
        var replay = service.create(request("12345678902"), "key-1");

        assertTrue(replay.replayed());
        assertEquals(first.application().getId(), replay.application().getId());
        assertEquals(1, repository.count());
    }

    @Test
    void differentIdempotencyKeysCreateDifferentApplications() {
        var first = service.create(request("12345678901"), "key-1");
        var second = service.create(request("12345678902"), "key-2");

        assertFalse(second.replayed());
        assertEquals(2, repository.count());
        assertFalse(first.application().getId().equals(second.application().getId()));
    }

    @Test
    void invalidDocumentFormatIsRejected() {
        assertThrows(InvalidDocumentException.class,
                () -> service.create(request("12345"), null));
    }

    @Test
    void underageApplicantIsRejected() {
        var underage = new CreateApplicationRequest(
                "Young Applicant", LocalDate.now().minusYears(17),
                "+2348066661895", "young@example.com",
                DocumentType.NIN, "12345678901", "NG");

        assertThrows(ValidationException.class, () -> service.create(underage, null));
    }

    @Test
    void futureDateOfBirthIsRejected() {
        var future = new CreateApplicationRequest(
                "Future Person", LocalDate.now().plusDays(1),
                "+2348066661895", "future@example.com",
                DocumentType.NIN, "12345678901", "NG");

        assertThrows(ValidationException.class, () -> service.create(future, null));
    }

    @Test
    void verifyWithEvenEndingDocumentVerifies() {
        var created = service.create(request("12345678902"), null);

        OnboardingApplication verified = service.verify(created.application().getId(), null);

        assertEquals(ApplicationStatus.VERIFIED, verified.getStatus());
        assertNotNull(verified.getVerificationReason());
        assertEquals(2, verified.getAuditTrail().size());
    }

    @Test
    void verifyWithOddEndingDocumentGoesToUnderReview() {
        var created = service.create(request("12345678901"), null);

        OnboardingApplication reviewed = service.verify(created.application().getId(), null);

        assertEquals(ApplicationStatus.UNDER_REVIEW, reviewed.getStatus());
    }

    @Test
    void verifyingAnUnderReviewApplicationMovesItToNeedsMoreInfo() {
        var created = service.create(request("12345678901"), null);
        service.verify(created.application().getId(), null);

        OnboardingApplication needsMoreInfo = service.verify(created.application().getId(), null);

        assertEquals(ApplicationStatus.NEEDS_MORE_INFO, needsMoreInfo.getStatus());
        assertEquals(3, needsMoreInfo.getAuditTrail().size());
    }

    @Test
    void verifyingANeedsMoreInfoApplicationReturnsItToUnderReview() {
        var created = service.create(request("12345678901"), null);
        service.verify(created.application().getId(), null);
        service.verify(created.application().getId(), null);

        OnboardingApplication reviewed = service.verify(created.application().getId(), null);

        assertEquals(ApplicationStatus.UNDER_REVIEW, reviewed.getStatus());
    }

    @Test
    void rejectRequiresAReason() {
        var created = service.create(request("12345678901"), null);

        assertThrows(ValidationException.class,
                () -> service.reject(created.application().getId(), "   "));
    }

    @Test
    void rejectMovesToRejectedWithReasonAndAudit() {
        var created = service.create(request("12345678901"), null);

        OnboardingApplication rejected = service.reject(
                created.application().getId(), "Document photo is unreadable");

        assertEquals(ApplicationStatus.REJECTED, rejected.getStatus());
        assertEquals("Document photo is unreadable", rejected.getRejectionReason());
        assertEquals(2, rejected.getAuditTrail().size());
    }

    @Test
    void verifyOnVerifiedApplicationIsAnInvalidTransition() {
        var created = service.create(request("12345678902"), null);
        service.verify(created.application().getId(), null);

        assertThrows(InvalidStateException.class,
                () -> service.verify(created.application().getId(), null));
    }

    @Test
    void rejectOnVerifiedApplicationIsAnInvalidTransition() {
        var created = service.create(request("12345678902"), null);
        service.verify(created.application().getId(), null);

        assertThrows(InvalidStateException.class,
                () -> service.reject(created.application().getId(), "Too late"));
    }

    @Test
    void getByIdThrowsForUnknownId() {
        assertThrows(ApplicationNotFoundException.class, () -> service.getById(UUID.randomUUID()));
    }

    @Test
    void listByStatusFiltersCorrectly() {
        service.create(request("12345678901"), null);
        var verified = service.create(request("12345678902"), null);
        service.verify(verified.application().getId(), null);

        assertEquals(1, service.listByStatus(ApplicationStatus.PENDING).size());
        assertEquals(1, service.listByStatus(ApplicationStatus.VERIFIED).size());
        assertEquals(2, service.listAll().size());
    }
}
