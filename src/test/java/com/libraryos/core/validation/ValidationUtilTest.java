package com.libraryos.core.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValidationUtilTest {

    @Test
    @DisplayName("Should validate Bangladesh mobile numbers")
    void testBdPhoneValidation() {
        // Valid formats
        assertTrue(ValidationUtil.isValidBdPhone("01712345678"));
        assertTrue(ValidationUtil.isValidBdPhone("+8801812345678"));
        assertTrue(ValidationUtil.isValidBdPhone("8801912345678"));
        assertTrue(ValidationUtil.isValidBdPhone("01312345678"));
        assertTrue(ValidationUtil.isValidBdPhone("01412345678"));
        assertTrue(ValidationUtil.isValidBdPhone("01512345678"));
        assertTrue(ValidationUtil.isValidBdPhone("01612345678"));

        // Invalid formats
        assertFalse(ValidationUtil.isValidBdPhone("01212345678")); // Invalid operator prefix
        assertFalse(ValidationUtil.isValidBdPhone("0171234567"));  // Too short
        assertFalse(ValidationUtil.isValidBdPhone("017123456789")); // Too long
        assertFalse(ValidationUtil.isValidBdPhone("abcd"));
        assertFalse(ValidationUtil.isValidBdPhone(""));
        assertFalse(ValidationUtil.isValidBdPhone(null));
    }

    @Test
    @DisplayName("Should normalize Bangladesh phone numbers to 11 digits")
    void testNormalizePhone() {
        assertEquals("01712345678", ValidationUtil.normalizeBdPhone("+8801712345678"));
        assertEquals("01712345678", ValidationUtil.normalizeBdPhone("8801712345678"));
        assertEquals("01712345678", ValidationUtil.normalizeBdPhone("01712345678"));
    }

    @Test
    @DisplayName("Should validate ISBN-10 and ISBN-13 checksums")
    void testIsbnValidation() {
        // Valid ISBN-10
        assertTrue(ValidationUtil.isValidIsbn10("0-306-40615-2"));
        assertTrue(ValidationUtil.isValidIsbn10("0306406152"));

        // Valid ISBN-13
        assertTrue(ValidationUtil.isValidIsbn13("978-0-306-40615-7"));
        assertTrue(ValidationUtil.isValidIsbn13("9780306406157"));

        // Invalid checksum
        assertFalse(ValidationUtil.isValidIsbn10("0-306-40615-9"));
        assertFalse(ValidationUtil.isValidIsbn13("978-0-306-40615-0"));
    }
}

