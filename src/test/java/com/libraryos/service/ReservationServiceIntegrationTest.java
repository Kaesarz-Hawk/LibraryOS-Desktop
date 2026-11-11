package com.libraryos.service;

import com.libraryos.config.DatabaseConfig;
import com.libraryos.core.exception.ValidationException;
import com.libraryos.core.security.SessionManager;
import com.libraryos.core.security.UserSession;
import com.libraryos.model.circulation.Loan;
import com.libraryos.model.inventory.BookCopy;
import com.libraryos.model.inventory.CopyStatus;
import com.libraryos.model.reservation.Reservation;
import com.libraryos.model.reservation.ReservationStatus;
import com.libraryos.repository.impl.*;
import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ReservationServiceIntegrationTest {

    private static JdbcDataSource dataSource;
    private static Flyway flyway;
    private ReservationService reservationService;
    private CirculationService circulationService;
    private JdbcReservationRepository reservationRepository;
    private JdbcBookCopyRepository bookCopyRepository;

    @BeforeAll
    static void setupDatabase() {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:libraryos_res_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1");
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

        reservationRepository = new JdbcReservationRepository();
        JdbcBookRepository bookRepository = new JdbcBookRepository(new JdbcAuthorRepository(), new JdbcCategoryRepository());
        bookCopyRepository = new JdbcBookCopyRepository();
        JdbcMemberRepository memberRepository = new JdbcMemberRepository();
        JdbcBorrowingPolicyRepository borrowingPolicyRepository = new JdbcBorrowingPolicyRepository();
        JdbcLoanRepository loanRepository = new JdbcLoanRepository();
        JdbcLoanRenewalRepository loanRenewalRepository = new JdbcLoanRenewalRepository();
        JdbcFineRepository fineRepository = new JdbcFineRepository();
        FineCalculationService fineCalculationService = new FineCalculationService();
        AuditService auditService = new AuditService(new JdbcAuditLogRepository());

        reservationService = new ReservationService(
                reservationRepository,
                bookRepository,
                bookCopyRepository,
                memberRepository,
                borrowingPolicyRepository,
                loanRepository,
                fineRepository,
                auditService
        );

        circulationService = new CirculationService(
                loanRepository,
                loanRenewalRepository,
                bookCopyRepository,
                memberRepository,
                borrowingPolicyRepository,
                fineRepository,
                reservationRepository,
                fineCalculationService,
                auditService
        );

        UserSession adminSession = new UserSession(1L, "admin", "Admin", null, null,
                Collections.singleton("SUPER_ADMIN"),
                Set.of("RESERVATION_MANAGE", "RESERVATION_SELF", "LOAN_ISSUE", "LOAN_RETURN", "BOOK_VIEW"));
        SessionManager.setSession(adminSession);
    }

    @AfterEach
    void tearDown() {
        SessionManager.clearSession();
    }

    @Test
    @DisplayName("Should successfully place hold reservation into FIFO queue")
    void testPlaceReservationSuccess() {
        // Member #2 reserves Book #1 (Clean Code)
        Reservation res = reservationService.placeReservation(1L, 2L, "Need for term project");

        assertNotNull(res);
        assertNotNull(res.getId());
        assertEquals(ReservationStatus.WAITING, res.getStatus());
        assertEquals(1L, res.getBookId());
        assertEquals(2L, res.getMemberId());
    }

    @Test
    @DisplayName("Should reject duplicate active reservation for the same book title")
    void testPreventDuplicateReservation() {
        reservationService.placeReservation(1L, 2L, "First hold");

        ValidationException ex = assertThrows(ValidationException.class, () ->
                reservationService.placeReservation(1L, 2L, "Second hold attempt"));

        assertTrue(ex.getMessage().contains("already has a pending reservation"));
    }

    @Test
    @DisplayName("Should reject reservation if member already has an active loan for that book")
    void testPreventReservationWhenBookAlreadyBorrowed() {
        // In seed data, Member #1 currently has Book #1 borrowed on active loan
        ValidationException ex = assertThrows(ValidationException.class, () ->
                reservationService.placeReservation(1L, 1L, "Hold attempt"));

        assertTrue(ex.getMessage().contains("already has an active borrowed copy"));
    }

    @Test
    @DisplayName("Should cancel reservation and release allocated copy back to AVAILABLE if no further waitlist")
    void testCancelReservationReleasesCopy() throws Exception {
        // Member #2 has a reservation, copy #2 is allocated
        Reservation res = reservationService.placeReservation(1L, 2L, "Hold");

        // Manually allocate copy #2
        try (Connection conn = dataSource.getConnection()) {
            reservationRepository.allocateCopy(res.getId(), 2L, LocalDateTime.now().plusDays(3), conn);
            bookCopyRepository.updateStatus(2L, CopyStatus.RESERVED, 1L, "Allocated to res", conn);
        }

        // Verify copy is RESERVED
        assertEquals(CopyStatus.RESERVED, bookCopyRepository.findById(2L).orElseThrow().getStatus());

        // Cancel reservation
        reservationService.cancelReservation(res.getId(), "User decided to cancel");

        // Reservation should be CANCELLED
        Reservation cancelled = reservationRepository.findById(res.getId()).orElseThrow();
        assertEquals(ReservationStatus.CANCELLED, cancelled.getStatus());

        // Copy should be released back to AVAILABLE
        assertEquals(CopyStatus.AVAILABLE, bookCopyRepository.findById(2L).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("Should detect expired hold reservations, mark EXPIRED, and pass copy to next queued patron")
    void testProcessExpiredHoldsAdvancesQueue() throws Exception {
        // Member #2 and Member #3 both queue for Book #1
        Reservation res1 = reservationService.placeReservation(1L, 2L, "First queued");
        Reservation res2 = reservationService.placeReservation(1L, 3L, "Second queued");

        // Allocate Copy #2 to res1 with expiration in the past (yesterday)
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE reservations SET allocated_copy_id = 2, status = 'READY_FOR_PICKUP', " +
                             "ready_at = ?, expires_at = ? WHERE id = ?")) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now().minusDays(5)));
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now().minusDays(1)));
            ps.setLong(3, res1.getId());
            ps.executeUpdate();
            bookCopyRepository.updateStatus(2L, CopyStatus.RESERVED, 1L, "Reserved for res1", conn);
        }

        // Run processExpiredHolds
        int processed = reservationService.processExpiredHolds();
        assertEquals(1, processed);

        // res1 must be EXPIRED
        Reservation expiredRes1 = reservationRepository.findById(res1.getId()).orElseThrow();
        assertEquals(ReservationStatus.EXPIRED, expiredRes1.getStatus());

        // res2 must now be READY_FOR_PICKUP with copy #2 allocated!
        Reservation advancedRes2 = reservationRepository.findById(res2.getId()).orElseThrow();
        assertEquals(ReservationStatus.READY_FOR_PICKUP, advancedRes2.getStatus());
        assertEquals(2L, advancedRes2.getAllocatedCopyId());
    }
}
