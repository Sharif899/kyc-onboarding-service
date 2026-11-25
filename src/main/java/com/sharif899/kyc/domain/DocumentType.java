package com.sharif899.kyc.domain;

/** Types of identity document accepted for KYC verification. */
public enum DocumentType {
    /** Nigerian National Identity Number — 11 digits. */
    NIN,
    /** International passport — 1 letter followed by 8 digits. */
    PASSPORT,
    /** Driver's licence — 3 letters followed by 7 digits. */
    DRIVERS_LICENCE
}
