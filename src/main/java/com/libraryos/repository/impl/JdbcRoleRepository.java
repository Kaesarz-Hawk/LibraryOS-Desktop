package com.libraryos.repository.impl;

import com.libraryos.config.DatabaseConfig;
import com.libraryos.core.exception.DatabaseException;
import com.libraryos.model.user.Permission;
import com.libraryos.model.user.Role;
import com.libraryos.repository.PermissionRepository;
import com.libraryos.repository.RoleRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class JdbcRoleRepository implements RoleRepository {

    private final PermissionRepository permissionRepository;

    public JdbcRoleRepository(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    @Override
    public List<Role> findAll() {
        String sql = "SELECT id, code, name, description FROM roles ORDER BY id ASC";
        List<Role> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Role role = mapRow(rs);
                if (permissionRepository != null) {
                    role.setPermissions(permissionRepository.findPermissionsByRoleId(role.getId()));
                }
                list.add(role);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch all roles", e);
        }
        return list;
    }

    @Override
    public Optional<Role> findById(int id) {
        String sql = "SELECT id, code, name, description FROM roles WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Role role = mapRow(rs);
                    if (permissionRepository != null) {
                        role.setPermissions(permissionRepository.findPermissionsByRoleId(role.getId()));
                    }
                    return Optional.of(role);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find role by id: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Role> findByCode(String code) {
        String sql = "SELECT id, code, name, description FROM roles WHERE code = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Role role = mapRow(rs);
                    if (permissionRepository != null) {
                        role.setPermissions(permissionRepository.findPermissionsByRoleId(role.getId()));
                    }
                    return Optional.of(role);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find role by code: " + code, e);
        }
        return Optional.empty();
    }

    @Override
    public Set<Role> findRolesByUserId(Long userId) {
        String sql = "SELECT r.id, r.code, r.name, r.description FROM roles r " +
                "INNER JOIN user_roles ur ON r.id = ur.role_id " +
                "WHERE ur.user_id = ?";
        Set<Role> roles = new HashSet<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Role role = mapRow(rs);
                    if (permissionRepository != null) {
                        role.setPermissions(permissionRepository.findPermissionsByRoleId(role.getId()));
                    }
                    roles.add(role);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch roles for user id: " + userId, e);
        }
        return roles;
    }

    private Role mapRow(ResultSet rs) throws SQLException {
        Role role = new Role();
        role.setId(rs.getInt("id"));
        role.setCode(rs.getString("code"));
        role.setName(rs.getString("name"));
        role.setDescription(rs.getString("description"));
        return role;
    }
}

