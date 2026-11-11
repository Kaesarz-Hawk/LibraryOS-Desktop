package com.libraryos.repository.impl;

import com.libraryos.config.DatabaseConfig;
import com.libraryos.core.exception.DatabaseException;
import com.libraryos.model.digital.Bookmark;
import com.libraryos.repository.BookmarkRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class JdbcBookmarkRepository implements BookmarkRepository {

    @Override
    public List<Bookmark> findByMemberAndBook(Long memberId, Long digitalBookId) {
        String sql = "SELECT id, member_id, digital_book_id, page_number, title, note, created_at " +
                "FROM bookmarks " +
                "WHERE member_id = ? AND digital_book_id = ? " +
                "ORDER BY page_number ASC";
        List<Bookmark> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, memberId);
            ps.setLong(2, digitalBookId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find bookmarks for member and book", e);
        }
        return list;
    }

    @Override
    public Bookmark save(Bookmark bookmark) {
        String sql = "INSERT INTO bookmarks (member_id, digital_book_id, page_number, title, note, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, bookmark.getMemberId());
            ps.setLong(2, bookmark.getDigitalBookId());
            ps.setInt(3, bookmark.getPageNumber());
            ps.setString(4, bookmark.getTitle());
            ps.setString(5, bookmark.getNote());
            ps.setTimestamp(6, Timestamp.valueOf(bookmark.getCreatedAt() != null ? bookmark.getCreatedAt() : java.time.LocalDateTime.now()));

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    bookmark.setId(keys.getLong(1));
                }
            }
            return bookmark;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save bookmark", e);
        }
    }

    @Override
    public boolean delete(Long id, Long memberId) {
        String sql = "DELETE FROM bookmarks WHERE id = ? AND member_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.setLong(2, memberId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete bookmark id: " + id, e);
        }
    }

    private Bookmark mapRow(ResultSet rs) throws SQLException {
        Bookmark b = new Bookmark();
        b.setId(rs.getLong("id"));
        b.setMemberId(rs.getLong("member_id"));
        b.setDigitalBookId(rs.getLong("digital_book_id"));
        b.setPageNumber(rs.getInt("page_number"));
        b.setTitle(rs.getString("title"));
        b.setNote(rs.getString("note"));

        Timestamp cat = rs.getTimestamp("created_at");
        if (cat != null) b.setCreatedAt(cat.toLocalDateTime());

        return b;
    }
}

