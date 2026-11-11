package com.libraryos.repository.impl;

import com.libraryos.config.DatabaseConfig;
import com.libraryos.core.exception.DatabaseException;
import com.libraryos.model.inventory.Shelf;
import com.libraryos.repository.ShelfRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcShelfRepository implements ShelfRepository {

    @Override
    public List<Shelf> findAll() {
        String sql = "SELECT id, code, section_name, rack_number, shelf_level, description, created_at " +
                "FROM shelves ORDER BY code ASC";
        List<Shelf> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch shelves", e);
        }
        return list;
    }

    @Override
    public Optional<Shelf> findById(Long id) {
        String sql = "SELECT id, code, section_name, rack_number, shelf_level, description, created_at FROM shelves WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find shelf by id: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Shelf> findByCode(String code) {
        String sql = "SELECT id, code, section_name, rack_number, shelf_level, description, created_at FROM shelves WHERE code = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find shelf by code: " + code, e);
        }
        return Optional.empty();
    }

    @Override
    public Shelf create(Shelf shelf) {
        String sql = "INSERT INTO shelves (code, section_name, rack_number, shelf_level, description) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, shelf.getCode());
            ps.setString(2, shelf.getSectionName());
            ps.setString(3, shelf.getRackNumber());
            ps.setString(4, shelf.getShelfLevel());
            ps.setString(5, shelf.getDescription());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    shelf.setId(rs.getLong(1));
                }
            }
            return shelf;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert shelf: " + shelf.getCode(), e);
        }
    }

    private Shelf mapRow(ResultSet rs) throws SQLException {
        Shelf s = new Shelf();
        s.setId(rs.getLong("id"));
        s.setCode(rs.getString("code"));
        s.setSectionName(rs.getString("section_name"));
        s.setRackNumber(rs.getString("rack_number"));
        s.setShelfLevel(rs.getString("shelf_level"));
        s.setDescription(rs.getString("description"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            s.setCreatedAt(ts.toLocalDateTime());
        }
        return s;
    }
}

