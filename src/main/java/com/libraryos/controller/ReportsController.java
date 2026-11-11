package com.libraryos.controller;

import com.libraryos.app.AppContext;
import com.libraryos.core.security.SessionManager;
import com.libraryos.dto.CatalogInventoryReportItem;
import com.libraryos.dto.CirculationStatsDTO;
import com.libraryos.dto.OverdueReportItem;
import com.libraryos.model.finance.Payment;
import com.libraryos.model.finance.PaymentMethod;
import com.libraryos.service.PdfExportService;
import com.libraryos.service.ReportService;
import com.libraryos.view.ReportsView;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ReportsController {

    private static final Logger log = LoggerFactory.getLogger(ReportsController.class);

    private final ReportsView view;
    private final ReportService reportService;
    private final PdfExportService pdfExportService;

    public ReportsController(ReportsView view) {
        this.view = view;
        this.reportService = AppContext.getInstance().getReportService();
        this.pdfExportService = AppContext.getInstance().getPdfExportService();

        initialize();
    }

    private void initialize() {
        boolean canExport = SessionManager.hasPermission("REPORT_EXPORT");

        view.getOverduePdfBtn().setDisable(!canExport);
        view.getOverdueExcelBtn().setDisable(!canExport);
        view.getCollectionsPdfBtn().setDisable(!canExport);
        view.getCollectionsExcelBtn().setDisable(!canExport);
        view.getCatalogPdfBtn().setDisable(!canExport);
        view.getCatalogExcelBtn().setDisable(!canExport);

        // Header
        view.getRefreshBtn().setOnAction(e -> handleRefreshActiveTab());

        // Tab 1: Overdue Wiring
        view.getOverdueFilterBtn().setOnAction(e -> loadOverdueReport());
        view.getOverdueDaysFilter().setOnAction(e -> loadOverdueReport());
        view.getOverduePdfBtn().setOnAction(e -> handleExportOverdue(true));
        view.getOverdueExcelBtn().setOnAction(e -> handleExportOverdue(false));

        // Tab 2: Collections Wiring
        view.getCollectionsFilterBtn().setOnAction(e -> loadCollectionsReport());
        view.getCollectionsResetBtn().setOnAction(e -> {
            view.getCollectionsStartDate().setValue(null);
            view.getCollectionsEndDate().setValue(null);
            view.getCollectionsMethodFilter().setValue("ALL");
            loadCollectionsReport();
        });
        view.getCollectionsPdfBtn().setOnAction(e -> handleExportCollections(true));
        view.getCollectionsExcelBtn().setOnAction(e -> handleExportCollections(false));
        setupCollectionsContextMenu();

        // Tab 4: Catalog Audit Wiring
        view.getCatalogFilterBtn().setOnAction(e -> loadCatalogReport());
        view.getCatalogSearchField().setOnAction(e -> loadCatalogReport());
        view.getCatalogPdfBtn().setOnAction(e -> handleExportCatalog(true));
        view.getCatalogExcelBtn().setOnAction(e -> handleExportCatalog(false));

        // Tab Switch Listener
        view.getTabPane().getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == view.getCollectionsTab() && view.getCollectionsTable().getItems().isEmpty()) {
                loadCollectionsReport();
            } else if (newTab == view.getStatsTab()) {
                loadCirculationStats();
            } else if (newTab == view.getCatalogTab() && view.getCatalogTable().getItems().isEmpty()) {
                loadCatalogReport();
            }
        });

        // Initial Load on default tab (Overdue)
        loadOverdueReport();
    }

    private void handleRefreshActiveTab() {
        Tab cur = view.getTabPane().getSelectionModel().getSelectedItem();
        if (cur == view.getOverdueTab()) {
            loadOverdueReport();
        } else if (cur == view.getCollectionsTab()) {
            loadCollectionsReport();
        } else if (cur == view.getStatsTab()) {
            loadCirculationStats();
        } else if (cur == view.getCatalogTab()) {
            loadCatalogReport();
        }
    }

    public void loadOverdueReport() {
        String filterStr = view.getOverdueDaysFilter().getValue();
        int minDays = 1;
        if (filterStr != null) {
            if (filterStr.contains("7+")) minDays = 7;
            else if (filterStr.contains("14+")) minDays = 14;
            else if (filterStr.contains("30+")) minDays = 30;
        }

        final int threshold = minDays;
        Task<List<OverdueReportItem>> task = new Task<>() {
            @Override
            protected List<OverdueReportItem> call() {
                return reportService.getOverdueLoansReport(threshold);
            }
        };

        task.setOnSucceeded(e -> {
            List<OverdueReportItem> list = task.getValue();
            view.getOverdueTable().setItems(FXCollections.observableArrayList(list));
            BigDecimal totalFines = list.stream().map(OverdueReportItem::getFineAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            view.getOverdueSummaryLabel().setText("Showing " + list.size() + " overdue item(s) • Total Accrued Fine Liability: ৳ " + totalFines.toPlainString());
        });

        task.setOnFailed(e -> {
            log.error("Failed to load overdue report", task.getException());
            showError("Report Error", task.getException().getMessage());
        });

        AppContext.getInstance().getExecutorService().execute(task);
    }

    private void handleExportOverdue(boolean isPdf) {
        String filterStr = view.getOverdueDaysFilter().getValue();
        int minDays = filterStr != null && filterStr.contains("7+") ? 7 : (filterStr != null && filterStr.contains("14+") ? 14 : (filterStr != null && filterStr.contains("30+") ? 30 : 1));

        FileChooser fc = new FileChooser();
        fc.setTitle(isPdf ? "Save Overdue Report PDF" : "Save Overdue Report Excel");
        fc.setInitialFileName("Overdue_Report_" + LocalDate.now() + (isPdf ? ".pdf" : ".xlsx"));
        fc.getExtensionFilters().add(isPdf ? new FileChooser.ExtensionFilter("PDF Document (*.pdf)", "*.pdf") : new FileChooser.ExtensionFilter("Excel Spreadsheet (*.xlsx)", "*.xlsx"));

        File file = fc.showSaveDialog(view.getScene().getWindow());
        if (file == null) return;

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    if (isPdf) {
                        reportService.exportOverdueToPdf(minDays, fos);
                    } else {
                        reportService.exportOverdueToExcel(minDays, fos);
                    }
                }
                return null;
            }
        };

        task.setOnSucceeded(e -> showSuccess("Export Complete", "Overdue report saved successfully to:\n" + file.getAbsolutePath()));
        task.setOnFailed(e -> showError("Export Failed", task.getException().getMessage()));
        AppContext.getInstance().getExecutorService().execute(task);
    }

    public void loadCollectionsReport() {
        LocalDate start = view.getCollectionsStartDate().getValue();
        LocalDate end = view.getCollectionsEndDate().getValue();
        String methodStr = view.getCollectionsMethodFilter().getValue();
        PaymentMethod method = (methodStr == null || "ALL".equals(methodStr)) ? null : PaymentMethod.valueOf(methodStr);

        Task<List<Payment>> task = new Task<>() {
            @Override
            protected List<Payment> call() {
                return reportService.getFineCollectionReport(start, end, method);
            }
        };

        task.setOnSucceeded(e -> {
            List<Payment> list = task.getValue();
            view.getCollectionsTable().setItems(FXCollections.observableArrayList(list));

            BigDecimal totalCollected = list.stream()
                    .filter(p -> !p.isReversed())
                    .map(Payment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            view.getCollectionsTotalKpi().setText("৳ " + totalCollected.toPlainString());
            view.getCollectionsCountKpi().setText(list.size() + " receipts");
            view.getCollectionsCountLabel().setText("Showing " + list.size() + " payment record(s)");
        });

        task.setOnFailed(e -> {
            log.error("Failed to load collections report", task.getException());
            showError("Report Error", task.getException().getMessage());
        });

        AppContext.getInstance().getExecutorService().execute(task);
    }

    private void handleExportCollections(boolean isPdf) {
        LocalDate start = view.getCollectionsStartDate().getValue();
        LocalDate end = view.getCollectionsEndDate().getValue();
        String methodStr = view.getCollectionsMethodFilter().getValue();
        PaymentMethod method = (methodStr == null || "ALL".equals(methodStr)) ? null : PaymentMethod.valueOf(methodStr);

        FileChooser fc = new FileChooser();
        fc.setTitle(isPdf ? "Save Fine Collections PDF" : "Save Fine Collections Excel");
        fc.setInitialFileName("Collections_Report_" + LocalDate.now() + (isPdf ? ".pdf" : ".xlsx"));
        fc.getExtensionFilters().add(isPdf ? new FileChooser.ExtensionFilter("PDF Document (*.pdf)", "*.pdf") : new FileChooser.ExtensionFilter("Excel Spreadsheet (*.xlsx)", "*.xlsx"));

        File file = fc.showSaveDialog(view.getScene().getWindow());
        if (file == null) return;

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    if (isPdf) {
                        reportService.exportFineCollectionsToPdf(start, end, method, fos);
                    } else {
                        reportService.exportFineCollectionsToExcel(start, end, method, fos);
                    }
                }
                return null;
            }
        };

        task.setOnSucceeded(e -> showSuccess("Export Complete", "Collections report saved successfully to:\n" + file.getAbsolutePath()));
        task.setOnFailed(e -> showError("Export Failed", task.getException().getMessage()));
        AppContext.getInstance().getExecutorService().execute(task);
    }

    private void setupCollectionsContextMenu() {
        view.getCollectionsTable().setRowFactory(tv -> {
            TableRow<Payment> row = new TableRow<>();
            ContextMenu cm = new ContextMenu();

            MenuItem printReceiptItem = new MenuItem("Print Receipt Voucher (PDF) / রসিদ প্রিন্ট");
            printReceiptItem.setOnAction(e -> {
                Payment p = row.getItem();
                if (p != null) {
                    handlePrintReceipt(p);
                }
            });

            cm.getItems().add(printReceiptItem);

            row.emptyProperty().addListener((obs, wasEmpty, isEmpty) -> {
                if (isEmpty) row.setContextMenu(null);
                else row.setContextMenu(cm);
            });

            return row;
        });
    }

    private void handlePrintReceipt(Payment payment) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Save Receipt Voucher PDF");
        fc.setInitialFileName("Receipt_" + payment.getReceiptNumber() + ".pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Document (*.pdf)", "*.pdf"));

        File file = fc.showSaveDialog(view.getScene().getWindow());
        if (file == null) return;

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    pdfExportService.generatePaymentReceiptPdf(payment, payment.getAllocations(), fos);
                }
                return null;
            }
        };

        task.setOnSucceeded(e -> showSuccess("Receipt Exported", "Payment receipt PDF saved to:\n" + file.getAbsolutePath()));
        task.setOnFailed(e -> showError("Print Error", task.getException().getMessage()));
        AppContext.getInstance().getExecutorService().execute(task);
    }

    public void loadCirculationStats() {
        Task<CirculationStatsDTO> task = new Task<>() {
            @Override
            protected CirculationStatsDTO call() {
                return reportService.getCirculationStats();
            }
        };

        task.setOnSucceeded(e -> {
            CirculationStatsDTO stats = task.getValue();
            view.getStatActiveLoans().setText(String.valueOf(stats.getTotalActiveLoans()));
            view.getStatOverdueLoans().setText(String.valueOf(stats.getTotalOverdueLoans()));
            view.getStatReturnedLoans().setText(String.valueOf(stats.getTotalReturnedLoans()));
            view.getStatCollectedFines().setText("৳ " + stats.getTotalFinesCollected().toPlainString());
            view.getTopBooksTable().setItems(FXCollections.observableArrayList(stats.getTopBorrowedBooks()));
        });

        task.setOnFailed(e -> {
            log.error("Failed to load circulation stats", task.getException());
            showError("Stats Error", task.getException().getMessage());
        });

        AppContext.getInstance().getExecutorService().execute(task);
    }

    public void loadCatalogReport() {
        Task<List<CatalogInventoryReportItem>> task = new Task<>() {
            @Override
            protected List<CatalogInventoryReportItem> call() {
                return reportService.getCatalogInventoryReport(null);
            }
        };

        task.setOnSucceeded(e -> {
            List<CatalogInventoryReportItem> list = task.getValue();
            String q = view.getCatalogSearchField().getText() != null ? view.getCatalogSearchField().getText().trim().toLowerCase() : "";
            if (!q.isBlank()) {
                list = list.stream().filter(item ->
                        (item.getTitleEn() != null && item.getTitleEn().toLowerCase().contains(q)) ||
                        (item.getTitleBn() != null && item.getTitleBn().toLowerCase().contains(q)) ||
                        (item.getIsbn() != null && item.getIsbn().toLowerCase().contains(q))
                ).toList();
            }
            view.getCatalogTable().setItems(FXCollections.observableArrayList(list));
            view.getCatalogSummaryLabel().setText("Showing " + list.size() + " catalog title(s)");
        });

        task.setOnFailed(e -> {
            log.error("Failed to load catalog report", task.getException());
            showError("Catalog Report Error", task.getException().getMessage());
        });

        AppContext.getInstance().getExecutorService().execute(task);
    }

    private void handleExportCatalog(boolean isPdf) {
        FileChooser fc = new FileChooser();
        fc.setTitle(isPdf ? "Save Catalog Inventory PDF" : "Save Catalog Inventory Excel");
        fc.setInitialFileName("Catalog_Inventory_" + LocalDate.now() + (isPdf ? ".pdf" : ".xlsx"));
        fc.getExtensionFilters().add(isPdf ? new FileChooser.ExtensionFilter("PDF Document (*.pdf)", "*.pdf") : new FileChooser.ExtensionFilter("Excel Spreadsheet (*.xlsx)", "*.xlsx"));

        File file = fc.showSaveDialog(view.getScene().getWindow());
        if (file == null) return;

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    if (isPdf) {
                        reportService.exportCatalogInventoryToPdf(null, fos);
                    } else {
                        reportService.exportCatalogInventoryToExcel(null, fos);
                    }
                }
                return null;
            }
        };

        task.setOnSucceeded(e -> showSuccess("Export Complete", "Catalog inventory report saved to:\n" + file.getAbsolutePath()));
        task.setOnFailed(e -> showError("Export Failed", task.getException().getMessage()));
        AppContext.getInstance().getExecutorService().execute(task);
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

