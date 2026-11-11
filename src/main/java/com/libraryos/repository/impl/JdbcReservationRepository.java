package com.libraryos.repository.impl;

import com.libraryos.config.DatabaseConfig;
import com.libraryos.core.exception.DatabaseException;
import com.libraryos.model.reservation.Reservation;
import com.libraryos.model.reservation.ReservationStatus;
import com.libraryos.repository.ReservationRepository;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcReservationRepository implements ReservationRepository {

    private static final String BASE_SELECT =
            "SELECT r.id, r.book_id, r.member_id, r.allocated_copy_id, r.status, " +
            "r.requested_at, r.ready_at, r.expires_at, r.closed_at, r.notes, " +
            "b.title_en as book_title, " +
            "COALESCE(m.full_name_en, m.full_name_bn) as member_name, m.membership_number, " +
            "c.barcode as allocated_barcode " +
            "FROM reservations r " +
            "JOIN books b ON r.book_id = b.id " +
            "JOIN members m ON r.member_id = m.id " +
            "LEFT JOIN book_copies c ON r.allocated_copy_id = c.id ";

    @Override
    public Reservation create(Reservation reservation) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            return create(reservation, conn);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to create reservation", e);
        }
    }

    @Override
    public Reservation create(Reservation reservation, Connection conn) {
        String sql = "INSERT INTO reservations (book_id, member_id, status, requested_at, notes) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, reservation.getBookId());
            ps.setLong(2, reservation.getMemberId());
            ps.setString(3, reservation.getStatus() != null ? reservation.getStatus().name() : ReservationStatus.WAITING.name());
            ps.setTimestamp(4, Timestamp.valueOf(reservation.getRequestedAt() != null ? reservation.getRequestedAt() : LocalDateTime.now()));
            ps.setString(5, reservation.getNotes());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    reservation.setId(rs.getLong(1));
                }
            }
            return reservation;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert reservation", e);
        }
    }

    @Override
    public void update(Reservation reservation, Connection conn) {
        String sql = "UPDATE reservations SET allocated_copy_id = ?, status = ?, ready_at = ?, " +
                "expires_at = ?, closed_at = ?, notes = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (reservation.getAllocatedCopyId() != null) {
                ps.setLong(1, reservation.getAllocatedCopyId());
            } else {
                ps.setNull(1, Types.BIGINT);
            }
            ps.setString(2, reservation.getStatus().name());
            if (reservation.getReadyAt() != null) {
                ps.setTimestamp(3, Timestamp.valueOf(reservation.getReadyAt()));
            } else {
                ps.setNull(3, Types.TIMESTAMP);
            }
            if (reservation.getExpiresAt() != null) {
                ps.setTimestamp(4, Timestamp.valueOf(reservation.getExpiresAt()));
            } else {
                ps.setNull(4, Types.TIMESTAMP);
            }
            if (reservation.getClosedAt() != null) {
                ps.setTimestamp(5, Timestamp.valueOf(reservation.getClosedAt()));
            } else {
                ps.setNull(5, Types.TIMESTAMP);
            }
            ps.setString(6, reservation.getNotes());
            ps.setLong(7, reservation.getId());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update reservation id: " + reservation.getId(), e);
        }
    }

    @Override
    public Optional<Reservation> findById(Long id) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            return findById(id, conn);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find reservation by id: " + id, e);
        }
    }

    @Override
    public Optional<Reservation> findById(Long id, Connection conn) {
        String sql = BASE_SELECT + "WHERE r.id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find reservation by id: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Reservation> findByIdForUpdate(Long id, Connection conn) {
        String sql = BASE_SELECT + "WHERE r.id = ? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to lock reservation by id: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Reservation> findFirstWaitingReservation(Long bookId, Connection conn) {
        String sql = BASE_SELECT + "WHERE r.book_id = ? AND r.status = 'WAITING' ORDER BY r.requested_at ASC LIMIT 1 FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find waiting reservation for book: " + bookId, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Reservation> findActiveReservationForCopy(Long copyId, Long memberId, Connection conn) {
        String sql = BASE_SELECT + "WHERE r.allocated_copy_id = ? AND r.member_id = ? AND r.status = 'READY_FOR_PICKUP' FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, copyId);
            ps.setLong(2, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find active pickup reservation for copy: " + copyId, e);
        }
        return Optional.empty();
    }

    @Override
    public void allocateCopy(Long reservationId, Long copyId, LocalDateTime expiresAt, Connection conn) {
        String sql = "UPDATE reservations SET allocated_copy_id = ?, status = 'READY_FOR_PICKUP', " +
                "ready_at = CURRENT_TIMESTAMP, expires_at = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, copyId);
            ps.setTimestamp(2, Timestamp.valueOf(expiresAt));
            ps.setLong(3, reservationId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to allocate copy for reservation: " + reservationId, e);
        }
    }

    @Override
    public void fulfillReservation(Long reservationId, Connection conn) {
        String sql = "UPDATE reservations SET status = 'FULFILLED', closed_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, reservationId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fulfill reservation: " + reservationId, e);
        }
    }

    @Override
    public void cancelReservation(Long reservationId, Connection conn) {
        String sql = "UPDATE reservations SET status = 'CANCELLED', closed_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, reservationId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to cancel reservation: " + reservationId, e);
        }
    }

    @Override
    public List<Reservation> findByBookId(Long bookId) {
        String sql = BASE_SELECT + "WHERE r.book_id = ? ORDER BY r.requested_at ASC";
        List<Reservation> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find reservations for book: " + bookId, e);
        }
        return list;
    }

    @Override
    public boolean hasActiveReservation(Long bookId, Long memberId) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            return hasActiveReservation(bookId, memberId, conn);
        } catch (SQLException e) {
            throw new DatabaseException("Failed to check active reservation", e);
        }
    }

    @Override
    public boolean hasActiveReservation(Long bookId, Long memberId, Connection conn) {
        String sql = "SELECT COUNT(*) FROM reservations WHERE book_id = ? AND member_id = ? AND status IN ('WAITING', 'READY_FOR_PICKUP')";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bookId);
            ps.setLong(2, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to check active reservation", e);
        }
        return false;
    }

    @Override
    public List<Reservation> findExpiredHoldReservations(LocalDateTime cutoff, Connection conn) {
        String sql = BASE_SELECT + "WHERE r.status = 'READY_FOR_PICKUP' AND r.expires_at < ? FOR UPDATE";
        List<Reservation> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(cutoff));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find expired hold reservations", e);
        }
        return list;
    }

    @Override
    public int countActiveReservationsByMemberId(Long memberId) {
        String sql = "SELECT COUNT(*) FROM reservations WHERE member_id = ? AND status IN ('WAITING', 'READY_FOR_PICKUP')";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to count active reservations for member: " + memberId, e);
        }
        return 0;
    }

    @Override
    public List<Reservation> findByMemberId(Long memberId) {
        String sql = BASE_SELECT + "WHERE r.member_id = ? ORDER BY r.requested_at DESC";
        List<Reservation> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find reservations for member: " + memberId, e);
        }
        return list;
    }

    @Override
    public List<Reservation> search(String query, ReservationStatus status) {
        StringBuilder sql = new StringBuilder(BASE_SELECT).append("WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (status != null) {
            sql.append("AND r.status = ? ");
            params.add(status.name());
        }

        if (query != null && !query.isBlank()) {
            String q = "%" + query.trim() + "%";
            sql.append("AND (b.title_en LIKE ? OR b.title_bn LIKE ? OR m.full_name_en LIKE ? OR m.full_name_bn LIKE ? OR m.membership_number LIKE ?) ");
            for (int i = 0; i < 5; i++) {
                params.add(q);
            }
        }

        sql.append("ORDER BY r.requested_at DESC LIMIT 300");

        List<Reservation> list = new ArrayList<>();
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
            throw new DatabaseException("Failed to search reservations", e);
        }
        return list;
    }

    private Reservation mapRow(ResultSet rs) throws SQLException {
        Reservation r = new Reservation();
        r.setId(rs.getLong("id"));
        r.setBookId(rs.getLong("book_id"));
        r.setBookTitle(rs.getString("book_title"));
        r.setMemberId(rs.getLong("member_id"));
        r.setMemberName(rs.getString("member_name"));
        r.setMembershipNumber(rs.getString("membership_number"));

        long copyIdVal = rs.getLong("allocated_copy_id");
        if (!rs.wasNull()) r.setAllocatedCopyId(copyIdVal);
        r.setAllocatedBarcode(rs.getString("allocated_barcode"));

        String statusStr = rs.getString("status");
        if (statusStr != null) r.setStatus(ReservationStatus.valueOf(statusStr));

        Timestamp reqTs = rs.getTimestamp("requested_at");
        if (reqTs != null) r.setRequestedAt(reqTs.toLocalDateTime());

        Timestamp readyTs = rs.getTimestamp("ready_at");
        if (readyTs != null) r.setReadyAt(readyTs.toLocalDateTime());

        Timestamp expTs = rs.getTimestamp("expires_at");
        if (expTs != null) r.setExpiresAt(expTs.toLocalDateTime());

        Timestamp closedTs = rs.getTimestamp("closed_at");
        if (closedTs != null) r.setClosedAt(closedTs.toLocalDateTime());

        r.setNotes(rs.getString("notes"));
        return r;
    }
}
