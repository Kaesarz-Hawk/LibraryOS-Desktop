package com.libraryos.repository.impl;

import com.libraryos.config.DatabaseConfig;
import com.libraryos.core.exception.DatabaseException;
import com.libraryos.model.catalog.Author;
import com.libraryos.repository.AuthorRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcAuthorRepository implements AuthorRepository {

    @Override
    public List<Author> findAll() {
        String sql = "SELECT id, name_en, name_bn, biography, is_archived, created_at FROM authors WHERE is_archived = FALSE ORDER BY name_en ASC";
        List<Author> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch authors", e);
        }
        return list;
    }

    @Override
    public Optional<Author> findById(Long id) {
        String sql = "SELECT id, name_en, name_bn, biography, is_archived, created_at FROM authors WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find author by id: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public Author create(Author author) {
        String sql = "INSERT INTO authors (name_en, name_bn, biography) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, author.getNameEn());
            ps.setString(2, author.getNameBn());
            ps.setString(3, author.getBiography());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    author.setId(rs.getLong(1));
                }
            }
            return author;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert author: " + author.getNameEn(), e);
        }
    }

    @Override
    public List<Author> findByBookId(Long bookId) {
        String sql = "SELECT a.id, a.name_en, a.name_bn, a.biography, a.is_archived, a.created_at " +
                "FROM authors a INNER JOIN book_authors ba ON a.id = ba.author_id " +
                "WHERE ba.book_id = ? ORDER BY a.name_en ASC";
        List<Author> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch authors for book id: " + bookId, e);
        }
        return list;
    }

    @Override
    public void setBookAuthors(Long bookId, List<Long> authorIds) {
        String deleteSql = "DELETE FROM book_authors WHERE book_id = ?";
        String insertSql = "INSERT INTO book_authors (book_id, author_id) VALUES (?, ?)";
        try (Connection conn = DatabaseConfig.getConnection()) {
            try (PreparedStatement psDel = conn.prepareStatement(deleteSql)) {
                psDel.setLong(1, bookId);
                psDel.executeUpdate();
            }
            if (authorIds != null && !authorIds.isEmpty()) {
                try (PreparedStatement psIns = conn.prepareStatement(insertSql)) {
                    for (Long authorId : authorIds) {
                        psIns.setLong(1, bookId);
                        psIns.setLong(2, authorId);
                        psIns.addBatch();
                    }
                    psIns.executeBatch();
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to set book authors for book id: " + bookId, e);
        }
    }

    private Author mapRow(ResultSet rs) throws SQLException {
        Author a = new Author();
        a.setId(rs.getLong("id"));
        a.setNameEn(rs.getString("name_en"));
        a.setNameBn(rs.getString("name_bn"));
        a.setBiography(rs.getString("biography"));
        a.setArchived(rs.getBoolean("is_archived"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            a.setCreatedAt(ts.toLocalDateTime());
        }
        return a;
    }
}

