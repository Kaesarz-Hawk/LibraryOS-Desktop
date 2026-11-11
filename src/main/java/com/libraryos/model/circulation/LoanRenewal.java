package com.libraryos.model.circulation;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class LoanRenewal {
    private Long id;
    private Long loanId;
    private LocalDate previousDueDate;
    private LocalDate newDueDate;
    private Long renewedBy;
    private String renewedByName;
    private LocalDateTime renewedAt;

    public LoanRenewal() {
    }

    public LoanRenewal(Long loanId, LocalDate previousDueDate, LocalDate newDueDate, Long renewedBy) {
        this.loanId = loanId;
        this.previousDueDate = previousDueDate;
        this.newDueDate = newDueDate;
        this.renewedBy = renewedBy;
        this.renewedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getLoanId() {
        return loanId;
    }

    public void setLoanId(Long loanId) {
        this.loanId = loanId;
    }

    public LocalDate getPreviousDueDate() {
        return previousDueDate;
    }

    public void setPreviousDueDate(LocalDate previousDueDate) {
        this.previousDueDate = previousDueDate;
    }

    public LocalDate getNewDueDate() {
        return newDueDate;
    }

    public void setNewDueDate(LocalDate newDueDate) {
        this.newDueDate = newDueDate;
    }

    public Long getRenewedBy() {
        return renewedBy;
    }

    public void setRenewedBy(Long renewedBy) {
        this.renewedBy = renewedBy;
    }

    public String getRenewedByName() {
        return renewedByName;
    }

    public void setRenewedByName(String renewedByName) {
        this.renewedByName = renewedByName;
    }

    public LocalDateTime getRenewedAt() {
        return renewedAt;
    }

    public void setRenewedAt(LocalDateTime renewedAt) {
        this.renewedAt = renewedAt;
    }
}

