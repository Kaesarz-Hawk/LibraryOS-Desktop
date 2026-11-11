package com.libraryos.service;

import com.libraryos.core.exception.UnauthorizedException;
import com.libraryos.core.exception.ValidationException;
import com.libraryos.core.security.SessionManager;
import com.libraryos.core.security.UserSession;
import com.libraryos.model.catalog.Book;
import com.libraryos.repository.AuthorRepository;
import com.libraryos.repository.BookRepository;
import com.libraryos.repository.CategoryRepository;
import com.libraryos.repository.PublisherRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    @Mock
    private BookRepository bookRepository;
    @Mock
    private AuthorRepository authorRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private PublisherRepository publisherRepository;
    @Mock
    private AuditService auditService;

    private CatalogService catalogService;

    @BeforeEach
    void setUp() {
        catalogService = new CatalogService(bookRepository, authorRepository, categoryRepository, publisherRepository, auditService);
        UserSession adminSession = new UserSession(1L, "admin", "Admin", null, null,
                Collections.singleton("SUPER_ADMIN"),
                Set.of("BOOK_CREATE", "BOOK_UPDATE", "BOOK_ARCHIVE", "BOOK_VIEW"));
        SessionManager.setSession(adminSession);
    }

    @AfterEach
    void tearDown() {
        SessionManager.clearSession();
    }

    @Test
    @DisplayName("Should create book title when valid and authorized")
    void testCreateBookSuccess() {
        Book book = new Book();
        book.setTitleEn("Effective Java");
        book.setIsbn13("9780134685991");

        when(bookRepository.findByIsbn("9780134685991")).thenReturn(Optional.empty());

        Book savedBook = new Book();
        savedBook.setId(10L);
        savedBook.setTitleEn("Effective Java");
        when(bookRepository.create(any(Book.class))).thenReturn(savedBook);
        when(bookRepository.findById(10L)).thenReturn(Optional.of(savedBook));

        Book created = catalogService.createBook(book, List.of(1L), List.of(2L));

        assertNotNull(created);
        assertEquals(10L, created.getId());
        verify(authorRepository).setBookAuthors(10L, List.of(1L));
        verify(categoryRepository).setBookCategories(10L, List.of(2L));
        verify(auditService).log(eq("BOOK_CREATED"), eq("BOOK"), eq("10"), anyString(), anyString());
    }

    @Test
    @DisplayName("Should reject book with duplicate ISBN-13")
    void testDuplicateIsbn() {
        Book book = new Book();
        book.setTitleEn("Duplicate Test");
        book.setIsbn13("9780134685991");

        Book existing = new Book();
        existing.setId(5L);
        existing.setTitleEn("Existing Book");
        when(bookRepository.findByIsbn("9780134685991")).thenReturn(Optional.of(existing));

        ValidationException ex = assertThrows(ValidationException.class,
                () -> catalogService.createBook(book, null, null));
        assertTrue(ex.getMessage().contains("already exists"));
    }

    @Test
    @DisplayName("Should reject book with empty title")
    void testEmptyTitle() {
        Book book = new Book();
        book.setTitleEn("");

        assertThrows(ValidationException.class, () -> catalogService.createBook(book, null, null));
    }

    @Test
    @DisplayName("Should enforce BOOK_CREATE permission")
    void testPermissionEnforced() {
        UserSession memberSession = new UserSession(2L, "member", "Member", null, 1L,
                Collections.singleton("MEMBER"), Collections.singleton("BOOK_VIEW"));
        SessionManager.setSession(memberSession);

        Book book = new Book();
        book.setTitleEn("Unauthorized Book");

        assertThrows(UnauthorizedException.class, () -> catalogService.createBook(book, null, null));
    }
}
