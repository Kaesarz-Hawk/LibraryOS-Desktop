package com.libraryos.service;

import com.libraryos.core.exception.ValidationException;
import com.libraryos.core.security.PermissionGuard;
import com.libraryos.core.security.SessionManager;
import com.libraryos.core.transaction.TransactionManager;
import com.libraryos.model.catalog.Book;
import com.libraryos.model.inventory.BookCopy;
import com.libraryos.model.inventory.CopyStatus;
import com.libraryos.model.member.BorrowingPolicy;
import com.libraryos.model.member.Member;
import com.libraryos.model.member.MemberStatus;
import com.libraryos.model.reservation.Reservation;
import com.libraryos.model.reservation.ReservationStatus;
import com.libraryos.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);

    private final ReservationRepository reservationRepository;
    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;
    private final MemberRepository memberRepository;
    private final BorrowingPolicyRepository borrowingPolicyRepository;
    private final LoanRepository loanRepository;
    private final FineRepository fineRepository;
    private final AuditService auditService;

    public ReservationService(ReservationRepository reservationRepository,
                              BookRepository bookRepository,
                              BookCopyRepository bookCopyRepository,
                              MemberRepository memberRepository,
                              BorrowingPolicyRepository borrowingPolicyRepository,
                              LoanRepository loanRepository,
                              FineRepository fineRepository,
                              AuditService auditService) {
        this.reservationRepository = reservationRepository;
        this.bookRepository = bookRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.memberRepository = memberRepository;
        this.borrowingPolicyRepository = borrowingPolicyRepository;
        this.loanRepository = loanRepository;
        this.fineRepository = fineRepository;
        this.auditService = auditService;
    }

    /**
     * Enqueues a member into the FIFO reservation hold queue for a book title.
     */
    public Reservation placeReservation(Long bookId, Long memberId, String notes) {
        if (!SessionManager.hasPermission("RESERVATION_MANAGE") && !SessionManager.hasPermission("RESERVATION_SELF")) {
            PermissionGuard.requirePermission("RESERVATION_MANAGE");
        }
        PermissionGuard.requireMemberIsolation(memberId);

        if (bookId == null) {
            throw new ValidationException("Book ID is required to place a reservation");
        }
        if (memberId == null) {
            throw new ValidationException("Member ID is required to place a reservation");
        }

        Long currentUserId = SessionManager.getCurrentUserId() != null ? SessionManager.getCurrentUserId() : 1L;

        return TransactionManager.execute(conn -> {
            Book book = bookRepository.findById(bookId)
                    .orElseThrow(() -> new ValidationException("Book not found with ID: " + bookId));

            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new ValidationException("Member not found with ID: " + memberId));

            if (member.getStatus() != MemberStatus.ACTIVE) {
                throw new ValidationException("Member account is " + member.getStatus() + ". Cannot place reservation.");
            }

            BorrowingPolicy policy = borrowingPolicyRepository.findByMemberTypeId(member.getMemberTypeId())
                    .orElseThrow(() -> new ValidationException("Policy not found for member type ID: " + member.getMemberTypeId()));

            // Check unpaid fines threshold
            BigDecimal unpaidTotal = fineRepository.getUnpaidTotalByMemberId(memberId, conn);
            if (policy.getMaxUnpaidFineLimitBdt() != null && unpaidTotal.compareTo(policy.getMaxUnpaidFineLimitBdt()) > 0) {
                throw new ValidationException("Cannot place reservation: Unpaid fines of ৳" + unpaidTotal +
                        " exceed allowed limit of ৳" + policy.getMaxUnpaidFineLimitBdt());
            }

            // Check if member already has an active loan for this book
            if (loanRepository.hasActiveLoanForBook(bookId, memberId, conn)) {
                throw new ValidationException("Member already has an active borrowed copy of: " + book.getTitleEn());
            }

            // Check if member already has an active WAITING or READY reservation for this book
            if (reservationRepository.hasActiveReservation(bookId, memberId, conn)) {
                throw new ValidationException("Member already has a pending reservation for: " + book.getTitleEn());
            }

            Reservation reservation = new Reservation();
            reservation.setBookId(book.getId());
            reservation.setBookTitle(book.getTitleEn());
            reservation.setMemberId(member.getId());
            reservation.setMemberName(member.getDisplayName());
            reservation.setMembershipNumber(member.getMembershipNumber());
            reservation.setStatus(ReservationStatus.WAITING);
            reservation.setRequestedAt(LocalDateTime.now());
            reservation.setNotes(notes);

            reservation = reservationRepository.create(reservation, conn);

            auditService.log("RESERVATION_PLACED", "RESERVATIONS", String.valueOf(reservation.getId()),
                    "Placed hold for member " + member.getMembershipNumber() + " on book '" + book.getTitleEn() + "'", null);

            log.info("Hold reservation #{} placed for member {} on book #{}", reservation.getId(), member.getMembershipNumber(), book.getId());
            return reservation;
        });
    }

    /**
     * Cancels an active or waiting reservation and safely frees or re-routes any allocated physical copy.
     */
    public void cancelReservation(Long reservationId, String reason) {
        if (!SessionManager.hasPermission("RESERVATION_MANAGE") && !SessionManager.hasPermission("RESERVATION_SELF")) {
            PermissionGuard.requirePermission("RESERVATION_MANAGE");
        }

        if (reservationId == null) {
            throw new ValidationException("Reservation ID is required");
        }

        Long currentUserId = SessionManager.getCurrentUserId() != null ? SessionManager.getCurrentUserId() : 1L;

        TransactionManager.executeVoid(conn -> {
            Reservation res = reservationRepository.findByIdForUpdate(reservationId, conn)
                    .orElseThrow(() -> new ValidationException("Reservation not found with ID: " + reservationId));

            PermissionGuard.requireMemberIsolation(res.getMemberId());

            if (res.getStatus() == ReservationStatus.FULFILLED || res.getStatus() == ReservationStatus.CANCELLED ||
                    res.getStatus() == ReservationStatus.EXPIRED) {
                throw new ValidationException("Reservation is already " + res.getStatus() + ". Cannot cancel.");
            }

            // If a copy was already allocated to this reservation (READY_FOR_PICKUP), release or re-route it
            if (res.getAllocatedCopyId() != null) {
                Long copyId = res.getAllocatedCopyId();
                BookCopy copy = bookCopyRepository.findByIdForUpdate(copyId, conn)
                        .orElseThrow(() -> new ValidationException("Allocated copy not found with ID: " + copyId));

                // Check FIFO queue for the next waiting reservation
                Optional<Reservation> nextWaiting = reservationRepository.findFirstWaitingReservation(copy.getBookId(), conn);
                if (nextWaiting.isPresent()) {
                    Member nextMember = memberRepository.findById(nextWaiting.get().getMemberId())
                            .orElseThrow(() -> new ValidationException("Next queued member not found"));
                    BorrowingPolicy policy = borrowingPolicyRepository.findByMemberTypeId(nextMember.getMemberTypeId())
                            .orElseThrow(() -> new ValidationException("Policy not found for member type"));

                    LocalDateTime holdExpiry = LocalDateTime.now().plusDays(policy.getReservationHoldDays());
                    reservationRepository.allocateCopy(nextWaiting.get().getId(), copy.getId(), holdExpiry, conn);
                    bookCopyRepository.updateStatus(copy.getId(), CopyStatus.RESERVED, currentUserId,
                            "Re-allocated to next queued member " + nextWaiting.get().getMembershipNumber(), conn);
                    log.info("Copy {} re-allocated to next reservation #{}", copy.getBarcode(), nextWaiting.get().getId());
                } else {
                    bookCopyRepository.updateStatus(copy.getId(), CopyStatus.AVAILABLE, currentUserId,
                            "Released back to inventory due to reservation cancellation", conn);
                    log.info("Copy {} released back to AVAILABLE", copy.getBarcode());
                }
            }

            reservationRepository.cancelReservation(reservationId, conn);

            auditService.log("RESERVATION_CANCELLED", "RESERVATIONS", String.valueOf(reservationId),
                    "Cancelled reservation for member " + res.getMembershipNumber() + " (" + reason + ")", reason);

            log.info("Reservation #{} cancelled. Reason: {}", reservationId, reason);
        });
    }

    /**
     * Identifies hold reservations that exceeded their pickup expiration deadline, marks them EXPIRED,
     * and advances the allocated copy to the next waiting patron in line or returns it to AVAILABLE.
     */
    public int processExpiredHolds() {
        PermissionGuard.requirePermission("RESERVATION_MANAGE");

        Long currentUserId = SessionManager.getCurrentUserId() != null ? SessionManager.getCurrentUserId() : 1L;

        return TransactionManager.execute(conn -> {
            List<Reservation> expiredHolds = reservationRepository.findExpiredHoldReservations(LocalDateTime.now(), conn);
            int count = 0;

            for (Reservation res : expiredHolds) {
                // Update status to EXPIRED
                res.setStatus(ReservationStatus.EXPIRED);
                res.setClosedAt(LocalDateTime.now());
                res.setNotes("Hold window expired on " + res.getExpiresAt());
                reservationRepository.update(res, conn);

                if (res.getAllocatedCopyId() != null) {
                    Long copyId = res.getAllocatedCopyId();
                    BookCopy copy = bookCopyRepository.findByIdForUpdate(copyId, conn).orElse(null);
                    if (copy != null) {
                        Optional<Reservation> nextWaiting = reservationRepository.findFirstWaitingReservation(copy.getBookId(), conn);
                        if (nextWaiting.isPresent()) {
                            Member nextMember = memberRepository.findById(nextWaiting.get().getMemberId()).orElse(null);
                            int holdDays = 3;
                            if (nextMember != null) {
                                BorrowingPolicy policy = borrowingPolicyRepository.findByMemberTypeId(nextMember.getMemberTypeId()).orElse(null);
                                if (policy != null) holdDays = policy.getReservationHoldDays();
                            }
                            LocalDateTime newHoldExpiry = LocalDateTime.now().plusDays(holdDays);
                            reservationRepository.allocateCopy(nextWaiting.get().getId(), copy.getId(), newHoldExpiry, conn);
                            bookCopyRepository.updateStatus(copy.getId(), CopyStatus.RESERVED, currentUserId,
                                    "Expired hold auto-passed to next queued member: " + nextWaiting.get().getMembershipNumber(), conn);
                        } else {
                            bookCopyRepository.updateStatus(copy.getId(), CopyStatus.AVAILABLE, currentUserId,
                                    "Hold expired without further waitlist; released to inventory", conn);
                        }
                    }
                }

                auditService.log("RESERVATION_EXPIRED", "RESERVATIONS", String.valueOf(res.getId()),
                        "Hold expired for member " + res.getMembershipNumber() + " on book '" + res.getBookTitle() + "'", null);
                count++;
            }

            log.info("Processed {} expired hold reservation(s).", count);
            return count;
        });
    }

    public List<Reservation> searchReservations(String query, ReservationStatus status) {
        if (!SessionManager.hasPermission("RESERVATION_MANAGE") && !SessionManager.hasPermission("RESERVATION_SELF")) {
            PermissionGuard.requirePermission("RESERVATION_MANAGE");
        }
        return reservationRepository.search(query, status);
    }

    public List<Reservation> getReservationsByMember(Long memberId) {
        if (!SessionManager.hasPermission("RESERVATION_MANAGE") && !SessionManager.hasPermission("RESERVATION_SELF")) {
            PermissionGuard.requirePermission("RESERVATION_MANAGE");
        }
        PermissionGuard.requireMemberIsolation(memberId);
        return reservationRepository.findByMemberId(memberId);
    }

    public List<Reservation> getReservationsByBook(Long bookId) {
        PermissionGuard.requirePermission("RESERVATION_MANAGE");
        return reservationRepository.findByBookId(bookId);
    }
}

