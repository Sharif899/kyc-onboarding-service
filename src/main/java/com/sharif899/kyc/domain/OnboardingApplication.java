package com.sharif899.kyc.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The aggregate root: one customer's onboarding application, from creation
 * through verification to a terminal decision. Every state change is
 * recorded in {@link AuditLogEntry}, so the full history of an application
 * can be reconstructed from the database.
 */
@Entity
@Table(name = "onboarding_applications", uniqueConstraints = {
        @UniqueConstraint(name = "uk_document_number", columnNames = "document_number"),
        @UniqueConstraint(name = "uk_idempotency_key", columnNames = "idempotency_key")
})
public class OnboardingApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Embedded
    private Customer customer;

    @Embedded
    private IdentityDocument document;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApplicationStatus status = ApplicationStatus.PENDING;

    @Column(name = "idempotency_key", unique = true, length = 100)
    private String idempotencyKey;

    @Column(length = 500)
    private String verificationReason;

    @Column(length = 500)
    private String rejectionReason;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("timestamp ASC")
    private List<AuditLogEntry> auditTrail = new ArrayList<>();

    protected OnboardingApplication() {
    }

    public OnboardingApplication(Customer customer, IdentityDocument document, String idempotencyKey) {
        this.customer = customer;
        this.document = document;
        this.idempotencyKey = idempotencyKey;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Move to a new status, enforcing the state machine and recording the
     * change in the audit trail. This is the only way the status changes.
     */
    public void transitionTo(ApplicationStatus target, String actor, String reason) {
        ApplicationStateMachine.requireTransition(this.status, target);
        ApplicationStatus from = this.status;
        this.status = target;
        addAudit(from, target, actor, reason);
    }

    /** Record an audit entry without changing the status (creation). */
    public void addAudit(ApplicationStatus from, ApplicationStatus to, String actor, String reason) {
        auditTrail.add(new AuditLogEntry(this, from, to, actor, reason));
    }

    public UUID getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public IdentityDocument getDocument() {
        return document;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getVerificationReason() {
        return verificationReason;
    }

    public void setVerificationReason(String verificationReason) {
        this.verificationReason = verificationReason;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<AuditLogEntry> getAuditTrail() {
        return List.copyOf(auditTrail);
    }
}
