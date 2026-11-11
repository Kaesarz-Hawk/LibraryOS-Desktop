package com.libraryos.model.circulation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class Loan {
    private Long id;
    private Long copyId;
    private String copyBarcode;
    private String copyAccessionNumber;
    private String bookTitle;
    private Long memberId;
    private String memberName;
    private String membershipNumber;
    private String memberPhone;
    private Long issuedBy;
    private String issuedByName;
    private LocalDateTime issuedAt;
    private LocalDate dueDate;
    private LocalDateTime returnedAt;
    private Long returnedTo;
    private String returnedToName;
    private String returnCondition;
    private int renewalCount = 0;
    private BigDecimal policyFinePerDaySnapshot = new BigDecimal("5.00");
    private LoanStatus status = LoanStatus.ACTIVE;
    private String notes;

    public Loan() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCopyId() {
        return copyId;
    }

    public void setCopyId(Long copyId) {
        this.copyId = copyId;
    }

    public String getCopyBarcode() {
        return copyBarcode;
    }

    public void setCopyBarcode(String copyBarcode) {
        this.copyBarcode = copyBarcode;
    }

    public String getCopyAccessionNumber() {
        return copyAccessionNumber;
    }

    public void setCopyAccessionNumber(String copyAccessionNumber) {
        this.copyAccessionNumber = copyAccessionNumber;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
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

    public String getMemberPhone() {
        return memberPhone;
    }

    public void setMemberPhone(String memberPhone) {
        this.memberPhone = memberPhone;
    }

    public Long getIssuedBy() {
        return issuedBy;
    }

    public void setIssuedBy(Long issuedBy) {
        this.issuedBy = issuedBy;
    }

    public String getIssuedByName() {
        return issuedByName;
    }

    public void setIssuedByName(String issuedByName) {
        this.issuedByName = issuedByName;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDateTime getReturnedAt() {
        return returnedAt;
    }

    public void setReturnedAt(LocalDateTime returnedAt) {
        this.returnedAt = returnedAt;
    }

    public Long getReturnedTo() {
        return returnedTo;
    }

    public void setReturnedTo(Long returnedTo) {
        this.returnedTo = returnedTo;
    }

    public String getReturnedToName() {
        return returnedToName;
    }

    public void setReturnedToName(String returnedToName) {
        this.returnedToName = returnedToName;
    }

    public String getReturnCondition() {
        return returnCondition;
    }

    public void setReturnCondition(String returnCondition) {
        this.returnCondition = returnCondition;
    }

    public int getRenewalCount() {
        return renewalCount;
    }

    public void setRenewalCount(int renewalCount) {
        this.renewalCount = renewalCount;
    }

    public BigDecimal getPolicyFinePerDaySnapshot() {
        return policyFinePerDaySnapshot;
    }

    public void setPolicyFinePerDaySnapshot(BigDecimal policyFinePerDaySnapshot) {
        this.policyFinePerDaySnapshot = policyFinePerDaySnapshot;
    }

    public LoanStatus getStatus() {
        return status;
    }

    public void setStatus(LoanStatus status) {
        this.status = status != null ? status : LoanStatus.ACTIVE;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean isOverdue() {
        if (status != LoanStatus.ACTIVE || dueDate == null) return false;
        return LocalDate.now().isAfter(dueDate);
    }

    public long getDaysOverdue() {
        if (!isOverdue()) return 0;
        return ChronoUnit.DAYS.between(dueDate, LocalDate.now());
    }

    @Override
    public String toString() {
        return "Loan #" + id + " [" + copyBarcode + " -> " + membershipNumber + "]";
    }
}

