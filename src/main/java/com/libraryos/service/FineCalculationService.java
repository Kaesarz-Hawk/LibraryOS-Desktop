package com.libraryos.service;

import com.libraryos.core.util.DateTimeUtil;
import com.libraryos.model.circulation.Loan;
import com.libraryos.model.member.BorrowingPolicy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class FineCalculationService {

    public static class OverdueResult {
        private final long totalCalendarDaysPastDue;
        private final long chargeableDays;
        private final BigDecimal dailyRate;
        private final BigDecimal fineAmount;

        public OverdueResult(long totalCalendarDaysPastDue, long chargeableDays, BigDecimal dailyRate, BigDecimal fineAmount) {
            this.totalCalendarDaysPastDue = totalCalendarDaysPastDue;
            this.chargeableDays = chargeableDays;
            this.dailyRate = dailyRate;
            this.fineAmount = fineAmount;
        }

        public long getTotalCalendarDaysPastDue() {
            return totalCalendarDaysPastDue;
        }

        public long getChargeableDays() {
            return chargeableDays;
        }

        public BigDecimal getDailyRate() {
            return dailyRate;
        }

        public BigDecimal getFineAmount() {
            return fineAmount;
        }

        public boolean isOverdue() {
            return fineAmount.compareTo(BigDecimal.ZERO) > 0;
        }
    }

    /**
     * Calculates deterministic overdue fine for a loan given the return date and member borrowing policy.
     */
    public OverdueResult calculateOverdueFine(Loan loan, BorrowingPolicy policy, LocalDate returnDate) {
        if (loan == null || loan.getDueDate() == null || returnDate == null) {
            return new OverdueResult(0, 0, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        LocalDate dueDate = loan.getDueDate();
        if (!returnDate.isAfter(dueDate)) {
            return new OverdueResult(0, 0, loan.getPolicyFinePerDaySnapshot(), BigDecimal.ZERO);
        }

        long totalCalendarDays = ChronoUnit.DAYS.between(dueDate, returnDate);
        int gracePeriod = policy != null ? policy.getGracePeriodDays() : 0;

        // If returned within grace period, no fine charged
        if (returnDate.isBefore(dueDate.plusDays(gracePeriod + 1L))) {
            return new OverdueResult(totalCalendarDays, 0, loan.getPolicyFinePerDaySnapshot(), BigDecimal.ZERO);
        }

        boolean countHolidays = policy != null && policy.isCountHolidaysInOverdue();
        long chargeableDays = 0;

        LocalDate current = dueDate.plusDays(1);
        while (!current.isAfter(returnDate)) {
            if (countHolidays || !DateTimeUtil.isBangladeshWeeklyHoliday(current)) {
                chargeableDays++;
            }
            current = current.plusDays(1);
        }

        BigDecimal dailyRate = loan.getPolicyFinePerDaySnapshot() != null ?
                loan.getPolicyFinePerDaySnapshot() : (policy != null ? policy.getFinePerDayBdt() : new BigDecimal("5.00"));

        BigDecimal fine = dailyRate.multiply(BigDecimal.valueOf(chargeableDays));

        // Apply maximum fine per loan cap if configured
        if (policy != null && policy.getMaxFinePerLoanBdt() != null && policy.getMaxFinePerLoanBdt().compareTo(BigDecimal.ZERO) > 0) {
            fine = fine.min(policy.getMaxFinePerLoanBdt());
        }

        return new OverdueResult(totalCalendarDays, chargeableDays, dailyRate, fine);
    }
}

