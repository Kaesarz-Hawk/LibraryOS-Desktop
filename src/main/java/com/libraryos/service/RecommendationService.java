package com.libraryos.service;

import com.libraryos.model.catalog.Book;
import com.libraryos.repository.BookRepository;
import com.libraryos.repository.ReportRepository;
import com.libraryos.repository.ReviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Intelligent book recommendation engine based on popularity, category affinity, and review ratings.
 */
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);

    private final BookRepository bookRepository;
    private final ReportRepository reportRepository;
    private final ReviewRepository reviewRepository;

    public RecommendationService(BookRepository bookRepository,
                                 ReportRepository reportRepository,
                                 ReviewRepository reviewRepository) {
        this.bookRepository = bookRepository;
        this.reportRepository = reportRepository;
        this.reviewRepository = reviewRepository;
    }

    /**
     * Recommends books similar to a given target book (matching category or author).
     */
    public List<Book> getSimilarBooks(Long targetBookId, int limit) {
        if (targetBookId == null) {
            return getPopularBooks(limit);
        }

        Optional<Book> targetOpt = bookRepository.findById(targetBookId);
        if (targetOpt.isEmpty()) {
            return getPopularBooks(limit);
        }

        Book target = targetOpt.get();
        List<Book> allBooks = bookRepository.findAll(false);

        Set<Long> targetCategoryIds = target.getCategories().stream()
                .map(c -> c.getId())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Long> targetAuthorIds = target.getAuthors().stream()
                .map(a -> a.getId())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return allBooks.stream()
                .filter(b -> !b.getId().equals(targetBookId))
                .sorted((b1, b2) -> {
                    int score1 = calculateAffinityScore(b1, targetCategoryIds, targetAuthorIds);
                    int score2 = calculateAffinityScore(b2, targetCategoryIds, targetAuthorIds);
                    return Integer.compare(score2, score1);
                })
                .limit(limit > 0 ? limit : 5)
                .collect(Collectors.toList());
    }

    /**
     * Returns top recommended books based on circulation trends and review ratings.
     */
    public List<Book> getPopularBooks(int limit) {
        List<Book> all = bookRepository.findAll(false);
        if (all.isEmpty()) {
            return Collections.emptyList();
        }

        // Rank by average rating and available copies
        return all.stream()
                .sorted((b1, b2) -> {
                    double r1 = reviewRepository != null ? reviewRepository.getAverageRatingForBook(b1.getId()) : 0.0;
                    double r2 = reviewRepository != null ? reviewRepository.getAverageRatingForBook(b2.getId()) : 0.0;
                    int ratingComp = Double.compare(r2, r1);
                    if (ratingComp != 0) return ratingComp;
                    return Integer.compare(b2.getAvailableCopies(), b1.getAvailableCopies());
                })
                .limit(limit > 0 ? limit : 6)
                .collect(Collectors.toList());
    }

    private int calculateAffinityScore(Book book, Set<Long> targetCategoryIds, Set<Long> targetAuthorIds) {
        int score = 0;
        if (book.getCategories() != null) {
            for (var c : book.getCategories()) {
                if (c.getId() != null && targetCategoryIds.contains(c.getId())) {
                    score += 3;
                }
            }
        }
        if (book.getAuthors() != null) {
            for (var a : book.getAuthors()) {
                if (a.getId() != null && targetAuthorIds.contains(a.getId())) {
                    score += 5;
                }
            }
        }
        return score;
    }
}

