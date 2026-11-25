package com.sharif899.kyc.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One entry in an application's audit trail: who (which actor) moved it
 * from which status to which, when, and why. The {@code fromStatus} is
 * null for the creation entry.
 */
@Entity
@Table(name = "audit_log_entries")
public class AuditLogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private OnboardingApplication application;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ApplicationStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApplicationStatus toStatus;

    @Column(nullable = false, length = 50)
    private String actor;

    @Column(length = 500)
    private String reason;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    protected AuditLogEntry() {
    }

    public AuditLogEntry(OnboardingApplication application, ApplicationStatus fromStatus,
                         ApplicationStatus toStatus, String actor, String reason) {
        this.application = application;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.actor = actor;
        this.reason = reason;
        this.timestamp = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public OnboardingApplication getApplication() {
        return application;
    }

    public ApplicationStatus getFromStatus() {
        return fromStatus;
    }

    public ApplicationStatus getToStatus() {
        return toStatus;
    }

    public String getActor() {
        return actor;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
