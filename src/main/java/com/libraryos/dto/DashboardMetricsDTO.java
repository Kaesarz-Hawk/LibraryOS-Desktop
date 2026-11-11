package com.libraryos.dto;

import java.math.BigDecimal;

public class DashboardMetricsDTO {
    private long totalBooks;
    private long totalCopies;
    private long activeLoans;
    private long overdueLoans;
    private long totalMembers;
    private BigDecimal pendingFinesBdt;

    public DashboardMetricsDTO() {
        this.totalBooks = 0;
        this.totalCopies = 0;
        this.activeLoans = 0;
        this.overdueLoans = 0;
        this.totalMembers = 0;
        this.pendingFinesBdt = BigDecimal.ZERO;
    }

    public DashboardMetricsDTO(long totalBooks, long totalCopies, long activeLoans, long overdueLoans, long totalMembers, BigDecimal pendingFinesBdt) {
        this.totalBooks = totalBooks;
        this.totalCopies = totalCopies;
        this.activeLoans = activeLoans;
        this.overdueLoans = overdueLoans;
        this.totalMembers = totalMembers;
        this.pendingFinesBdt = pendingFinesBdt != null ? pendingFinesBdt : BigDecimal.ZERO;
    }

    public long getTotalBooks() {
        return totalBooks;
    }

    public void setTotalBooks(long totalBooks) {
        this.totalBooks = totalBooks;
    }

    public long getTotalCopies() {
        return totalCopies;
    }

    public void setTotalCopies(long totalCopies) {
        this.totalCopies = totalCopies;
    }

    public long getActiveLoans() {
        return activeLoans;
    }

    public void setActiveLoans(long activeLoans) {
        this.activeLoans = activeLoans;
    }

    public long getOverdueLoans() {
        return overdueLoans;
    }

    public void setOverdueLoans(long overdueLoans) {
        this.overdueLoans = overdueLoans;
    }

    public long getTotalMembers() {
        return totalMembers;
    }

    public void setTotalMembers(long totalMembers) {
        this.totalMembers = totalMembers;
    }

    public BigDecimal getPendingFinesBdt() {
        return pendingFinesBdt;
    }

    public void setPendingFinesBdt(BigDecimal pendingFinesBdt) {
        this.pendingFinesBdt = pendingFinesBdt;
    }
}
