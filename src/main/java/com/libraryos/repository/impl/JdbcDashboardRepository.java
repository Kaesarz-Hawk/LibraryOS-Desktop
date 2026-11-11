package com.libraryos.repository.impl;

import com.libraryos.config.DatabaseConfig;
import com.libraryos.dto.DashboardMetricsDTO;
import com.libraryos.repository.DashboardRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

public class JdbcDashboardRepository implements DashboardRepository {

    private static final Logger log = LoggerFactory.getLogger(JdbcDashboardRepository.class);

    @Override
    public DashboardMetricsDTO getMetrics() {
        DashboardMetricsDTO dto = new DashboardMetricsDTO();
        if (!DatabaseConfig.isDbAvailable()) {
            return dto;
        }

        try (Connection conn = DatabaseConfig.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();

            if (tableExists(meta, "books")) {
                dto.setTotalBooks(queryCount(conn, "SELECT COUNT(*) FROM books WHERE is_archived = FALSE"));
            }
            if (tableExists(meta, "book_copies")) {
                dto.setTotalCopies(queryCount(conn, "SELECT COUNT(*) FROM book_copies WHERE status != 'WITHDRAWN'"));
            }
            if (tableExists(meta, "loans")) {
                dto.setActiveLoans(queryCount(conn, "SELECT COUNT(*) FROM loans WHERE status = 'ACTIVE'"));
                dto.setOverdueLoans(queryCount(conn, "SELECT COUNT(*) FROM loans WHERE status = 'ACTIVE' AND due_date < CURRENT_DATE"));
            }
            if (tableExists(meta, "members")) {
                dto.setTotalMembers(queryCount(conn, "SELECT COUNT(*) FROM members WHERE status = 'ACTIVE'"));
            }
            if (tableExists(meta, "fines")) {
                dto.setPendingFinesBdt(querySum(conn, "SELECT COALESCE(SUM(original_amount_bdt - waived_amount_bdt - paid_amount_bdt), 0.00) FROM fines WHERE status IN ('UNPAID', 'PARTIALLY_PAID')"));
            }
        } catch (Exception e) {
            log.warn("Could not query dashboard metrics from DB: {}", e.getMessage());
        }
        return dto;
    }

    private boolean tableExists(DatabaseMetaData meta, String tableName) {
        try (ResultSet rs = meta.getTables(null, null, tableName, new String[]{"TABLE"})) {
            if (rs.next()) return true;
        } catch (Exception ignored) {
        }
        try (ResultSet rs = meta.getTables(null, null, tableName.toUpperCase(), new String[]{"TABLE"})) {
            return rs.next();
        } catch (Exception ignored) {
        }
        return false;
    }

    private long queryCount(Connection conn, String sql) {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (Exception e) {
            log.debug("Query count failed: {}", e.getMessage());
        }
        return 0L;
    }

    private BigDecimal querySum(Connection conn, String sql) {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                BigDecimal sum = rs.getBigDecimal(1);
                return sum != null ? sum : BigDecimal.ZERO;
            }
        } catch (Exception e) {
            log.debug("Query sum failed: {}", e.getMessage());
        }
        return BigDecimal.ZERO;
    }
}

