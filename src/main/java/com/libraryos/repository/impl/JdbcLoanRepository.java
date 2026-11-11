package com.libraryos.repository.impl;

import com.libraryos.config.DatabaseConfig;
import com.libraryos.core.exception.DatabaseException;
import com.libraryos.model.circulation.Loan;
import com.libraryos.model.circulation.LoanStatus;
import com.libraryos.repository.LoanRepository;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcLoanRepository implements LoanRepository {

    private static final String BASE_SELECT =
            "SELECT l.id, l.copy_id, l.member_id, l.issued_by, l.issued_at, l.due_date, " +
            "l.returned_at, l.returned_to, l.return_condition, l.renewal_count, " +
            "l.policy_fine_per_day_snapshot, l.status, l.notes, " +
            "c.barcode as copy_barcode, c.accession_number as copy_accession_number, " +
            "b.title_en as book_title, " +
            "COALESCE(m.full_name_en, m.full_name_bn) as member_name, m.membership_number, m.phone as member_phone, " +
            "u1.username as issued_by_name, u2.username as returned_to_name " +
            "FROM loans l " +
            "JOIN book_copies c ON l.copy_id = c.id " +
            "JOIN books b ON c.book_id = b.id " +
            "JOIN members m ON l.member_id = m.id " +
            "JOIN users u1 ON l.issued_by = u1.id " +
            "LEFT JOIN users u2 ON l.returned_to = u2.id ";

    @Override
    public Loan create(Loan loan, Connection conn) {
        String sql = "INSERT INTO loans (copy_id, member_id, issued_by, issued_at, due_date, " +
                "renewal_count, policy_fine_per_day_snapshot, status, notes) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, loan.getCopyId());
            ps.setLong(2, loan.getMemberId());
            ps.setLong(3, loan.getIssuedBy());
            ps.setTimestamp(4, Timestamp.valueOf(loan.getIssuedAt() != null ? loan.getIssuedAt() : java.time.LocalDateTime.now()));
            ps.setDate(5, Date.valueOf(loan.getDueDate()));
            ps.setInt(6, loan.getRenewalCount());
            ps.setBigDecimal(7, loan.getPolicyFinePerDaySnapshot());
            ps.setString(8, loan.getStatus() != null ? loan.getStatus().name() : LoanStatus.ACTIVE.name());
            ps.setString(9, loan.getNotes());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    loan.setId(rs.getLong(1));
                }
            }
            return loan;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert loan", e);
        }
    }

    @Override
    public void update(Loan loan, Connection conn) {
        String sql = "UPDATE loans SET returned_at = ?, returned_to = ?, return_condition = ?, " +
                "renewal_count = ?, due_date = ?, status = ?, notes = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (loan.getReturnedAt() != null) {
                ps.setTimestamp(1, Timestamp.valueOf(loan.getReturnedAt()));
            } else {
                ps.setNull(1, Types.TIMESTAMP);
            }
            if (loan.getReturnedTo() != null) {
                ps.setLong(2, loan.getReturnedTo());
            } else {
                ps.setNull(2, Types.BIGINT);
            }
            ps.setString(3, loan.getReturnCondition());
            ps.setInt(4, loan.getRenewalCount());
            ps.setDate(5, Date.valueOf(loan.getDueDate()));
            ps.setString(6, loan.getStatus().name());
            ps.setString(7, loan.getNotes());
            ps.setLong(8, loan.getId());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update loan id: " + loan.getId(), e);
        }
    }

    @Override
    public Optional<Loan> findById(Long id) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            return findById(id, conn);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find loan by id: " + id, e);
        }
    }

    @Override
    public Optional<Loan> findById(Long id, Connection conn) {
        String sql = BASE_SELECT + "WHERE l.id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find loan by id: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Loan> findByIdForUpdate(Long id, Connection conn) {
        String sql = BASE_SELECT + "WHERE l.id = ? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to lock loan by id: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Loan> findActiveLoanByCopyId(Long copyId) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            return findActiveLoanByCopyId(copyId, conn);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find active loan for copy: " + copyId, e);
        }
    }

    @Override
    public Optional<Loan> findActiveLoanByCopyId(Long copyId, Connection conn) {
        String sql = BASE_SELECT + "WHERE l.copy_id = ? AND l.status = 'ACTIVE'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, copyId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find active loan for copy: " + copyId, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Loan> findActiveLoanByCopyIdForUpdate(Long copyId, Connection conn) {
        String sql = BASE_SELECT + "WHERE l.copy_id = ? AND l.status = 'ACTIVE' FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, copyId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to lock active loan for copy: " + copyId, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Loan> findActiveLoansByMemberId(Long memberId) {
        String sql = BASE_SELECT + "WHERE l.member_id = ? AND l.status = 'ACTIVE' ORDER BY l.due_date ASC";
        List<Loan> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch active loans for member: " + memberId, e);
        }
        return list;
    }

    @Override
    public int countActiveLoansByMemberId(Long memberId) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            return countActiveLoansByMemberId(memberId, conn);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to count active loans for member: " + memberId, e);
        }
    }

    @Override
    public int countActiveLoansByMemberId(Long memberId, Connection conn) {
        String sql = "SELECT COUNT(*) FROM loans WHERE member_id = ? AND status = 'ACTIVE'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to count active loans for member: " + memberId, e);
        }
        return 0;
    }

    @Override
    public boolean hasActiveLoanForBook(Long bookId, Long memberId) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            return hasActiveLoanForBook(bookId, memberId, conn);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to check active loan for book: " + bookId, e);
        }
    }

    @Override
    public boolean hasActiveLoanForBook(Long bookId, Long memberId, Connection conn) {
        String sql = "SELECT COUNT(*) FROM loans l JOIN book_copies c ON l.copy_id = c.id " +
                "WHERE c.book_id = ? AND l.member_id = ? AND l.status = 'ACTIVE'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bookId);
            ps.setLong(2, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to check active loan for book: " + bookId, e);
        }
        return false;
    }

    @Override
    public List<Loan> searchLoans(String query, LoanStatus status, Boolean overdueOnly) {
        StringBuilder sql = new StringBuilder(BASE_SELECT).append("WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (status != null) {
            sql.append("AND l.status = ? ");
            params.add(status.name());
        }

        if (Boolean.TRUE.equals(overdueOnly)) {
            sql.append("AND l.status = 'ACTIVE' AND l.due_date < ? ");
            params.add(Date.valueOf(LocalDate.now()));
        }

        if (query != null && !query.isBlank()) {
            String q = "%" + query.trim() + "%";
            sql.append("AND (c.barcode LIKE ? OR c.accession_number LIKE ? OR b.title_en LIKE ? " +
                    "OR b.title_bn LIKE ? OR m.full_name_en LIKE ? OR m.full_name_bn LIKE ? OR m.membership_number LIKE ? OR m.phone LIKE ?) ");
            for (int i = 0; i < 8; i++) {
                params.add(q);
            }
        }

        sql.append("ORDER BY l.due_date ASC, l.id DESC LIMIT 300");

        List<Loan> list = new ArrayList<>();
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
            throw new DatabaseException("Failed to search loans", e);
        }
        return list;
    }

    private Loan mapRow(ResultSet rs) throws SQLException {
        Loan l = new Loan();
        l.setId(rs.getLong("id"));
        l.setCopyId(rs.getLong("copy_id"));
        l.setCopyBarcode(rs.getString("copy_barcode"));
        l.setCopyAccessionNumber(rs.getString("copy_accession_number"));
        l.setBookTitle(rs.getString("book_title"));
        l.setMemberId(rs.getLong("member_id"));
        l.setMemberName(rs.getString("member_name"));
        l.setMembershipNumber(rs.getString("membership_number"));
        l.setMemberPhone(rs.getString("member_phone"));
        l.setIssuedBy(rs.getLong("issued_by"));
        l.setIssuedByName(rs.getString("issued_by_name"));

        Timestamp issuedAt = rs.getTimestamp("issued_at");
        if (issuedAt != null) l.setIssuedAt(issuedAt.toLocalDateTime());

        Date dueDate = rs.getDate("due_date");
        if (dueDate != null) l.setDueDate(dueDate.toLocalDate());

        Timestamp retAt = rs.getTimestamp("returned_at");
        if (retAt != null) l.setReturnedAt(retAt.toLocalDateTime());

        long retTo = rs.getLong("returned_to");
        if (!rs.wasNull()) l.setReturnedTo(retTo);
        l.setReturnedToName(rs.getString("returned_to_name"));

        l.setReturnCondition(rs.getString("return_condition"));
        l.setRenewalCount(rs.getInt("renewal_count"));
        l.setPolicyFinePerDaySnapshot(rs.getBigDecimal("policy_fine_per_day_snapshot"));

        String statusStr = rs.getString("status");
        if (statusStr != null) l.setStatus(LoanStatus.valueOf(statusStr));

        l.setNotes(rs.getString("notes"));
        return l;
    }
}
