package com.libraryos.service;

import com.libraryos.core.exception.ValidationException;
import com.libraryos.core.security.PermissionGuard;
import com.libraryos.core.security.SessionManager;
import com.libraryos.core.transaction.TransactionManager;
import com.libraryos.model.finance.*;
import com.libraryos.model.member.Member;
import com.libraryos.repository.FineAdjustmentRepository;
import com.libraryos.repository.FineRepository;
import com.libraryos.repository.MemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class FineService {

    private static final Logger log = LoggerFactory.getLogger(FineService.class);

    private final FineRepository fineRepository;
    private final FineAdjustmentRepository fineAdjustmentRepository;
    private final MemberRepository memberRepository;
    private final AuditService auditService;

    public FineService(FineRepository fineRepository,
                       FineAdjustmentRepository fineAdjustmentRepository,
                       MemberRepository memberRepository,
                       AuditService auditService) {
        this.fineRepository = fineRepository;
        this.fineAdjustmentRepository = fineAdjustmentRepository;
        this.memberRepository = memberRepository;
        this.auditService = auditService;
    }

    /**
     * Manually registers a fine (e.g. for Damaged Book, Lost Book replacement fee, or processing fee).
     */
    public Fine createFine(Long memberId, Long loanId, FineType type, BigDecimal amount, String reason) {
        PermissionGuard.requirePermission("FINE_VIEW");

        if (memberId == null) {
            throw new ValidationException("Member is required to create a fine liability");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Fine amount must be greater than ৳ 0.00");
        }

        Long currentUserId = SessionManager.getCurrentUserId() != null ? SessionManager.getCurrentUserId() : 1L;

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ValidationException("Member not found with ID: " + memberId));

        Fine fine = new Fine();
        fine.setMemberId(member.getId());
        fine.setMemberName(member.getDisplayName());
        fine.setMembershipNumber(member.getMembershipNumber());
        fine.setLoanId(loanId);
        fine.setFineType(type != null ? type : FineType.OTHER);
        fine.setOriginalAmountBdt(amount);
        fine.setWaivedAmountBdt(BigDecimal.ZERO);
        fine.setPaidAmountBdt(BigDecimal.ZERO);
        fine.setStatus(FineStatus.UNPAID);
        fine.setCreatedBy(currentUserId);

        fine = fineRepository.create(fine);

        auditService.log("FINE_CREATED", "FINES", String.valueOf(fine.getId()),
                "Created fine of ৳" + amount + " (" + fine.getFineType() + ") for member " + member.getMembershipNumber() + ": " + reason, reason);

        log.info("Fine #{} created for member {} (৳{})", fine.getId(), member.getMembershipNumber(), amount);
        return fine;
    }

    /**
     * Authorizes and records a fine waiver or reduction with mandatory administrative rationale.
     */
    public FineAdjustment waiveFine(Long fineId, BigDecimal waiveAmount, String reason) {
        PermissionGuard.requirePermission("FINE_WAIVE");

        if (fineId == null) {
            throw new ValidationException("Fine ID is required");
        }
        if (waiveAmount == null || waiveAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Waiver amount must be greater than ৳ 0.00");
        }
        if (reason == null || reason.trim().length() < 5) {
            throw new ValidationException("Mandatory audit reason required (minimum 5 characters).");
        }

        Long currentUserId = SessionManager.getCurrentUserId() != null ? SessionManager.getCurrentUserId() : 1L;

        return TransactionManager.execute(conn -> {
            Fine fine = fineRepository.findByIdForUpdate(fineId, conn)
                    .orElseThrow(() -> new ValidationException("Fine not found with ID: " + fineId));

            BigDecimal remaining = fine.getRemainingAmountBdt();
            if (remaining.compareTo(BigDecimal.ZERO) <= 0 || fine.getStatus() == FineStatus.PAID || fine.getStatus() == FineStatus.WAIVED) {
                throw new ValidationException("Fine #" + fineId + " has no outstanding balance to waive. Status: " + fine.getStatus());
            }

            if (waiveAmount.compareTo(remaining) > 0) {
                throw new ValidationException("Waiver amount (৳" + waiveAmount + ") cannot exceed outstanding balance (৳" + remaining + ").");
            }

            // Create adjustment
            FineAdjustment adjustment = new FineAdjustment();
            adjustment.setFineId(fine.getId());
            adjustment.setAdjustmentType(AdjustmentType.WAIVER);
            adjustment.setAmountBdt(waiveAmount);
            adjustment.setApprovedBy(currentUserId);
            adjustment.setReason(reason.trim());
            adjustment = fineAdjustmentRepository.create(adjustment, conn);

            // Update fine balance
            BigDecimal newWaived = fine.getWaivedAmountBdt().add(waiveAmount);
            fine.setWaivedAmountBdt(newWaived);

            BigDecimal newRemaining = fine.getRemainingAmountBdt();
            if (newRemaining.compareTo(BigDecimal.ZERO) == 0) {
                fine.setStatus(fine.getPaidAmountBdt().compareTo(BigDecimal.ZERO) > 0 ? FineStatus.PAID : FineStatus.WAIVED);
            } else {
                fine.setStatus(FineStatus.PARTIALLY_PAID);
            }

            fineRepository.update(fine, conn);

            auditService.log("FINE_WAIVED", "FINES", String.valueOf(fine.getId()),
                    "Waived ৳" + waiveAmount + " on fine #" + fine.getId() + " for member " + fine.getMembershipNumber() + ". Rationale: " + reason, reason);

            log.info("Fine #{} waived by ৳{}. New balance: ৳{}", fine.getId(), waiveAmount, newRemaining);
            return adjustment;
        });
    }

    public List<Fine> searchFines(String query, FineStatus status) {
        PermissionGuard.requirePermission("FINE_VIEW");
        return fineRepository.searchFines(query, status);
    }

    public List<Fine> getFinesByMember(Long memberId) {
        PermissionGuard.requirePermission("FINE_VIEW");
        PermissionGuard.requireMemberIsolation(memberId);
        return fineRepository.findByMemberId(memberId);
    }

    public List<Fine> getUnpaidFinesByMember(Long memberId) {
        PermissionGuard.requirePermission("FINE_VIEW");
        PermissionGuard.requireMemberIsolation(memberId);
        return fineRepository.findUnpaidByMemberId(memberId);
    }

    public BigDecimal getUnpaidTotalByMember(Long memberId) {
        PermissionGuard.requirePermission("FINE_VIEW");
        PermissionGuard.requireMemberIsolation(memberId);
        return fineRepository.getUnpaidTotalByMemberId(memberId);
    }

    public BigDecimal getTotalUnpaidFines(Long memberId) {
        return getUnpaidTotalByMember(memberId);
    }

    public List<FineAdjustment> getFineAdjustments(Long fineId) {
        PermissionGuard.requirePermission("FINE_VIEW");
        return fineAdjustmentRepository.findByFineId(fineId);
    }

    public Optional<Fine> getFineById(Long id) {
        PermissionGuard.requirePermission("FINE_VIEW");
        return fineRepository.findById(id);
    }
}
