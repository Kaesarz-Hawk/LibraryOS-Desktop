package com.libraryos.service;

import com.libraryos.core.exception.ValidationException;
import com.libraryos.core.security.PermissionGuard;
import com.libraryos.core.security.SessionManager;
import com.libraryos.core.transaction.TransactionManager;
import com.libraryos.model.finance.*;
import com.libraryos.model.member.Member;
import com.libraryos.repository.FineRepository;
import com.libraryos.repository.MemberRepository;
import com.libraryos.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final DateTimeFormatter RECEIPT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final PaymentRepository paymentRepository;
    private final FineRepository fineRepository;
    private final MemberRepository memberRepository;
    private final AuditService auditService;

    public PaymentService(PaymentRepository paymentRepository,
                          FineRepository fineRepository,
                          MemberRepository memberRepository,
                          AuditService auditService) {
        this.paymentRepository = paymentRepository;
        this.fineRepository = fineRepository;
        this.memberRepository = memberRepository;
        this.auditService = auditService;
    }

    public Payment recordPayment(Long memberId, BigDecimal amount, PaymentMethod method,
                                 String externalReference, String notes) {
        return recordPayment(memberId, amount, method, externalReference, null, notes);
    }

    /**
     * Records an immutable BDT payment receipt and atomically allocates the funds against outstanding fines.
     */
    public Payment recordPayment(Long memberId, BigDecimal amount, PaymentMethod method,
                                 String externalReference, List<Long> targetFineIds, String notes) {
        PermissionGuard.requirePermission("PAYMENT_RECORD");

        if (memberId == null) {
            throw new ValidationException("Member is required to collect payment");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Payment amount must be greater than ৳ 0.00");
        }
        if (method == null) {
            method = PaymentMethod.CASH;
        }

        // Verify MFS external reference uniqueness if provided
        if (externalReference != null && !externalReference.isBlank()) {
            String trimmedRef = externalReference.trim();
            paymentRepository.findByExternalReference(trimmedRef).ifPresent(existing -> {
                throw new ValidationException("Transaction reference ID '" + trimmedRef +
                        "' was already posted on Receipt #" + existing.getReceiptNumber());
            });
        }

        Long currentUserId = SessionManager.getCurrentUserId() != null ? SessionManager.getCurrentUserId() : 1L;
        PaymentMethod finalMethod = method;

        return TransactionManager.execute(conn -> {
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new ValidationException("Member not found with ID: " + memberId));

            // Load unpaid fines for member
            List<Fine> unpaidFines;
            if (targetFineIds != null && !targetFineIds.isEmpty()) {
                unpaidFines = new ArrayList<>();
                for (Long fineId : targetFineIds) {
                    fineRepository.findByIdForUpdate(fineId, conn).ifPresent(f -> {
                        if (f.getMemberId().equals(memberId) && f.getRemainingAmountBdt().compareTo(BigDecimal.ZERO) > 0) {
                            unpaidFines.add(f);
                        }
                    });
                }
            } else {
                unpaidFines = fineRepository.findUnpaidByMemberId(memberId, conn);
            }

            if (unpaidFines.isEmpty()) {
                throw new ValidationException("Member " + member.getMembershipNumber() + " has no outstanding unpaid fines.");
            }

            BigDecimal totalDue = unpaidFines.stream()
                    .map(Fine::getRemainingAmountBdt)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (amount.compareTo(totalDue) > 0) {
                throw new ValidationException("Payment amount of ৳" + amount +
                        " exceeds total outstanding balance of ৳" + totalDue + ".");
            }

            // Generate unique receipt number: REC-20261104-XXXXXX
            String receiptNumber = "REC-" + LocalDate.now().format(RECEIPT_DATE_FORMAT) + "-" +
                    String.format("%06d", (System.currentTimeMillis() % 1000000));

            Payment payment = new Payment();
            payment.setReceiptNumber(receiptNumber);
            payment.setMemberId(member.getId());
            payment.setMemberName(member.getDisplayName());
            payment.setMembershipNumber(member.getMembershipNumber());
            payment.setAmountBdt(amount);
            payment.setPaymentMethod(finalMethod);
            payment.setExternalReference(externalReference != null && !externalReference.isBlank() ? externalReference.trim() : null);
            payment.setRecordedBy(currentUserId);
            payment.setPaidAt(LocalDateTime.now());
            payment.setReversed(false);
            payment.setNotes(notes);

            payment = paymentRepository.create(payment, conn);

            // Allocate funds FIFO across the fines
            BigDecimal remainingPayment = amount;
            List<PaymentAllocation> allocations = new ArrayList<>();

            for (Fine fine : unpaidFines) {
                BigDecimal fineDue = fine.getRemainingAmountBdt();
                if (fineDue.compareTo(BigDecimal.ZERO) <= 0) continue;

                BigDecimal toAllocate = remainingPayment.min(fineDue);

                PaymentAllocation allocation = new PaymentAllocation(payment.getId(), fine.getId(), toAllocate);
                allocation.setFineType(fine.getFineType().name());
                allocation.setBookTitle(fine.getBookTitle());
                paymentRepository.insertAllocation(allocation, conn);
                allocations.add(allocation);

                // Update Fine paid balance and status
                fine.setPaidAmountBdt(fine.getPaidAmountBdt().add(toAllocate));
                if (fine.getRemainingAmountBdt().compareTo(BigDecimal.ZERO) == 0) {
                    fine.setStatus(FineStatus.PAID);
                } else {
                    fine.setStatus(FineStatus.PARTIALLY_PAID);
                }
                fineRepository.update(fine, conn);

                remainingPayment = remainingPayment.subtract(toAllocate);
                if (remainingPayment.compareTo(BigDecimal.ZERO) == 0) {
                    break;
                }
            }

            payment.setAllocations(allocations);

            auditService.log("PAYMENT_COLLECTED", "PAYMENTS", String.valueOf(payment.getId()),
                    "Collected ৳" + amount + " via " + finalMethod.name() + " (Receipt: " + receiptNumber + ") for member " + member.getMembershipNumber(), notes);

            log.info("Payment #{} (Receipt {}) recorded: ৳{} for member {}", payment.getId(), receiptNumber, amount, member.getMembershipNumber());
            return payment;
        });
    }

    /**
     * Administratively reverses a payment receipt, marking it reversed and restoring the fine liabilities.
     */
    public void reversePayment(Long paymentId, String reason) {
        PermissionGuard.requirePermission("FINE_WAIVE"); // Admin permission required for financial reversal

        if (paymentId == null) {
            throw new ValidationException("Payment ID is required for reversal");
        }
        if (reason == null || reason.trim().length() < 5) {
            throw new ValidationException("Mandatory administrative audit reason required for payment reversal (min 5 characters).");
        }

        TransactionManager.executeVoid(conn -> {
            Payment payment = paymentRepository.findByIdForUpdate(paymentId, conn)
                    .orElseThrow(() -> new ValidationException("Payment not found with ID: " + paymentId));

            if (payment.isReversed()) {
                throw new ValidationException("Payment [Receipt: " + payment.getReceiptNumber() + "] is already marked REVERSED.");
            }

            List<PaymentAllocation> allocations = paymentRepository.findAllocationsByPaymentId(paymentId, conn);

            // Revert each fine's paid balance
            for (PaymentAllocation pa : allocations) {
                Fine fine = fineRepository.findByIdForUpdate(pa.getFineId(), conn).orElse(null);
                if (fine != null) {
                    BigDecimal revertedPaid = fine.getPaidAmountBdt().subtract(pa.getAllocatedAmountBdt());
                    fine.setPaidAmountBdt(revertedPaid.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : revertedPaid);

                    if (fine.getPaidAmountBdt().compareTo(BigDecimal.ZERO) == 0 &&
                            fine.getWaivedAmountBdt().compareTo(BigDecimal.ZERO) == 0) {
                        fine.setStatus(FineStatus.UNPAID);
                    } else if (fine.getRemainingAmountBdt().compareTo(BigDecimal.ZERO) > 0) {
                        fine.setStatus(FineStatus.PARTIALLY_PAID);
                    }
                    fineRepository.update(fine, conn);
                }
            }

            payment.setReversed(true);
            payment.setReversalReason(reason.trim());
            paymentRepository.update(payment, conn);

            auditService.log("PAYMENT_REVERSED", "PAYMENTS", String.valueOf(payment.getId()),
                    "Reversed receipt " + payment.getReceiptNumber() + " (৳" + payment.getAmountBdt() + "). Reason: " + reason, reason);

            log.info("Payment #{} reversed. Reason: {}", paymentId, reason);
        });
    }

    public List<Payment> searchPayments(String query, PaymentMethod method, Boolean reversedOnly) {
        PermissionGuard.requirePermission("FINE_VIEW");
        return paymentRepository.search(query, method, reversedOnly);
    }

    public List<Payment> getPaymentsByMember(Long memberId) {
        PermissionGuard.requirePermission("FINE_VIEW");
        PermissionGuard.requireMemberIsolation(memberId);
        return paymentRepository.findByMemberId(memberId);
    }

    public Optional<Payment> getPaymentById(Long id) {
        PermissionGuard.requirePermission("FINE_VIEW");
        return paymentRepository.findById(id);
    }

    public Optional<Payment> getPaymentByReceiptNumber(String receiptNumber) {
        PermissionGuard.requirePermission("FINE_VIEW");
        return paymentRepository.findByReceiptNumber(receiptNumber);
    }
}
