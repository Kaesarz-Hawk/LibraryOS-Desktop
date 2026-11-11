package com.libraryos.service;

import com.libraryos.model.catalog.Author;
import com.libraryos.model.catalog.Book;
import com.libraryos.model.catalog.Category;
import com.libraryos.repository.BookRepository;
import com.libraryos.repository.ReportRepository;
import com.libraryos.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class RecommendationServiceTest {

    private BookRepository bookRepository;
    private ReportRepository reportRepository;
    private ReviewRepository reviewRepository;
    private RecommendationService recommendationService;

    @BeforeEach
    void setUp() {
        bookRepository = Mockito.mock(BookRepository.class);
        reportRepository = Mockito.mock(ReportRepository.class);
        reviewRepository = Mockito.mock(ReviewRepository.class);
        recommendationService = new RecommendationService(bookRepository, reportRepository, reviewRepository);
    }

    @Test
    void testGetSimilarBooksBasedOnCategoryAndAuthor() {
        Category cat1 = new Category(1L, "Software Engineering", "সফটওয়্যার", "SE", null);
        Author author1 = new Author(1L, "Robert C. Martin", "রবার্ট সি মার্টিন", null);

        Book target = new Book();
        target.setId(1L);
        target.setTitleEn("Clean Code");
        target.setCategories(List.of(cat1));
        target.setAuthors(List.of(author1));

        Book book2 = new Book();
        book2.setId(2L);
        book2.setTitleEn("Clean Architecture");
        book2.setCategories(List.of(cat1));
        book2.setAuthors(List.of(author1));

        Book book3 = new Book();
        book3.setId(3L);
        book3.setTitleEn("The Hobbit");
        book3.setCategories(List.of(new Category(2L, "Fantasy", "কল্পকাহিনি", "FAN", null)));
        book3.setAuthors(List.of(new Author(2L, "J.R.R. Tolkien", "টলকিন", null)));

        when(bookRepository.findById(1L)).thenReturn(Optional.of(target));
        when(bookRepository.findAll(false)).thenReturn(List.of(target, book2, book3));

        List<Book> similar = recommendationService.getSimilarBooks(1L, 5);
        assertFalse(similar.isEmpty());
        assertEquals("Clean Architecture", similar.get(0).getTitleEn());
    }

    @Test
    void testGetPopularBooksRanksByReviewRatings() {
        Book b1 = new Book();
        b1.setId(1L);
        b1.setTitleEn("Book Low Rating");

        Book b2 = new Book();
        b2.setId(2L);
        b2.setTitleEn("Book High Rating");

        when(bookRepository.findAll(false)).thenReturn(List.of(b1, b2));
        when(reviewRepository.getAverageRatingForBook(1L)).thenReturn(3.2);
        when(reviewRepository.getAverageRatingForBook(2L)).thenReturn(4.9);

        List<Book> popular = recommendationService.getPopularBooks(2);
        assertEquals(2, popular.size());
        assertEquals("Book High Rating", popular.get(0).getTitleEn());
    }
}

