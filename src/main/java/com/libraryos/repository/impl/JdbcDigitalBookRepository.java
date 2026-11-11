package com.libraryos.repository.impl;

import com.libraryos.config.DatabaseConfig;
import com.libraryos.core.exception.DatabaseException;
import com.libraryos.model.digital.DigitalBook;
import com.libraryos.repository.DigitalBookRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcDigitalBookRepository implements DigitalBookRepository {

    private static final String BASE_SELECT =
            "SELECT d.id, d.book_id, b.title_en AS book_title, b.title_bn AS book_title_bn, " +
            "d.file_name, d.file_path, d.file_size_bytes, d.mime_type, d.sha256_checksum, " +
            "d.total_pages, d.is_download_allowed, d.uploaded_by, u.username AS uploader_username, " +
            "d.uploaded_at, d.updated_at " +
            "FROM digital_books d " +
            "JOIN books b ON d.book_id = b.id " +
            "JOIN users u ON d.uploaded_by = u.id ";

    @Override
    public Optional<DigitalBook> findById(Long id) {
        String sql = BASE_SELECT + "WHERE d.id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find digital book by id: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<DigitalBook> findByBookId(Long bookId) {
        String sql = BASE_SELECT + "WHERE d.book_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find digital book for book id: " + bookId, e);
        }
        return Optional.empty();
    }

    @Override
    public List<DigitalBook> findAll() {
        String sql = BASE_SELECT + "ORDER BY d.uploaded_at DESC";
        List<DigitalBook> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch all digital books", e);
        }
        return list;
    }

    @Override
    public List<DigitalBook> search(String query) {
        if (query == null || query.isBlank()) {
            return findAll();
        }
        String sql = BASE_SELECT +
                "WHERE LOWER(b.title_en) LIKE ? OR LOWER(COALESCE(b.title_bn, '')) LIKE ? OR LOWER(d.file_name) LIKE ? " +
                "ORDER BY d.uploaded_at DESC";
        String pattern = "%" + query.trim().toLowerCase() + "%";
        List<DigitalBook> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to search digital books with query: " + query, e);
        }
        return list;
    }

    @Override
    public DigitalBook save(DigitalBook digitalBook) {
        String sql = "INSERT INTO digital_books (book_id, file_name, file_path, file_size_bytes, " +
                "mime_type, sha256_checksum, total_pages, is_download_allowed, uploaded_by, uploaded_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, digitalBook.getBookId());
            ps.setString(2, digitalBook.getFileName());
            ps.setString(3, digitalBook.getFilePath());
            ps.setLong(4, digitalBook.getFileSizeBytes());
            ps.setString(5, digitalBook.getMimeType());
            ps.setString(6, digitalBook.getSha256Checksum());
            ps.setInt(7, digitalBook.getTotalPages());
            ps.setBoolean(8, digitalBook.isDownloadAllowed());
            ps.setLong(9, digitalBook.getUploadedBy());
            ps.setTimestamp(10, Timestamp.valueOf(digitalBook.getUploadedAt() != null ? digitalBook.getUploadedAt() : java.time.LocalDateTime.now()));

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    digitalBook.setId(keys.getLong(1));
                }
            }
            return digitalBook;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save digital book record", e);
        }
    }

    @Override
    public boolean update(DigitalBook digitalBook) {
        String sql = "UPDATE digital_books SET file_name = ?, file_path = ?, file_size_bytes = ?, " +
                "mime_type = ?, sha256_checksum = ?, total_pages = ?, is_download_allowed = ?, updated_at = ? " +
                "WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, digitalBook.getFileName());
            ps.setString(2, digitalBook.getFilePath());
            ps.setLong(3, digitalBook.getFileSizeBytes());
            ps.setString(4, digitalBook.getMimeType());
            ps.setString(5, digitalBook.getSha256Checksum());
            ps.setInt(6, digitalBook.getTotalPages());
            ps.setBoolean(7, digitalBook.isDownloadAllowed());
            ps.setTimestamp(8, Timestamp.valueOf(java.time.LocalDateTime.now()));
            ps.setLong(9, digitalBook.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update digital book id: " + digitalBook.getId(), e);
        }
    }

    @Override
    public boolean delete(Long id) {
        String sql = "DELETE FROM digital_books WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete digital book id: " + id, e);
        }
    }

    private DigitalBook mapRow(ResultSet rs) throws SQLException {
        DigitalBook d = new DigitalBook();
        d.setId(rs.getLong("id"));
        d.setBookId(rs.getLong("book_id"));
        d.setBookTitle(rs.getString("book_title"));
        d.setBookTitleBn(rs.getString("book_title_bn"));
        d.setFileName(rs.getString("file_name"));
        d.setFilePath(rs.getString("file_path"));
        d.setFileSizeBytes(rs.getLong("file_size_bytes"));
        d.setMimeType(rs.getString("mime_type"));
        d.setSha256Checksum(rs.getString("sha256_checksum"));
        d.setTotalPages(rs.getInt("total_pages"));
        d.setDownloadAllowed(rs.getBoolean("is_download_allowed"));
        d.setUploadedBy(rs.getLong("uploaded_by"));
        d.setUploaderUsername(rs.getString("uploader_username"));

        Timestamp uat = rs.getTimestamp("uploaded_at");
        if (uat != null) d.setUploadedAt(uat.toLocalDateTime());

        Timestamp upat = rs.getTimestamp("updated_at");
        if (upat != null) d.setUpdatedAt(upat.toLocalDateTime());

        return d;
    }
}

