package com.sharif899.kyc.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DocumentMaskingTest {

    @Test
    void masksAllButLastFourDigits() {
        assertEquals("*******8901", DocumentMasking.mask("12345678901"));
    }

    @Test
    void shortNumbersAreFullyMasked() {
        assertEquals("****", DocumentMasking.mask("1234"));
        assertEquals("**", DocumentMasking.mask("12"));
    }

    @Test
    void nullStaysNull() {
        assertNull(DocumentMasking.mask(null));
    }
}
