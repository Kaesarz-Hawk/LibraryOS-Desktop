package com.libraryos.service;

import com.libraryos.core.security.SessionManager;
import com.libraryos.core.security.UserSession;
import com.libraryos.model.finance.Payment;
import com.libraryos.model.finance.PaymentAllocation;
import com.libraryos.model.finance.PaymentMethod;
import com.libraryos.model.inventory.BookCopy;
import com.libraryos.model.inventory.CopyStatus;
import com.libraryos.model.member.Member;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PdfExportServiceTest {

    private PdfExportService pdfService;

    @BeforeEach
    void setUp() {
        pdfService = new PdfExportService();
        UserSession adminSession = new UserSession(1L, "admin", "Admin", null, null,
                Collections.singleton("SUPER_ADMIN"),
                Set.of("BARCODE_GENERATE", "MEMBER_VIEW", "FINE_VIEW", "REPORT_EXPORT"));
        SessionManager.setSession(adminSession);
    }

    @AfterEach
    void tearDown() {
        SessionManager.clearSession();
    }

    @Test
    @DisplayName("Should generate valid spine labels PDF")
    void testGenerateSpineLabelsPdf() {
        BookCopy copy1 = new BookCopy();
        copy1.setId(1L);
        copy1.setBookId(1L);
        copy1.setBookTitle("Clean Code (ক্লিন কোড)");
        copy1.setBarcode("9780132350884-01");
        copy1.setAccessionNumber("ACC-2026-0001");
        copy1.setShelfLocation("A1-S2-R3");
        copy1.setStatus(CopyStatus.AVAILABLE);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        pdfService.generateSpineLabelsPdf(List.of(copy1), baos);

        byte[] pdfBytes = baos.toByteArray();
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500);
        // Verify standard PDF magic header (%PDF-)
        String header = new String(pdfBytes, 0, 5);
        assertEquals("%PDF-", header);
    }

    @Test
    @DisplayName("Should generate valid Member ID Card PDF")
    void testGenerateMemberIdCardPdf() {
        Member member = new Member();
        member.setId(1L);
        member.setMembershipNumber("LIB-2026-0001");
        member.setFullNameEn("Tariqul Islam");
        member.setFullNameBn("তারিকুল ইসলাম");
        member.setMemberTypeName("Student");
        member.setInstitutionalId("CSE-2022-042");
        member.setMembershipExpiryDate(LocalDate.of(2027, 1, 1));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        pdfService.generateMemberIdCardPdf(member, baos);

        byte[] pdfBytes = baos.toByteArray();
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500);
        assertEquals("%PDF-", new String(pdfBytes, 0, 5));
    }

    @Test
    @DisplayName("Should generate valid Payment Receipt Voucher PDF")
    void testGeneratePaymentReceiptPdf() {
        Payment p = new Payment();
        p.setId(1L);
        p.setReceiptNumber("REC-20261104-000001");
        p.setMemberName("Tariqul Islam");
        p.setMembershipNumber("LIB-2026-0001");
        p.setAmountBdt(new BigDecimal("150.00"));
        p.setPaymentMethod(PaymentMethod.BKASH);
        p.setExternalReference("BKASH-TRX-12345");
        p.setRecordedByName("admin");
        p.setPaidAt(LocalDateTime.now());

        PaymentAllocation alloc = new PaymentAllocation();
        alloc.setFineId(10L);
        alloc.setAllocatedAmountBdt(new BigDecimal("150.00"));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        pdfService.generatePaymentReceiptPdf(p, List.of(alloc), baos);

        byte[] pdfBytes = baos.toByteArray();
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500);
        assertEquals("%PDF-", new String(pdfBytes, 0, 5));
    }

    @Test
    @DisplayName("Should generate valid generic tabular report PDF")
    void testGenerateTabularReportPdf() {
        List<String> headers = List.of("ID", "Title", "Status");
        List<List<String>> rows = List.of(
                List.of("1", "Clean Code", "AVAILABLE"),
                List.of("2", "Design Patterns", "ISSUED")
        );
        List<String> totals = List.of("TOTAL", "2 books", "");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        pdfService.generateTabularReportPdf("Sample Report", "Subtitle", headers, rows, totals, baos);

        byte[] pdfBytes = baos.toByteArray();
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500);
        assertEquals("%PDF-", new String(pdfBytes, 0, 5));
    }
}

