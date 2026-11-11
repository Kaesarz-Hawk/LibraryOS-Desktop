package com.libraryos.repository;

import com.libraryos.model.circulation.LoanRenewal;

import java.sql.Connection;
import java.util.List;

public interface LoanRenewalRepository {
    void insert(LoanRenewal renewal, Connection conn);
    List<LoanRenewal> findByLoanId(Long loanId);
}

