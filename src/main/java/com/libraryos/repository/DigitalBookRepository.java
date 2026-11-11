package com.libraryos.repository;

import com.libraryos.model.digital.DigitalBook;

import java.util.List;
import java.util.Optional;

public interface DigitalBookRepository {
    Optional<DigitalBook> findById(Long id);
    Optional<DigitalBook> findByBookId(Long bookId);
    List<DigitalBook> findAll();
    List<DigitalBook> search(String query);
    DigitalBook save(DigitalBook digitalBook);
    boolean update(DigitalBook digitalBook);
    boolean delete(Long id);
}

