package com.libraryos.repository;

import com.libraryos.model.user.Permission;

import java.util.List;
import java.util.Set;

public interface PermissionRepository {
    List<Permission> findAll();
    Set<Permission> findPermissionsByRoleId(int roleId);
    Set<Permission> findPermissionsByUserId(Long userId);
}
