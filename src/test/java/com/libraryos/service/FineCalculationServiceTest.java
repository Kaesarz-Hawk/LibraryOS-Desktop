package com.libraryos.service;

import com.libraryos.model.circulation.Loan;
import com.libraryos.model.member.BorrowingPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class FineCalculationServiceTest {

    private FineCalculationService fineCalculationService;
    private BorrowingPolicy policy;
    private Loan loan;

    @BeforeEach
    void setUp() {
        fineCalculationService = new FineCalculationService();

        policy = new BorrowingPolicy();
        policy.setGracePeriodDays(1);
        policy.setFinePerDayBdt(new BigDecimal("10.00"));
        policy.setMaxFinePerLoanBdt(new BigDecimal("150.00"));
        policy.setCountHolidaysInOverdue(false); // Skip Fridays

        loan = new Loan();
        loan.setDueDate(LocalDate.of(2026, 11, 1)); // Suppose due date
        loan.setPolicyFinePerDaySnapshot(new BigDecimal("10.00"));
    }

    @Test
    @DisplayName("Should return 0 fine if returned on or before due date")
    void testReturnOnOrBeforeDueDate() {
        FineCalculationService.OverdueResult result = fineCalculationService.calculateOverdueFine(
                loan, policy, LocalDate.of(2026, 11, 1));

        assertEquals(0, result.getTotalCalendarDaysPastDue());
        assertEquals(0, result.getChargeableDays());
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getFineAmount()));
        assertFalse(result.isOverdue());
    }

    @Test
    @DisplayName("Should return 0 fine if returned within grace period")
    void testReturnWithinGracePeriod() {
        // Due on Nov 1, grace period = 1 day, returned on Nov 2
        FineCalculationService.OverdueResult result = fineCalculationService.calculateOverdueFine(
                loan, policy, LocalDate.of(2026, 11, 2));

        assertEquals(1, result.getTotalCalendarDaysPastDue());
        assertEquals(0, result.getChargeableDays());
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getFineAmount()));
        assertFalse(result.isOverdue());
    }

    @Test
    @DisplayName("Should calculate fine excluding Fridays when countHolidaysInOverdue is false")
    void testExcludingWeeklyFridayHolidays() {
        // Find a Thursday due date: 2026-11-05 is a Thursday
        LocalDate thursday = LocalDate.of(2026, 11, 5);
        assertEquals(DayOfWeek.THURSDAY, thursday.getDayOfWeek());

        loan.setDueDate(thursday);
        policy.setGracePeriodDays(0);
        policy.setCountHolidaysInOverdue(false);

        // Returned on Sunday 2026-11-08:
        // Days: Fri Nov 6 (Holiday! Skipped), Sat Nov 7 (Counted), Sun Nov 8 (Counted)
        // Chargeable days = 2
        LocalDate sunday = LocalDate.of(2026, 11, 8);
        assertEquals(DayOfWeek.SUNDAY, sunday.getDayOfWeek());

        FineCalculationService.OverdueResult result = fineCalculationService.calculateOverdueFine(
                loan, policy, sunday);

        assertEquals(3, result.getTotalCalendarDaysPastDue());
        assertEquals(2, result.getChargeableDays());
        assertEquals(new BigDecimal("20.00"), result.getFineAmount());
        assertTrue(result.isOverdue());
    }

    @Test
    @DisplayName("Should calculate fine including Fridays when countHolidaysInOverdue is true")
    void testIncludingWeeklyFridayHolidays() {
        LocalDate thursday = LocalDate.of(2026, 11, 5);
        loan.setDueDate(thursday);
        policy.setGracePeriodDays(0);
        policy.setCountHolidaysInOverdue(true);

        LocalDate sunday = LocalDate.of(2026, 11, 8);
        FineCalculationService.OverdueResult result = fineCalculationService.calculateOverdueFine(
                loan, policy, sunday);

        assertEquals(3, result.getTotalCalendarDaysPastDue());
        assertEquals(3, result.getChargeableDays());
        assertEquals(new BigDecimal("30.00"), result.getFineAmount());
    }

    @Test
    @DisplayName("Should cap fine at maxFinePerLoanBdt when calculated amount exceeds cap")
    void testFineCapEnforced() {
        LocalDate dueDate = LocalDate.of(2026, 10, 1);
        loan.setDueDate(dueDate);
        policy.setGracePeriodDays(0);
        policy.setCountHolidaysInOverdue(true);
        policy.setMaxFinePerLoanBdt(new BigDecimal("50.00")); // Cap at ৳50

        // Returned 20 days later (20 * 10 = 200 > 50)
        LocalDate returnDate = dueDate.plusDays(20);
        FineCalculationService.OverdueResult result = fineCalculationService.calculateOverdueFine(
                loan, policy, returnDate);

        assertEquals(20, result.getChargeableDays());
        assertEquals(new BigDecimal("50.00"), result.getFineAmount());
    }
}

