package com.libraryos.service;

import com.libraryos.core.exception.ValidationException;
import com.libraryos.core.security.PermissionGuard;
import com.libraryos.core.security.SessionManager;
import com.libraryos.core.validation.ValidationUtil;
import com.libraryos.model.member.*;
import com.libraryos.model.user.PermissionCode;
import com.libraryos.repository.BorrowingPolicyRepository;
import com.libraryos.repository.MemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MemberService {

    private static final Logger log = LoggerFactory.getLogger(MemberService.class);

    private final MemberRepository memberRepository;
    private final BorrowingPolicyRepository borrowingPolicyRepository;
    private final AuditService auditService;

    public MemberService(MemberRepository memberRepository,
                         BorrowingPolicyRepository borrowingPolicyRepository,
                         AuditService auditService) {
        this.memberRepository = memberRepository;
        this.borrowingPolicyRepository = borrowingPolicyRepository;
        this.auditService = auditService;
    }

    public List<Member> getAllMembers() {
        PermissionGuard.requirePermission(PermissionCode.MEMBER_VIEW);
        return memberRepository.findAll();
    }

    public List<Member> searchMembers(String query, Integer memberTypeId, MemberStatus status) {
        PermissionGuard.requirePermission(PermissionCode.MEMBER_VIEW);
        return memberRepository.search(query, memberTypeId, status);
    }

    public List<Member> searchMembers(String query) {
        return searchMembers(query, null, null);
    }

    public Optional<Member> getMemberById(Long id) {
        PermissionGuard.requirePermission(PermissionCode.MEMBER_VIEW);
        PermissionGuard.requireMemberIsolation(id);
        return memberRepository.findById(id);
    }

    public Optional<Member> getMemberByNumber(String membershipNumber) {
        PermissionGuard.requirePermission(PermissionCode.MEMBER_VIEW);
        return memberRepository.findByMembershipNumber(membershipNumber);
    }

    public Optional<Member> getMemberByPhone(String phone) {
        PermissionGuard.requirePermission(PermissionCode.MEMBER_VIEW);
        return memberRepository.findByPhone(phone);
    }

    public Optional<Member> getMemberByInstitutionalId(String institutionalId) {
        PermissionGuard.requirePermission(PermissionCode.MEMBER_VIEW);
        return memberRepository.findByInstitutionalId(institutionalId);
    }

    public Member registerMember(Member member) {
        PermissionGuard.requirePermission(PermissionCode.MEMBER_CREATE);
        validateMember(member);

        // Normalize BD Phone
        String normalizedPhone = ValidationUtil.normalizeBdPhone(member.getPhone());
        member.setPhone(normalizedPhone);

        // Check duplicate membership number
        memberRepository.findByMembershipNumber(member.getMembershipNumber()).ifPresent(existing -> {
            throw new ValidationException("Membership number '" + member.getMembershipNumber() + "' is already registered to: " + existing.getFullNameEn());
        });

        // Check duplicate phone
        memberRepository.findByPhone(member.getPhone()).ifPresent(existing -> {
            throw new ValidationException("Phone number '" + member.getPhone() + "' is already registered to: " + existing.getFullNameEn());
        });

        Member created = memberRepository.create(member);
        auditService.log("MEMBER_REGISTERED", "MEMBER", String.valueOf(created.getId()),
                "Registered member: " + created.getFullNameEn() + " (" + created.getMembershipNumber() + ")", "Member registration");
        log.info("Member registered: #{} - {}", created.getId(), created.getFullNameEn());
        return created;
    }

    public void updateMember(Member member) {
        if (member.getId() == null) {
            throw new ValidationException("Member ID is required for update.");
        }
        PermissionGuard.requirePermission(PermissionCode.MEMBER_UPDATE);
        PermissionGuard.requireMemberIsolation(member.getId());
        validateMember(member);

        member.setPhone(ValidationUtil.normalizeBdPhone(member.getPhone()));
        memberRepository.update(member);
        auditService.log("MEMBER_UPDATED", "MEMBER", String.valueOf(member.getId()),
                "Updated member profile: " + member.getFullNameEn(), "Member profile update");
        log.info("Member updated: #{} - {}", member.getId(), member.getFullNameEn());
    }

    public void setMemberStatus(Long memberId, MemberStatus status, String reason) {
        PermissionGuard.requirePermission(PermissionCode.MEMBER_SUSPEND);
        memberRepository.updateStatus(memberId, status);
        auditService.log("MEMBER_STATUS_CHANGED", "MEMBER", String.valueOf(memberId),
                "Changed status to " + status.name() + " (" + reason + ")", reason);
        log.info("Member #{} status changed to {}", memberId, status);
    }

    private void validateMember(Member member) {
        List<String> errors = new ArrayList<>();
        if (member.getMembershipNumber() == null || member.getMembershipNumber().isBlank()) {
            errors.add("Membership Number is mandatory.");
        }
        if (member.getFullNameEn() == null || member.getFullNameEn().isBlank()) {
            errors.add("English Full Name is mandatory.");
        }
        if (!ValidationUtil.isValidBdPhone(member.getPhone())) {
            errors.add("Invalid Bangladesh mobile number. Must match standard format (e.g., 01712345678 or +8801712345678).");
        }
        if (member.getEmail() != null && !member.getEmail().isBlank() && !ValidationUtil.isValidEmail(member.getEmail())) {
            errors.add("Invalid email address format.");
        }
        if (member.getMembershipStartDate() == null) {
            errors.add("Membership start date is required.");
        }
        if (member.getMembershipExpiryDate() == null) {
            errors.add("Membership expiry date is required.");
        } else if (member.getMembershipStartDate() != null && member.getMembershipExpiryDate().isBefore(member.getMembershipStartDate())) {
            errors.add("Membership expiry date cannot be earlier than start date.");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    // Policies & Member Types
    public List<MemberType> getAllMemberTypes() {
        return borrowingPolicyRepository.findAllMemberTypes();
    }

    public List<BorrowingPolicy> getAllPolicies() {
        PermissionGuard.requirePermission(PermissionCode.SETTINGS_MANAGE);
        return borrowingPolicyRepository.findAll();
    }

    public BorrowingPolicy getPolicyForMemberType(int memberTypeId) {
        return borrowingPolicyRepository.findByMemberTypeId(memberTypeId)
                .orElseThrow(() -> new ValidationException("No borrowing policy configured for member type: " + memberTypeId));
    }

    public void updatePolicy(BorrowingPolicy policy) {
        PermissionGuard.requirePermission(PermissionCode.SETTINGS_MANAGE);
        borrowingPolicyRepository.update(policy);
        auditService.log("POLICY_UPDATED", "BORROWING_POLICY", String.valueOf(policy.getId()),
                "Updated borrowing policy for member type: " + policy.getMemberTypeCode(), "Policy configuration");
        log.info("Borrowing policy updated for member type: {}", policy.getMemberTypeCode());
    }
}

