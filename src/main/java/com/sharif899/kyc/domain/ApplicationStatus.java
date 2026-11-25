package com.sharif899.kyc.domain;

/**
 * Lifecycle of an onboarding application.
 *
 * <ul>
 *   <li>{@code PENDING} — created, awaiting verification.</li>
 *   <li>{@code UNDER_REVIEW} — the verification provider could not decide
 *       automatically; a human reviewer must decide.</li>
 *   <li>{@code VERIFIED} — identity verified. Terminal.</li>
 *   <li>{@code REJECTED} — application rejected. Terminal.</li>
 *   <li>{@code NEEDS_MORE_INFO} — the reviewer asked the applicant for more
 *       information; the application can be verified again afterwards.</li>
 * </ul>
 */
public enum ApplicationStatus {
    PENDING,
    UNDER_REVIEW,
    VERIFIED,
    REJECTED,
    NEEDS_MORE_INFO
}
