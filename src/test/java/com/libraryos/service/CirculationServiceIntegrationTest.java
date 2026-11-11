package com.libraryos.service;

import com.libraryos.config.DatabaseConfig;
import com.libraryos.core.exception.ValidationException;
import com.libraryos.core.security.SessionManager;
import com.libraryos.core.security.UserSession;
import com.libraryos.model.circulation.Loan;
import com.libraryos.model.circulation.LoanStatus;
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

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CirculationServiceIntegrationTest {

    private static JdbcDataSource dataSource;
    private static Flyway flyway;
    private CirculationService circulationService;
    private JdbcLoanRepository loanRepository;
    private JdbcBookCopyRepository bookCopyRepository;
    private JdbcReservationRepository reservationRepository;
    private JdbcFineRepository fineRepository;

    @BeforeAll
    static void setupDatabase() {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:libraryos_circ_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1");
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

        loanRepository = new JdbcLoanRepository();
        JdbcLoanRenewalRepository loanRenewalRepository = new JdbcLoanRenewalRepository();
        bookCopyRepository = new JdbcBookCopyRepository();
        JdbcMemberRepository memberRepository = new JdbcMemberRepository();
        JdbcBorrowingPolicyRepository borrowingPolicyRepository = new JdbcBorrowingPolicyRepository();
        fineRepository = new JdbcFineRepository();
        reservationRepository = new JdbcReservationRepository();
        FineCalculationService fineCalculationService = new FineCalculationService();
        AuditService auditService = new AuditService(new JdbcAuditLogRepository());

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
                Set.of("LOAN_ISSUE", "LOAN_RETURN", "LOAN_RENEW", "SETTINGS_MANAGE"));
        SessionManager.setSession(adminSession);
    }

    @AfterEach
    void tearDown() {
        SessionManager.clearSession();
    }

    @Test
    @DisplayName("Should issue available book copy to member and transition copy to ISSUED")
    void testIssueBookSuccess() {
        // Copy #2 is available (Clean Code copy 2)
        BookCopy copyBefore = bookCopyRepository.findById(2L).orElseThrow();
        assertEquals(CopyStatus.AVAILABLE, copyBefore.getStatus());

        // Issue to Member #2 (Fatema Khatun)
        Loan loan = circulationService.issueBook(2L, 2L);

        assertNotNull(loan);
        assertNotNull(loan.getId());
        assertEquals(LoanStatus.ACTIVE, loan.getStatus());
        assertEquals(2L, loan.getCopyId());
        assertEquals(2L, loan.getMemberId());
        assertTrue(loan.getDueDate().isAfter(LocalDate.now()));

        // Verify copy status changed to ISSUED in DB
        BookCopy copyAfter = bookCopyRepository.findById(2L).orElseThrow();
        assertEquals(CopyStatus.ISSUED, copyAfter.getStatus());
    }

    @Test
    @DisplayName("Should reject issuing a copy that is already ISSUED (double-issue prevention)")
    void testPreventDoubleIssue() {
        // Copy #1 was already issued in seed migration
        ValidationException ex = assertThrows(ValidationException.class, () ->
                circulationService.issueBook(1L, 2L));

        assertTrue(ex.getMessage().contains("already ISSUED"));
    }

    @Test
    @DisplayName("Should successfully return an active book and mark copy AVAILABLE")
    void testReturnBookSuccess() {
        // Return Copy #1 (currently issued to Member #1)
        CirculationService.ReturnResult result = circulationService.returnBook(1L, "GOOD", "Normal return");

        assertNotNull(result);
        assertEquals(LoanStatus.RETURNED, result.getLoan().getStatus());
        assertNotNull(result.getLoan().getReturnedAt());

        // Verify copy status is now AVAILABLE
        BookCopy copy = bookCopyRepository.findById(1L).orElseThrow();
        assertEquals(CopyStatus.AVAILABLE, copy.getStatus());
    }

    @Test
    @DisplayName("Should create overdue fine if loan is returned past due date")
    void testReturnOverdueGeneratesFine() throws Exception {
        // Issue copy #3 to Member #3 (Dr. M. A. Rashid)
        Loan loan = circulationService.issueBook(3L, 3L);

        // Artificially update due date in DB to 10 days ago
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE loans SET due_date = ? WHERE id = ?")) {
            ps.setDate(1, java.sql.Date.valueOf(LocalDate.now().minusDays(10)));
            ps.setLong(2, loan.getId());
            ps.executeUpdate();
        }

        // Return book
        CirculationService.ReturnResult result = circulationService.returnBook(3L, "GOOD", "Returned late");

        assertNotNull(result.getGeneratedFine(), "Expected overdue fine to be generated");
        assertTrue(result.getGeneratedFine().getOriginalAmountBdt().compareTo(BigDecimal.ZERO) > 0);
        assertEquals(loan.getId(), result.getGeneratedFine().getLoanId());
        assertEquals(3L, result.getGeneratedFine().getMemberId());
    }

    @Test
    @DisplayName("Should allocate returned copy to waiting reservation in FIFO order")
    void testReturnAllocatesToReservationQueue() {
        // Issue copy #4 to Member #2
        circulationService.issueBook(4L, 2L);
        BookCopy copy4 = bookCopyRepository.findById(4L).orElseThrow();

        // Create a waiting reservation for Member #3 on the same book
        Reservation res = new Reservation();
        res.setBookId(copy4.getBookId());
        res.setMemberId(3L);
        res.setStatus(ReservationStatus.WAITING);
        res.setNotes("Hold requested");
        reservationRepository.create(res);

        // Now return copy #4
        CirculationService.ReturnResult result = circulationService.returnBook(4L, "GOOD", "Returned");

        assertTrue(result.isCopyReservedForNextMember());

        // Copy should now be RESERVED, not AVAILABLE
        BookCopy copyAfter = bookCopyRepository.findById(4L).orElseThrow();
        assertEquals(CopyStatus.RESERVED, copyAfter.getStatus());
    }

    @Test
    @DisplayName("Should successfully renew active non-overdue loan")
    void testRenewLoanSuccess() {
        // Issue copy #5 to Member #2
        Loan loan = circulationService.issueBook(5L, 2L);
        LocalDate oldDueDate = loan.getDueDate();

        Loan renewed = circulationService.renewLoan(loan.getId());

        assertEquals(1, renewed.getRenewalCount());
        assertTrue(renewed.getDueDate().isAfter(oldDueDate));
    }
}
