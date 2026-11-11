package com.libraryos.core.security;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Enterprise password hashing utility using BCrypt with salt rounds = 12.
 */
public final class PasswordHasher {

    private static final int BCRYPT_LOG_ROUNDS = 12;

    private PasswordHasher() {
    }

    /**
     * Hashes a plain-text password with a freshly generated salt (work factor 12).
     */
    public static String hash(String plainTextPassword) {
        if (plainTextPassword == null || plainTextPassword.isBlank()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        return BCrypt.hashpw(plainTextPassword, BCrypt.gensalt(BCRYPT_LOG_ROUNDS));
    }

    /**
     * Verifies whether a plain-text password matches a stored BCrypt hash.
     */
    public static boolean checkPassword(String plainTextPassword, String hashedPassword) {
        if (plainTextPassword == null || hashedPassword == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainTextPassword, hashedPassword);
        } catch (IllegalArgumentException e) {
            // Invalid salt or corrupted hash
            return false;
        }
    }
}
