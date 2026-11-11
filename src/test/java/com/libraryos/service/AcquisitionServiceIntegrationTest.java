package com.libraryos.service;

import com.libraryos.config.DatabaseConfig;
import com.libraryos.core.exception.DomainException;
import com.libraryos.core.security.SessionManager;
import com.libraryos.core.security.UserSession;
import com.libraryos.model.acquisition.*;
import com.libraryos.model.inventory.BookCopy;
import com.libraryos.model.inventory.CopyStatus;
import com.libraryos.repository.impl.JdbcAcquisitionRepository;
import com.libraryos.repository.impl.JdbcAuditLogRepository;
import com.libraryos.repository.impl.JdbcBookCopyRepository;
import com.libraryos.repository.impl.JdbcSupplierRepository;
import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AcquisitionServiceIntegrationTest {

    private static JdbcDataSource dataSource;
    private static Flyway flyway;
    private AcquisitionService acquisitionService;
    private JdbcBookCopyRepository copyRepository;

    @BeforeAll
    static void setupDatabase() {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:libraryos_acq_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1");
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

        JdbcSupplierRepository supplierRepo = new JdbcSupplierRepository();
        JdbcAcquisitionRepository acqRepo = new JdbcAcquisitionRepository();
        copyRepository = new JdbcBookCopyRepository();
        JdbcAuditLogRepository auditRepo = new JdbcAuditLogRepository();
        AuditService auditService = new AuditService(auditRepo);

        acquisitionService = new AcquisitionService(supplierRepo, acqRepo, copyRepository, auditService);

        // Authenticate Super Admin session
        UserSession adminSession = new UserSession(
                1L, "admin", "Admin", null, null,
                Collections.singleton("SUPER_ADMIN"),
                Set.of("SUPPLIER_MANAGE", "BOOK_VIEW")
        );
        SessionManager.setSession(adminSession);
    }

    @AfterEach
    void tearDown() {
        SessionManager.clearSession();
    }

    @Test
    void testSupplierLifecycle() {
        Supplier supplier = new Supplier();
        supplier.setName("Prothoma Prokashan (প্রথমা প্রকাশন)");
        supplier.setContactPerson("Mahfuz Anam");
        supplier.setPhone("+8801811223344");
        supplier.setEmail("orders@prothoma.com");
        supplier.setAddress("Karwan Bazar, Dhaka-1215");
        supplier.setActive(true);

        Supplier saved = acquisitionService.createSupplier(supplier);
        assertNotNull(saved.getId());

        List<Supplier> search = acquisitionService.searchSuppliers("Prothoma");
        assertFalse(search.isEmpty());
        assertEquals("Prothoma Prokashan (প্রথমা প্রকাশন)", search.get(0).getName());

        saved.setContactPerson("Zahirul Islam");
        boolean updated = acquisitionService.updateSupplier(saved);
        assertTrue(updated);

        Optional<Supplier> fetched = acquisitionService.getSupplierById(saved.getId());
        assertTrue(fetched.isPresent());
        assertEquals("Zahirul Islam", fetched.get().getContactPerson());

        boolean deactivated = acquisitionService.deactivateSupplier(saved.getId());
        assertTrue(deactivated);
    }

    @Test
    void testCreateAndReceiveAcquisitionBatch() {
        // Line items: 2 copies of book #1 at ৳350.00 each, 3 copies of book #2 at ৳400.00 each
        List<AcquisitionItem> items = List.of(
                new AcquisitionItem(null, null, 1L, 2, new BigDecimal("350.00")),
                new AcquisitionItem(null, null, 2L, 3, new BigDecimal("400.00"))
        );

        Acquisition order = acquisitionService.createAcquisitionOrder(
                1L, "INV-2026-999", LocalDate.now(), "Annual library expansion grant", items
        );

        assertNotNull(order.getId());
        assertTrue(order.getAcquisitionNumber().startsWith("PO-"));
        assertEquals(AcquisitionStatus.ORDERED, order.getStatus());
        assertEquals(new BigDecimal("1900.00"), order.getTotalCostBdt()); // (2 * 350) + (3 * 400) = 700 + 1200 = 1900
        assertEquals(5, order.getTotalQuantity());

        // Now receive the acquisition batch and assign to Shelf #1
        List<BookCopy> generatedCopies = acquisitionService.receiveAcquisitionBatch(order.getId(), 1L);
        assertEquals(5, generatedCopies.size());

        for (BookCopy copy : generatedCopies) {
            assertNotNull(copy.getId());
            assertNotNull(copy.getAccessionNumber());
            assertTrue(copy.getAccessionNumber().startsWith("ACC-"));
            assertNotNull(copy.getBarcode());
            assertTrue(copy.getBarcode().startsWith("BC"));
            assertEquals(1L, copy.getShelfId());
            assertEquals(order.getId(), copy.getAcquisitionId());
            assertEquals(CopyStatus.AVAILABLE, copy.getStatus());

            // Verify exists in database
            Optional<BookCopy> found = copyRepository.findById(copy.getId());
            assertTrue(found.isPresent());
            assertEquals(copy.getBarcode(), found.get().getBarcode());
        }

        // Verify acquisition is marked as RECEIVED
        Acquisition receivedOrder = acquisitionService.getAcquisitionById(order.getId()).orElseThrow();
        assertEquals(AcquisitionStatus.RECEIVED, receivedOrder.getStatus());
        assertEquals(LocalDate.now(), receivedOrder.getReceivedDate());

        // Verify cannot cancel an already received order
        assertThrows(DomainException.class, () -> acquisitionService.cancelAcquisitionOrder(order.getId(), "Cancel mistake"));
    }

    @Test
    void testCancelAcquisitionOrder() {
        List<AcquisitionItem> items = List.of(
                new AcquisitionItem(null, null, 1L, 1, new BigDecimal("250.00"))
        );

        Acquisition order = acquisitionService.createAcquisitionOrder(
                2L, "INV-CANCEL-01", LocalDate.now(), "Test cancellation", items
        );

        boolean cancelled = acquisitionService.cancelAcquisitionOrder(order.getId(), "Supplier out of stock");
        assertTrue(cancelled);

        Acquisition fetched = acquisitionService.getAcquisitionById(order.getId()).orElseThrow();
        assertEquals(AcquisitionStatus.CANCELLED, fetched.getStatus());
    }
}
