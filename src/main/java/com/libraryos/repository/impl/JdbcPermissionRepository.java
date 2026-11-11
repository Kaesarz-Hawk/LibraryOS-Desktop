package com.libraryos.repository.impl;

import com.libraryos.config.DatabaseConfig;
import com.libraryos.core.exception.DatabaseException;
import com.libraryos.model.user.Permission;
import com.libraryos.repository.PermissionRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JdbcPermissionRepository implements PermissionRepository {

    @Override
    public List<Permission> findAll() {
        String sql = "SELECT id, code, module, description FROM permissions ORDER BY module ASC, code ASC";
        List<Permission> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch all permissions", e);
        }
        return list;
    }

    @Override
    public Set<Permission> findPermissionsByRoleId(int roleId) {
        String sql = "SELECT p.id, p.code, p.module, p.description FROM permissions p " +
                "INNER JOIN role_permissions rp ON p.id = rp.permission_id " +
                "WHERE rp.role_id = ?";
        Set<Permission> permissions = new HashSet<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    permissions.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch permissions for role id: " + roleId, e);
        }
        return permissions;
    }

    @Override
    public Set<Permission> findPermissionsByUserId(Long userId) {
        String sql = "SELECT DISTINCT p.id, p.code, p.module, p.description FROM permissions p " +
                "INNER JOIN role_permissions rp ON p.id = rp.permission_id " +
                "INNER JOIN user_roles ur ON rp.role_id = ur.role_id " +
                "WHERE ur.user_id = ?";
        Set<Permission> permissions = new HashSet<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    permissions.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch permissions for user id: " + userId, e);
        }
        return permissions;
    }

    private Permission mapRow(ResultSet rs) throws SQLException {
        return new Permission(
                rs.getInt("id"),
                rs.getString("code"),
                rs.getString("module"),
                rs.getString("description")
        );
    }
}

