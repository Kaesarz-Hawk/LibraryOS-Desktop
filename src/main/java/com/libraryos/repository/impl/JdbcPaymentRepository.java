package com.libraryos.repository.impl;

import com.libraryos.config.DatabaseConfig;
import com.libraryos.core.exception.DatabaseException;
import com.libraryos.model.finance.Payment;
import com.libraryos.model.finance.PaymentAllocation;
import com.libraryos.model.finance.PaymentMethod;
import com.libraryos.repository.PaymentRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcPaymentRepository implements PaymentRepository {

    private static final String BASE_SELECT =
            "SELECT p.id, p.receipt_number, p.member_id, p.amount_bdt, p.payment_method, " +
            "p.external_reference, p.recorded_by, p.paid_at, p.is_reversed, p.reversal_reason, p.notes, " +
            "COALESCE(m.full_name_en, m.full_name_bn) as member_name, m.membership_number, " +
            "u.username as recorded_by_name " +
            "FROM payments p " +
            "JOIN members m ON p.member_id = m.id " +
            "JOIN users u ON p.recorded_by = u.id ";

    @Override
    public Payment create(Payment payment, Connection conn) {
        String sql = "INSERT INTO payments (receipt_number, member_id, amount_bdt, payment_method, " +
                "external_reference, recorded_by, paid_at, is_reversed, notes) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, payment.getReceiptNumber());
            ps.setLong(2, payment.getMemberId());
            ps.setBigDecimal(3, payment.getAmountBdt());
            ps.setString(4, payment.getPaymentMethod().name());
            ps.setString(5, payment.getExternalReference());
            ps.setLong(6, payment.getRecordedBy());
            ps.setTimestamp(7, Timestamp.valueOf(payment.getPaidAt() != null ? payment.getPaidAt() : java.time.LocalDateTime.now()));
            ps.setBoolean(8, payment.isReversed());
            ps.setString(9, payment.getNotes());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    payment.setId(rs.getLong(1));
                }
            }
            return payment;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert payment: " + payment.getReceiptNumber(), e);
        }
    }

    @Override
    public void update(Payment payment, Connection conn) {
        String sql = "UPDATE payments SET is_reversed = ?, reversal_reason = ?, notes = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, payment.isReversed());
            ps.setString(2, payment.getReversalReason());
            ps.setString(3, payment.getNotes());
            ps.setLong(4, payment.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update payment id: " + payment.getId(), e);
        }
    }

    @Override
    public Optional<Payment> findById(Long id) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = BASE_SELECT + "WHERE p.id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        Payment p = mapRow(rs);
                        p.setAllocations(findAllocationsByPaymentId(p.getId(), conn));
                        return Optional.of(p);
                    }
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find payment by id: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Payment> findByIdForUpdate(Long id, Connection conn) {
        String sql = BASE_SELECT + "WHERE p.id = ? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Payment p = mapRow(rs);
                    p.setAllocations(findAllocationsByPaymentId(p.getId(), conn));
                    return Optional.of(p);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to lock payment by id: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Payment> findByReceiptNumber(String receiptNumber) {
        if (receiptNumber == null || receiptNumber.isBlank()) return Optional.empty();
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = BASE_SELECT + "WHERE p.receipt_number = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, receiptNumber.trim());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        Payment p = mapRow(rs);
                        p.setAllocations(findAllocationsByPaymentId(p.getId(), conn));
                        return Optional.of(p);
                    }
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find payment by receipt number: " + receiptNumber, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Payment> findByExternalReference(String externalReference) {
        if (externalReference == null || externalReference.isBlank()) return Optional.empty();
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = BASE_SELECT + "WHERE p.external_reference = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, externalReference.trim());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        Payment p = mapRow(rs);
                        p.setAllocations(findAllocationsByPaymentId(p.getId(), conn));
                        return Optional.of(p);
                    }
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find payment by external reference: " + externalReference, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Payment> findByMemberId(Long memberId) {
        String sql = BASE_SELECT + "WHERE p.member_id = ? ORDER BY p.paid_at DESC";
        List<Payment> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find payments for member: " + memberId, e);
        }
        return list;
    }

    @Override
    public List<Payment> search(String query, PaymentMethod method, Boolean reversedOnly) {
        StringBuilder sql = new StringBuilder(BASE_SELECT).append("WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (method != null) {
            sql.append("AND p.payment_method = ? ");
            params.add(method.name());
        }

        if (Boolean.TRUE.equals(reversedOnly)) {
            sql.append("AND p.is_reversed = TRUE ");
        }

        if (query != null && !query.isBlank()) {
            String q = "%" + query.trim().toLowerCase() + "%";
            sql.append("AND (LOWER(p.receipt_number) LIKE ? OR LOWER(p.external_reference) LIKE ? OR " +
                    "LOWER(m.full_name_en) LIKE ? OR LOWER(m.full_name_bn) LIKE ? OR LOWER(m.membership_number) LIKE ?) ");
            for (int i = 0; i < 5; i++) {
                params.add(q);
            }
        }

        sql.append("ORDER BY p.paid_at DESC LIMIT 300");

        List<Payment> list = new ArrayList<>();
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
            throw new DatabaseException("Failed to search payments", e);
        }
        return list;
    }

    @Override
    public void insertAllocation(PaymentAllocation allocation, Connection conn) {
        String sql = "INSERT INTO payment_allocations (payment_id, fine_id, allocated_amount_bdt) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, allocation.getPaymentId());
            ps.setLong(2, allocation.getFineId());
            ps.setBigDecimal(3, allocation.getAllocatedAmountBdt());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    allocation.setId(rs.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert payment allocation", e);
        }
    }

    @Override
    public List<PaymentAllocation> findAllocationsByPaymentId(Long paymentId) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            return findAllocationsByPaymentId(paymentId, conn);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find allocations for payment: " + paymentId, e);
        }
    }

    @Override
    public List<PaymentAllocation> findAllocationsByPaymentId(Long paymentId, Connection conn) {
        String sql = "SELECT pa.id, pa.payment_id, pa.fine_id, pa.allocated_amount_bdt, " +
                "f.fine_type, b.title_en as book_title " +
                "FROM payment_allocations pa " +
                "JOIN fines f ON pa.fine_id = f.id " +
                "LEFT JOIN loans l ON f.loan_id = l.id " +
                "LEFT JOIN book_copies c ON l.copy_id = c.id " +
                "LEFT JOIN books b ON c.book_id = b.id " +
                "WHERE pa.payment_id = ?";
        List<PaymentAllocation> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, paymentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PaymentAllocation pa = new PaymentAllocation();
                    pa.setId(rs.getLong("id"));
                    pa.setPaymentId(rs.getLong("payment_id"));
                    pa.setFineId(rs.getLong("fine_id"));
                    pa.setAllocatedAmountBdt(rs.getBigDecimal("allocated_amount_bdt"));
                    pa.setFineType(rs.getString("fine_type"));
                    pa.setBookTitle(rs.getString("book_title"));
                    list.add(pa);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find allocations for payment: " + paymentId, e);
        }
        return list;
    }

    private Payment mapRow(ResultSet rs) throws SQLException {
        Payment p = new Payment();
        p.setId(rs.getLong("id"));
        p.setReceiptNumber(rs.getString("receipt_number"));
        p.setMemberId(rs.getLong("member_id"));
        p.setMemberName(rs.getString("member_name"));
        p.setMembershipNumber(rs.getString("membership_number"));
        p.setAmountBdt(rs.getBigDecimal("amount_bdt"));

        String methodStr = rs.getString("payment_method");
        if (methodStr != null) p.setPaymentMethod(PaymentMethod.valueOf(methodStr));

        p.setExternalReference(rs.getString("external_reference"));
        p.setRecordedBy(rs.getLong("recorded_by"));
        p.setRecordedByName(rs.getString("recorded_by_name"));

        Timestamp ts = rs.getTimestamp("paid_at");
        if (ts != null) p.setPaidAt(ts.toLocalDateTime());

        p.setReversed(rs.getBoolean("is_reversed"));
        p.setReversalReason(rs.getString("reversal_reason"));
        p.setNotes(rs.getString("notes"));
        return p;
    }
}
