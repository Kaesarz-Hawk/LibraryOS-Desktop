package com.libraryos.service;

import com.libraryos.core.security.PermissionGuard;
import com.libraryos.core.util.DateTimeUtil;
import com.libraryos.dto.CatalogInventoryReportItem;
import com.libraryos.dto.CirculationStatsDTO;
import com.libraryos.dto.OverdueReportItem;
import com.libraryos.model.finance.Payment;
import com.libraryos.model.finance.PaymentMethod;
import com.libraryos.repository.ReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    private final ReportRepository reportRepository;
    private final PdfExportService pdfExportService;
    private final ExcelExportService excelExportService;

    public ReportService(ReportRepository reportRepository,
                         PdfExportService pdfExportService,
                         ExcelExportService excelExportService) {
        this.reportRepository = reportRepository;
        this.pdfExportService = pdfExportService;
        this.excelExportService = excelExportService;
    }

    public List<OverdueReportItem> getOverdueLoansReport(int minDaysOverdue) {
        PermissionGuard.requirePermission("REPORT_VIEW");
        return reportRepository.getOverdueLoansReport(minDaysOverdue);
    }

    public List<Payment> getFineCollectionReport(LocalDate startDate, LocalDate endDate, PaymentMethod method) {
        PermissionGuard.requirePermission("REPORT_VIEW");
        return reportRepository.getFineCollectionReport(startDate, endDate, method);
    }

    public CirculationStatsDTO getCirculationStats() {
        PermissionGuard.requirePermission("REPORT_VIEW");
        return reportRepository.getCirculationStats();
    }

    public List<CatalogInventoryReportItem> getCatalogInventoryReport(Long categoryId) {
        PermissionGuard.requirePermission("REPORT_VIEW");
        return reportRepository.getCatalogInventoryReport(categoryId);
    }

    // --- OVERDUE REPORT EXPORTS ---

    public void exportOverdueToPdf(int minDaysOverdue, OutputStream out) {
        PermissionGuard.requirePermission("REPORT_EXPORT");
        List<OverdueReportItem> items = getOverdueLoansReport(minDaysOverdue);

        List<String> headers = List.of("Loan #", "Book Title", "Barcode", "Member Name", "Card #", "Phone", "Due Date", "Days Late", "Accrued Fine (৳)");
        List<List<String>> rows = new ArrayList<>();
        BigDecimal totalFines = BigDecimal.ZERO;

        for (OverdueReportItem i : items) {
            rows.add(List.of(
                    String.valueOf(i.getLoanId()),
                    i.getBookTitle() != null ? i.getBookTitle() : "—",
                    i.getBarcode() != null ? i.getBarcode() : "—",
                    i.getMemberName() != null ? i.getMemberName() : "—",
                    i.getMembershipNumber() != null ? i.getMembershipNumber() : "—",
                    i.getMemberPhone() != null ? i.getMemberPhone() : "—",
                    DateTimeUtil.formatDate(i.getDueDate()),
                    i.getOverdueDays() + " days",
                    "৳ " + i.getFineAmount().toPlainString()
            ));
            totalFines = totalFines.add(i.getFineAmount());
        }

        List<String> totals = List.of("TOTAL", items.size() + " overdue items", "", "", "", "", "", "", "৳ " + totalFines.toPlainString());

        pdfExportService.generateTabularReportPdf(
                "Overdue Loans & Fine Liability Report",
                "Items overdue by " + minDaysOverdue + "+ days",
                headers, rows, totals, out
        );
    }

    public void exportOverdueToExcel(int minDaysOverdue, OutputStream out) {
        PermissionGuard.requirePermission("REPORT_EXPORT");
        List<OverdueReportItem> items = getOverdueLoansReport(minDaysOverdue);

        List<String> headers = List.of("Loan ID", "Book Title", "Barcode", "Member Name", "Membership Card", "Phone", "Issued Date", "Due Date", "Days Overdue", "Fine Amount (BDT)");
        List<List<String>> rows = new ArrayList<>();

        for (OverdueReportItem i : items) {
            rows.add(List.of(
                    String.valueOf(i.getLoanId()),
                    i.getBookTitle() != null ? i.getBookTitle() : "",
                    i.getBarcode() != null ? i.getBarcode() : "",
                    i.getMemberName() != null ? i.getMemberName() : "",
                    i.getMembershipNumber() != null ? i.getMembershipNumber() : "",
                    i.getMemberPhone() != null ? i.getMemberPhone() : "",
                    i.getIssuedDate() != null ? DateTimeUtil.formatDate(i.getIssuedDate()) : "",
                    DateTimeUtil.formatDate(i.getDueDate()),
                    String.valueOf(i.getOverdueDays()),
                    i.getFineAmount().toPlainString()
            ));
        }

        excelExportService.exportToExcel("Overdue_Loans", headers, rows, out);
    }

    // --- FINE COLLECTIONS EXPORTS ---

    public void exportFineCollectionsToPdf(LocalDate startDate, LocalDate endDate, PaymentMethod method, OutputStream out) {
        PermissionGuard.requirePermission("REPORT_EXPORT");
        List<Payment> payments = getFineCollectionReport(startDate, endDate, method);

        List<String> headers = List.of("Receipt #", "Member", "Card #", "Amount (৳)", "Method", "Trx Ref", "Collected By", "Date", "Status");
        List<List<String>> rows = new ArrayList<>();
        BigDecimal totalCollected = BigDecimal.ZERO;

        for (Payment p : payments) {
            rows.add(List.of(
                    p.getReceiptNumber(),
                    p.getMemberName() != null ? p.getMemberName() : "—",
                    p.getMembershipNumber() != null ? p.getMembershipNumber() : "—",
                    "৳ " + p.getAmount().toPlainString(),
                    p.getPaymentMethod().name(),
                    p.getTransactionReference() != null ? p.getTransactionReference() : "—",
                    p.getReceivedByUsername() != null ? p.getReceivedByUsername() : "—",
                    p.getCreatedAt() != null ? DateTimeUtil.formatDateTime(p.getCreatedAt()) : "—",
                    p.isReversed() ? "REVERSED" : "COMPLETED"
            ));
            if (!p.isReversed()) {
                totalCollected = totalCollected.add(p.getAmount());
            }
        }

        List<String> totals = List.of("TOTAL", payments.size() + " receipts", "", "৳ " + totalCollected.toPlainString(), "", "", "", "", "");

        String subtitle = "Date Range: " + (startDate != null ? DateTimeUtil.formatDate(startDate) : "Beginning") + " to " + (endDate != null ? DateTimeUtil.formatDate(endDate) : "Present");
        pdfExportService.generateTabularReportPdf("Fine Collections & Payment Ledger", subtitle, headers, rows, totals, out);
    }

    public void exportFineCollectionsToExcel(LocalDate startDate, LocalDate endDate, PaymentMethod method, OutputStream out) {
        PermissionGuard.requirePermission("REPORT_EXPORT");
        List<Payment> payments = getFineCollectionReport(startDate, endDate, method);

        List<String> headers = List.of("Receipt Number", "Member Name", "Membership Card", "Amount (BDT)", "Payment Method", "Transaction Ref", "Collected By", "Paid Date", "Status", "Notes");
        List<List<String>> rows = new ArrayList<>();

        for (Payment p : payments) {
            rows.add(List.of(
                    p.getReceiptNumber(),
                    p.getMemberName() != null ? p.getMemberName() : "",
                    p.getMembershipNumber() != null ? p.getMembershipNumber() : "",
                    p.getAmount().toPlainString(),
                    p.getPaymentMethod().name(),
                    p.getTransactionReference() != null ? p.getTransactionReference() : "",
                    p.getReceivedByUsername() != null ? p.getReceivedByUsername() : "",
                    p.getCreatedAt() != null ? DateTimeUtil.formatDateTime(p.getCreatedAt()) : "",
                    p.isReversed() ? "REVERSED" : "COMPLETED",
                    p.getNotes() != null ? p.getNotes() : ""
            ));
        }

        excelExportService.exportToExcel("Fine_Collections", headers, rows, out);
    }

    // --- CATALOG INVENTORY EXPORTS ---

    public void exportCatalogInventoryToPdf(Long categoryId, OutputStream out) {
        PermissionGuard.requirePermission("REPORT_EXPORT");
        List<CatalogInventoryReportItem> items = getCatalogInventoryReport(categoryId);

        List<String> headers = List.of("ID", "Book Title", "ISBN", "Publisher", "Total Copies", "Available", "Issued", "Damaged/Lost");
        List<List<String>> rows = new ArrayList<>();

        int sumTotal = 0;
        int sumAvail = 0;
        int sumIssued = 0;

        for (CatalogInventoryReportItem i : items) {
            rows.add(List.of(
                    String.valueOf(i.getBookId()),
                    i.getDisplayName(),
                    i.getIsbn() != null ? i.getIsbn() : "—",
                    i.getPublisher() != null ? i.getPublisher() : "—",
                    String.valueOf(i.getTotalCopies()),
                    String.valueOf(i.getAvailableCopies()),
                    String.valueOf(i.getIssuedCopies()),
                    String.valueOf(i.getDamagedOrLostCopies())
            ));
            sumTotal += i.getTotalCopies();
            sumAvail += i.getAvailableCopies();
            sumIssued += i.getIssuedCopies();
        }

        List<String> totals = List.of("TOTAL", items.size() + " book titles", "", "", String.valueOf(sumTotal), String.valueOf(sumAvail), String.valueOf(sumIssued), "");
        pdfExportService.generateTabularReportPdf("Catalog Inventory & Physical Copy Audit", "All active catalog titles", headers, rows, totals, out);
    }

    public void exportCatalogInventoryToExcel(Long categoryId, OutputStream out) {
        PermissionGuard.requirePermission("REPORT_EXPORT");
        List<CatalogInventoryReportItem> items = getCatalogInventoryReport(categoryId);

        List<String> headers = List.of("Book ID", "Title (English)", "Title (Bangla)", "ISBN", "Publisher", "Total Copies", "Available Copies", "Issued Copies", "Damaged / Lost Copies");
        List<List<String>> rows = new ArrayList<>();

        for (CatalogInventoryReportItem i : items) {
            rows.add(List.of(
                    String.valueOf(i.getBookId()),
                    i.getTitleEn() != null ? i.getTitleEn() : "",
                    i.getTitleBn() != null ? i.getTitleBn() : "",
                    i.getIsbn() != null ? i.getIsbn() : "",
                    i.getPublisher() != null ? i.getPublisher() : "",
                    String.valueOf(i.getTotalCopies()),
                    String.valueOf(i.getAvailableCopies()),
                    String.valueOf(i.getIssuedCopies()),
                    String.valueOf(i.getDamagedOrLostCopies())
            ));
        }

        excelExportService.exportToExcel("Catalog_Inventory", headers, rows, out);
    }
}

