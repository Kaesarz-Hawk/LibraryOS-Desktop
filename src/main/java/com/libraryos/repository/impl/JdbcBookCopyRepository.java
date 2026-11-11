package com.libraryos.repository.impl;

import com.libraryos.config.DatabaseConfig;
import com.libraryos.core.exception.DatabaseException;
import com.libraryos.model.inventory.*;
import com.libraryos.repository.BookCopyRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcBookCopyRepository implements BookCopyRepository {

    @Override
    public List<BookCopy> findByBookId(Long bookId) {
        String sql = "SELECT c.id, c.book_id, b.title_en as book_title, c.accession_number, c.barcode, " +
                "c.shelf_id, s.code as shelf_code, c.acquisition_id, c.acquisition_source, c.purchase_cost, " +
                "c.acquired_date, c.condition_status, c.status, c.notes, c.created_at, c.updated_at " +
                "FROM book_copies c " +
                "LEFT JOIN books b ON c.book_id = b.id " +
                "LEFT JOIN shelves s ON c.shelf_id = s.id " +
                "WHERE c.book_id = ? AND c.status != 'WITHDRAWN' " +
                "ORDER BY c.accession_number ASC";
        List<BookCopy> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch copies for book id: " + bookId, e);
        }
        return list;
    }

    @Override
    public Optional<BookCopy> findById(Long id) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            return findById(id, conn);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find copy by id: " + id, e);
        }
    }

    @Override
    public Optional<BookCopy> findById(Long id, Connection conn) {
        String sql = "SELECT c.id, c.book_id, b.title_en as book_title, c.accession_number, c.barcode, " +
                "c.shelf_id, s.code as shelf_code, c.acquisition_id, c.acquisition_source, c.purchase_cost, " +
                "c.acquired_date, c.condition_status, c.status, c.notes, c.created_at, c.updated_at " +
                "FROM book_copies c " +
                "LEFT JOIN books b ON c.book_id = b.id " +
                "LEFT JOIN shelves s ON c.shelf_id = s.id " +
                "WHERE c.id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find copy by id: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<BookCopy> findByIdForUpdate(Long id, Connection conn) {
        String sql = "SELECT c.id, c.book_id, b.title_en as book_title, c.accession_number, c.barcode, " +
                "c.shelf_id, s.code as shelf_code, c.acquisition_id, c.acquisition_source, c.purchase_cost, " +
                "c.acquired_date, c.condition_status, c.status, c.notes, c.created_at, c.updated_at " +
                "FROM book_copies c " +
                "LEFT JOIN books b ON c.book_id = b.id " +
                "LEFT JOIN shelves s ON c.shelf_id = s.id " +
                "WHERE c.id = ? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to lock copy by id: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<BookCopy> findByBarcode(String barcode) {
        if (barcode == null || barcode.isBlank()) return Optional.empty();
        try (Connection conn = DatabaseConfig.getConnection()) {
            return findByBarcode(barcode, conn);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find copy by barcode: " + barcode, e);
        }
    }

    @Override
    public Optional<BookCopy> findByBarcode(String barcode, Connection conn) {
        if (barcode == null || barcode.isBlank()) return Optional.empty();
        String sql = "SELECT c.id, c.book_id, b.title_en as book_title, c.accession_number, c.barcode, " +
                "c.shelf_id, s.code as shelf_code, c.acquisition_id, c.acquisition_source, c.purchase_cost, " +
                "c.acquired_date, c.condition_status, c.status, c.notes, c.created_at, c.updated_at " +
                "FROM book_copies c " +
                "LEFT JOIN books b ON c.book_id = b.id " +
                "LEFT JOIN shelves s ON c.shelf_id = s.id " +
                "WHERE c.barcode = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, barcode.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find copy by barcode: " + barcode, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<BookCopy> findByBarcodeForUpdate(String barcode, Connection conn) {
        if (barcode == null || barcode.isBlank()) return Optional.empty();
        String sql = "SELECT c.id, c.book_id, b.title_en as book_title, c.accession_number, c.barcode, " +
                "c.shelf_id, s.code as shelf_code, c.acquisition_id, c.acquisition_source, c.purchase_cost, " +
                "c.acquired_date, c.condition_status, c.status, c.notes, c.created_at, c.updated_at " +
                "FROM book_copies c " +
                "LEFT JOIN books b ON c.book_id = b.id " +
                "LEFT JOIN shelves s ON c.shelf_id = s.id " +
                "WHERE c.barcode = ? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, barcode.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to lock copy by barcode: " + barcode, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<BookCopy> findByAccessionNumber(String accessionNumber) {
        if (accessionNumber == null || accessionNumber.isBlank()) return Optional.empty();
        String sql = "SELECT c.id, c.book_id, b.title_en as book_title, c.accession_number, c.barcode, " +
                "c.shelf_id, s.code as shelf_code, c.acquisition_id, c.acquisition_source, c.purchase_cost, " +
                "c.acquired_date, c.condition_status, c.status, c.notes, c.created_at, c.updated_at " +
                "FROM book_copies c " +
                "LEFT JOIN books b ON c.book_id = b.id " +
                "LEFT JOIN shelves s ON c.shelf_id = s.id " +
                "WHERE c.accession_number = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, accessionNumber.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find copy by accession number: " + accessionNumber, e);
        }
        return Optional.empty();
    }

    @Override
    public BookCopy create(BookCopy copy) {
        String sql = "INSERT INTO book_copies (book_id, accession_number, barcode, shelf_id, acquisition_id, " +
                "acquisition_source, purchase_cost, acquired_date, condition_status, status, notes) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, copy.getBookId());
            ps.setString(2, copy.getAccessionNumber());
            ps.setString(3, copy.getBarcode());
            if (copy.getShelfId() != null) {
                ps.setLong(4, copy.getShelfId());
            } else {
                ps.setNull(4, Types.BIGINT);
            }
            if (copy.getAcquisitionId() != null) {
                ps.setLong(5, copy.getAcquisitionId());
            } else {
                ps.setNull(5, Types.BIGINT);
            }
            ps.setString(6, copy.getAcquisitionSource() != null ? copy.getAcquisitionSource().name() : AcquisitionSource.PURCHASE.name());
            ps.setBigDecimal(7, copy.getPurchaseCost());
            if (copy.getAcquiredDate() != null) {
                ps.setDate(8, Date.valueOf(copy.getAcquiredDate()));
            } else {
                ps.setDate(8, Date.valueOf(java.time.LocalDate.now()));
            }
            ps.setString(9, copy.getConditionStatus() != null ? copy.getConditionStatus().name() : ConditionStatus.GOOD.name());
            ps.setString(10, copy.getStatus() != null ? copy.getStatus().name() : CopyStatus.AVAILABLE.name());
            ps.setString(11, copy.getNotes());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    copy.setId(rs.getLong(1));
                }
            }

            // Record initial history
            insertHistory(conn, copy.getId(), null, copy.getStatus().name(), null, "Initial acquisition");
            return copy;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert book copy: " + copy.getAccessionNumber(), e);
        }
    }

    @Override
    public void update(BookCopy copy) {
        String sql = "UPDATE book_copies SET shelf_id = ?, acquisition_source = ?, purchase_cost = ?, " +
                "condition_status = ?, notes = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (copy.getShelfId() != null) {
                ps.setLong(1, copy.getShelfId());
            } else {
                ps.setNull(1, Types.BIGINT);
            }
            ps.setString(2, copy.getAcquisitionSource().name());
            ps.setBigDecimal(3, copy.getPurchaseCost());
            ps.setString(4, copy.getConditionStatus().name());
            ps.setString(5, copy.getNotes());
            ps.setLong(6, copy.getId());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update copy: " + copy.getId(), e);
        }
    }

    @Override
    public void updateStatus(Long copyId, CopyStatus newStatus, Long actorUserId, String reason) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            updateStatus(copyId, newStatus, actorUserId, reason, conn);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update status for copy id: " + copyId, e);
        }
    }

    @Override
    public void updateStatus(Long copyId, CopyStatus newStatus, Long actorUserId, String reason, Connection conn) {
        String queryOldSql = "SELECT status FROM book_copies WHERE id = ?";
        String updateSql = "UPDATE book_copies SET status = ? WHERE id = ?";

        try {
            String oldStatus = null;
            try (PreparedStatement psQ = conn.prepareStatement(queryOldSql)) {
                psQ.setLong(1, copyId);
                try (ResultSet rs = psQ.executeQuery()) {
                    if (rs.next()) {
                        oldStatus = rs.getString("status");
                    }
                }
            }

            try (PreparedStatement psU = conn.prepareStatement(updateSql)) {
                psU.setString(1, newStatus.name());
                psU.setLong(2, copyId);
                psU.executeUpdate();
            }

            insertHistory(conn, copyId, oldStatus, newStatus.name(), actorUserId, reason);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update status for copy id: " + copyId, e);
        }
    }

    @Override
    public void updateCondition(Long copyId, ConditionStatus condition, Connection conn) {
        String sql = "UPDATE book_copies SET condition_status = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, condition.name());
            ps.setLong(2, copyId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update condition for copy id: " + copyId, e);
        }
    }

    @Override
    public List<CopyStatusHistory> getStatusHistory(Long copyId) {
        String sql = "SELECT csh.id, csh.copy_id, csh.old_status, csh.new_status, csh.changed_by, " +
                "u.username as changed_by_name, csh.reason, csh.changed_at " +
                "FROM copy_status_history csh " +
                "LEFT JOIN users u ON csh.changed_by = u.id " +
                "WHERE csh.copy_id = ? ORDER BY csh.changed_at DESC";
        List<CopyStatusHistory> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, copyId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CopyStatusHistory h = new CopyStatusHistory();
                    h.setId(rs.getLong("id"));
                    h.setCopyId(rs.getLong("copy_id"));
                    h.setOldStatus(rs.getString("old_status"));
                    h.setNewStatus(rs.getString("new_status"));
                    long userId = rs.getLong("changed_by");
                    if (!rs.wasNull()) h.setChangedBy(userId);
                    h.setChangedByName(rs.getString("changed_by_name"));
                    h.setReason(rs.getString("reason"));
                    Timestamp ts = rs.getTimestamp("changed_at");
                    if (ts != null) h.setChangedAt(ts.toLocalDateTime());
                    list.add(h);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch copy status history", e);
        }
        return list;
    }

    @Override
    public List<BookCopy> search(String query, CopyStatus status, Long shelfId) {
        StringBuilder sql = new StringBuilder("SELECT c.id, c.book_id, b.title_en as book_title, c.accession_number, " +
                "c.barcode, c.shelf_id, s.code as shelf_code, c.acquisition_id, c.acquisition_source, c.purchase_cost, " +
                "c.acquired_date, c.condition_status, c.status, c.notes, c.created_at, c.updated_at " +
                "FROM book_copies c " +
                "LEFT JOIN books b ON c.book_id = b.id " +
                "LEFT JOIN shelves s ON c.shelf_id = s.id " +
                "WHERE 1=1 ");

        List<Object> params = new ArrayList<>();

        if (query != null && !query.isBlank()) {
            String q = "%" + query.trim().toLowerCase() + "%";
            sql.append("AND (LOWER(c.accession_number) LIKE ? OR LOWER(c.barcode) LIKE ? OR LOWER(b.title_en) LIKE ?) ");
            params.add(q);
            params.add(q);
            params.add(q);
        }

        if (status != null) {
            sql.append("AND c.status = ? ");
            params.add(status.name());
        }

        if (shelfId != null && shelfId > 0) {
            sql.append("AND c.shelf_id = ? ");
            params.add(shelfId);
        }

        sql.append("ORDER BY c.id DESC");

        List<BookCopy> list = new ArrayList<>();
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
            throw new DatabaseException("Failed to search book copies", e);
        }
        return list;
    }

    private void insertHistory(Connection conn, Long copyId, String oldStatus, String newStatus, Long changedBy, String reason) throws SQLException {
        String historySql = "INSERT INTO copy_status_history (copy_id, old_status, new_status, changed_by, reason) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(historySql)) {
            ps.setLong(1, copyId);
            ps.setString(2, oldStatus);
            ps.setString(3, newStatus);
            if (changedBy != null) {
                ps.setLong(4, changedBy);
            } else {
                ps.setNull(4, Types.BIGINT);
            }
            ps.setString(5, reason);
            ps.executeUpdate();
        }
    }

    private BookCopy mapRow(ResultSet rs) throws SQLException {
        BookCopy c = new BookCopy();
        c.setId(rs.getLong("id"));
        c.setBookId(rs.getLong("book_id"));
        c.setBookTitle(rs.getString("book_title"));
        c.setAccessionNumber(rs.getString("accession_number"));
        c.setBarcode(rs.getString("barcode"));

        long shelfIdVal = rs.getLong("shelf_id");
        if (!rs.wasNull()) {
            c.setShelfId(shelfIdVal);
        }
        c.setShelfCode(rs.getString("shelf_code"));

        long acqId = rs.getLong("acquisition_id");
        if (!rs.wasNull()) {
            c.setAcquisitionId(acqId);
        }

        String acqSrc = rs.getString("acquisition_source");
        if (acqSrc != null) {
            c.setAcquisitionSource(AcquisitionSource.valueOf(acqSrc));
        }

        c.setPurchaseCost(rs.getBigDecimal("purchase_cost"));

        Date acqDate = rs.getDate("acquired_date");
        if (acqDate != null) {
            c.setAcquiredDate(acqDate.toLocalDate());
        }

        String condStr = rs.getString("condition_status");
        if (condStr != null) {
            c.setConditionStatus(ConditionStatus.valueOf(condStr));
        }

        String statusStr = rs.getString("status");
        if (statusStr != null) {
            c.setStatus(CopyStatus.valueOf(statusStr));
        }

        c.setNotes(rs.getString("notes"));

        Timestamp cTs = rs.getTimestamp("created_at");
        if (cTs != null) c.setCreatedAt(cTs.toLocalDateTime());

        Timestamp uTs = rs.getTimestamp("updated_at");
        if (uTs != null) c.setUpdatedAt(uTs.toLocalDateTime());

        return c;
    }
}

