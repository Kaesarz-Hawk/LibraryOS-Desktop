package com.libraryos.repository.impl;

import com.libraryos.config.DatabaseConfig;
import com.libraryos.core.exception.DatabaseException;
import com.libraryos.model.audit.AuditLog;
import com.libraryos.repository.AuditLogRepository;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class JdbcAuditLogRepository implements AuditLogRepository {

    @Override
    public void insert(AuditLog log) {
        String sql = "INSERT INTO audit_logs (actor_user_id, action, entity_type, entity_id, summary, reason, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (log.getActorUserId() != null) {
                ps.setLong(1, log.getActorUserId());
            } else {
                ps.setNull(1, Types.BIGINT);
            }
            ps.setString(2, log.getAction());
            ps.setString(3, log.getEntityType());
            ps.setString(4, log.getEntityId());
            ps.setString(5, log.getSummary());
            ps.setString(6, log.getReason());
            ps.setTimestamp(7, Timestamp.valueOf(log.getCreatedAt() != null ? log.getCreatedAt() : LocalDateTime.now()));

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    log.setId(rs.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert audit log entry", e);
        }
    }

    @Override
    public List<AuditLog> findRecent(int limit) {
        String sql = "SELECT a.id, a.actor_user_id, u.username as actor_username, a.action, a.entity_type, " +
                "a.entity_id, a.summary, a.reason, a.created_at " +
                "FROM audit_logs a LEFT JOIN users u ON a.actor_user_id = u.id " +
                "ORDER BY a.created_at DESC, a.id DESC LIMIT ?";
        List<AuditLog> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch recent audit logs", e);
        }
        return list;
    }

    @Override
    public List<AuditLog> findByEntityType(String entityType, int limit) {
        String sql = "SELECT a.id, a.actor_user_id, u.username as actor_username, a.action, a.entity_type, " +
                "a.entity_id, a.summary, a.reason, a.created_at " +
                "FROM audit_logs a LEFT JOIN users u ON a.actor_user_id = u.id " +
                "WHERE a.entity_type = ? " +
                "ORDER BY a.created_at DESC, a.id DESC LIMIT ?";
        List<AuditLog> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, entityType);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch audit logs for entity: " + entityType, e);
        }
        return list;
    }

    @Override
    public List<AuditLog> search(String query, String action, String entityType, int limit) {
        StringBuilder sql = new StringBuilder("SELECT a.id, a.actor_user_id, u.username as actor_username, a.action, a.entity_type, ")
                .append("a.entity_id, a.summary, a.reason, a.created_at ")
                .append("FROM audit_logs a LEFT JOIN users u ON a.actor_user_id = u.id ")
                .append("WHERE 1=1 ");

        List<Object> params = new ArrayList<>();

        if (action != null && !action.isBlank() && !"ALL".equalsIgnoreCase(action)) {
            sql.append("AND a.action = ? ");
            params.add(action.trim());
        }

        if (entityType != null && !entityType.isBlank() && !"ALL".equalsIgnoreCase(entityType)) {
            sql.append("AND a.entity_type = ? ");
            params.add(entityType.trim());
        }

        if (query != null && !query.isBlank()) {
            String q = "%" + query.trim().toLowerCase() + "%";
            sql.append("AND (LOWER(a.summary) LIKE ? OR LOWER(COALESCE(a.reason, '')) LIKE ? OR LOWER(COALESCE(u.username, '')) LIKE ? OR LOWER(a.action) LIKE ?) ");
            for (int i = 0; i < 4; i++) {
                params.add(q);
            }
        }

        sql.append("ORDER BY a.created_at DESC, a.id DESC LIMIT ?");
        params.add(limit > 0 ? limit : 200);

        List<AuditLog> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to search audit logs", e);
        }
        return list;
    }

    private AuditLog mapRow(ResultSet rs) throws SQLException {
        AuditLog log = new AuditLog();
        log.setId(rs.getLong("id"));

        long actorId = rs.getLong("actor_user_id");
        if (!rs.wasNull()) {
            log.setActorUserId(actorId);
        }

        log.setActorUsername(rs.getString("actor_username"));
        log.setAction(rs.getString("action"));
        log.setEntityType(rs.getString("entity_type"));
        log.setEntityId(rs.getString("entity_id"));
        log.setSummary(rs.getString("summary"));
        log.setReason(rs.getString("reason"));

        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            log.setCreatedAt(ts.toLocalDateTime());
        }
        return log;
    }
}

