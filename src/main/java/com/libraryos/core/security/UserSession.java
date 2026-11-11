package com.libraryos.core.security;

import com.libraryos.model.user.PermissionCode;
import com.libraryos.model.user.RoleCode;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable session context for the currently authenticated user.
 */
public class UserSession {

    private final Long userId;
    private final String username;
    private final String fullName;
    private final String fullNameBn;
    private final Long memberId;
    private final Set<String> roles;
    private final Set<String> permissions;
    private final LocalDateTime loginTime;

    public UserSession(Long userId, String username, String fullName, String fullNameBn,
                       Long memberId, Set<String> roles, Set<String> permissions) {
        this.userId = Objects.requireNonNull(userId, "userId cannot be null");
        this.username = Objects.requireNonNull(username, "username cannot be null");
        this.fullName = Objects.requireNonNull(fullName, "fullName cannot be null");
        this.fullNameBn = fullNameBn;
        this.memberId = memberId;
        this.roles = roles != null ? Collections.unmodifiableSet(new HashSet<>(roles)) : Collections.emptySet();
        this.permissions = permissions != null ? Collections.unmodifiableSet(new HashSet<>(permissions)) : Collections.emptySet();
        this.loginTime = LocalDateTime.now();
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public String getFullNameBn() {
        return fullNameBn;
    }

    public Long getMemberId() {
        return memberId;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    public boolean hasPermission(PermissionCode permission) {
        if (permission == null) {
            return false;
        }
        return hasPermission(permission.name());
    }

    public boolean hasPermission(String permissionCode) {
        if (permissionCode == null || permissionCode.isBlank()) {
            return false;
        }
        return permissions.stream().anyMatch(p -> p.equalsIgnoreCase(permissionCode.trim()));
    }

    public boolean hasRole(RoleCode roleCode) {
        if (roleCode == null) {
            return false;
        }
        return hasRole(roleCode.name());
    }

    public boolean hasRole(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            return false;
        }
        return roles.stream().anyMatch(r -> r.equalsIgnoreCase(roleCode.trim()));
    }

    public boolean isSuperAdmin() {
        return hasRole(RoleCode.SUPER_ADMIN);
    }

    public boolean isMemberOnly() {
        return hasRole(RoleCode.MEMBER) && roles.size() == 1;
    }

    public String getPrimaryRoleName() {
        if (hasRole(RoleCode.SUPER_ADMIN)) return RoleCode.SUPER_ADMIN.getDisplayName();
        if (hasRole(RoleCode.LIBRARY_ADMIN)) return RoleCode.LIBRARY_ADMIN.getDisplayName();
        if (hasRole(RoleCode.LIBRARIAN)) return RoleCode.LIBRARIAN.getDisplayName();
        if (hasRole(RoleCode.ASSISTANT_LIBRARIAN)) return RoleCode.ASSISTANT_LIBRARIAN.getDisplayName();
        if (hasRole(RoleCode.MEMBER)) return RoleCode.MEMBER.getDisplayName();
        return roles.isEmpty() ? "User" : roles.iterator().next();
    }

    @Override
    public String toString() {
        return "UserSession{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", fullName='" + fullName + '\'' +
                ", roles=" + roles +
                '}';
    }
}
