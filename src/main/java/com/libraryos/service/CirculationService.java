package com.libraryos.service;

import com.libraryos.core.exception.ValidationException;
import com.libraryos.core.security.PermissionGuard;
import com.libraryos.core.security.SessionManager;
import com.libraryos.core.transaction.TransactionManager;
import com.libraryos.core.util.DateTimeUtil;
import com.libraryos.model.circulation.Loan;
import com.libraryos.model.circulation.LoanRenewal;
import com.libraryos.model.circulation.LoanStatus;
import com.libraryos.model.finance.Fine;
import com.libraryos.model.finance.FineStatus;
import com.libraryos.model.finance.FineType;
import com.libraryos.model.inventory.BookCopy;
import com.libraryos.model.inventory.ConditionStatus;
import com.libraryos.model.inventory.CopyStatus;
import com.libraryos.model.member.BorrowingPolicy;
import com.libraryos.model.member.Member;
import com.libraryos.model.member.MemberStatus;
import com.libraryos.model.reservation.Reservation;
import com.libraryos.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class CirculationService {

    private static final Logger log = LoggerFactory.getLogger(CirculationService.class);

    private final LoanRepository loanRepository;
    private final LoanRenewalRepository loanRenewalRepository;
    private final BookCopyRepository bookCopyRepository;
    private final MemberRepository memberRepository;
    private final BorrowingPolicyRepository borrowingPolicyRepository;
    private final FineRepository fineRepository;
    private final ReservationRepository reservationRepository;
    private final FineCalculationService fineCalculationService;
    private final AuditService auditService;

    public CirculationService(LoanRepository loanRepository,
                              LoanRenewalRepository loanRenewalRepository,
                              BookCopyRepository bookCopyRepository,
                              MemberRepository memberRepository,
                              BorrowingPolicyRepository borrowingPolicyRepository,
                              FineRepository fineRepository,
                              ReservationRepository reservationRepository,
                              FineCalculationService fineCalculationService,
                              AuditService auditService) {
        this.loanRepository = loanRepository;
        this.loanRenewalRepository = loanRenewalRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.memberRepository = memberRepository;
        this.borrowingPolicyRepository = borrowingPolicyRepository;
        this.fineRepository = fineRepository;
        this.reservationRepository = reservationRepository;
        this.fineCalculationService = fineCalculationService;
        this.auditService = auditService;
    }

    public static class ReturnResult {
        private final Loan loan;
        private final Fine generatedFine;
        private final FineCalculationService.OverdueResult overdueResult;
        private final boolean copyReservedForNextMember;

        public ReturnResult(Loan loan, Fine generatedFine, FineCalculationService.OverdueResult overdueResult, boolean copyReservedForNextMember) {
            this.loan = loan;
            this.generatedFine = generatedFine;
            this.overdueResult = overdueResult;
            this.copyReservedForNextMember = copyReservedForNextMember;
        }

        public Loan getLoan() {
            return loan;
        }

        public Fine getGeneratedFine() {
            return generatedFine;
        }

        public FineCalculationService.OverdueResult getOverdueResult() {
            return overdueResult;
        }

        public boolean isCopyReservedForNextMember() {
            return copyReservedForNextMember;
        }
    }

    /**
     * Executes atomic book issue workflow.
     */
    public Loan issueBook(Long copyId, Long memberId) {
        PermissionGuard.requirePermission("LOAN_ISSUE");

        if (copyId == null) {
            throw new ValidationException("Book copy ID or barcode is required");
        }
        if (memberId == null) {
            throw new ValidationException("Member is required for issuing a book");
        }

        Long currentUserId = SessionManager.getCurrentUserId() != null ?
                SessionManager.getCurrentUserId() : 1L;

        return TransactionManager.execute(conn -> {
            // 1. Member validation
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new ValidationException("Member not found with ID: " + memberId));

            if (member.getStatus() != MemberStatus.ACTIVE) {
                throw new ValidationException("Cannot issue book: Member account is " + member.getStatus());
            }

            if (member.getMembershipExpiryDate() != null && member.getMembershipExpiryDate().isBefore(LocalDate.now())) {
                throw new ValidationException("Cannot issue book: Membership expired on " +
                        DateTimeUtil.formatDate(member.getMembershipExpiryDate()));
            }

            BorrowingPolicy policy = borrowingPolicyRepository.findByMemberTypeId(member.getMemberTypeId())
                    .orElseThrow(() -> new ValidationException("Borrowing policy not configured for member type ID: " + member.getMemberTypeId()));

            // Active loans limit check
            int activeLoans = loanRepository.countActiveLoansByMemberId(memberId, conn);
            if (activeLoans >= policy.getMaxActiveLoans()) {
                throw new ValidationException("Member has reached maximum active loan limit of " + policy.getMaxActiveLoans() + " books");
            }

            // Unpaid fines limit check
            BigDecimal unpaidTotal = fineRepository.getUnpaidTotalByMemberId(memberId, conn);
            if (policy.getMaxUnpaidFineLimitBdt() != null && unpaidTotal.compareTo(policy.getMaxUnpaidFineLimitBdt()) > 0) {
                throw new ValidationException("Member has unpaid fines of ৳" + unpaidTotal +
                        ", exceeding maximum allowed threshold of ৳" + policy.getMaxUnpaidFineLimitBdt());
            }

            // 2. Lock Book Copy & Validate Availability
            BookCopy copy = bookCopyRepository.findByIdForUpdate(copyId, conn)
                    .orElseThrow(() -> new ValidationException("Book copy not found with ID: " + copyId));

            if (copy.getStatus() == CopyStatus.ISSUED) {
                throw new ValidationException("Copy [Barcode: " + copy.getBarcode() + "] is already ISSUED to another borrower");
            }
            if (copy.getStatus() == CopyStatus.LOST || copy.getStatus() == CopyStatus.WITHDRAWN ||
                    copy.getStatus() == CopyStatus.UNDER_MAINTENANCE) {
                throw new ValidationException("Copy [Barcode: " + copy.getBarcode() + "] cannot be issued with status: " + copy.getStatus());
            }

            // Check reservation state
            if (copy.getStatus() == CopyStatus.RESERVED) {
                Optional<Reservation> activeRes = reservationRepository.findActiveReservationForCopy(copyId, memberId, conn);
                if (activeRes.isEmpty()) {
                    throw new ValidationException("Copy [Barcode: " + copy.getBarcode() + "] is reserved for another member");
                }
                // Fulfill member's reservation
                reservationRepository.fulfillReservation(activeRes.get().getId(), conn);
            }

            // 3. Due Date Calculation
            LocalDate dueDate = LocalDate.now().plusDays(policy.getLoanDurationDays());
            dueDate = DateTimeUtil.adjustForWeeklyHoliday(dueDate);

            // 4. Create Loan
            Loan loan = new Loan();
            loan.setCopyId(copy.getId());
            loan.setCopyBarcode(copy.getBarcode());
            loan.setCopyAccessionNumber(copy.getAccessionNumber());
            loan.setBookTitle(copy.getBookTitle());
            loan.setMemberId(member.getId());
            loan.setMemberName(member.getDisplayName());
            loan.setMembershipNumber(member.getMembershipNumber());
            loan.setMemberPhone(member.getPhone());
            loan.setIssuedBy(currentUserId);
            loan.setIssuedAt(LocalDateTime.now());
            loan.setDueDate(dueDate);
            loan.setRenewalCount(0);
            loan.setPolicyFinePerDaySnapshot(policy.getFinePerDayBdt());
            loan.setStatus(LoanStatus.ACTIVE);

            loan = loanRepository.create(loan, conn);

            // 5. Update Copy Status
            bookCopyRepository.updateStatus(copyId, CopyStatus.ISSUED, currentUserId,
                    "Issued to member " + member.getMembershipNumber() + " (" + member.getDisplayName() + ")", conn);

            // 6. Security Audit Log
            auditService.log("LOAN_ISSUED", "LOANS", String.valueOf(loan.getId()),
                    "Issued copy " + copy.getBarcode() + " to member " + member.getMembershipNumber() + " due " + dueDate, null);

            log.info("Successfully issued copy {} to member {} with due date {}", copy.getBarcode(), member.getMembershipNumber(), dueDate);
            return loan;
        });
    }

    /**
     * Executes atomic book return workflow.
     */
    public ReturnResult returnBook(Long copyId, String returnConditionStr, String notes) {
        PermissionGuard.requirePermission("LOAN_RETURN");

        if (copyId == null) {
            throw new ValidationException("Book copy ID is required for return");
        }

        Long currentUserId = SessionManager.getCurrentUserId() != null ?
                SessionManager.getCurrentUserId() : 1L;

        return TransactionManager.execute(conn -> {
            // 1. Lock Copy and Active Loan
            BookCopy copy = bookCopyRepository.findByIdForUpdate(copyId, conn)
                    .orElseThrow(() -> new ValidationException("Book copy not found with ID: " + copyId));

            Loan loan = loanRepository.findActiveLoanByCopyIdForUpdate(copyId, conn)
                    .orElseThrow(() -> new ValidationException("No active loan found for copy: " + copy.getBarcode()));

            Member member = memberRepository.findById(loan.getMemberId())
                    .orElseThrow(() -> new ValidationException("Member not found with ID: " + loan.getMemberId()));

            BorrowingPolicy policy = borrowingPolicyRepository.findByMemberTypeId(member.getMemberTypeId())
                    .orElseThrow(() -> new ValidationException("Policy not found for member type ID: " + member.getMemberTypeId()));

            // 2. Deterministic Fine Calculation
            LocalDate returnDate = LocalDate.now();
            FineCalculationService.OverdueResult overdueResult = fineCalculationService.calculateOverdueFine(loan, policy, returnDate);

            Fine createdFine = null;
            if (overdueResult.isOverdue()) {
                Fine fine = new Fine();
                fine.setMemberId(member.getId());
                fine.setMemberName(member.getDisplayName());
                fine.setMembershipNumber(member.getMembershipNumber());
                fine.setLoanId(loan.getId());
                fine.setCopyBarcode(copy.getBarcode());
                fine.setBookTitle(copy.getBookTitle());
                fine.setFineType(FineType.OVERDUE);
                fine.setOriginalAmountBdt(overdueResult.getFineAmount());
                fine.setWaivedAmountBdt(BigDecimal.ZERO);
                fine.setPaidAmountBdt(BigDecimal.ZERO);
                fine.setStatus(FineStatus.UNPAID);
                fine.setCreatedBy(currentUserId);

                createdFine = fineRepository.create(fine, conn);
                log.info("Overdue fine of ৳{} registered for member {} on loan {}", overdueResult.getFineAmount(), member.getMembershipNumber(), loan.getId());
            }

            // 3. Condition Assessment & Next Status
            ConditionStatus returnCond = ConditionStatus.GOOD;
            if (returnConditionStr != null && !returnConditionStr.isBlank()) {
                try {
                    returnCond = ConditionStatus.valueOf(returnConditionStr.trim().toUpperCase());
                } catch (IllegalArgumentException e) {
                    returnCond = ConditionStatus.GOOD;
                }
            }

            boolean allocatedToReservation = false;

            if (returnCond == ConditionStatus.DAMAGED) {
                bookCopyRepository.updateCondition(copy.getId(), ConditionStatus.DAMAGED, conn);
                bookCopyRepository.updateStatus(copy.getId(), CopyStatus.UNDER_MAINTENANCE, currentUserId,
                        "Returned in DAMAGED condition by member " + member.getMembershipNumber(), conn);
            } else if ("LOST".equalsIgnoreCase(returnConditionStr)) {
                bookCopyRepository.updateCondition(copy.getId(), ConditionStatus.DAMAGED, conn);
                bookCopyRepository.updateStatus(copy.getId(), CopyStatus.LOST, currentUserId,
                        "Reported LOST upon return by member " + member.getMembershipNumber(), conn);
            } else {
                // Update condition to GOOD or MINOR_WEAR
                bookCopyRepository.updateCondition(copy.getId(), returnCond, conn);

                // Check FIFO reservation queue for this book
                Optional<Reservation> waitingRes = reservationRepository.findFirstWaitingReservation(copy.getBookId(), conn);
                if (waitingRes.isPresent()) {
                    LocalDateTime holdExpiry = LocalDateTime.now().plusDays(policy.getReservationHoldDays());
                    reservationRepository.allocateCopy(waitingRes.get().getId(), copy.getId(), holdExpiry, conn);
                    bookCopyRepository.updateStatus(copy.getId(), CopyStatus.RESERVED, currentUserId,
                            "Reserved for queued member " + waitingRes.get().getMembershipNumber(), conn);
                    allocatedToReservation = true;
                    log.info("Copy {} allocated to queued reservation {} for member {}", copy.getBarcode(), waitingRes.get().getId(), waitingRes.get().getMembershipNumber());
                } else {
                    bookCopyRepository.updateStatus(copy.getId(), CopyStatus.AVAILABLE, currentUserId,
                            "Returned by member " + member.getMembershipNumber(), conn);
                }
            }

            // 4. Update Loan Record
            loan.setStatus(LoanStatus.RETURNED);
            loan.setReturnedAt(LocalDateTime.now());
            loan.setReturnedTo(currentUserId);
            loan.setReturnCondition(returnCond.name());
            loan.setNotes(notes);
            loanRepository.update(loan, conn);

            // 5. Audit
            auditService.log("LOAN_RETURNED", "LOANS", String.valueOf(loan.getId()),
                    "Returned copy " + copy.getBarcode() + " from member " + member.getMembershipNumber() +
                            (createdFine != null ? " with fine ৳" + createdFine.getOriginalAmountBdt() : ""), null);

            log.info("Successfully returned copy {} for member {}", copy.getBarcode(), member.getMembershipNumber());
            return new ReturnResult(loan, createdFine, overdueResult, allocatedToReservation);
        });
    }

    /**
     * Executes atomic loan renewal.
     */
    public Loan renewLoan(Long loanId) {
        PermissionGuard.requirePermission("LOAN_RENEW");

        if (loanId == null) {
            throw new ValidationException("Loan ID is required for renewal");
        }

        Long currentUserId = SessionManager.getCurrentUserId() != null ?
                SessionManager.getCurrentUserId() : 1L;

        return TransactionManager.execute(conn -> {
            Loan loan = loanRepository.findByIdForUpdate(loanId, conn)
                    .orElseThrow(() -> new ValidationException("Loan not found with ID: " + loanId));

            if (loan.getStatus() != LoanStatus.ACTIVE) {
                throw new ValidationException("Only ACTIVE loans can be renewed. Current status: " + loan.getStatus());
            }

            if (loan.isOverdue()) {
                throw new ValidationException("Cannot renew an overdue loan. Please return the book and resolve overdue fees.");
            }

            Member member = memberRepository.findById(loan.getMemberId())
                    .orElseThrow(() -> new ValidationException("Member not found for loan: " + loan.getMemberId()));

            BorrowingPolicy policy = borrowingPolicyRepository.findByMemberTypeId(member.getMemberTypeId())
                    .orElseThrow(() -> new ValidationException("Borrowing policy not found for member type ID: " + member.getMemberTypeId()));

            if (loan.getRenewalCount() >= policy.getMaxRenewals()) {
                throw new ValidationException("Maximum renewal limit of " + policy.getMaxRenewals() + " reached for this loan");
            }

            BookCopy copy = bookCopyRepository.findById(loan.getCopyId(), conn)
                    .orElseThrow(() -> new ValidationException("Book copy not found with ID: " + loan.getCopyId()));

            // Check reservation queue
            Optional<Reservation> waitingRes = reservationRepository.findFirstWaitingReservation(copy.getBookId(), conn);
            if (waitingRes.isPresent()) {
                throw new ValidationException("Cannot renew: Book has waiting reservations in the hold queue");
            }

            // Calculate new due date
            LocalDate newDueDate = loan.getDueDate().plusDays(policy.getRenewalDurationDays());
            newDueDate = DateTimeUtil.adjustForWeeklyHoliday(newDueDate);

            // Record renewal audit record
            LoanRenewal renewal = new LoanRenewal();
            renewal.setLoanId(loan.getId());
            renewal.setPreviousDueDate(loan.getDueDate());
            renewal.setNewDueDate(newDueDate);
            renewal.setRenewedBy(currentUserId);
            renewal.setRenewedAt(LocalDateTime.now());
            loanRenewalRepository.insert(renewal, conn);

            // Update loan
            loan.setDueDate(newDueDate);
            loan.setRenewalCount(loan.getRenewalCount() + 1);
            loanRepository.update(loan, conn);

            // Security Audit
            auditService.log("LOAN_RENEWED", "LOANS", String.valueOf(loan.getId()),
                    "Renewed loan for copy " + loan.getCopyBarcode() + " until " + newDueDate + " (renewal #" + loan.getRenewalCount() + ")", null);

            log.info("Successfully renewed loan {} until {}", loan.getId(), newDueDate);
            return loan;
        });
    }

    /**
     * Preview overdue fine calculation for an active loan without returning it.
     */
    public FineCalculationService.OverdueResult calculatePreviewFine(Long loanId, LocalDate returnDate) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ValidationException("Loan not found with ID: " + loanId));
        Member member = memberRepository.findById(loan.getMemberId())
                .orElseThrow(() -> new ValidationException("Member not found with ID: " + loan.getMemberId()));
        BorrowingPolicy policy = borrowingPolicyRepository.findByMemberTypeId(member.getMemberTypeId())
                .orElseThrow(() -> new ValidationException("Policy not found for member type ID: " + member.getMemberTypeId()));

        return fineCalculationService.calculateOverdueFine(loan, policy, returnDate != null ? returnDate : LocalDate.now());
    }

    public List<Loan> searchLoans(String query, LoanStatus status, Boolean overdueOnly) {
        return loanRepository.searchLoans(query, status, overdueOnly);
    }

    public List<Loan> findActiveLoansByMember(Long memberId) {
        return loanRepository.findActiveLoansByMemberId(memberId);
    }

    public List<LoanRenewal> findRenewalsByLoan(Long loanId) {
        return loanRenewalRepository.findByLoanId(loanId);
    }

    public List<BorrowingPolicy> getAllBorrowingPolicies() {
        return borrowingPolicyRepository.findAll();
    }

    public void updateBorrowingPolicy(BorrowingPolicy policy) {
        PermissionGuard.requirePermission("SETTINGS_MANAGE");
        if (policy == null || policy.getMemberTypeId() <= 0) {
            throw new ValidationException("Valid borrowing policy is required");
        }
        if (policy.getMaxActiveLoans() <= 0) {
            throw new ValidationException("Max active loans must be at least 1");
        }
        if (policy.getLoanDurationDays() <= 0) {
            throw new ValidationException("Loan duration must be at least 1 day");
        }
        if (policy.getFinePerDayBdt() == null || policy.getFinePerDayBdt().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Fine per day must be non-negative");
        }

        borrowingPolicyRepository.update(policy);
        Long currentUserId = SessionManager.getCurrentUserId() != null ?
                SessionManager.getCurrentUserId() : 1L;
        auditService.log("POLICY_UPDATED", "BORROWING_POLICIES", String.valueOf(policy.getId()),
                "Updated policy for member type ID " + policy.getMemberTypeId(), null);
    }
}
