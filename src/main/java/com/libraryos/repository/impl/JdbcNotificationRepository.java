package com.libraryos.repository.impl;

import com.libraryos.config.DatabaseConfig;
import com.libraryos.core.exception.DatabaseException;
import com.libraryos.model.interaction.Notification;
import com.libraryos.repository.NotificationRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class JdbcNotificationRepository implements NotificationRepository {

    @Override
    public List<Notification> findByUserId(Long userId) {
        String sql = "SELECT id, user_id, title, message, notification_type, is_read, created_at " +
                "FROM notifications WHERE user_id = ? ORDER BY created_at DESC";
        List<Notification> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find notifications for user id: " + userId, e);
        }
        return list;
    }

    @Override
    public List<Notification> findUnreadByUserId(Long userId) {
        String sql = "SELECT id, user_id, title, message, notification_type, is_read, created_at " +
                "FROM notifications WHERE user_id = ? AND is_read = FALSE ORDER BY created_at DESC";
        List<Notification> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find unread notifications for user id: " + userId, e);
        }
        return list;
    }

    @Override
    public Notification save(Notification notification) {
        String sql = "INSERT INTO notifications (user_id, title, message, notification_type, is_read, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, notification.getUserId());
            ps.setString(2, notification.getTitle());
            ps.setString(3, notification.getMessage());
            ps.setString(4, notification.getNotificationType());
            ps.setBoolean(5, notification.isRead());
            ps.setTimestamp(6, Timestamp.valueOf(notification.getCreatedAt() != null ? notification.getCreatedAt() : java.time.LocalDateTime.now()));

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    notification.setId(keys.getLong(1));
                }
            }
            return notification;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save notification", e);
        }
    }

    @Override
    public boolean markAsRead(Long id, Long userId) {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE id = ? AND user_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.setLong(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to mark notification as read id: " + id, e);
        }
    }

    @Override
    public boolean markAllAsRead(Long userId) {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE user_id = ? AND is_read = FALSE";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to mark all notifications as read for user id: " + userId, e);
        }
    }

    private Notification mapRow(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setId(rs.getLong("id"));
        n.setUserId(rs.getLong("user_id"));
        n.setTitle(rs.getString("title"));
        n.setMessage(rs.getString("message"));
        n.setNotificationType(rs.getString("notification_type"));
        n.setRead(rs.getBoolean("is_read"));

        Timestamp cat = rs.getTimestamp("created_at");
        if (cat != null) n.setCreatedAt(cat.toLocalDateTime());

        return n;
    }
}

