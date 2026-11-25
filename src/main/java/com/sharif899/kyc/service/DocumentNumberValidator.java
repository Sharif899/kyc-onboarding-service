package com.sharif899.kyc.service;

import com.sharif899.kyc.domain.DocumentType;

/**
 * Document-number format rules, one per document type. These are the
 * format checks a real KYC intake runs before any call to a verification
 * provider — a malformed number is rejected immediately, not sent onward.
 *
 * <p>The patterns are illustrative stand-ins for the real issuing
 * authorities' formats (Nigerian NIN is 11 digits; a Nigerian passport is
 * one letter followed by eight digits). They are deliberately strict:
 * a number that does not match its type's pattern is invalid.
 */
public final class DocumentNumberValidator {

    private DocumentNumberValidator() {
    }

    /**
     * Returns {@code null} when the number is valid for its type, otherwise
     * a human-readable reason it is invalid.
     */
    public static String validate(DocumentType type, String number) {
        if (number == null || number.isBlank()) {
            return "Document number must not be blank";
        }
        String normalised = number.trim().toUpperCase();
        boolean valid = switch (type) {
            case NIN -> normalised.matches("\\d{11}");
            case PASSPORT -> normalised.matches("[A-Z]\\d{8}");
            case DRIVERS_LICENCE -> normalised.matches("[A-Z]{3}\\d{7}");
        };
        if (valid) {
            return null;
        }
        return "Invalid document number format for " + type
                + ": expected " + expectedFormat(type) + ", got '" + number.trim() + "'";
    }

    public static boolean isValid(DocumentType type, String number) {
        return validate(type, number) == null;
    }

    public static String expectedFormat(DocumentType type) {
        return switch (type) {
            case NIN -> "11 digits (e.g. 12345678901)";
            case PASSPORT -> "1 letter followed by 8 digits (e.g. A12345678)";
            case DRIVERS_LICENCE -> "3 letters followed by 7 digits (e.g. ABC1234567)";
        };
    }

    /** Normalise a number before storage so duplicate checks are case-insensitive. */
    public static String normalise(String number) {
        return number == null ? null : number.trim().toUpperCase();
    }
}
