package com.sharif899.kyc.web.dto;

import com.sharif899.kyc.domain.ApplicationStatus;
import com.sharif899.kyc.domain.AuditLogEntry;
import com.sharif899.kyc.domain.OnboardingApplication;
import com.sharif899.kyc.service.DocumentMasking;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * What the API returns for an application. The document number is always
 * masked — only the last 4 digits are shown — and the full audit trail
 * is included so a reviewer can see every decision that was made.
 */
public record ApplicationResponse(
        UUID id,
        String fullName,
        LocalDate dateOfBirth,
        String phone,
        String email,
        String documentType,
        String maskedDocumentNumber,
        String issuingCountry,
        ApplicationStatus status,
        String verificationReason,
        String rejectionReason,
        List<AuditEntryResponse> auditTrail,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ApplicationResponse from(OnboardingApplication app) {
        return new ApplicationResponse(
                app.getId(),
                app.getCustomer().getFullName(),
                app.getCustomer().getDateOfBirth(),
                app.getCustomer().getPhone(),
                app.getCustomer().getEmail(),
                app.getDocument().getDocumentType().name(),
                DocumentMasking.mask(app.getDocument().getDocumentNumber()),
                app.getDocument().getIssuingCountry(),
                app.getStatus(),
                app.getVerificationReason(),
                app.getRejectionReason(),
                app.getAuditTrail().stream().map(AuditEntryResponse::from).toList(),
                app.getCreatedAt(),
                app.getUpdatedAt()
        );
    }

    public record AuditEntryResponse(
            ApplicationStatus fromStatus,
            ApplicationStatus toStatus,
            String actor,
            String reason,
            LocalDateTime timestamp
    ) {
        public static AuditEntryResponse from(AuditLogEntry entry) {
            return new AuditEntryResponse(
                    entry.getFromStatus(),
                    entry.getToStatus(),
                    entry.getActor(),
                    entry.getReason(),
                    entry.getTimestamp()
            );
        }
    }
}
