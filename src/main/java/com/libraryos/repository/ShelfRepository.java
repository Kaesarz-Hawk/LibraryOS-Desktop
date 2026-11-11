package com.libraryos.repository;

import com.libraryos.model.inventory.Shelf;

import java.util.List;
import java.util.Optional;

public interface ShelfRepository {
    List<Shelf> findAll();
    Optional<Shelf> findById(Long id);
    Optional<Shelf> findByCode(String code);
    Shelf create(Shelf shelf);
}

