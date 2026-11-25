package com.sharif899.kyc.service;

import com.sharif899.kyc.domain.ApplicationStateMachine;
import com.sharif899.kyc.domain.ApplicationStatus;
import com.sharif899.kyc.exception.InvalidStateException;
import org.junit.jupiter.api.Test;

import static com.sharif899.kyc.domain.ApplicationStatus.NEEDS_MORE_INFO;
import static com.sharif899.kyc.domain.ApplicationStatus.PENDING;
import static com.sharif899.kyc.domain.ApplicationStatus.REJECTED;
import static com.sharif899.kyc.domain.ApplicationStatus.UNDER_REVIEW;
import static com.sharif899.kyc.domain.ApplicationStatus.VERIFIED;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationStateMachineTest {

    @Test
    void pendingCanBeVerifiedRejectedOrSentToReview() {
        assertTrue(ApplicationStateMachine.canTransition(PENDING, VERIFIED));
        assertTrue(ApplicationStateMachine.canTransition(PENDING, REJECTED));
        assertTrue(ApplicationStateMachine.canTransition(PENDING, UNDER_REVIEW));
    }

    @Test
    void pendingCannotGoStraightToNeedsMoreInfo() {
        assertFalse(ApplicationStateMachine.canTransition(PENDING, NEEDS_MORE_INFO));
    }

    @Test
    void underReviewCanBeDecidedOrSentBackForMoreInfo() {
        assertTrue(ApplicationStateMachine.canTransition(UNDER_REVIEW, VERIFIED));
        assertTrue(ApplicationStateMachine.canTransition(UNDER_REVIEW, REJECTED));
        assertTrue(ApplicationStateMachine.canTransition(UNDER_REVIEW, NEEDS_MORE_INFO));
    }

    @Test
    void needsMoreInfoCanBeVerifiedAgain() {
        assertTrue(ApplicationStateMachine.canTransition(NEEDS_MORE_INFO, VERIFIED));
        assertTrue(ApplicationStateMachine.canTransition(NEEDS_MORE_INFO, UNDER_REVIEW));
    }

    @Test
    void verifiedIsTerminal() {
        assertFalse(ApplicationStateMachine.canTransition(VERIFIED, PENDING));
        assertFalse(ApplicationStateMachine.canTransition(VERIFIED, REJECTED));
        assertFalse(ApplicationStateMachine.canTransition(VERIFIED, UNDER_REVIEW));
    }

    @Test
    void rejectedIsTerminal() {
        assertFalse(ApplicationStateMachine.canTransition(REJECTED, VERIFIED));
        assertFalse(ApplicationStateMachine.canTransition(REJECTED, PENDING));
    }

    @Test
    void requireTransitionThrowsForIllegalMoves() {
        assertThrows(InvalidStateException.class,
                () -> ApplicationStateMachine.requireTransition(VERIFIED, REJECTED));
        assertThrows(InvalidStateException.class,
                () -> ApplicationStateMachine.requireTransition(PENDING, NEEDS_MORE_INFO));
    }
}
