package com.libraryos.repository.impl;

import com.libraryos.config.DatabaseConfig;
import com.libraryos.core.exception.DatabaseException;
import com.libraryos.model.catalog.Category;
import com.libraryos.repository.CategoryRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcCategoryRepository implements CategoryRepository {

    @Override
    public List<Category> findAll() {
        String sql = "SELECT id, parent_id, name_en, name_bn, code, description, is_archived, created_at " +
                "FROM categories WHERE is_archived = FALSE ORDER BY name_en ASC";
        List<Category> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch categories", e);
        }
        return list;
    }

    @Override
    public Optional<Category> findById(Long id) {
        String sql = "SELECT id, parent_id, name_en, name_bn, code, description, is_archived, created_at FROM categories WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find category by id: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public Category create(Category category) {
        String sql = "INSERT INTO categories (parent_id, name_en, name_bn, code, description) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (category.getParentId() != null) {
                ps.setLong(1, category.getParentId());
            } else {
                ps.setNull(1, Types.BIGINT);
            }
            ps.setString(2, category.getNameEn());
            ps.setString(3, category.getNameBn());
            ps.setString(4, category.getCode());
            ps.setString(5, category.getDescription());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    category.setId(rs.getLong(1));
                }
            }
            return category;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert category: " + category.getNameEn(), e);
        }
    }

    @Override
    public List<Category> findByBookId(Long bookId) {
        String sql = "SELECT c.id, c.parent_id, c.name_en, c.name_bn, c.code, c.description, c.is_archived, c.created_at " +
                "FROM categories c INNER JOIN book_categories bc ON c.id = bc.category_id " +
                "WHERE bc.book_id = ? ORDER BY c.name_en ASC";
        List<Category> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch categories for book id: " + bookId, e);
        }
        return list;
    }

    @Override
    public void setBookCategories(Long bookId, List<Long> categoryIds) {
        String deleteSql = "DELETE FROM book_categories WHERE book_id = ?";
        String insertSql = "INSERT INTO book_categories (book_id, category_id) VALUES (?, ?)";
        try (Connection conn = DatabaseConfig.getConnection()) {
            try (PreparedStatement psDel = conn.prepareStatement(deleteSql)) {
                psDel.setLong(1, bookId);
                psDel.executeUpdate();
            }
            if (categoryIds != null && !categoryIds.isEmpty()) {
                try (PreparedStatement psIns = conn.prepareStatement(insertSql)) {
                    for (Long catId : categoryIds) {
                        psIns.setLong(1, bookId);
                        psIns.setLong(2, catId);
                        psIns.addBatch();
                    }
                    psIns.executeBatch();
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to set book categories for book id: " + bookId, e);
        }
    }

    private Category mapRow(ResultSet rs) throws SQLException {
        Category c = new Category();
        c.setId(rs.getLong("id"));
        long parentIdVal = rs.getLong("parent_id");
        if (!rs.wasNull()) {
            c.setParentId(parentIdVal);
        }
        c.setNameEn(rs.getString("name_en"));
        c.setNameBn(rs.getString("name_bn"));
        c.setCode(rs.getString("code"));
        c.setDescription(rs.getString("description"));
        c.setArchived(rs.getBoolean("is_archived"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            c.setCreatedAt(ts.toLocalDateTime());
        }
        return c;
    }
}

