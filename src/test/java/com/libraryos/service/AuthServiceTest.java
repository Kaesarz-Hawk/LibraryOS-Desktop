package com.libraryos.service;

import com.libraryos.config.DatabaseConfig;
import com.libraryos.core.exception.AuthenticationException;
import com.libraryos.core.security.PasswordHasher;
import com.libraryos.core.security.SessionManager;
import com.libraryos.core.security.UserSession;
import com.libraryos.model.user.*;
import com.libraryos.repository.AuditLogRepository;
import com.libraryos.repository.PermissionRepository;
import com.libraryos.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private DataSource mockDataSource;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        DatabaseConfig.setDataSource(mockDataSource);
        authService = new AuthService(userRepository, permissionRepository, auditLogRepository);
        SessionManager.clearSession();
    }

    @AfterEach
    void tearDown() {
        SessionManager.clearSession();
    }

    private User createSampleUser(String username, String password, UserStatus status) {
        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        user.setPasswordHash(PasswordHasher.hash(password));
        user.setFullName("Test User");
        user.setFullNameBn("টেস্ট ইউজার");
        user.setStatus(status);
        user.setFailedLoginAttempts(0);

        Role adminRole = new Role(1, "SUPER_ADMIN", "Super Admin", "Super Admin Description");
        user.setRoles(Collections.singleton(adminRole));
        return user;
    }

    @Test
    @DisplayName("Successful login should return active UserSession and register it")
    void testSuccessfulLogin() {
        User user = createSampleUser("admin", "Admin@1234", UserStatus.ACTIVE);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        Permission perm = new Permission(1, "BOOK_VIEW", "Catalog", "View books");
        when(permissionRepository.findPermissionsByUserId(1L)).thenReturn(Collections.singleton(perm));

        UserSession session = authService.login("admin", "Admin@1234");
        assertNotNull(session);
        assertEquals("admin", session.getUsername());
        assertEquals(1L, session.getUserId());
        assertTrue(session.hasRole(RoleCode.SUPER_ADMIN));
        assertTrue(session.hasPermission(PermissionCode.BOOK_VIEW));
        assertTrue(SessionManager.isAuthenticated());

        verify(userRepository).resetFailedAttempts(1L);
        verify(userRepository).updateLastLogin(eq(1L), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Login with incorrect password should increment failed attempts")
    void testWrongPassword() {
        User user = createSampleUser("librarian", "Secret123", UserStatus.ACTIVE);
        when(userRepository.findByUsername("librarian")).thenReturn(Optional.of(user));

        AuthenticationException ex = assertThrows(AuthenticationException.class,
                () -> authService.login("librarian", "WrongPassword"));
        assertEquals("INVALID_CREDENTIALS", ex.getErrorCode());
        verify(userRepository).updateFailedAttempts(eq(1L), eq(1), isNull());
    }

    @Test
    @DisplayName("Login should be rejected for suspended accounts")
    void testSuspendedAccount() {
        User user = createSampleUser("suspended_user", "Secret123", UserStatus.SUSPENDED);
        when(userRepository.findByUsername("suspended_user")).thenReturn(Optional.of(user));

        AuthenticationException ex = assertThrows(AuthenticationException.class,
                () -> authService.login("suspended_user", "Secret123"));
        assertEquals("ACCOUNT_INACTIVE", ex.getErrorCode());
    }

    @Test
    @DisplayName("Login should be rejected for locked accounts")
    void testLockedAccount() {
        User user = createSampleUser("locked_user", "Secret123", UserStatus.ACTIVE);
        user.setLockedUntil(LocalDateTime.now().plusMinutes(10));
        when(userRepository.findByUsername("locked_user")).thenReturn(Optional.of(user));

        AuthenticationException ex = assertThrows(AuthenticationException.class,
                () -> authService.login("locked_user", "Secret123"));
        assertEquals("ACCOUNT_LOCKED", ex.getErrorCode());
    }

    @Test
    @DisplayName("Login should reject blank or null credentials")
    void testEmptyCredentials() {
        assertThrows(AuthenticationException.class, () -> authService.login("", "pass"));
        assertThrows(AuthenticationException.class, () -> authService.login("user", ""));
        assertThrows(AuthenticationException.class, () -> authService.login(null, null));
    }

    @Test
    @DisplayName("Logout should clear session context")
    void testLogout() {
        UserSession session = new UserSession(2L, "staff", "Staff User", null, null,
                Collections.singleton("LIBRARIAN"), Collections.emptySet());
        SessionManager.setSession(session);
        assertTrue(SessionManager.isAuthenticated());

        authService.logout();
        assertFalse(SessionManager.isAuthenticated());
        assertTrue(SessionManager.getCurrentSession().isEmpty());
    }
}
