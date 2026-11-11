package com.libraryos.core.security;

import com.libraryos.core.exception.UnauthorizedException;
import com.libraryos.model.user.PermissionCode;
import com.libraryos.model.user.RoleCode;

import java.util.Arrays;
import java.util.Objects;

/**
 * Service-layer security enforcer for RBAC permissions and member data isolation.
 */
public final class PermissionGuard {

    private PermissionGuard() {
    }

    /**
     * Asserts that the current session holds the specified permission.
     * Throws UnauthorizedException if not authenticated or permission missing.
     */
    public static void requirePermission(PermissionCode permission) {
        Objects.requireNonNull(permission, "Permission cannot be null");
        requirePermission(permission.name());
    }

    /**
     * Asserts that the current session holds the specified permission code string.
     */
    public static void requirePermission(String permissionCode) {
        UserSession session = SessionManager.requireCurrentSession();
        if (!session.hasPermission(permissionCode)) {
            throw new UnauthorizedException(permissionCode,
                    "Access denied. User '" + session.getUsername() + "' does not possess required permission: " + permissionCode);
        }
    }

    /**
     * Asserts that the current session holds at least one of the given permissions.
     */
    public static void requireAnyPermission(PermissionCode... permissions) {
        if (permissions == null || permissions.length == 0) {
            return;
        }
        UserSession session = SessionManager.requireCurrentSession();
        for (PermissionCode perm : permissions) {
            if (session.hasPermission(perm)) {
                return;
            }
        }
        String required = Arrays.toString(permissions);
        throw new UnauthorizedException(required,
                "Access denied. User '" + session.getUsername() + "' lacks any of the required permissions: " + required);
    }

    /**
     * Enforces member data isolation:
     * If the current user has only the MEMBER role, they may only view or mutate
     * records where memberId matches their linked member ID.
     */
    public static void requireMemberIsolation(Long targetMemberId) {
        UserSession session = SessionManager.requireCurrentSession();
        if (session.isMemberOnly()) {
            Long linkedMemberId = session.getMemberId();
            if (linkedMemberId == null || !linkedMemberId.equals(targetMemberId)) {
                throw new UnauthorizedException("MEMBER_ISOLATION_VIOLATION",
                        "Security violation: Member '" + session.getUsername() + "' is prohibited from accessing other member data.");
            }
        }
    }

    /**
     * Asserts that the current user has the specified role.
     */
    public static void requireRole(RoleCode roleCode) {
        UserSession session = SessionManager.requireCurrentSession();
        if (!session.hasRole(roleCode)) {
            throw new UnauthorizedException(roleCode.name(),
                    "Access denied. User '" + session.getUsername() + "' is not assigned required role: " + roleCode.name());
        }
    }
}
