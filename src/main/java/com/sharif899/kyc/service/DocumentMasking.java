package com.sharif899.kyc.service;

/**
 * Document numbers are sensitive personal data. They are stored in full
 * (a KYC audit store must be able to prove what was verified), but they
 * are never returned in full: API responses and logs only ever show the
 * masked form.
 */
public final class DocumentMasking {

    private DocumentMasking() {
    }

    /**
     * Shows only the last 4 characters, e.g. {@code 12345678901} becomes
     * {@code *******8901}. Numbers of 4 characters or fewer are fully masked.
     */
    public static String mask(String documentNumber) {
        if (documentNumber == null) {
            return null;
        }
        String number = documentNumber.trim();
        if (number.length() <= 4) {
            return "*".repeat(number.length());
        }
        return "*".repeat(number.length() - 4) + number.substring(number.length() - 4);
    }
}
