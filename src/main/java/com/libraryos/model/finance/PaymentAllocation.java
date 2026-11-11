package com.libraryos.model.finance;

import java.math.BigDecimal;

public class PaymentAllocation {
    private Long id;
    private Long paymentId;
    private Long fineId;
    private BigDecimal allocatedAmountBdt = BigDecimal.ZERO;
    private String fineType;
    private String bookTitle;

    public PaymentAllocation() {
    }

    public PaymentAllocation(Long paymentId, Long fineId, BigDecimal allocatedAmountBdt) {
        this.paymentId = paymentId;
        this.fineId = fineId;
        this.allocatedAmountBdt = allocatedAmountBdt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public Long getFineId() {
        return fineId;
    }

    public void setFineId(Long fineId) {
        this.fineId = fineId;
    }

    public BigDecimal getAllocatedAmountBdt() {
        return allocatedAmountBdt;
    }

    public void setAllocatedAmountBdt(BigDecimal allocatedAmountBdt) {
        this.allocatedAmountBdt = allocatedAmountBdt != null ? allocatedAmountBdt : BigDecimal.ZERO;
    }

    public String getFineType() {
        return fineType;
    }

    public void setFineType(String fineType) {
        this.fineType = fineType;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }
}

