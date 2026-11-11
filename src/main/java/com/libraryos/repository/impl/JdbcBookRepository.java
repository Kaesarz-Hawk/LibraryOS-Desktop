package com.libraryos.repository.impl;

import com.libraryos.config.DatabaseConfig;
import com.libraryos.core.exception.DatabaseException;
import com.libraryos.model.catalog.Book;
import com.libraryos.repository.AuthorRepository;
import com.libraryos.repository.BookRepository;
import com.libraryos.repository.CategoryRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcBookRepository implements BookRepository {

    private final AuthorRepository authorRepository;
    private final CategoryRepository categoryRepository;

    public JdbcBookRepository(AuthorRepository authorRepository, CategoryRepository categoryRepository) {
        this.authorRepository = authorRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public List<Book> findAll(boolean includeArchived) {
        String sql = "SELECT b.id, b.title_en, b.title_bn, b.subtitle, b.isbn_10, b.isbn_13, " +
                "b.publisher_id, p.name as publisher_name, b.publication_year, b.edition, " +
                "b.language, b.description, b.cover_image_path, b.is_archived, b.created_at, b.updated_at " +
                "FROM books b LEFT JOIN publishers p ON b.publisher_id = p.id " +
                (includeArchived ? "" : "WHERE b.is_archived = FALSE ") +
                "ORDER BY b.id DESC";
        List<Book> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Book book = mapRow(rs);
                populateRelationsAndCounts(book);
                list.add(book);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch books", e);
        }
        return list;
    }

    @Override
    public List<Book> search(String query, Long categoryId, Long publisherId) {
        StringBuilder sql = new StringBuilder("SELECT DISTINCT b.id, b.title_en, b.title_bn, b.subtitle, " +
                "b.isbn_10, b.isbn_13, b.publisher_id, p.name as publisher_name, b.publication_year, " +
                "b.edition, b.language, b.description, b.cover_image_path, b.is_archived, b.created_at, b.updated_at " +
                "FROM books b " +
                "LEFT JOIN publishers p ON b.publisher_id = p.id " +
                "LEFT JOIN book_categories bc ON b.id = bc.book_id " +
                "LEFT JOIN book_authors ba ON b.id = ba.book_id " +
                "LEFT JOIN authors a ON ba.author_id = a.id " +
                "WHERE b.is_archived = FALSE ");

        List<Object> params = new ArrayList<>();

        if (query != null && !query.isBlank()) {
            String q = "%" + query.trim().toLowerCase() + "%";
            sql.append("AND (LOWER(b.title_en) LIKE ? OR LOWER(b.title_bn) LIKE ? OR b.isbn_13 LIKE ? OR b.isbn_10 LIKE ? " +
                    "OR LOWER(a.name_en) LIKE ? OR LOWER(a.name_bn) LIKE ?) ");
            params.add(q);
            params.add(q);
            params.add(q);
            params.add(q);
            params.add(q);
            params.add(q);
        }

        if (categoryId != null && categoryId > 0) {
            sql.append("AND bc.category_id = ? ");
            params.add(categoryId);
        }

        if (publisherId != null && publisherId > 0) {
            sql.append("AND b.publisher_id = ? ");
            params.add(publisherId);
        }

        sql.append("ORDER BY b.id DESC");

        List<Book> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Book book = mapRow(rs);
                    populateRelationsAndCounts(book);
                    list.add(book);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to search books", e);
        }
        return list;
    }

    @Override
    public Optional<Book> findById(Long id) {
        String sql = "SELECT b.id, b.title_en, b.title_bn, b.subtitle, b.isbn_10, b.isbn_13, " +
                "b.publisher_id, p.name as publisher_name, b.publication_year, b.edition, " +
                "b.language, b.description, b.cover_image_path, b.is_archived, b.created_at, b.updated_at " +
                "FROM books b LEFT JOIN publishers p ON b.publisher_id = p.id WHERE b.id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Book book = mapRow(rs);
                    populateRelationsAndCounts(book);
                    return Optional.of(book);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find book by id: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Book> findByIsbn(String isbn) {
        if (isbn == null || isbn.isBlank()) return Optional.empty();
        String clean = isbn.replaceAll("[\\s-]", "");
        String sql = "SELECT b.id, b.title_en, b.title_bn, b.subtitle, b.isbn_10, b.isbn_13, " +
                "b.publisher_id, p.name as publisher_name, b.publication_year, b.edition, " +
                "b.language, b.description, b.cover_image_path, b.is_archived, b.created_at, b.updated_at " +
                "FROM books b LEFT JOIN publishers p ON b.publisher_id = p.id WHERE b.isbn_13 = ? OR b.isbn_10 = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, clean);
            ps.setString(2, clean);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Book book = mapRow(rs);
                    populateRelationsAndCounts(book);
                    return Optional.of(book);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find book by ISBN: " + isbn, e);
        }
        return Optional.empty();
    }

    @Override
    public Book create(Book book) {
        String sql = "INSERT INTO books (title_en, title_bn, subtitle, isbn_10, isbn_13, publisher_id, " +
                "publication_year, edition, language, description, cover_image_path) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, book.getTitleEn());
            ps.setString(2, book.getTitleBn());
            ps.setString(3, book.getSubtitle());
            ps.setString(4, book.getIsbn10());
            ps.setString(5, book.getIsbn13());
            if (book.getPublisherId() != null) {
                ps.setLong(6, book.getPublisherId());
            } else {
                ps.setNull(6, Types.BIGINT);
            }
            if (book.getPublicationYear() != null) {
                ps.setInt(7, book.getPublicationYear());
            } else {
                ps.setNull(7, Types.SMALLINT);
            }
            ps.setString(8, book.getEdition());
            ps.setString(9, book.getLanguage() != null ? book.getLanguage() : "Bangla");
            ps.setString(10, book.getDescription());
            ps.setString(11, book.getCoverImagePath());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    book.setId(rs.getLong(1));
                }
            }
            return book;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert book title: " + book.getTitleEn(), e);
        }
    }

    @Override
    public void update(Book book) {
        String sql = "UPDATE books SET title_en = ?, title_bn = ?, subtitle = ?, isbn_10 = ?, isbn_13 = ?, " +
                "publisher_id = ?, publication_year = ?, edition = ?, language = ?, description = ?, " +
                "cover_image_path = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, book.getTitleEn());
            ps.setString(2, book.getTitleBn());
            ps.setString(3, book.getSubtitle());
            ps.setString(4, book.getIsbn10());
            ps.setString(5, book.getIsbn13());
            if (book.getPublisherId() != null) {
                ps.setLong(6, book.getPublisherId());
            } else {
                ps.setNull(6, Types.BIGINT);
            }
            if (book.getPublicationYear() != null) {
                ps.setInt(7, book.getPublicationYear());
            } else {
                ps.setNull(7, Types.SMALLINT);
            }
            ps.setString(8, book.getEdition());
            ps.setString(9, book.getLanguage());
            ps.setString(10, book.getDescription());
            ps.setString(11, book.getCoverImagePath());
            ps.setLong(12, book.getId());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update book: " + book.getId(), e);
        }
    }

    @Override
    public void archive(Long bookId) {
        String sql = "UPDATE books SET is_archived = TRUE WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bookId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to archive book: " + bookId, e);
        }
    }

    @Override
    public int countTotalCopies(Long bookId) {
        String sql = "SELECT COUNT(*) FROM book_copies WHERE book_id = ? AND status != 'WITHDRAWN'";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            // Handle gracefully if table doesn't exist yet
        }
        return 0;
    }

    @Override
    public int countAvailableCopies(Long bookId) {
        String sql = "SELECT COUNT(*) FROM book_copies WHERE book_id = ? AND status = 'AVAILABLE'";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            // Handle gracefully
        }
        return 0;
    }

    private void populateRelationsAndCounts(Book book) {
        if (authorRepository != null) {
            book.setAuthors(authorRepository.findByBookId(book.getId()));
        }
        if (categoryRepository != null) {
            book.setCategories(categoryRepository.findByBookId(book.getId()));
        }
        book.setTotalCopies(countTotalCopies(book.getId()));
        book.setAvailableCopies(countAvailableCopies(book.getId()));
    }

    private Book mapRow(ResultSet rs) throws SQLException {
        Book b = new Book();
        b.setId(rs.getLong("id"));
        b.setTitleEn(rs.getString("title_en"));
        b.setTitleBn(rs.getString("title_bn"));
        b.setSubtitle(rs.getString("subtitle"));
        b.setIsbn10(rs.getString("isbn_10"));
        b.setIsbn13(rs.getString("isbn_13"));

        long pubId = rs.getLong("publisher_id");
        if (!rs.wasNull()) {
            b.setPublisherId(pubId);
        }
        b.setPublisherName(rs.getString("publisher_name"));

        int pubYear = rs.getInt("publication_year");
        if (!rs.wasNull()) {
            b.setPublicationYear(pubYear);
        }
        b.setEdition(rs.getString("edition"));
        b.setLanguage(rs.getString("language"));
        b.setDescription(rs.getString("description"));
        b.setCoverImagePath(rs.getString("cover_image_path"));
        b.setArchived(rs.getBoolean("is_archived"));

        Timestamp cTs = rs.getTimestamp("created_at");
        if (cTs != null) b.setCreatedAt(cTs.toLocalDateTime());

        Timestamp uTs = rs.getTimestamp("updated_at");
        if (uTs != null) b.setUpdatedAt(uTs.toLocalDateTime());

        return b;
    }
}

