package com.libraryos.repository;

import com.libraryos.model.acquisition.Supplier;

import java.util.List;
import java.util.Optional;

public interface SupplierRepository {
    Optional<Supplier> findById(Long id);
    List<Supplier> findAll();
    List<Supplier> search(String query);
    Supplier save(Supplier supplier);
    boolean update(Supplier supplier);
    boolean delete(Long id);
}

