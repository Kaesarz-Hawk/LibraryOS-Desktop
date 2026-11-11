package com.libraryos.repository.impl;

import com.libraryos.config.DatabaseConfig;
import com.libraryos.core.exception.DatabaseException;
import com.libraryos.model.circulation.LoanRenewal;
import com.libraryos.repository.LoanRenewalRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class JdbcLoanRenewalRepository implements LoanRenewalRepository {

    @Override
    public void insert(LoanRenewal renewal, Connection conn) {
        String sql = "INSERT INTO loan_renewals (loan_id, previous_due_date, new_due_date, renewed_by, renewed_at) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, renewal.getLoanId());
            ps.setDate(2, Date.valueOf(renewal.getPreviousDueDate()));
            ps.setDate(3, Date.valueOf(renewal.getNewDueDate()));
            ps.setLong(4, renewal.getRenewedBy());
            ps.setTimestamp(5, Timestamp.valueOf(renewal.getRenewedAt() != null ? renewal.getRenewedAt() : java.time.LocalDateTime.now()));

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    renewal.setId(rs.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert loan renewal", e);
        }
    }

    @Override
    public List<LoanRenewal> findByLoanId(Long loanId) {
        String sql = "SELECT lr.id, lr.loan_id, lr.previous_due_date, lr.new_due_date, lr.renewed_by, " +
                "u.username as renewed_by_name, lr.renewed_at " +
                "FROM loan_renewals lr " +
                "JOIN users u ON lr.renewed_by = u.id " +
                "WHERE lr.loan_id = ? ORDER BY lr.renewed_at DESC";
        List<LoanRenewal> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, loanId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LoanRenewal lr = new LoanRenewal();
                    lr.setId(rs.getLong("id"));
                    lr.setLoanId(rs.getLong("loan_id"));
                    lr.setPreviousDueDate(rs.getDate("previous_due_date").toLocalDate());
                    lr.setNewDueDate(rs.getDate("new_due_date").toLocalDate());
                    lr.setRenewedBy(rs.getLong("renewed_by"));
                    lr.setRenewedByName(rs.getString("renewed_by_name"));
                    Timestamp ts = rs.getTimestamp("renewed_at");
                    if (ts != null) lr.setRenewedAt(ts.toLocalDateTime());
                    list.add(lr);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find renewals for loan: " + loanId, e);
        }
        return list;
    }
}

