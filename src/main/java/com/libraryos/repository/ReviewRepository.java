package com.libraryos.repository;

import com.libraryos.model.interaction.Review;

import java.util.List;

public interface ReviewRepository {
    List<Review> findByBookId(Long bookId);
    List<Review> findApprovedByBookId(Long bookId);
    List<Review> findAllPendingModeration();
    Review save(Review review);
    boolean updateStatus(Long id, boolean approved, boolean reported);
    double getAverageRatingForBook(Long bookId);
    int getReviewCountForBook(Long bookId);
}

