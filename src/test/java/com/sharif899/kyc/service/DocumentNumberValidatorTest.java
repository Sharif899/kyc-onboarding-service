package com.sharif899.kyc.service;

import com.sharif899.kyc.domain.DocumentType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentNumberValidatorTest {

    @Test
    void ninElevenDigitsIsValid() {
        assertTrue(DocumentNumberValidator.isValid(DocumentType.NIN, "12345678901"));
        assertNull(DocumentNumberValidator.validate(DocumentType.NIN, "12345678901"));
    }

    @Test
    void ninWithTenDigitsIsInvalid() {
        assertFalse(DocumentNumberValidator.isValid(DocumentType.NIN, "1234567890"));
    }

    @Test
    void ninWithLettersIsInvalid() {
        assertFalse(DocumentNumberValidator.isValid(DocumentType.NIN, "1234567890A"));
    }

    @Test
    void passportLetterPlusEightDigitsIsValid() {
        assertTrue(DocumentNumberValidator.isValid(DocumentType.PASSPORT, "A12345678"));
    }

    @Test
    void passportWithoutLeadingLetterIsInvalid() {
        assertFalse(DocumentNumberValidator.isValid(DocumentType.PASSPORT, "123456789"));
    }

    @Test
    void driversLicenceThreeLettersPlusSevenDigitsIsValid() {
        assertTrue(DocumentNumberValidator.isValid(DocumentType.DRIVERS_LICENCE, "ABC1234567"));
    }

    @Test
    void driversLicenceWithWrongShapeIsInvalid() {
        assertFalse(DocumentNumberValidator.isValid(DocumentType.DRIVERS_LICENCE, "AB1234567"));
    }

    @Test
    void blankNumberIsInvalidWithAUsefulMessage() {
        String error = DocumentNumberValidator.validate(DocumentType.NIN, "   ");
        assertEquals("Document number must not be blank", error);
    }

    @Test
    void lowerCaseNumbersAreNormalisedBeforeChecking() {
        assertTrue(DocumentNumberValidator.isValid(DocumentType.PASSPORT, "a12345678"));
        assertEquals("A12345678", DocumentNumberValidator.normalise("  a12345678 "));
    }
}
