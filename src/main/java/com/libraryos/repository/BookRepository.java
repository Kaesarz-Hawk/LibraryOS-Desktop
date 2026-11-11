package com.libraryos.repository;

import com.libraryos.model.catalog.Book;

import java.util.List;
import java.util.Optional;

public interface BookRepository {
    List<Book> findAll(boolean includeArchived);
    List<Book> search(String query, Long categoryId, Long publisherId);
    Optional<Book> findById(Long id);
    Optional<Book> findByIsbn(String isbn);
    Book create(Book book);
    void update(Book book);
    void archive(Long bookId);
    int countTotalCopies(Long bookId);
    int countAvailableCopies(Long bookId);
}

