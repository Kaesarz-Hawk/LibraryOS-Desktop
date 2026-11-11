package com.libraryos.repository.impl;

import com.libraryos.config.DatabaseConfig;
import com.libraryos.core.exception.DatabaseException;
import com.libraryos.repository.AppSettingRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class JdbcAppSettingRepository implements AppSettingRepository {

    @Override
    public Optional<String> getSetting(String key) {
        String sql = "SELECT setting_value FROM app_settings WHERE setting_key = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.ofNullable(rs.getString("setting_value"));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to read setting: " + key, e);
        }
        return Optional.empty();
    }

    @Override
    public void setSetting(String key, String value, String valueType, Long updatedBy) {
        String sql = "INSERT INTO app_settings (setting_key, setting_value, value_type, updated_by) " +
                "VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value), " +
                "value_type = VALUES(value_type), updated_by = VALUES(updated_by)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.setString(3, valueType != null ? valueType : "STRING");
            if (updatedBy != null) {
                ps.setLong(4, updatedBy);
            } else {
                ps.setNull(4, java.sql.Types.BIGINT);
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update setting: " + key, e);
        }
    }

    @Override
    public Map<String, String> getAllSettings() {
        String sql = "SELECT setting_key, setting_value FROM app_settings";
        Map<String, String> map = new HashMap<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                map.put(rs.getString("setting_key"), rs.getString("setting_value"));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch all settings", e);
        }
        return map;
    }
}

