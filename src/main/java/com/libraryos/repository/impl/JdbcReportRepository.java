package com.libraryos.repository.impl;

import com.libraryos.config.DatabaseConfig;
import com.libraryos.core.exception.DatabaseException;
import com.libraryos.dto.CatalogInventoryReportItem;
import com.libraryos.dto.CirculationStatsDTO;
import com.libraryos.dto.OverdueReportItem;
import com.libraryos.model.finance.Payment;
import com.libraryos.model.finance.PaymentMethod;
import com.libraryos.repository.ReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class JdbcReportRepository implements ReportRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcReportRepository.class);

    @Override
    public List<OverdueReportItem> getOverdueLoansReport(int minDaysOverdue) {
        String sql = "SELECT l.id, l.issued_at, l.due_date, l.policy_fine_per_day_snapshot, " +
                "b.title_en, b.title_bn, c.barcode, " +
                "COALESCE(m.full_name_en, m.full_name_bn) as member_name, m.membership_number, m.phone " +
                "FROM loans l " +
                "JOIN book_copies c ON l.copy_id = c.id " +
                "JOIN books b ON c.book_id = b.id " +
                "JOIN members m ON l.member_id = m.id " +
                "WHERE l.status = 'ACTIVE' AND l.due_date < CURRENT_DATE " +
                "ORDER BY l.due_date ASC";

        List<OverdueReportItem> list = new ArrayList<>();
        LocalDate today = LocalDate.now();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                LocalDate dueDate = rs.getDate("due_date").toLocalDate();
                long days = ChronoUnit.DAYS.between(dueDate, today);

                if (days >= minDaysOverdue) {
                    OverdueReportItem item = new OverdueReportItem();
                    item.setLoanId(rs.getLong("id"));
                    Timestamp issuedTs = rs.getTimestamp("issued_at");
                    if (issuedTs != null) item.setIssuedDate(issuedTs.toLocalDateTime().toLocalDate());
                    item.setDueDate(dueDate);
                    item.setOverdueDays(days);

                    String tEn = rs.getString("title_en");
                    String tBn = rs.getString("title_bn");
                    item.setBookTitle(tBn != null && !tBn.isBlank() ? tEn + " (" + tBn + ")" : tEn);

                    item.setBarcode(rs.getString("barcode"));
                    item.setMemberName(rs.getString("member_name"));
                    item.setMembershipNumber(rs.getString("membership_number"));
                    item.setMemberPhone(rs.getString("phone"));

                    BigDecimal rate = rs.getBigDecimal("policy_fine_per_day_snapshot");
                    if (rate == null) rate = new BigDecimal("5.00");
                    item.setFineAmount(rate.multiply(BigDecimal.valueOf(days)));

                    list.add(item);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to query overdue loans report", e);
        }
        return list;
    }

    @Override
    public List<Payment> getFineCollectionReport(LocalDate startDate, LocalDate endDate, PaymentMethod method) {
        StringBuilder sql = new StringBuilder("SELECT p.id, p.receipt_number, p.member_id, p.amount_bdt, p.payment_method, " +
                "p.external_reference, p.recorded_by, p.paid_at, p.is_reversed, p.reversal_reason, p.notes, " +
                "COALESCE(m.full_name_en, m.full_name_bn) as member_name, m.membership_number, " +
                "u.username as recorded_by_name " +
                "FROM payments p " +
                "JOIN members m ON p.member_id = m.id " +
                "JOIN users u ON p.recorded_by = u.id " +
                "WHERE 1=1 ");

        List<Object> params = new ArrayList<>();

        if (startDate != null) {
            sql.append("AND p.paid_at >= ? ");
            params.add(Timestamp.valueOf(startDate.atStartOfDay()));
        }
        if (endDate != null) {
            sql.append("AND p.paid_at <= ? ");
            params.add(Timestamp.valueOf(endDate.atTime(23, 59, 59)));
        }
        if (method != null) {
            sql.append("AND p.payment_method = ? ");
            params.add(method.name());
        }

        sql.append("ORDER BY p.paid_at DESC");

        List<Payment> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Payment p = new Payment();
                    p.setId(rs.getLong("id"));
                    p.setReceiptNumber(rs.getString("receipt_number"));
                    p.setMemberId(rs.getLong("member_id"));
                    p.setMemberName(rs.getString("member_name"));
                    p.setMembershipNumber(rs.getString("membership_number"));
                    p.setAmountBdt(rs.getBigDecimal("amount_bdt"));

                    String methStr = rs.getString("payment_method");
                    if (methStr != null) p.setPaymentMethod(PaymentMethod.valueOf(methStr));
                    p.setExternalReference(rs.getString("external_reference"));
                    p.setRecordedBy(rs.getLong("recorded_by"));
                    p.setRecordedByName(rs.getString("recorded_by_name"));

                    Timestamp paidAtTs = rs.getTimestamp("paid_at");
                    if (paidAtTs != null) p.setPaidAt(paidAtTs.toLocalDateTime());

                    p.setReversed(rs.getBoolean("is_reversed"));
                    p.setReversalReason(rs.getString("reversal_reason"));
                    p.setNotes(rs.getString("notes"));

                    list.add(p);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to query fine collection report", e);
        }
        return list;
    }

    @Override
    public CirculationStatsDTO getCirculationStats() {
        CirculationStatsDTO dto = new CirculationStatsDTO();

        try (Connection conn = DatabaseConfig.getConnection()) {
            dto.setTotalActiveLoans(queryLong(conn, "SELECT COUNT(*) FROM loans WHERE status = 'ACTIVE'"));
            dto.setTotalOverdueLoans(queryLong(conn, "SELECT COUNT(*) FROM loans WHERE status = 'ACTIVE' AND due_date < CURRENT_DATE"));
            dto.setTotalReturnedLoans(queryLong(conn, "SELECT COUNT(*) FROM loans WHERE status = 'RETURNED'"));

            dto.setTotalReservationsWaiting(queryLong(conn, "SELECT COUNT(*) FROM reservations WHERE status = 'WAITING'"));
            dto.setTotalReservationsReady(queryLong(conn, "SELECT COUNT(*) FROM reservations WHERE status = 'READY_FOR_PICKUP'"));

            dto.setTotalFinesCollected(queryBigDecimal(conn, "SELECT COALESCE(SUM(amount_bdt), 0.00) FROM payments WHERE is_reversed = FALSE"));
            dto.setTotalFinesUnpaid(queryBigDecimal(conn, "SELECT COALESCE(SUM(original_amount_bdt - waived_amount_bdt - paid_amount_bdt), 0.00) FROM fines WHERE status IN ('UNPAID', 'PARTIALLY_PAID')"));
            dto.setTotalFinesWaived(queryBigDecimal(conn, "SELECT COALESCE(SUM(waived_amount_bdt), 0.00) FROM fines"));

            // Most borrowed books (Top 5)
            String topBooksSql = "SELECT b.title_en, COALESCE(b.isbn_13, b.isbn_10) as isbn, COUNT(l.id) as borrow_count " +
                    "FROM loans l " +
                    "JOIN book_copies c ON l.copy_id = c.id " +
                    "JOIN books b ON c.book_id = b.id " +
                    "GROUP BY b.id, b.title_en, b.isbn_13, b.isbn_10 " +
                    "ORDER BY borrow_count DESC LIMIT 5";

            List<CirculationStatsDTO.PopularBookItem> topBooks = new ArrayList<>();
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(topBooksSql)) {
                while (rs.next()) {
                    topBooks.add(new CirculationStatsDTO.PopularBookItem(
                            rs.getString("title_en"),
                            rs.getString("isbn"),
                            rs.getLong("borrow_count")
                    ));
                }
            }
            dto.setTopBorrowedBooks(topBooks);

        } catch (SQLException e) {
            log.warn("Failed to query circulation statistics: {}", e.getMessage());
        }
        return dto;
    }

    @Override
    public List<CatalogInventoryReportItem> getCatalogInventoryReport(Long categoryId) {
        StringBuilder sql = new StringBuilder("SELECT b.id, b.title_en, b.title_bn, COALESCE(b.isbn_13, b.isbn_10) as isbn, " +
                "p.name as publisher_name, " +
                "COUNT(c.id) as total_copies, " +
                "SUM(CASE WHEN c.status = 'AVAILABLE' THEN 1 ELSE 0 END) as available_copies, " +
                "SUM(CASE WHEN c.status = 'ISSUED' THEN 1 ELSE 0 END) as issued_copies, " +
                "SUM(CASE WHEN c.status IN ('DAMAGED', 'LOST', 'WITHDRAWN') THEN 1 ELSE 0 END) as other_copies " +
                "FROM books b " +
                "LEFT JOIN publishers p ON b.publisher_id = p.id " +
                "LEFT JOIN book_copies c ON b.id = c.book_id " +
                "WHERE b.is_archived = FALSE ");

        List<Object> params = new ArrayList<>();
        if (categoryId != null) {
            sql.append("AND b.id IN (SELECT book_id FROM book_categories WHERE category_id = ?) ");
            params.add(categoryId);
        }

        sql.append("GROUP BY b.id, b.title_en, b.title_bn, b.isbn_13, b.isbn_10, p.name ORDER BY b.title_en ASC");

        List<CatalogInventoryReportItem> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CatalogInventoryReportItem item = new CatalogInventoryReportItem();
                    item.setBookId(rs.getLong("id"));
                    item.setTitleEn(rs.getString("title_en"));
                    item.setTitleBn(rs.getString("title_bn"));
                    item.setIsbn(rs.getString("isbn"));
                    item.setPublisher(rs.getString("publisher_name"));
                    item.setTotalCopies(rs.getInt("total_copies"));
                    item.setAvailableCopies(rs.getInt("available_copies"));
                    item.setIssuedCopies(rs.getInt("issued_copies"));
                    item.setDamagedOrLostCopies(rs.getInt("other_copies"));
                    list.add(item);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to query catalog inventory report", e);
        }
        return list;
    }

    private long queryLong(Connection conn, String sql) {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getLong(1);
        } catch (Exception e) {
            log.debug("queryLong failed: {}", e.getMessage());
        }
        return 0L;
    }

    private BigDecimal queryBigDecimal(Connection conn, String sql) {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                BigDecimal val = rs.getBigDecimal(1);
                return val != null ? val : BigDecimal.ZERO;
            }
        } catch (Exception e) {
            log.debug("queryBigDecimal failed: {}", e.getMessage());
        }
        return BigDecimal.ZERO;
    }
}
