package com.libraryos.repository;

import com.libraryos.model.catalog.Author;

import java.util.List;
import java.util.Optional;

public interface AuthorRepository {
    List<Author> findAll();
    Optional<Author> findById(Long id);
    Author create(Author author);
    List<Author> findByBookId(Long bookId);
    void setBookAuthors(Long bookId, List<Long> authorIds);
}

