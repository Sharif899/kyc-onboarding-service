package com.sharif899.kyc.domain;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import com.sharif899.kyc.exception.InvalidStateException;

/**
 * The allowed transitions of the onboarding state machine.
 *
 * <pre>
 *   PENDING ──┬──▶ UNDER_REVIEW ──┬──▶ VERIFIED (terminal)
 *             │                   ├──▶ REJECTED  (terminal)
 *             │                   └──▶ NEEDS_MORE_INFO ──┬──▶ UNDER_REVIEW
 *             ├──▶ VERIFIED                              ├──▶ VERIFIED
 *             └──▶ REJECTED                              └──▶ REJECTED
 * </pre>
 *
 * VERIFIED and REJECTED are terminal: no transition out of them is allowed.
 * Every transition is enforced in one place — {@link OnboardingApplication#transitionTo}
 * — so the rule cannot be bypassed by a new endpoint.
 */
public final class ApplicationStateMachine {

    private static final Map<ApplicationStatus, Set<ApplicationStatus>> ALLOWED;

    static {
        Map<ApplicationStatus, Set<ApplicationStatus>> allowed = new EnumMap<>(ApplicationStatus.class);
        allowed.put(ApplicationStatus.PENDING,
                Set.of(ApplicationStatus.UNDER_REVIEW, ApplicationStatus.VERIFIED, ApplicationStatus.REJECTED));
        allowed.put(ApplicationStatus.UNDER_REVIEW,
                Set.of(ApplicationStatus.VERIFIED, ApplicationStatus.REJECTED, ApplicationStatus.NEEDS_MORE_INFO));
        allowed.put(ApplicationStatus.NEEDS_MORE_INFO,
                Set.of(ApplicationStatus.UNDER_REVIEW, ApplicationStatus.VERIFIED, ApplicationStatus.REJECTED));
        allowed.put(ApplicationStatus.VERIFIED, Set.of());
        allowed.put(ApplicationStatus.REJECTED, Set.of());
        ALLOWED = Collections.unmodifiableMap(allowed);
    }

    private ApplicationStateMachine() {
    }

    public static boolean canTransition(ApplicationStatus from, ApplicationStatus to) {
        if (from == null || to == null) {
            return false;
        }
        return ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }

    public static void requireTransition(ApplicationStatus from, ApplicationStatus to) {
        if (!canTransition(from, to)) {
            throw new InvalidStateException(
                    "Cannot move an application from " + from + " to " + to);
        }
    }
}
