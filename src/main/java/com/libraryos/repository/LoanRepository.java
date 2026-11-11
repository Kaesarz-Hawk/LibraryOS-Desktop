package com.libraryos.repository;

import com.libraryos.model.circulation.Loan;
import com.libraryos.model.circulation.LoanStatus;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public interface LoanRepository {
    Loan create(Loan loan, Connection conn);
    void update(Loan loan, Connection conn);
    Optional<Loan> findById(Long id);
    Optional<Loan> findById(Long id, Connection conn);
    Optional<Loan> findByIdForUpdate(Long id, Connection conn);
    Optional<Loan> findActiveLoanByCopyId(Long copyId);
    Optional<Loan> findActiveLoanByCopyId(Long copyId, Connection conn);
    Optional<Loan> findActiveLoanByCopyIdForUpdate(Long copyId, Connection conn);
    List<Loan> findActiveLoansByMemberId(Long memberId);
    int countActiveLoansByMemberId(Long memberId);
    int countActiveLoansByMemberId(Long memberId, Connection conn);
    boolean hasActiveLoanForBook(Long bookId, Long memberId);
    boolean hasActiveLoanForBook(Long bookId, Long memberId, Connection conn);
    List<Loan> searchLoans(String query, LoanStatus status, Boolean overdueOnly);
}

