package com.libraryos.repository;

import com.libraryos.model.finance.Fine;
import com.libraryos.model.finance.FineStatus;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public interface FineRepository {
    Fine create(Fine fine);
    Fine create(Fine fine, Connection conn);
    void update(Fine fine, Connection conn);
    Optional<Fine> findById(Long id);
    Optional<Fine> findById(Long id, Connection conn);
    Optional<Fine> findByIdForUpdate(Long id, Connection conn);
    List<Fine> findByMemberId(Long memberId);
    List<Fine> findUnpaidByMemberId(Long memberId);
    List<Fine> findUnpaidByMemberId(Long memberId, Connection conn);
    BigDecimal getUnpaidTotalByMemberId(Long memberId);
    BigDecimal getUnpaidTotalByMemberId(Long memberId, Connection conn);
    List<Fine> searchFines(String query, FineStatus status);
}
