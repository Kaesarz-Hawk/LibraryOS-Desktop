package com.libraryos.repository;

import com.libraryos.model.finance.FineAdjustment;

import java.sql.Connection;
import java.util.List;

public interface FineAdjustmentRepository {
    FineAdjustment create(FineAdjustment adjustment, Connection conn);
    List<FineAdjustment> findByFineId(Long fineId);
}

