package com.libraryos.repository.impl;

import com.libraryos.config.DatabaseConfig;
import com.libraryos.core.exception.DatabaseException;
import com.libraryos.model.user.User;
import com.libraryos.model.user.UserStatus;
import com.libraryos.repository.RoleRepository;
import com.libraryos.repository.UserRepository;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcUserRepository implements UserRepository {

    private final RoleRepository roleRepository;

    public JdbcUserRepository(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT id, username, password_hash, full_name, full_name_bn, email, phone, " +
                "member_id, status, failed_login_attempts, locked_until, last_login_at, created_at, updated_at " +
                "FROM users WHERE username = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User user = mapRow(rs);
                    if (roleRepository != null) {
                        user.setRoles(roleRepository.findRolesByUserId(user.getId()));
                    }
                    return Optional.of(user);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find user by username: " + username, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<User> findById(Long id) {
        String sql = "SELECT id, username, password_hash, full_name, full_name_bn, email, phone, " +
                "member_id, status, failed_login_attempts, locked_until, last_login_at, created_at, updated_at " +
                "FROM users WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User user = mapRow(rs);
                    if (roleRepository != null) {
                        user.setRoles(roleRepository.findRolesByUserId(user.getId()));
                    }
                    return Optional.of(user);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find user by id: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public List<User> findAll() {
        String sql = "SELECT id, username, password_hash, full_name, full_name_bn, email, phone, " +
                "member_id, status, failed_login_attempts, locked_until, last_login_at, created_at, updated_at " +
                "FROM users ORDER BY id ASC";
        List<User> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                User user = mapRow(rs);
                if (roleRepository != null) {
                    user.setRoles(roleRepository.findRolesByUserId(user.getId()));
                }
                list.add(user);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch all users", e);
        }
        return list;
    }

    @Override
    public User create(User user) {
        String sql = "INSERT INTO users (username, password_hash, full_name, full_name_bn, email, phone, member_id, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getFullName());
            ps.setString(4, user.getFullNameBn());
            ps.setString(5, user.getEmail());
            ps.setString(6, user.getPhone());
            if (user.getMemberId() != null) {
                ps.setLong(7, user.getMemberId());
            } else {
                ps.setNull(7, Types.BIGINT);
            }
            ps.setString(8, user.getStatus() != null ? user.getStatus().name() : UserStatus.ACTIVE.name());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    user.setId(rs.getLong(1));
                }
            }
            return user;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert user: " + user.getUsername(), e);
        }
    }

    @Override
    public void update(User user) {
        String sql = "UPDATE users SET full_name = ?, full_name_bn = ?, email = ?, phone = ?, status = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getFullNameBn());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getPhone());
            ps.setString(5, user.getStatus().name());
            ps.setLong(6, user.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update user: " + user.getId(), e);
        }
    }

    @Override
    public void updateFailedAttempts(Long userId, int attempts, LocalDateTime lockedUntil) {
        String sql = "UPDATE users SET failed_login_attempts = ?, locked_until = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, attempts);
            if (lockedUntil != null) {
                ps.setTimestamp(2, Timestamp.valueOf(lockedUntil));
            } else {
                ps.setNull(2, Types.TIMESTAMP);
            }
            ps.setLong(3, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update failed login attempts for user id: " + userId, e);
        }
    }

    @Override
    public void resetFailedAttempts(Long userId) {
        String sql = "UPDATE users SET failed_login_attempts = 0, locked_until = NULL WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to reset failed attempts for user id: " + userId, e);
        }
    }

    @Override
    public void updateLastLogin(Long userId, LocalDateTime loginTime) {
        String sql = "UPDATE users SET last_login_at = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(loginTime));
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update last login for user id: " + userId, e);
        }
    }

    @Override
    public void assignRole(Long userId, int roleId) {
        String sql = "INSERT IGNORE INTO user_roles (user_id, role_id) VALUES (?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt(2, roleId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to assign role to user id: " + userId, e);
        }
    }

    @Override
    public void removeRole(Long userId, int roleId) {
        String sql = "DELETE FROM user_roles WHERE user_id = ? AND role_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt(2, roleId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to remove role from user id: " + userId, e);
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setFullName(rs.getString("full_name"));
        user.setFullNameBn(rs.getString("full_name_bn"));
        user.setEmail(rs.getString("email"));
        user.setPhone(rs.getString("phone"));

        long memberIdVal = rs.getLong("member_id");
        if (!rs.wasNull()) {
            user.setMemberId(memberIdVal);
        }

        String statusStr = rs.getString("status");
        if (statusStr != null) {
            user.setStatus(UserStatus.valueOf(statusStr));
        }

        user.setFailedLoginAttempts(rs.getInt("failed_login_attempts"));

        Timestamp lockedTs = rs.getTimestamp("locked_until");
        if (lockedTs != null) {
            user.setLockedUntil(lockedTs.toLocalDateTime());
        }

        Timestamp lastLoginTs = rs.getTimestamp("last_login_at");
        if (lastLoginTs != null) {
            user.setLastLoginAt(lastLoginTs.toLocalDateTime());
        }

        Timestamp createdTs = rs.getTimestamp("created_at");
        if (createdTs != null) {
            user.setCreatedAt(createdTs.toLocalDateTime());
        }

        Timestamp updatedTs = rs.getTimestamp("updated_at");
        if (updatedTs != null) {
            user.setUpdatedAt(updatedTs.toLocalDateTime());
        }

        return user;
    }
}

