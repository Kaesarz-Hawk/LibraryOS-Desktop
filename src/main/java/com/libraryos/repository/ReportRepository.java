package com.libraryos.repository;

import com.libraryos.dto.CatalogInventoryReportItem;
import com.libraryos.dto.CirculationStatsDTO;
import com.libraryos.dto.OverdueReportItem;
import com.libraryos.model.finance.Payment;
import com.libraryos.model.finance.PaymentMethod;

import java.time.LocalDate;
import java.util.List;

public interface ReportRepository {

    List<OverdueReportItem> getOverdueLoansReport(int minDaysOverdue);

    List<Payment> getFineCollectionReport(LocalDate startDate, LocalDate endDate, PaymentMethod method);

    CirculationStatsDTO getCirculationStats();

    List<CatalogInventoryReportItem> getCatalogInventoryReport(Long categoryId);
}

