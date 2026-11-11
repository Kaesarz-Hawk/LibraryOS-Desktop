package com.libraryos.repository.impl;

import com.libraryos.config.DatabaseConfig;
import com.libraryos.core.exception.DatabaseException;
import com.libraryos.model.finance.AdjustmentType;
import com.libraryos.model.finance.FineAdjustment;
import com.libraryos.repository.FineAdjustmentRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class JdbcFineAdjustmentRepository implements FineAdjustmentRepository {

    @Override
    public FineAdjustment create(FineAdjustment adjustment, Connection conn) {
        String sql = "INSERT INTO fine_adjustments (fine_id, adjustment_type, amount_bdt, approved_by, reason) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, adjustment.getFineId());
            ps.setString(2, adjustment.getAdjustmentType().name());
            ps.setBigDecimal(3, adjustment.getAmountBdt());
            ps.setLong(4, adjustment.getApprovedBy());
            ps.setString(5, adjustment.getReason());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    adjustment.setId(rs.getLong(1));
                }
            }
            return adjustment;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert fine adjustment", e);
        }
    }

    @Override
    public List<FineAdjustment> findByFineId(Long fineId) {
        String sql = "SELECT fa.id, fa.fine_id, fa.adjustment_type, fa.amount_bdt, fa.approved_by, " +
                "u.username as approved_by_name, fa.reason, fa.created_at " +
                "FROM fine_adjustments fa " +
                "JOIN users u ON fa.approved_by = u.id " +
                "WHERE fa.fine_id = ? ORDER BY fa.created_at DESC";
        List<FineAdjustment> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, fineId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    FineAdjustment fa = new FineAdjustment();
                    fa.setId(rs.getLong("id"));
                    fa.setFineId(rs.getLong("fine_id"));
                    fa.setAdjustmentType(AdjustmentType.valueOf(rs.getString("adjustment_type")));
                    fa.setAmountBdt(rs.getBigDecimal("amount_bdt"));
                    fa.setApprovedBy(rs.getLong("approved_by"));
                    fa.setApprovedByName(rs.getString("approved_by_name"));
                    fa.setReason(rs.getString("reason"));
                    Timestamp ts = rs.getTimestamp("created_at");
                    if (ts != null) fa.setCreatedAt(ts.toLocalDateTime());
                    list.add(fa);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find adjustments for fine: " + fineId, e);
        }
        return list;
    }
}

