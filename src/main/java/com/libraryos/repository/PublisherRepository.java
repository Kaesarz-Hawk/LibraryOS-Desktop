package com.libraryos.repository;

import com.libraryos.model.catalog.Publisher;

import java.util.List;
import java.util.Optional;

public interface PublisherRepository {
    List<Publisher> findAll();
    Optional<Publisher> findById(Long id);
    Publisher create(Publisher publisher);
}

