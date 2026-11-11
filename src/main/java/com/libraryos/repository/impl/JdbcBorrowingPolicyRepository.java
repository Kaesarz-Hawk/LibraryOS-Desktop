package com.libraryos.repository.impl;

import com.libraryos.config.DatabaseConfig;
import com.libraryos.core.exception.DatabaseException;
import com.libraryos.model.member.BorrowingPolicy;
import com.libraryos.model.member.MemberType;
import com.libraryos.repository.BorrowingPolicyRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcBorrowingPolicyRepository implements BorrowingPolicyRepository {

    @Override
    public List<BorrowingPolicy> findAll() {
        String sql = "SELECT bp.id, bp.member_type_id, mt.code as member_type_code, bp.max_active_loans, " +
                "bp.loan_duration_days, bp.max_renewals, bp.renewal_duration_days, bp.grace_period_days, " +
                "bp.fine_per_day_bdt, bp.max_fine_per_loan_bdt, bp.max_unpaid_fine_limit_bdt, " +
                "bp.reservation_hold_days, bp.count_holidays_in_overdue, bp.updated_at " +
                "FROM borrowing_policies bp " +
                "JOIN member_types mt ON bp.member_type_id = mt.id " +
                "ORDER BY mt.id ASC";
        List<BorrowingPolicy> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch borrowing policies", e);
        }
        return list;
    }

    @Override
    public Optional<BorrowingPolicy> findByMemberTypeId(int memberTypeId) {
        String sql = "SELECT bp.id, bp.member_type_id, mt.code as member_type_code, bp.max_active_loans, " +
                "bp.loan_duration_days, bp.max_renewals, bp.renewal_duration_days, bp.grace_period_days, " +
                "bp.fine_per_day_bdt, bp.max_fine_per_loan_bdt, bp.max_unpaid_fine_limit_bdt, " +
                "bp.reservation_hold_days, bp.count_holidays_in_overdue, bp.updated_at " +
                "FROM borrowing_policies bp " +
                "JOIN member_types mt ON bp.member_type_id = mt.id " +
                "WHERE bp.member_type_id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, memberTypeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find borrowing policy for member type: " + memberTypeId, e);
        }
        return Optional.empty();
    }

    @Override
    public void update(BorrowingPolicy policy) {
        String sql = "UPDATE borrowing_policies SET max_active_loans = ?, loan_duration_days = ?, " +
                "max_renewals = ?, renewal_duration_days = ?, grace_period_days = ?, fine_per_day_bdt = ?, " +
                "max_fine_per_loan_bdt = ?, max_unpaid_fine_limit_bdt = ?, reservation_hold_days = ?, " +
                "count_holidays_in_overdue = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, policy.getMaxActiveLoans());
            ps.setInt(2, policy.getLoanDurationDays());
            ps.setInt(3, policy.getMaxRenewals());
            ps.setInt(4, policy.getRenewalDurationDays());
            ps.setInt(5, policy.getGracePeriodDays());
            ps.setBigDecimal(6, policy.getFinePerDayBdt());
            ps.setBigDecimal(7, policy.getMaxFinePerLoanBdt());
            ps.setBigDecimal(8, policy.getMaxUnpaidFineLimitBdt());
            ps.setInt(9, policy.getReservationHoldDays());
            ps.setBoolean(10, policy.isCountHolidaysInOverdue());
            ps.setInt(11, policy.getId());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update borrowing policy: " + policy.getId(), e);
        }
    }

    @Override
    public List<MemberType> findAllMemberTypes() {
        String sql = "SELECT id, code, name, description FROM member_types ORDER BY id ASC";
        List<MemberType> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new MemberType(
                        rs.getInt("id"),
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getString("description")
                ));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch member types", e);
        }
        return list;
    }

    private BorrowingPolicy mapRow(ResultSet rs) throws SQLException {
        BorrowingPolicy p = new BorrowingPolicy();
        p.setId(rs.getInt("id"));
        p.setMemberTypeId(rs.getInt("member_type_id"));
        p.setMemberTypeCode(rs.getString("member_type_code"));
        p.setMaxActiveLoans(rs.getInt("max_active_loans"));
        p.setLoanDurationDays(rs.getInt("loan_duration_days"));
        p.setMaxRenewals(rs.getInt("max_renewals"));
        p.setRenewalDurationDays(rs.getInt("renewal_duration_days"));
        p.setGracePeriodDays(rs.getInt("grace_period_days"));
        p.setFinePerDayBdt(rs.getBigDecimal("fine_per_day_bdt"));
        p.setMaxFinePerLoanBdt(rs.getBigDecimal("max_fine_per_loan_bdt"));
        p.setMaxUnpaidFineLimitBdt(rs.getBigDecimal("max_unpaid_fine_limit_bdt"));
        p.setReservationHoldDays(rs.getInt("reservation_hold_days"));
        p.setCountHolidaysInOverdue(rs.getBoolean("count_holidays_in_overdue"));
        Timestamp ts = rs.getTimestamp("updated_at");
        if (ts != null) p.setUpdatedAt(ts.toLocalDateTime());
        return p;
    }
}

