package com.libraryos.repository;

import com.libraryos.model.reservation.Reservation;
import com.libraryos.model.reservation.ReservationStatus;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository {
    Reservation create(Reservation reservation);
    Reservation create(Reservation reservation, Connection conn);
    void update(Reservation reservation, Connection conn);
    Optional<Reservation> findById(Long id);
    Optional<Reservation> findById(Long id, Connection conn);
    Optional<Reservation> findByIdForUpdate(Long id, Connection conn);
    Optional<Reservation> findFirstWaitingReservation(Long bookId, Connection conn);
    Optional<Reservation> findActiveReservationForCopy(Long copyId, Long memberId, Connection conn);
    void allocateCopy(Long reservationId, Long copyId, LocalDateTime expiresAt, Connection conn);
    void fulfillReservation(Long reservationId, Connection conn);
    void cancelReservation(Long reservationId, Connection conn);
    List<Reservation> findByMemberId(Long memberId);
    List<Reservation> findByBookId(Long bookId);
    boolean hasActiveReservation(Long bookId, Long memberId);
    boolean hasActiveReservation(Long bookId, Long memberId, Connection conn);
    List<Reservation> findExpiredHoldReservations(LocalDateTime cutoff, Connection conn);
    int countActiveReservationsByMemberId(Long memberId);
    List<Reservation> search(String query, ReservationStatus status);
}
