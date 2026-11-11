package com.libraryos.repository;

import com.libraryos.model.acquisition.Acquisition;
import com.libraryos.model.acquisition.AcquisitionItem;
import com.libraryos.model.acquisition.AcquisitionStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AcquisitionRepository {
    Optional<Acquisition> findById(Long id);
    Optional<Acquisition> findByAcquisitionNumber(String acqNumber);
    List<Acquisition> findAll();
    List<Acquisition> findByStatus(AcquisitionStatus status);
    List<AcquisitionItem> findItemsByAcquisitionId(Long acquisitionId);
    Acquisition save(Acquisition acquisition);
    void saveItem(AcquisitionItem item);
    boolean updateStatus(Long id, AcquisitionStatus status, LocalDate receivedDate);
    boolean update(Acquisition acquisition);
}

