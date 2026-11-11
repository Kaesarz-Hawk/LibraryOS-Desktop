package com.libraryos.service;

import com.libraryos.config.DatabaseConfig;
import com.libraryos.core.security.SessionManager;
import com.libraryos.core.security.UserSession;
import com.libraryos.model.digital.Bookmark;
import com.libraryos.model.digital.DigitalBook;
import com.libraryos.model.digital.ReadingProgress;
import com.libraryos.repository.impl.JdbcAuditLogRepository;
import com.libraryos.repository.impl.JdbcBookmarkRepository;
import com.libraryos.repository.impl.JdbcDigitalBookRepository;
import com.libraryos.repository.impl.JdbcReadingProgressRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DigitalLibraryServiceIntegrationTest {

    private static JdbcDataSource dataSource;
    private static Flyway flyway;
    private DigitalLibraryService digitalLibraryService;
    private File samplePdfFile;

    @BeforeAll
    static void setupDatabase() {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:libraryos_digital_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1");
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
    void setUp() throws IOException {
        flyway.clean();
        flyway.migrate();

        JdbcDigitalBookRepository bookRepo = new JdbcDigitalBookRepository();
        JdbcReadingProgressRepository progressRepo = new JdbcReadingProgressRepository();
        JdbcBookmarkRepository bookmarkRepo = new JdbcBookmarkRepository();
        JdbcAuditLogRepository auditRepo = new JdbcAuditLogRepository();
        AuditService auditService = new AuditService(auditRepo);

        digitalLibraryService = new DigitalLibraryService(bookRepo, progressRepo, bookmarkRepo, auditService);

        // Authenticate Super Admin session
        UserSession adminSession = new UserSession(
                1L, "admin", "Admin", null, null,
                Collections.singleton("SUPER_ADMIN"),
                Set.of("DIGITAL_READ", "DIGITAL_MANAGE")
        );
        SessionManager.setSession(adminSession);

        // Generate valid multi-page PDF for test
        samplePdfFile = File.createTempFile("test_libraryos_", ".pdf");
        samplePdfFile.deleteOnExit();
        try (PDDocument doc = new PDDocument()) {
            doc.addPage(new PDPage());
            doc.addPage(new PDPage());
            doc.addPage(new PDPage());
            doc.save(samplePdfFile);
        }
    }

    @AfterEach
    void tearDown() {
        SessionManager.clearSession();
        if (samplePdfFile != null && samplePdfFile.exists()) {
            samplePdfFile.delete();
        }
    }

    @Test
    void testUploadAndRetrieveDigitalBook() throws IOException {
        DigitalBook uploaded = digitalLibraryService.uploadDigitalBook(1L, samplePdfFile, true);

        assertNotNull(uploaded.getId());
        assertEquals(1L, uploaded.getBookId());
        assertEquals(3, uploaded.getTotalPages());
        assertTrue(uploaded.getFileSizeBytes() > 0);
        assertNotNull(uploaded.getSha256Checksum());
        assertEquals(64, uploaded.getSha256Checksum().length());
        assertTrue(uploaded.isDownloadAllowed());

        Optional<DigitalBook> fetched = digitalLibraryService.getDigitalBookById(uploaded.getId());
        assertTrue(fetched.isPresent());
        assertEquals("Paradoxical Sajid", fetched.get().getBookTitle());
    }

    @Test
    void testRenderPdfPageToPng() throws IOException {
        DigitalBook uploaded = digitalLibraryService.uploadDigitalBook(1L, samplePdfFile, true);

        byte[] pngBytes = digitalLibraryService.renderPageToPng(uploaded.getId(), 0, 72f);
        assertNotNull(pngBytes);
        assertTrue(pngBytes.length > 0);

        // Verify PNG magic header: 0x89 0x50 0x4E 0x47
        assertEquals((byte) 0x89, pngBytes[0]);
        assertEquals((byte) 'P', pngBytes[1]);
        assertEquals((byte) 'N', pngBytes[2]);
        assertEquals((byte) 'G', pngBytes[3]);
    }

    @Test
    void testReadingProgressAndBookmarks() throws IOException {
        DigitalBook uploaded = digitalLibraryService.uploadDigitalBook(1L, samplePdfFile, true);
        Long memberId = 1L;

        // Record progress on page 2 of 3
        digitalLibraryService.recordReadingProgress(memberId, uploaded.getId(), 2, 3);

        Optional<ReadingProgress> progressOpt = digitalLibraryService.getReadingProgress(memberId, uploaded.getId());
        assertTrue(progressOpt.isPresent());
        assertEquals(2, progressOpt.get().getLastPageRead());
        assertEquals(3, progressOpt.get().getTotalPages());
        assertEquals(66.66, progressOpt.get().getProgressPercentage(), 0.1);

        // Add Bookmark
        Bookmark bm = digitalLibraryService.addBookmark(memberId, uploaded.getId(), 2, "Important Chapter", "Exam revision note");
        assertNotNull(bm.getId());
        assertEquals(2, bm.getPageNumber());

        List<Bookmark> list = digitalLibraryService.getBookmarks(memberId, uploaded.getId());
        assertEquals(1, list.size());
        assertEquals("Important Chapter", list.get(0).getTitle());

        // Remove Bookmark
        boolean deleted = digitalLibraryService.removeBookmark(bm.getId(), memberId);
        assertTrue(deleted);
        assertTrue(digitalLibraryService.getBookmarks(memberId, uploaded.getId()).isEmpty());
    }

    @Test
    void testSearchAndDeleteDigitalBook() throws IOException {
        DigitalBook uploaded = digitalLibraryService.uploadDigitalBook(1L, samplePdfFile, false);

        List<DigitalBook> searchResults = digitalLibraryService.searchDigitalBooks("Paradoxical");
        assertFalse(searchResults.isEmpty());

        boolean deleted = digitalLibraryService.deleteDigitalBook(uploaded.getId());
        assertTrue(deleted);
        assertTrue(digitalLibraryService.getDigitalBookById(uploaded.getId()).isEmpty());
    }
}
