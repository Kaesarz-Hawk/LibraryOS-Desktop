package com.libraryos.service;

import com.libraryos.config.DatabaseConfig;
import com.libraryos.core.security.SessionManager;
import com.libraryos.core.security.UserSession;
import com.libraryos.dto.CatalogInventoryReportItem;
import com.libraryos.dto.CirculationStatsDTO;
import com.libraryos.dto.OverdueReportItem;
import com.libraryos.model.finance.FineType;
import com.libraryos.model.finance.Payment;
import com.libraryos.model.finance.PaymentMethod;
import com.libraryos.repository.impl.*;
import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ReportServiceIntegrationTest {

    private static JdbcDataSource dataSource;
    private static Flyway flyway;
    private ReportService reportService;
    private FineService fineService;
    private PaymentService paymentService;

    @BeforeAll
    static void setupDatabase() {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:libraryos_report_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1");
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

        JdbcReportRepository reportRepo = new JdbcReportRepository();
        PdfExportService pdfService = new PdfExportService();
        ExcelExportService excelService = new ExcelExportService();
        reportService = new ReportService(reportRepo, pdfService, excelService);

        JdbcAuditLogRepository auditRepo = new JdbcAuditLogRepository();
        AuditService auditService = new AuditService(auditRepo);
        JdbcFineRepository fineRepo = new JdbcFineRepository();
        JdbcFineAdjustmentRepository adjRepo = new JdbcFineAdjustmentRepository();
        JdbcMemberRepository memberRepo = new JdbcMemberRepository();
        JdbcPaymentRepository paymentRepo = new JdbcPaymentRepository();

        fineService = new FineService(fineRepo, adjRepo, memberRepo, auditService);
        paymentService = new PaymentService(paymentRepo, fineRepo, memberRepo, auditService);

        UserSession adminSession = new UserSession(1L, "admin", "Admin", null, null,
                Collections.singleton("SUPER_ADMIN"),
                Set.of("REPORT_VIEW", "REPORT_EXPORT", "FINE_VIEW", "PAYMENT_RECORD"));
        SessionManager.setSession(adminSession);
    }

    @AfterEach
    void tearDown() {
        SessionManager.clearSession();
    }

    @Test
    @DisplayName("Should query overdue loans with correct overdue days and fine estimates")
    void testGetOverdueLoansReport() throws Exception {
        // Insert an explicitly overdue loan (due 10 days ago)
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO loans (copy_id, member_id, issued_by, issued_at, due_date, renewal_count, policy_fine_per_day_snapshot, status) " +
                             "VALUES (2, 2, 1, ?, ?, 0, 5.00, 'ACTIVE')")) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now().minusDays(24)));
            ps.setDate(2, java.sql.Date.valueOf(LocalDate.now().minusDays(10)));
            ps.executeUpdate();
        }

        List<OverdueReportItem> items = reportService.getOverdueLoansReport(1);
        assertNotNull(items);
        assertFalse(items.isEmpty());

        OverdueReportItem item = items.stream().filter(i -> i.getOverdueDays() == 10).findFirst().orElseThrow();
        assertEquals(10, item.getOverdueDays());
        assertEquals(new BigDecimal("50.00"), item.getFineAmount());
        assertNotNull(item.getBookTitle());
        assertNotNull(item.getMemberName());
    }

    @Test
    @DisplayName("Should aggregate circulation statistics accurately")
    void testGetCirculationStats() {
        CirculationStatsDTO stats = reportService.getCirculationStats();
        assertNotNull(stats);
        assertTrue(stats.getTotalActiveLoans() >= 1); // Seed loan #1 is ACTIVE
    }

    @Test
    @DisplayName("Should query catalog inventory copies report")
    void testGetCatalogInventoryReport() {
        List<CatalogInventoryReportItem> items = reportService.getCatalogInventoryReport(null);
        assertNotNull(items);
        assertFalse(items.isEmpty());

        CatalogInventoryReportItem first = items.get(0);
        assertNotNull(first.getTitleEn());
        assertTrue(first.getTotalCopies() >= 1);
    }

    @Test
    @DisplayName("Should query fine collections and export to PDF and Excel")
    void testFineCollectionsAndExports() {
        fineService.createFine(1L, null, FineType.DAMAGED_BOOK, new BigDecimal("100.00"), "Damaged pages");
        paymentService.recordPayment(1L, new BigDecimal("100.00"), PaymentMethod.CASH, null, "Cash collection");

        List<Payment> collections = reportService.getFineCollectionReport(null, null, null);
        assertFalse(collections.isEmpty());

        ByteArrayOutputStream pdfOut = new ByteArrayOutputStream();
        reportService.exportFineCollectionsToPdf(null, null, null, pdfOut);
        assertTrue(pdfOut.toByteArray().length > 500);

        ByteArrayOutputStream excelOut = new ByteArrayOutputStream();
        reportService.exportFineCollectionsToExcel(null, null, null, excelOut);
        assertTrue(excelOut.toByteArray().length > 1000);
    }
}

