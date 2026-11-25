package com.sharif899.kyc.service;

import com.sharif899.kyc.domain.OnboardingApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Deterministic stand-in for a real verification provider. It applies
 * fixed, documented rules so the whole verification flow can be exercised
 * end to end without a live identity registry:
 *
 * <ul>
 *   <li>Document number ending in an <b>even</b> digit — the document
 *       matches the issuing authority's record: {@code VERIFIED}.</li>
 *   <li>Document number ending in an <b>odd</b> digit — the provider
 *       cannot match it automatically: {@code NEEDS_REVIEW}, and the
 *       application moves to {@code UNDER_REVIEW} for a human.</li>
 *   <li>Document number not ending in a digit — {@code NEEDS_REVIEW}.</li>
 * </ul>
 *
 * <p>This is intentionally simple and honest: it is a stand-in, not a
 * real identity check. Wiring a real provider means implementing
 * {@link VerificationProvider} and registering that bean instead.
 * Note the log line masks the document number — full numbers are never
 * written to logs.
 */
@Component
public class MockVerificationProvider implements VerificationProvider {

    private static final Logger log = LoggerFactory.getLogger(MockVerificationProvider.class);

    @Override
    public VerificationResult verify(OnboardingApplication application) {
        String number = application.getDocument().getDocumentNumber();
        log.info("Mock verification for application {} (document {})",
                application.getId(), DocumentMasking.mask(number));

        char last = number.charAt(number.length() - 1);
        if (!Character.isDigit(last)) {
            return new VerificationResult(VerificationResult.Decision.NEEDS_REVIEW,
                    "Document number does not end in a digit; manual review required");
        }
        int digit = last - '0';
        if (digit % 2 == 0) {
            return new VerificationResult(VerificationResult.Decision.VERIFIED,
                    "Document matched the issuing authority record (mock provider)");
        }
        return new VerificationResult(VerificationResult.Decision.NEEDS_REVIEW,
                "Document could not be matched automatically; referred for manual review (mock provider)");
    }
}
