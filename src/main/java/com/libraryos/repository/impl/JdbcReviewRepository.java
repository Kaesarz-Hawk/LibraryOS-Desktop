package com.libraryos.repository.impl;

import com.libraryos.config.DatabaseConfig;
import com.libraryos.core.exception.DatabaseException;
import com.libraryos.model.interaction.Review;
import com.libraryos.repository.ReviewRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class JdbcReviewRepository implements ReviewRepository {

    private static final String BASE_SELECT =
            "SELECT r.id, r.book_id, b.title_en AS book_title, r.member_id, m.full_name_en AS member_name, " +
            "r.rating, r.review_title, r.review_text, r.is_approved, r.is_reported, r.created_at " +
            "FROM reviews r " +
            "JOIN books b ON r.book_id = b.id " +
            "JOIN members m ON r.member_id = m.id ";

    @Override
    public List<Review> findByBookId(Long bookId) {
        String sql = BASE_SELECT + "WHERE r.book_id = ? ORDER BY r.created_at DESC";
        List<Review> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find reviews for book id: " + bookId, e);
        }
        return list;
    }

    @Override
    public List<Review> findApprovedByBookId(Long bookId) {
        String sql = BASE_SELECT + "WHERE r.book_id = ? AND r.is_approved = TRUE ORDER BY r.created_at DESC";
        List<Review> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find approved reviews for book id: " + bookId, e);
        }
        return list;
    }

    @Override
    public List<Review> findAllPendingModeration() {
        String sql = BASE_SELECT + "WHERE r.is_approved = FALSE OR r.is_reported = TRUE ORDER BY r.created_at DESC";
        List<Review> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find reviews pending moderation", e);
        }
        return list;
    }

    @Override
    public Review save(Review review) {
        String sql = "INSERT INTO reviews (book_id, member_id, rating, review_title, review_text, is_approved, is_reported, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, review.getBookId());
            ps.setLong(2, review.getMemberId());
            ps.setInt(3, review.getRating());
            ps.setString(4, review.getReviewTitle());
            ps.setString(5, review.getReviewText());
            ps.setBoolean(6, review.isApproved());
            ps.setBoolean(7, review.isReported());
            ps.setTimestamp(8, Timestamp.valueOf(review.getCreatedAt() != null ? review.getCreatedAt() : java.time.LocalDateTime.now()));

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    review.setId(keys.getLong(1));
                }
            }
            return review;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save book review", e);
        }
    }

    @Override
    public boolean updateStatus(Long id, boolean approved, boolean reported) {
        String sql = "UPDATE reviews SET is_approved = ?, is_reported = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, approved);
            ps.setBoolean(2, reported);
            ps.setLong(3, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update review status id: " + id, e);
        }
    }

    @Override
    public double getAverageRatingForBook(Long bookId) {
        String sql = "SELECT AVG(rating) FROM reviews WHERE book_id = ? AND is_approved = TRUE";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to calculate average rating for book id: " + bookId, e);
        }
        return 0.0;
    }

    @Override
    public int getReviewCountForBook(Long bookId) {
        String sql = "SELECT COUNT(*) FROM reviews WHERE book_id = ? AND is_approved = TRUE";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to count reviews for book id: " + bookId, e);
        }
        return 0;
    }

    private Review mapRow(ResultSet rs) throws SQLException {
        Review r = new Review();
        r.setId(rs.getLong("id"));
        r.setBookId(rs.getLong("book_id"));
        r.setBookTitle(rs.getString("book_title"));
        r.setMemberId(rs.getLong("member_id"));
        r.setMemberName(rs.getString("member_name"));
        r.setRating(rs.getInt("rating"));
        r.setReviewTitle(rs.getString("review_title"));
        r.setReviewText(rs.getString("review_text"));
        r.setApproved(rs.getBoolean("is_approved"));
        r.setReported(rs.getBoolean("is_reported"));

        Timestamp cat = rs.getTimestamp("created_at");
        if (cat != null) r.setCreatedAt(cat.toLocalDateTime());

        return r;
    }
}

