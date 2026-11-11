package com.libraryos.core.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordHasherTest {

    @Test
    @DisplayName("Should hash password and verify successfully")
    void testHashAndVerify() {
        String raw = "Admin@1234";
        String hashed = PasswordHasher.hash(raw);

        assertNotNull(hashed);
        assertTrue(hashed.startsWith("$2a$12$") || hashed.startsWith("$2b$12$") || hashed.startsWith("$2y$12$"),
                "Expected BCrypt format with cost factor 12, got: " + hashed);

        assertTrue(PasswordHasher.checkPassword(raw, hashed));
        assertFalse(PasswordHasher.checkPassword("WrongPassword", hashed));
    }

    @Test
    @DisplayName("Should reject empty or null passwords")
    void testEmptyPassword() {
        assertThrows(IllegalArgumentException.class, () -> PasswordHasher.hash(""));
        assertThrows(IllegalArgumentException.class, () -> PasswordHasher.hash(null));
        assertFalse(PasswordHasher.checkPassword(null, "somehash"));
        assertFalse(PasswordHasher.checkPassword("pwd", null));
    }
}

