package com.libraryos.service;

import com.libraryos.core.exception.ValidationException;
import com.libraryos.core.security.PermissionGuard;
import com.libraryos.core.security.SessionManager;
import com.libraryos.core.security.UserSession;
import com.libraryos.model.interaction.Review;
import com.libraryos.model.user.PermissionCode;
import com.libraryos.repository.ReviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

public class ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewService.class);

    private final ReviewRepository reviewRepository;
    private final AuditService auditService;

    public ReviewService(ReviewRepository reviewRepository, AuditService auditService) {
        this.reviewRepository = reviewRepository;
        this.auditService = auditService;
    }

    public List<Review> getApprovedReviewsForBook(Long bookId) {
        return reviewRepository.findApprovedByBookId(bookId);
    }

    public List<Review> getPendingOrReportedReviews() {
        PermissionGuard.requirePermission(PermissionCode.BOOK_UPDATE);
        return reviewRepository.findAllPendingModeration();
    }

    public double getAverageRating(Long bookId) {
        return reviewRepository.getAverageRatingForBook(bookId);
    }

    public int getReviewCount(Long bookId) {
        return reviewRepository.getReviewCountForBook(bookId);
    }

    public Review submitReview(Long bookId, Long memberId, int rating, String title, String text) {
        if (bookId == null) throw new ValidationException("Book ID is required");
        if (memberId == null) throw new ValidationException("Member ID is required");
        if (rating < 1 || rating > 5) throw new ValidationException("Rating must be between 1 and 5 stars");

        UserSession session = SessionManager.getCurrentSession().orElse(null);
        if (session != null && session.isMemberOnly() && session.getMemberId() != null) {
            if (!session.getMemberId().equals(memberId)) {
                throw new ValidationException("Members may only submit reviews under their own membership.");
            }
        }

        Review review = new Review();
        review.setBookId(bookId);
        review.setMemberId(memberId);
        review.setRating(rating);
        review.setReviewTitle(title != null ? title.trim() : null);
        review.setReviewText(text != null ? text.trim() : null);
        review.setApproved(true); // Auto-approve by default; subject to moderation
        review.setReported(false);
        review.setCreatedAt(LocalDateTime.now());

        Review saved = reviewRepository.save(review);
        log.info("Review submitted for book #{} by member #{} (Rating: {} stars)", bookId, memberId, rating);
        return saved;
    }

    public boolean moderateReview(Long reviewId, boolean approved, boolean reported) {
        PermissionGuard.requirePermission(PermissionCode.BOOK_UPDATE);
        boolean updated = reviewRepository.updateStatus(reviewId, approved, reported);
        if (updated && auditService != null) {
            auditService.log("REVIEW_MODERATE", "Review", String.valueOf(reviewId),
                    "Moderated review: approved=" + approved + ", reported=" + reported, null);
        }
        return updated;
    }
}
