package com.libraryos.model.finance;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Fine {
    private Long id;
    private Long memberId;
    private String memberName;
    private String membershipNumber;
    private Long loanId;
    private String copyBarcode;
    private String bookTitle;
    private FineType fineType = FineType.OVERDUE;
    private BigDecimal originalAmountBdt = BigDecimal.ZERO;
    private BigDecimal waivedAmountBdt = BigDecimal.ZERO;
    private BigDecimal paidAmountBdt = BigDecimal.ZERO;
    private FineStatus status = FineStatus.UNPAID;
    private Long createdBy;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Fine() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public String getMemberName() {
        return memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }

    public String getMembershipNumber() {
        return membershipNumber;
    }

    public void setMembershipNumber(String membershipNumber) {
        this.membershipNumber = membershipNumber;
    }

    public Long getLoanId() {
        return loanId;
    }

    public void setLoanId(Long loanId) {
        this.loanId = loanId;
    }

    public String getCopyBarcode() {
        return copyBarcode;
    }

    public void setCopyBarcode(String copyBarcode) {
        this.copyBarcode = copyBarcode;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }

    public FineType getFineType() {
        return fineType;
    }

    public void setFineType(FineType fineType) {
        this.fineType = fineType != null ? fineType : FineType.OVERDUE;
    }

    public BigDecimal getOriginalAmountBdt() {
        return originalAmountBdt;
    }

    public void setOriginalAmountBdt(BigDecimal originalAmountBdt) {
        this.originalAmountBdt = originalAmountBdt != null ? originalAmountBdt : BigDecimal.ZERO;
    }

    public BigDecimal getWaivedAmountBdt() {
        return waivedAmountBdt;
    }

    public void setWaivedAmountBdt(BigDecimal waivedAmountBdt) {
        this.waivedAmountBdt = waivedAmountBdt != null ? waivedAmountBdt : BigDecimal.ZERO;
    }

    public BigDecimal getPaidAmountBdt() {
        return paidAmountBdt;
    }

    public void setPaidAmountBdt(BigDecimal paidAmountBdt) {
        this.paidAmountBdt = paidAmountBdt != null ? paidAmountBdt : BigDecimal.ZERO;
    }

    public FineStatus getStatus() {
        return status;
    }

    public void setStatus(FineStatus status) {
        this.status = status != null ? status : FineStatus.UNPAID;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public BigDecimal getRemainingAmountBdt() {
        BigDecimal balance = originalAmountBdt.subtract(waivedAmountBdt).subtract(paidAmountBdt);
        return balance.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : balance;
    }

    public BigDecimal getAmount() {
        return getOriginalAmountBdt();
    }

    public BigDecimal getPaidAmount() {
        return getPaidAmountBdt();
    }

    public BigDecimal getUnpaidBalance() {
        return getRemainingAmountBdt();
    }

    public String getDescription() {
        return "Fine #" + (id != null ? id : "—") + (bookTitle != null ? " (" + bookTitle + ")" : "");
    }

    @Override
    public String toString() {
        return "Fine #" + id + " [৳" + originalAmountBdt + " - " + status + "]";
    }
}

