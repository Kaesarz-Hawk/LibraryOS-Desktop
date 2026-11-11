package com.libraryos.service;

import com.libraryos.config.AppConfig;
import com.libraryos.config.DatabaseConfig;
import com.libraryos.core.exception.AuthenticationException;
import com.libraryos.core.security.PasswordHasher;
import com.libraryos.core.security.SessionManager;
import com.libraryos.core.security.UserSession;
import com.libraryos.model.audit.AuditLog;
import com.libraryos.model.user.Permission;
import com.libraryos.model.user.Role;
import com.libraryos.model.user.User;
import com.libraryos.model.user.UserStatus;
import com.libraryos.repository.AuditLogRepository;
import com.libraryos.repository.PermissionRepository;
import com.libraryos.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;
    private final AuditLogRepository auditLogRepository;

    public AuthService(UserRepository userRepository,
                       PermissionRepository permissionRepository,
                       AuditLogRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.permissionRepository = permissionRepository;
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Authenticates a user, verifies BCrypt password hash, enforces account status/lockout rules,
     * builds UserSession, registers it with SessionManager, and records audit logs.
     */
    public UserSession login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new AuthenticationException("INVALID_INPUT", "Username and password must not be empty.");
        }

        if (!DatabaseConfig.isDbAvailable()) {
            throw new AuthenticationException("DB_OFFLINE",
                    "Database is currently unreachable. " +
                    (DatabaseConfig.getConnectionErrorDetail() != null ? DatabaseConfig.getConnectionErrorDetail() : "Please check your database settings."));
        }

        Optional<User> userOpt = userRepository.findByUsername(username.trim());
        if (userOpt.isEmpty()) {
            recordAudit(null, "LOGIN_FAILED", "USER", username, "Failed login attempt: username not found", "Bad credentials");
            throw new AuthenticationException("INVALID_CREDENTIALS", "Invalid username or password.");
        }

        User user = userOpt.get();

        // 1. Check account lock status
        if (!user.isAccountNonLocked()) {
            recordAudit(user.getId(), "LOGIN_BLOCKED", "USER", String.valueOf(user.getId()),
                    "Login attempt on locked account until " + user.getLockedUntil(), "Account locked");
            throw new AuthenticationException("ACCOUNT_LOCKED",
                    "Account is temporarily locked due to consecutive failed attempts. Please try again after " + user.getLockedUntil());
        }

        // 2. Check user status (ACTIVE, SUSPENDED, DISABLED)
        if (user.getStatus() != UserStatus.ACTIVE) {
            recordAudit(user.getId(), "LOGIN_REJECTED", "USER", String.valueOf(user.getId()),
                    "Login attempt on account with status " + user.getStatus(), "Account inactive");
            throw new AuthenticationException("ACCOUNT_INACTIVE",
                    "Account is currently " + user.getStatus().name() + ". Please contact your library administrator.");
        }

        // 3. Verify BCrypt password hash
        boolean passwordMatches = PasswordHasher.checkPassword(password, user.getPasswordHash());
        if (!passwordMatches) {
            int newAttempts = user.getFailedLoginAttempts() + 1;
            int maxAllowed = AppConfig.getMaxFailedLoginAttempts();
            LocalDateTime lockedUntil = null;

            if (newAttempts >= maxAllowed) {
                int lockoutMinutes = AppConfig.getLockoutMinutes();
                lockedUntil = LocalDateTime.now().plusMinutes(lockoutMinutes);
                log.warn("User '{}' reached {} failed attempts. Locking account for {} minutes.", username, newAttempts, lockoutMinutes);
            }

            userRepository.updateFailedAttempts(user.getId(), newAttempts, lockedUntil);
            recordAudit(user.getId(), "LOGIN_FAILED", "USER", String.valueOf(user.getId()),
                    "Failed login attempt (" + newAttempts + "/" + maxAllowed + ")", "Wrong password");

            if (lockedUntil != null) {
                throw new AuthenticationException("ACCOUNT_LOCKED",
                        "Maximum login attempts exceeded. Account is locked for " + AppConfig.getLockoutMinutes() + " minutes.");
            } else {
                throw new AuthenticationException("INVALID_CREDENTIALS", "Invalid username or password.");
            }
        }

        // 4. Reset failed attempts & update last login
        userRepository.resetFailedAttempts(user.getId());
        LocalDateTime now = LocalDateTime.now();
        userRepository.updateLastLogin(user.getId(), now);

        // 5. Gather roles and all permission codes
        Set<String> roleCodes = user.getRoles().stream()
                .map(Role::getCode)
                .collect(Collectors.toSet());

        Set<String> permissionCodes = new HashSet<>();
        if (permissionRepository != null) {
            Set<Permission> perms = permissionRepository.findPermissionsByUserId(user.getId());
            for (Permission p : perms) {
                permissionCodes.add(p.getCode());
            }
        }
        // Also add role-embedded permissions if present
        for (Role role : user.getRoles()) {
            for (Permission p : role.getPermissions()) {
                permissionCodes.add(p.getCode());
            }
        }

        // 6. Build session and set into SessionManager
        UserSession session = new UserSession(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getFullNameBn(),
                user.getMemberId(),
                roleCodes,
                permissionCodes
        );

        SessionManager.setSession(session);
        recordAudit(user.getId(), "LOGIN_SUCCESS", "USER", String.valueOf(user.getId()),
                "User '" + user.getUsername() + "' logged in successfully", "Normal login");

        log.info("User '{}' successfully authenticated with roles: {}", user.getUsername(), roleCodes);
        return session;
    }

    /**
     * Clears current session context and records logout audit log.
     */
    public void logout() {
        SessionManager.getCurrentSession().ifPresent(session -> {
            recordAudit(session.getUserId(), "LOGOUT", "USER", String.valueOf(session.getUserId()),
                    "User '" + session.getUsername() + "' logged out", "Normal logout");
            log.info("User '{}' logged out.", session.getUsername());
        });
        SessionManager.clearSession();
    }

    private void recordAudit(Long actorUserId, String action, String entityType, String entityId, String summary, String reason) {
        if (auditLogRepository != null && DatabaseConfig.isDbAvailable()) {
            try {
                auditLogRepository.insert(new AuditLog(actorUserId, action, entityType, entityId, summary, reason));
            } catch (Exception e) {
                log.warn("Failed recording audit log: {}", e.getMessage());
            }
        }
    }
}

