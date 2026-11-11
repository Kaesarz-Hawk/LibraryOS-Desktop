package com.libraryos.repository.impl;

import com.libraryos.config.DatabaseConfig;
import com.libraryos.core.exception.DatabaseException;
import com.libraryos.model.digital.ReadingProgress;
import com.libraryos.repository.ReadingProgressRepository;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcReadingProgressRepository implements ReadingProgressRepository {

    private static final String BASE_SELECT =
            "SELECT rp.id, rp.member_id, m.full_name_en AS member_name, rp.digital_book_id, " +
            "b.title_en AS book_title, rp.last_page_read, rp.total_pages, rp.last_read_at " +
            "FROM reading_progress rp " +
            "JOIN members m ON rp.member_id = m.id " +
            "JOIN digital_books d ON rp.digital_book_id = d.id " +
            "JOIN books b ON d.book_id = b.id ";

    @Override
    public Optional<ReadingProgress> findByMemberAndBook(Long memberId, Long digitalBookId) {
        String sql = BASE_SELECT + "WHERE rp.member_id = ? AND rp.digital_book_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, memberId);
            ps.setLong(2, digitalBookId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find reading progress", e);
        }
        return Optional.empty();
    }

    @Override
    public List<ReadingProgress> findByMemberId(Long memberId) {
        String sql = BASE_SELECT + "WHERE rp.member_id = ? ORDER BY rp.last_read_at DESC";
        List<ReadingProgress> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find reading progress for member id: " + memberId, e);
        }
        return list;
    }

    @Override
    public void saveOrUpdate(ReadingProgress progress) {
        Optional<ReadingProgress> existing = findByMemberAndBook(progress.getMemberId(), progress.getDigitalBookId());
        if (existing.isPresent()) {
            String sql = "UPDATE reading_progress SET last_page_read = ?, total_pages = ?, last_read_at = ? " +
                    "WHERE member_id = ? AND digital_book_id = ?";
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, progress.getLastPageRead());
                ps.setInt(2, progress.getTotalPages());
                ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                ps.setLong(4, progress.getMemberId());
                ps.setLong(5, progress.getDigitalBookId());
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new DatabaseException("Failed to update reading progress", e);
            }
        } else {
            String sql = "INSERT INTO reading_progress (member_id, digital_book_id, last_page_read, total_pages, last_read_at) " +
                    "VALUES (?, ?, ?, ?, ?)";
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, progress.getMemberId());
                ps.setLong(2, progress.getDigitalBookId());
                ps.setInt(3, progress.getLastPageRead());
                ps.setInt(4, progress.getTotalPages());
                ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        progress.setId(keys.getLong(1));
                    }
                }
            } catch (SQLException e) {
                throw new DatabaseException("Failed to insert reading progress", e);
            }
        }
    }

    private ReadingProgress mapRow(ResultSet rs) throws SQLException {
        ReadingProgress p = new ReadingProgress();
        p.setId(rs.getLong("id"));
        p.setMemberId(rs.getLong("member_id"));
        p.setMemberName(rs.getString("member_name"));
        p.setDigitalBookId(rs.getLong("digital_book_id"));
        p.setDigitalBookTitle(rs.getString("book_title"));
        p.setLastPageRead(rs.getInt("last_page_read"));
        p.setTotalPages(rs.getInt("total_pages"));

        Timestamp lra = rs.getTimestamp("last_read_at");
        if (lra != null) p.setLastReadAt(lra.toLocalDateTime());

        return p;
    }
}

