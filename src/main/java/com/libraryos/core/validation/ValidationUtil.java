package com.libraryos.core.validation;

import java.util.regex.Pattern;

/**
 * Validation utilities for Bangladesh localized data inputs and general catalog integrity.
 */
public final class ValidationUtil {

    private static final Pattern BD_PHONE_PATTERN = Pattern.compile("^(?:\\+?88)?01[3-9]\\d{8}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private ValidationUtil() {
    }

    public static boolean isValidBdPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return false;
        }
        String sanitized = phone.replaceAll("[\\s-]", "");
        return BD_PHONE_PATTERN.matcher(sanitized).matches();
    }

    public static String normalizeBdPhone(String phone) {
        if (phone == null) return null;
        String sanitized = phone.replaceAll("[\\s-]", "");
        if (sanitized.startsWith("+88")) {
            sanitized = sanitized.substring(3);
        } else if (sanitized.startsWith("88")) {
            sanitized = sanitized.substring(2);
        }
        return sanitized;
    }

    public static boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    public static boolean isValidIsbn10(String isbn) {
        if (isbn == null) return false;
        String clean = isbn.replaceAll("[\\s-]", "");
        if (clean.length() != 10) return false;

        int sum = 0;
        for (int i = 0; i < 9; i++) {
            char c = clean.charAt(i);
            if (!Character.isDigit(c)) return false;
            sum += (c - '0') * (10 - i);
        }
        char last = clean.charAt(9);
        if (last == 'X' || last == 'x') {
            sum += 10;
        } else if (Character.isDigit(last)) {
            sum += (last - '0');
        } else {
            return false;
        }
        return (sum % 11 == 0);
    }

    public static boolean isValidIsbn13(String isbn) {
        if (isbn == null) return false;
        String clean = isbn.replaceAll("[\\s-]", "");
        if (clean.length() != 13) return false;

        int sum = 0;
        for (int i = 0; i < 12; i++) {
            char c = clean.charAt(i);
            if (!Character.isDigit(c)) return false;
            sum += (c - '0') * (i % 2 == 0 ? 1 : 3);
        }
        char last = clean.charAt(12);
        if (!Character.isDigit(last)) return false;
        int check = 10 - (sum % 10);
        if (check == 10) check = 0;
        return check == (last - '0');
    }
}
