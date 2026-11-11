package com.libraryos.core.security;

import com.libraryos.core.exception.UnauthorizedException;
import com.libraryos.model.user.PermissionCode;
import com.libraryos.model.user.RoleCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PermissionGuardTest {

    @BeforeEach
    void setUp() {
        SessionManager.clearSession();
    }

    @AfterEach
    void tearDown() {
        SessionManager.clearSession();
    }

    @Test
    @DisplayName("Should pass when user has required permission")
    void testHasPermission() {
        Set<String> perms = new HashSet<>();
        perms.add("BOOK_VIEW");
        perms.add("BOOK_CREATE");

        UserSession session = new UserSession(1L, "admin", "Admin", null, null,
                Collections.singleton("SUPER_ADMIN"), perms);
        SessionManager.setSession(session);

        assertDoesNotThrow(() -> PermissionGuard.requirePermission(PermissionCode.BOOK_VIEW));
        assertDoesNotThrow(() -> PermissionGuard.requirePermission("BOOK_CREATE"));
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when permission is missing")
    void testMissingPermission() {
        Set<String> perms = Collections.singleton("BOOK_VIEW");
        UserSession session = new UserSession(2L, "staff", "Staff", null, null,
                Collections.singleton("ASSISTANT_LIBRARIAN"), perms);
        SessionManager.setSession(session);

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> PermissionGuard.requirePermission(PermissionCode.SETTINGS_MANAGE));
        assertEquals("SETTINGS_MANAGE", ex.getRequiredPermission());
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when not authenticated")
    void testUnauthenticated() {
        assertThrows(UnauthorizedException.class,
                () -> PermissionGuard.requirePermission(PermissionCode.BOOK_VIEW));
    }

    @Test
    @DisplayName("Should pass when user holds at least one required permission")
    void testRequireAnyPermission() {
        Set<String> perms = Collections.singleton("LOAN_RETURN");
        UserSession session = new UserSession(3L, "clerk", "Clerk", null, null,
                Collections.singleton("LIBRARIAN"), perms);
        SessionManager.setSession(session);

        assertDoesNotThrow(() -> PermissionGuard.requireAnyPermission(
                PermissionCode.LOAN_ISSUE, PermissionCode.LOAN_RETURN));

        assertThrows(UnauthorizedException.class, () -> PermissionGuard.requireAnyPermission(
                PermissionCode.BOOK_CREATE, PermissionCode.USER_MANAGE));
    }

    @Test
    @DisplayName("Should enforce member isolation strictly")
    void testMemberIsolation() {
        // Staff user can access any member id
        UserSession staffSession = new UserSession(10L, "librarian", "Librarian", null, null,
                Collections.singleton("LIBRARIAN"), Collections.emptySet());
        SessionManager.setSession(staffSession);
        assertDoesNotThrow(() -> PermissionGuard.requireMemberIsolation(999L));

        // Member user can only access own member id
        UserSession memberSession = new UserSession(20L, "student1", "Student One", null, 101L,
                Collections.singleton("MEMBER"), Collections.emptySet());
        SessionManager.setSession(memberSession);

        assertDoesNotThrow(() -> PermissionGuard.requireMemberIsolation(101L));

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> PermissionGuard.requireMemberIsolation(102L));
        assertEquals("MEMBER_ISOLATION_VIOLATION", ex.getRequiredPermission());
    }
}

