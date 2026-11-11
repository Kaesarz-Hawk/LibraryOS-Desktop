package com.libraryos.core.security;

import com.libraryos.model.user.PermissionCode;
import com.libraryos.model.user.RoleCode;

import java.util.Optional;

/**
 * Thread-safe holder for the authenticated application session context.
 */
public final class SessionManager {

    private static volatile UserSession currentSession;

    private SessionManager() {
    }

    public static synchronized void setSession(UserSession session) {
        currentSession = session;
    }

    public static synchronized void clearSession() {
        currentSession = null;
    }

    public static Optional<UserSession> getCurrentSession() {
        return Optional.ofNullable(currentSession);
    }

    public static UserSession requireCurrentSession() {
        UserSession session = currentSession;
        if (session == null) {
            throw new com.libraryos.core.exception.UnauthorizedException("No active user session. Please log in.");
        }
        return session;
    }

    public static boolean isAuthenticated() {
        return currentSession != null;
    }

    public static boolean hasPermission(PermissionCode permission) {
        UserSession session = currentSession;
        return session != null && session.hasPermission(permission);
    }

    public static boolean hasPermission(String permissionCode) {
        UserSession session = currentSession;
        return session != null && session.hasPermission(permissionCode);
    }

    public static boolean hasRole(RoleCode roleCode) {
        UserSession session = currentSession;
        return session != null && session.hasRole(roleCode);
    }

    public static Long getCurrentUserId() {
        UserSession session = currentSession;
        return session != null ? session.getUserId() : null;
    }
}
