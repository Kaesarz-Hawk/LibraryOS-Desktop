package com.libraryos.service;

import com.libraryos.config.DatabaseConfig;
import com.libraryos.core.exception.UnauthorizedException;
import com.libraryos.core.exception.ValidationException;
import com.libraryos.core.security.SessionManager;
import com.libraryos.core.security.UserSession;
import com.libraryos.model.finance.*;
import com.libraryos.repository.impl.*;
import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FinanceServiceIntegrationTest {

    private static JdbcDataSource dataSource;
    private static Flyway flyway;
    private FineService fineService;
    private PaymentService paymentService;
    private JdbcFineRepository fineRepository;
    private JdbcPaymentRepository paymentRepository;

    @BeforeAll
    static void setupDatabase() {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:libraryos_fin_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .cleanDisabled(false)
                .load();

        DatabaseConfig.setDataSource(dataSource);
    }

    @BeforeEach
    void setUp() {
        flyway.clean();
        flyway.migrate();

        fineRepository = new JdbcFineRepository();
        JdbcFineAdjustmentRepository adjustmentRepository = new JdbcFineAdjustmentRepository();
        paymentRepository = new JdbcPaymentRepository();
        JdbcMemberRepository memberRepository = new JdbcMemberRepository();
        AuditService auditService = new AuditService(new JdbcAuditLogRepository());

        fineService = new FineService(fineRepository, adjustmentRepository, memberRepository, auditService);
        paymentService = new PaymentService(paymentRepository, fineRepository, memberRepository, auditService);

        UserSession adminSession = new UserSession(1L, "admin", "Admin", null, null,
                Collections.singleton("SUPER_ADMIN"),
                Set.of("FINE_VIEW", "FINE_WAIVE", "PAYMENT_RECORD"));
        SessionManager.setSession(adminSession);
    }

    @AfterEach
    void tearDown() {
        SessionManager.clearSession();
    }

    @Test
    @DisplayName("Should create fine liability and query member unpaid balance")
    void testCreateFineSuccess() {
        Fine fine = fineService.createFine(1L, null, FineType.DAMAGED_BOOK, new BigDecimal("150.00"), "Damaged water spill on cover");

        assertNotNull(fine);
        assertNotNull(fine.getId());
        assertEquals(FineStatus.UNPAID, fine.getStatus());
        assertEquals(new BigDecimal("150.00"), fine.getOriginalAmountBdt());

        BigDecimal unpaid = fineService.getUnpaidTotalByMember(1L);
        assertEquals(new BigDecimal("150.00"), unpaid);
    }

    @Test
    @DisplayName("Should waive fine with mandatory reason and update status")
    void testWaiveFineSuccess() {
        Fine fine = fineService.createFine(1L, null, FineType.OVERDUE, new BigDecimal("100.00"), "Late return");

        FineAdjustment adjustment = fineService.waiveFine(fine.getId(), new BigDecimal("100.00"), "Student medical emergency waiver approved by Principal");

        assertNotNull(adjustment);
        assertEquals(new BigDecimal("100.00"), adjustment.getAmountBdt());

        Fine updatedFine = fineRepository.findById(fine.getId()).orElseThrow();
        assertEquals(FineStatus.WAIVED, updatedFine.getStatus());
        assertEquals(0, BigDecimal.ZERO.compareTo(updatedFine.getRemainingAmountBdt()));
    }

    @Test
    @DisplayName("Should reject fine waiver if user lacks FINE_WAIVE permission")
    void testWaiveFinePermissionDenied() {
        Fine fine = fineService.createFine(1L, null, FineType.OVERDUE, new BigDecimal("50.00"), "Late fee");

        // Set session with only FINE_VIEW and PAYMENT_RECORD (no FINE_WAIVE)
        UserSession clerkSession = new UserSession(2L, "clerk", "Clerk", null, null,
                Collections.singleton("LIBRARIAN"),
                Set.of("FINE_VIEW", "PAYMENT_RECORD"));
        SessionManager.setSession(clerkSession);

        assertThrows(UnauthorizedException.class, () ->
                fineService.waiveFine(fine.getId(), new BigDecimal("20.00"), "Attempted waiver"));
    }

    @Test
    @DisplayName("Should record payment, generate receipt number, and allocate funds FIFO")
    void testRecordPaymentAllocatesFIFO() {
        // Create 2 fines for Member #1: ৳60 and ৳40 (Total ৳100)
        Fine f1 = fineService.createFine(1L, null, FineType.OVERDUE, new BigDecimal("60.00"), "Fine 1");
        Fine f2 = fineService.createFine(1L, null, FineType.OVERDUE, new BigDecimal("40.00"), "Fine 2");

        // Pay ৳80 via bKash with external TrxID
        Payment payment = paymentService.recordPayment(1L, new BigDecimal("80.00"), PaymentMethod.BKASH,
                "BKASH-TRX-998877", null, "Paid via counter QR code");

        assertNotNull(payment);
        assertTrue(payment.getReceiptNumber().startsWith("REC-"));
        assertEquals(new BigDecimal("80.00"), payment.getAmountBdt());
        assertEquals("BKASH-TRX-998877", payment.getExternalReference());

        // Check fine #1: ৳60 fully paid
        Fine updatedF1 = fineRepository.findById(f1.getId()).orElseThrow();
        assertEquals(FineStatus.PAID, updatedF1.getStatus());
        assertEquals(new BigDecimal("60.00"), updatedF1.getPaidAmountBdt());
        assertEquals(0, BigDecimal.ZERO.compareTo(updatedF1.getRemainingAmountBdt()));

        // Check fine #2: ৳20 paid, ৳20 remaining (PARTIALLY_PAID)
        Fine updatedF2 = fineRepository.findById(f2.getId()).orElseThrow();
        assertEquals(FineStatus.PARTIALLY_PAID, updatedF2.getStatus());
        assertEquals(new BigDecimal("20.00"), updatedF2.getPaidAmountBdt());
        assertEquals(new BigDecimal("20.00"), updatedF2.getRemainingAmountBdt());
    }

    @Test
    @DisplayName("Should reject double-posting of identical MFS external reference ID")
    void testPreventDuplicateMfsTrxId() {
        fineService.createFine(1L, null, FineType.OVERDUE, new BigDecimal("100.00"), "Fine");

        paymentService.recordPayment(1L, new BigDecimal("50.00"), PaymentMethod.NAGAD, "NAGAD-TX-12345", null, "First payment");

        ValidationException ex = assertThrows(ValidationException.class, () ->
                paymentService.recordPayment(1L, new BigDecimal("50.00"), PaymentMethod.NAGAD, "NAGAD-TX-12345", null, "Duplicate attempt"));

        assertTrue(ex.getMessage().contains("already posted"));
    }

    @Test
    @DisplayName("Should reverse payment receipt and restore fine liabilities")
    void testReversePaymentRestoresFines() {
        Fine fine = fineService.createFine(1L, null, FineType.OVERDUE, new BigDecimal("50.00"), "Fine");
        Payment payment = paymentService.recordPayment(1L, new BigDecimal("50.00"), PaymentMethod.CASH, null, null, "Cash payment");

        // Verify fine is PAID
        assertEquals(FineStatus.PAID, fineRepository.findById(fine.getId()).orElseThrow().getStatus());

        // Reverse payment
        paymentService.reversePayment(payment.getId(), "Mistaken cash collection receipt posted to wrong account");

        // Verify payment is reversed
        Payment reversedPayment = paymentRepository.findById(payment.getId()).orElseThrow();
        assertTrue(reversedPayment.isReversed());
        assertEquals("Mistaken cash collection receipt posted to wrong account", reversedPayment.getReversalReason());

        // Verify fine liability is restored to UNPAID
        Fine restoredFine = fineRepository.findById(fine.getId()).orElseThrow();
        assertEquals(FineStatus.UNPAID, restoredFine.getStatus());
        assertEquals(new BigDecimal("50.00"), restoredFine.getRemainingAmountBdt());
    }

    @Test
    @DisplayName("Should search fines and payments by keyword filter")
    void testSearchFinesAndPayments() {
        fineService.createFine(1L, null, FineType.LOST_BOOK, new BigDecimal("450.00"), "Lost textbook volume");
        
        List<Fine> beforePayment = fineService.searchFines("Lost", FineStatus.UNPAID);
        assertFalse(beforePayment.isEmpty());
        assertEquals(FineType.LOST_BOOK, beforePayment.get(0).getFineType());

        paymentService.recordPayment(1L, new BigDecimal("100.00"), PaymentMethod.ROCKET, "ROK-7788", "Advance payment");

        List<Fine> afterPayment = fineService.searchFines("Lost", FineStatus.PARTIALLY_PAID);
        assertFalse(afterPayment.isEmpty());
        assertEquals(FineType.LOST_BOOK, afterPayment.get(0).getFineType());

        List<Payment> payments = paymentService.searchPayments("ROK", null, null);
        assertFalse(payments.isEmpty());
        assertEquals(PaymentMethod.ROCKET, payments.get(0).getPaymentMethod());
    }
}
