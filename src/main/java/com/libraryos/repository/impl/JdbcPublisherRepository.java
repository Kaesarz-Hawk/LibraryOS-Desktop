package com.libraryos.repository.impl;

import com.libraryos.config.DatabaseConfig;
import com.libraryos.core.exception.DatabaseException;
import com.libraryos.model.catalog.Publisher;
import com.libraryos.repository.PublisherRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcPublisherRepository implements PublisherRepository {

    @Override
    public List<Publisher> findAll() {
        String sql = "SELECT id, name, country, is_archived, created_at FROM publishers WHERE is_archived = FALSE ORDER BY name ASC";
        List<Publisher> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch publishers", e);
        }
        return list;
    }

    @Override
    public Optional<Publisher> findById(Long id) {
        String sql = "SELECT id, name, country, is_archived, created_at FROM publishers WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find publisher by id: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public Publisher create(Publisher publisher) {
        String sql = "INSERT INTO publishers (name, country) VALUES (?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, publisher.getName());
            ps.setString(2, publisher.getCountry() != null ? publisher.getCountry() : "Bangladesh");
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    publisher.setId(rs.getLong(1));
                }
            }
            return publisher;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert publisher: " + publisher.getName(), e);
        }
    }

    private Publisher mapRow(ResultSet rs) throws SQLException {
        Publisher p = new Publisher();
        p.setId(rs.getLong("id"));
        p.setName(rs.getString("name"));
        p.setCountry(rs.getString("country"));
        p.setArchived(rs.getBoolean("is_archived"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            p.setCreatedAt(ts.toLocalDateTime());
        }
        return p;
    }
}

