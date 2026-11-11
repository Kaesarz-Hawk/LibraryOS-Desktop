package com.libraryos.repository.impl;

import com.libraryos.config.DatabaseConfig;
import com.libraryos.core.exception.DatabaseException;
import com.libraryos.model.acquisition.Supplier;
import com.libraryos.repository.SupplierRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcSupplierRepository implements SupplierRepository {

    @Override
    public Optional<Supplier> findById(Long id) {
        String sql = "SELECT id, name, contact_person, phone, email, address, is_active, created_at, updated_at " +
                "FROM suppliers WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find supplier by id: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Supplier> findAll() {
        String sql = "SELECT id, name, contact_person, phone, email, address, is_active, created_at, updated_at " +
                "FROM suppliers ORDER BY name ASC";
        List<Supplier> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch all suppliers", e);
        }
        return list;
    }

    @Override
    public List<Supplier> search(String query) {
        if (query == null || query.isBlank()) {
            return findAll();
        }
        String sql = "SELECT id, name, contact_person, phone, email, address, is_active, created_at, updated_at " +
                "FROM suppliers " +
                "WHERE LOWER(name) LIKE ? OR LOWER(contact_person) LIKE ? OR phone LIKE ? OR LOWER(email) LIKE ? " +
                "ORDER BY name ASC";
        String pattern = "%" + query.trim().toLowerCase() + "%";
        List<Supplier> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            ps.setString(4, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to search suppliers with query: " + query, e);
        }
        return list;
    }

    @Override
    public Supplier save(Supplier supplier) {
        String sql = "INSERT INTO suppliers (name, contact_person, phone, email, address, is_active, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, supplier.getName());
            ps.setString(2, supplier.getContactPerson());
            ps.setString(3, supplier.getPhone());
            ps.setString(4, supplier.getEmail());
            ps.setString(5, supplier.getAddress());
            ps.setBoolean(6, supplier.isActive());
            ps.setTimestamp(7, Timestamp.valueOf(supplier.getCreatedAt() != null ? supplier.getCreatedAt() : java.time.LocalDateTime.now()));

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    supplier.setId(keys.getLong(1));
                }
            }
            return supplier;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert supplier: " + supplier.getName(), e);
        }
    }

    @Override
    public boolean update(Supplier supplier) {
        String sql = "UPDATE suppliers SET name = ?, contact_person = ?, phone = ?, email = ?, address = ?, " +
                "is_active = ?, updated_at = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, supplier.getName());
            ps.setString(2, supplier.getContactPerson());
            ps.setString(3, supplier.getPhone());
            ps.setString(4, supplier.getEmail());
            ps.setString(5, supplier.getAddress());
            ps.setBoolean(6, supplier.isActive());
            ps.setTimestamp(7, Timestamp.valueOf(java.time.LocalDateTime.now()));
            ps.setLong(8, supplier.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update supplier id: " + supplier.getId(), e);
        }
    }

    @Override
    public boolean delete(Long id) {
        String sql = "UPDATE suppliers SET is_active = FALSE, updated_at = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(java.time.LocalDateTime.now()));
            ps.setLong(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to deactivate supplier id: " + id, e);
        }
    }

    private Supplier mapRow(ResultSet rs) throws SQLException {
        Supplier s = new Supplier();
        s.setId(rs.getLong("id"));
        s.setName(rs.getString("name"));
        s.setContactPerson(rs.getString("contact_person"));
        s.setPhone(rs.getString("phone"));
        s.setEmail(rs.getString("email"));
        s.setAddress(rs.getString("address"));
        s.setActive(rs.getBoolean("is_active"));

        Timestamp cat = rs.getTimestamp("created_at");
        if (cat != null) s.setCreatedAt(cat.toLocalDateTime());

        Timestamp uat = rs.getTimestamp("updated_at");
        if (uat != null) s.setUpdatedAt(uat.toLocalDateTime());

        return s;
    }
}

