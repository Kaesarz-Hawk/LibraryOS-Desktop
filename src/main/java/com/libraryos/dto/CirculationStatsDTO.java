package com.libraryos.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CirculationStatsDTO {
    private long totalActiveLoans;
    private long totalOverdueLoans;
    private long totalReturnedLoans;
    private long totalReservationsWaiting;
    private long totalReservationsReady;
    private BigDecimal totalFinesCollected = BigDecimal.ZERO;
    private BigDecimal totalFinesUnpaid = BigDecimal.ZERO;
    private BigDecimal totalFinesWaived = BigDecimal.ZERO;

    private List<PopularBookItem> topBorrowedBooks = new ArrayList<>();

    public static class PopularBookItem {
        private String title;
        private String isbn;
        private long borrowCount;

        public PopularBookItem(String title, String isbn, long borrowCount) {
            this.title = title;
            this.isbn = isbn;
            this.borrowCount = borrowCount;
        }

        public String getTitle() { return title; }
        public String getIsbn() { return isbn; }
        public long getBorrowCount() { return borrowCount; }
    }

    public CirculationStatsDTO() {
    }

    public long getTotalActiveLoans() { return totalActiveLoans; }
    public void setTotalActiveLoans(long totalActiveLoans) { this.totalActiveLoans = totalActiveLoans; }

    public long getTotalOverdueLoans() { return totalOverdueLoans; }
    public void setTotalOverdueLoans(long totalOverdueLoans) { this.totalOverdueLoans = totalOverdueLoans; }

    public long getTotalReturnedLoans() { return totalReturnedLoans; }
    public void setTotalReturnedLoans(long totalReturnedLoans) { this.totalReturnedLoans = totalReturnedLoans; }

    public long getTotalReservationsWaiting() { return totalReservationsWaiting; }
    public void setTotalReservationsWaiting(long totalReservationsWaiting) { this.totalReservationsWaiting = totalReservationsWaiting; }

    public long getTotalReservationsReady() { return totalReservationsReady; }
    public void setTotalReservationsReady(long totalReservationsReady) { this.totalReservationsReady = totalReservationsReady; }

    public BigDecimal getTotalFinesCollected() { return totalFinesCollected; }
    public void setTotalFinesCollected(BigDecimal totalFinesCollected) { this.totalFinesCollected = totalFinesCollected != null ? totalFinesCollected : BigDecimal.ZERO; }

    public BigDecimal getTotalFinesUnpaid() { return totalFinesUnpaid; }
    public void setTotalFinesUnpaid(BigDecimal totalFinesUnpaid) { this.totalFinesUnpaid = totalFinesUnpaid != null ? totalFinesUnpaid : BigDecimal.ZERO; }

    public BigDecimal getTotalFinesWaived() { return totalFinesWaived; }
    public void setTotalFinesWaived(BigDecimal totalFinesWaived) { this.totalFinesWaived = totalFinesWaived != null ? totalFinesWaived : BigDecimal.ZERO; }

    public List<PopularBookItem> getTopBorrowedBooks() { return topBorrowedBooks; }
    public void setTopBorrowedBooks(List<PopularBookItem> topBorrowedBooks) { this.topBorrowedBooks = topBorrowedBooks != null ? topBorrowedBooks : new ArrayList<>(); }
}

