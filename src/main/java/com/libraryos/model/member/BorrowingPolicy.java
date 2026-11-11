package com.libraryos.model.member;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BorrowingPolicy {
    private int id;
    private int memberTypeId;
    private String memberTypeCode;
    private int maxActiveLoans = 3;
    private int loanDurationDays = 14;
    private int maxRenewals = 1;
    private int renewalDurationDays = 7;
    private int gracePeriodDays = 0;
    private BigDecimal finePerDayBdt = new BigDecimal("5.00");
    private BigDecimal maxFinePerLoanBdt;
    private BigDecimal maxUnpaidFineLimitBdt = new BigDecimal("100.00");
    private int reservationHoldDays = 3;
    private boolean countHolidaysInOverdue = false;
    private LocalDateTime updatedAt;

    public BorrowingPolicy() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getMemberTypeId() {
        return memberTypeId;
    }

    public void setMemberTypeId(int memberTypeId) {
        this.memberTypeId = memberTypeId;
    }

    public String getMemberTypeCode() {
        return memberTypeCode;
    }

    public void setMemberTypeCode(String memberTypeCode) {
        this.memberTypeCode = memberTypeCode;
    }

    /**
     * Backward-compatible alias for callers that represent the policy type as a name.
     */
    public String getMemberTypeName() {
        return memberTypeCode;
    }

    public void setMemberTypeName(String memberTypeName) {
        this.memberTypeCode = memberTypeName;
    }

    public int getMaxActiveLoans() {
        return maxActiveLoans;
    }

    public void setMaxActiveLoans(int maxActiveLoans) {
        this.maxActiveLoans = maxActiveLoans;
    }

    public int getLoanDurationDays() {
        return loanDurationDays;
    }

    public void setLoanDurationDays(int loanDurationDays) {
        this.loanDurationDays = loanDurationDays;
    }

    public int getMaxRenewals() {
        return maxRenewals;
    }

    public void setMaxRenewals(int maxRenewals) {
        this.maxRenewals = maxRenewals;
    }

    public int getRenewalDurationDays() {
        return renewalDurationDays;
    }

    public void setRenewalDurationDays(int renewalDurationDays) {
        this.renewalDurationDays = renewalDurationDays;
    }

    public int getGracePeriodDays() {
        return gracePeriodDays;
    }

    public void setGracePeriodDays(int gracePeriodDays) {
        this.gracePeriodDays = gracePeriodDays;
    }

    public BigDecimal getFinePerDayBdt() {
        return finePerDayBdt;
    }

    public void setFinePerDayBdt(BigDecimal finePerDayBdt) {
        this.finePerDayBdt = finePerDayBdt;
    }

    public BigDecimal getMaxFinePerLoanBdt() {
        return maxFinePerLoanBdt;
    }

    public void setMaxFinePerLoanBdt(BigDecimal maxFinePerLoanBdt) {
        this.maxFinePerLoanBdt = maxFinePerLoanBdt;
    }

    public BigDecimal getMaxUnpaidFineLimitBdt() {
        return maxUnpaidFineLimitBdt;
    }

    public void setMaxUnpaidFineLimitBdt(BigDecimal maxUnpaidFineLimitBdt) {
        this.maxUnpaidFineLimitBdt = maxUnpaidFineLimitBdt;
    }

    public int getReservationHoldDays() {
        return reservationHoldDays;
    }

    public void setReservationHoldDays(int reservationHoldDays) {
        this.reservationHoldDays = reservationHoldDays;
    }

    public boolean isCountHolidaysInOverdue() {
        return countHolidaysInOverdue;
    }

    public void setCountHolidaysInOverdue(boolean countHolidaysInOverdue) {
        this.countHolidaysInOverdue = countHolidaysInOverdue;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
