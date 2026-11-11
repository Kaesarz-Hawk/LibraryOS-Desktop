package com.libraryos.repository;

import com.libraryos.model.catalog.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository {
    List<Category> findAll();
    Optional<Category> findById(Long id);
    Category create(Category category);
    List<Category> findByBookId(Long bookId);
    void setBookCategories(Long bookId, List<Long> categoryIds);
}

