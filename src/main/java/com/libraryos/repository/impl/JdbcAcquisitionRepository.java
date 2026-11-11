package com.libraryos.repository.impl;

import com.libraryos.config.DatabaseConfig;
import com.libraryos.core.exception.DatabaseException;
import com.libraryos.model.acquisition.Acquisition;
import com.libraryos.model.acquisition.AcquisitionItem;
import com.libraryos.model.acquisition.AcquisitionStatus;
import com.libraryos.repository.AcquisitionRepository;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcAcquisitionRepository implements AcquisitionRepository {

    private static final String BASE_SELECT =
            "SELECT a.id, a.acquisition_number, a.supplier_id, s.name AS supplier_name, a.invoice_number, " +
            "a.order_date, a.received_date, a.total_cost_bdt, a.status, a.notes, a.created_by, u.username AS creator_username, " +
            "a.created_at, a.updated_at " +
            "FROM acquisitions a " +
            "JOIN suppliers s ON a.supplier_id = s.id " +
            "JOIN users u ON a.created_by = u.id ";

    @Override
    public Optional<Acquisition> findById(Long id) {
        String sql = BASE_SELECT + "WHERE a.id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Acquisition acq = mapRow(rs);
                    acq.setItems(findItemsByAcquisitionId(id));
                    return Optional.of(acq);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find acquisition by id: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Acquisition> findByAcquisitionNumber(String acqNumber) {
        String sql = BASE_SELECT + "WHERE a.acquisition_number = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, acqNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Acquisition acq = mapRow(rs);
                    acq.setItems(findItemsByAcquisitionId(acq.getId()));
                    return Optional.of(acq);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find acquisition by number: " + acqNumber, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Acquisition> findAll() {
        String sql = BASE_SELECT + "ORDER BY a.created_at DESC";
        List<Acquisition> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Acquisition acq = mapRow(rs);
                list.add(acq);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch all acquisitions", e);
        }
        for (Acquisition acq : list) {
            acq.setItems(findItemsByAcquisitionId(acq.getId()));
        }
        return list;
    }

    @Override
    public List<Acquisition> findByStatus(AcquisitionStatus status) {
        String sql = BASE_SELECT + "WHERE a.status = ? ORDER BY a.created_at DESC";
        List<Acquisition> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch acquisitions by status: " + status, e);
        }
        for (Acquisition acq : list) {
            acq.setItems(findItemsByAcquisitionId(acq.getId()));
        }
        return list;
    }

    @Override
    public List<AcquisitionItem> findItemsByAcquisitionId(Long acquisitionId) {
        String sql = "SELECT i.id, i.acquisition_id, i.book_id, b.title_en AS book_title, " +
                "COALESCE(b.isbn_13, b.isbn_10) AS book_isbn, i.quantity, i.unit_cost_bdt, i.received_quantity " +
                "FROM acquisition_items i " +
                "JOIN books b ON i.book_id = b.id " +
                "WHERE i.acquisition_id = ? ORDER BY i.id ASC";
        List<AcquisitionItem> items = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, acquisitionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AcquisitionItem item = new AcquisitionItem();
                    item.setId(rs.getLong("id"));
                    item.setAcquisitionId(rs.getLong("acquisition_id"));
                    item.setBookId(rs.getLong("book_id"));
                    item.setBookTitle(rs.getString("book_title"));
                    item.setBookIsbn(rs.getString("book_isbn"));
                    item.setQuantity(rs.getInt("quantity"));
                    item.setUnitCostBdt(rs.getBigDecimal("unit_cost_bdt"));
                    item.setReceivedQuantity(rs.getInt("received_quantity"));
                    items.add(item);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch items for acquisition id: " + acquisitionId, e);
        }
        return items;
    }

    @Override
    public Acquisition save(Acquisition acquisition) {
        String sql = "INSERT INTO acquisitions (acquisition_number, supplier_id, invoice_number, order_date, " +
                "received_date, total_cost_bdt, status, notes, created_by, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, acquisition.getAcquisitionNumber());
            ps.setLong(2, acquisition.getSupplierId());
            ps.setString(3, acquisition.getInvoiceNumber());
            ps.setDate(4, Date.valueOf(acquisition.getOrderDate()));
            if (acquisition.getReceivedDate() != null) {
                ps.setDate(5, Date.valueOf(acquisition.getReceivedDate()));
            } else {
                ps.setNull(5, Types.DATE);
            }
            ps.setBigDecimal(6, acquisition.getTotalCostBdt());
            ps.setString(7, acquisition.getStatus().name());
            ps.setString(8, acquisition.getNotes());
            ps.setLong(9, acquisition.getCreatedBy());
            ps.setTimestamp(10, Timestamp.valueOf(acquisition.getCreatedAt() != null ? acquisition.getCreatedAt() : java.time.LocalDateTime.now()));

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    acquisition.setId(keys.getLong(1));
                }
            }

            if (acquisition.getItems() != null && !acquisition.getItems().isEmpty()) {
                for (AcquisitionItem item : acquisition.getItems()) {
                    item.setAcquisitionId(acquisition.getId());
                    saveItem(item);
                }
            }
            return acquisition;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save acquisition: " + acquisition.getAcquisitionNumber(), e);
        }
    }

    @Override
    public void saveItem(AcquisitionItem item) {
        String sql = "INSERT INTO acquisition_items (acquisition_id, book_id, quantity, unit_cost_bdt, received_quantity) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, item.getAcquisitionId());
            ps.setLong(2, item.getBookId());
            ps.setInt(3, item.getQuantity());
            ps.setBigDecimal(4, item.getUnitCostBdt());
            ps.setInt(5, item.getReceivedQuantity());

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    item.setId(keys.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save acquisition item for book id: " + item.getBookId(), e);
        }
    }

    @Override
    public boolean updateStatus(Long id, AcquisitionStatus status, LocalDate receivedDate) {
        String sql = "UPDATE acquisitions SET status = ?, received_date = ?, updated_at = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            if (receivedDate != null) {
                ps.setDate(2, Date.valueOf(receivedDate));
            } else {
                ps.setNull(2, Types.DATE);
            }
            ps.setTimestamp(3, Timestamp.valueOf(java.time.LocalDateTime.now()));
            ps.setLong(4, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update status for acquisition id: " + id, e);
        }
    }

    @Override
    public boolean update(Acquisition acquisition) {
        String sql = "UPDATE acquisitions SET supplier_id = ?, invoice_number = ?, order_date = ?, " +
                "total_cost_bdt = ?, notes = ?, updated_at = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, acquisition.getSupplierId());
            ps.setString(2, acquisition.getInvoiceNumber());
            ps.setDate(3, Date.valueOf(acquisition.getOrderDate()));
            ps.setBigDecimal(4, acquisition.getTotalCostBdt());
            ps.setString(5, acquisition.getNotes());
            ps.setTimestamp(6, Timestamp.valueOf(java.time.LocalDateTime.now()));
            ps.setLong(7, acquisition.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update acquisition id: " + acquisition.getId(), e);
        }
    }

    private Acquisition mapRow(ResultSet rs) throws SQLException {
        Acquisition a = new Acquisition();
        a.setId(rs.getLong("id"));
        a.setAcquisitionNumber(rs.getString("acquisition_number"));
        a.setSupplierId(rs.getLong("supplier_id"));
        a.setSupplierName(rs.getString("supplier_name"));
        a.setInvoiceNumber(rs.getString("invoice_number"));

        Date oDate = rs.getDate("order_date");
        if (oDate != null) a.setOrderDate(oDate.toLocalDate());

        Date rDate = rs.getDate("received_date");
        if (rDate != null) a.setReceivedDate(rDate.toLocalDate());

        a.setTotalCostBdt(rs.getBigDecimal("total_cost_bdt"));
        a.setStatus(AcquisitionStatus.valueOf(rs.getString("status")));
        a.setNotes(rs.getString("notes"));
        a.setCreatedBy(rs.getLong("created_by"));
        a.setCreatorUsername(rs.getString("creator_username"));

        Timestamp cat = rs.getTimestamp("created_at");
        if (cat != null) a.setCreatedAt(cat.toLocalDateTime());

        Timestamp uat = rs.getTimestamp("updated_at");
        if (uat != null) a.setUpdatedAt(uat.toLocalDateTime());

        return a;
    }
}

