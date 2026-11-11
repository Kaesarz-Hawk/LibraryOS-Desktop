package com.libraryos.repository.impl;

import com.libraryos.config.DatabaseConfig;
import com.libraryos.core.exception.DatabaseException;
import com.libraryos.model.finance.Fine;
import com.libraryos.model.finance.FineStatus;
import com.libraryos.model.finance.FineType;
import com.libraryos.repository.FineRepository;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcFineRepository implements FineRepository {

    private static final String BASE_SELECT =
            "SELECT f.id, f.member_id, f.loan_id, f.fine_type, f.original_amount_bdt, " +
            "f.waived_amount_bdt, f.paid_amount_bdt, f.status, f.created_by, f.created_at, f.updated_at, " +
            "COALESCE(m.full_name_en, m.full_name_bn) as member_name, m.membership_number, " +
            "c.barcode as copy_barcode, b.title_en as book_title, " +
            "u.username as created_by_name " +
            "FROM fines f " +
            "JOIN members m ON f.member_id = m.id " +
            "LEFT JOIN loans l ON f.loan_id = l.id " +
            "LEFT JOIN book_copies c ON l.copy_id = c.id " +
            "LEFT JOIN books b ON c.book_id = b.id " +
            "LEFT JOIN users u ON f.created_by = u.id ";

    @Override
    public Fine create(Fine fine) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            return create(fine, conn);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to create fine", e);
        }
    }

    @Override
    public Fine create(Fine fine, Connection conn) {
        String sql = "INSERT INTO fines (member_id, loan_id, fine_type, original_amount_bdt, " +
                "waived_amount_bdt, paid_amount_bdt, status, created_by) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, fine.getMemberId());
            if (fine.getLoanId() != null) {
                ps.setLong(2, fine.getLoanId());
            } else {
                ps.setNull(2, Types.BIGINT);
            }
            ps.setString(3, fine.getFineType() != null ? fine.getFineType().name() : FineType.OVERDUE.name());
            ps.setBigDecimal(4, fine.getOriginalAmountBdt());
            ps.setBigDecimal(5, fine.getWaivedAmountBdt() != null ? fine.getWaivedAmountBdt() : BigDecimal.ZERO);
            ps.setBigDecimal(6, fine.getPaidAmountBdt() != null ? fine.getPaidAmountBdt() : BigDecimal.ZERO);
            ps.setString(7, fine.getStatus() != null ? fine.getStatus().name() : FineStatus.UNPAID.name());
            if (fine.getCreatedBy() != null) {
                ps.setLong(8, fine.getCreatedBy());
            } else {
                ps.setNull(8, Types.BIGINT);
            }

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    fine.setId(rs.getLong(1));
                }
            }
            return fine;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert fine", e);
        }
    }

    @Override
    public void update(Fine fine, Connection conn) {
        String sql = "UPDATE fines SET waived_amount_bdt = ?, paid_amount_bdt = ?, status = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, fine.getWaivedAmountBdt());
            ps.setBigDecimal(2, fine.getPaidAmountBdt());
            ps.setString(3, fine.getStatus().name());
            ps.setLong(4, fine.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update fine id: " + fine.getId(), e);
        }
    }

    @Override
    public Optional<Fine> findById(Long id) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            return findById(id, conn);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find fine by id: " + id, e);
        }
    }

    @Override
    public Optional<Fine> findById(Long id, Connection conn) {
        String sql = BASE_SELECT + "WHERE f.id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find fine by id: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Fine> findByIdForUpdate(Long id, Connection conn) {
        String sql = BASE_SELECT + "WHERE f.id = ? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to lock fine by id: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Fine> findByMemberId(Long memberId) {
        String sql = BASE_SELECT + "WHERE f.member_id = ? ORDER BY f.created_at DESC";
        List<Fine> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find fines for member: " + memberId, e);
        }
        return list;
    }

    @Override
    public List<Fine> findUnpaidByMemberId(Long memberId) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            return findUnpaidByMemberId(memberId, conn);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find unpaid fines for member: " + memberId, e);
        }
    }

    @Override
    public List<Fine> findUnpaidByMemberId(Long memberId, Connection conn) {
        String sql = BASE_SELECT + "WHERE f.member_id = ? AND f.status IN ('UNPAID', 'PARTIALLY_PAID') ORDER BY f.created_at ASC";
        List<Fine> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find unpaid fines for member: " + memberId, e);
        }
        return list;
    }

    @Override
    public BigDecimal getUnpaidTotalByMemberId(Long memberId) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            return getUnpaidTotalByMemberId(memberId, conn);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to compute unpaid fine total for member: " + memberId, e);
        }
    }

    @Override
    public BigDecimal getUnpaidTotalByMemberId(Long memberId, Connection conn) {
        String sql = "SELECT COALESCE(SUM(original_amount_bdt - waived_amount_bdt - paid_amount_bdt), 0.00) " +
                "FROM fines WHERE member_id = ? AND status IN ('UNPAID', 'PARTIALLY_PAID')";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal(1);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to compute unpaid fine total for member: " + memberId, e);
        }
        return BigDecimal.ZERO;
    }

    @Override
    public List<Fine> searchFines(String query, FineStatus status) {
        StringBuilder sql = new StringBuilder(BASE_SELECT).append("WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (status != null) {
            sql.append("AND f.status = ? ");
            params.add(status.name());
        }

        if (query != null && !query.isBlank()) {
            String q = "%" + query.trim().toLowerCase() + "%";
            sql.append("AND (LOWER(m.full_name_en) LIKE ? OR LOWER(m.full_name_bn) LIKE ? OR LOWER(m.membership_number) LIKE ? OR LOWER(m.phone) LIKE ? OR LOWER(c.barcode) LIKE ? OR LOWER(f.fine_type) LIKE ? OR LOWER(b.title_en) LIKE ?) ");
            for (int i = 0; i < 7; i++) {
                params.add(q);
            }
        }

        sql.append("ORDER BY f.created_at DESC LIMIT 300");

        List<Fine> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to search fines", e);
        }
        return list;
    }

    private Fine mapRow(ResultSet rs) throws SQLException {
        Fine f = new Fine();
        f.setId(rs.getLong("id"));
        f.setMemberId(rs.getLong("member_id"));
        f.setMemberName(rs.getString("member_name"));
        f.setMembershipNumber(rs.getString("membership_number"));

        long loanIdVal = rs.getLong("loan_id");
        if (!rs.wasNull()) f.setLoanId(loanIdVal);
        f.setCopyBarcode(rs.getString("copy_barcode"));
        f.setBookTitle(rs.getString("book_title"));

        String typeStr = rs.getString("fine_type");
        if (typeStr != null) f.setFineType(FineType.valueOf(typeStr));

        f.setOriginalAmountBdt(rs.getBigDecimal("original_amount_bdt"));
        f.setWaivedAmountBdt(rs.getBigDecimal("waived_amount_bdt"));
        f.setPaidAmountBdt(rs.getBigDecimal("paid_amount_bdt"));

        String statusStr = rs.getString("status");
        if (statusStr != null) f.setStatus(FineStatus.valueOf(statusStr));

        long cb = rs.getLong("created_by");
        if (!rs.wasNull()) f.setCreatedBy(cb);
        f.setCreatedByName(rs.getString("created_by_name"));

        Timestamp cTs = rs.getTimestamp("created_at");
        if (cTs != null) f.setCreatedAt(cTs.toLocalDateTime());

        Timestamp uTs = rs.getTimestamp("updated_at");
        if (uTs != null) f.setUpdatedAt(uTs.toLocalDateTime());

        return f;
    }
}
