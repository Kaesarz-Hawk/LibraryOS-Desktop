package com.libraryos.service;

import com.libraryos.core.exception.UnauthorizedException;
import com.libraryos.core.exception.ValidationException;
import com.libraryos.core.security.SessionManager;
import com.libraryos.core.security.UserSession;
import com.libraryos.model.member.Member;
import com.libraryos.model.member.MemberStatus;
import com.libraryos.repository.BorrowingPolicyRepository;
import com.libraryos.repository.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private BorrowingPolicyRepository borrowingPolicyRepository;
    @Mock
    private AuditService auditService;

    private MemberService memberService;

    @BeforeEach
    void setUp() {
        memberService = new MemberService(memberRepository, borrowingPolicyRepository, auditService);
        UserSession adminSession = new UserSession(1L, "admin", "Admin", null, null,
                Collections.singleton("SUPER_ADMIN"),
                Set.of("MEMBER_CREATE", "MEMBER_UPDATE", "MEMBER_VIEW", "MEMBER_SUSPEND"));
        SessionManager.setSession(adminSession);
    }

    @AfterEach
    void tearDown() {
        SessionManager.clearSession();
    }

    @Test
    @DisplayName("Should register member with valid Bangladesh phone number")
    void testRegisterMemberSuccess() {
        Member member = new Member();
        member.setMembershipNumber("LIB-2026-0005");
        member.setMemberTypeId(1);
        member.setFullNameEn("Rashid Al Hasan");
        member.setPhone("+8801711223344"); // Needs normalization
        member.setMembershipStartDate(LocalDate.now());
        member.setMembershipExpiryDate(LocalDate.now().plusYears(1));

        when(memberRepository.findByMembershipNumber("LIB-2026-0005")).thenReturn(Optional.empty());
        when(memberRepository.findByPhone("01711223344")).thenReturn(Optional.empty());

        Member saved = new Member();
        saved.setId(100L);
        saved.setMembershipNumber("LIB-2026-0005");
        saved.setPhone("01711223344");
        saved.setFullNameEn("Rashid Al Hasan");
        when(memberRepository.create(any(Member.class))).thenReturn(saved);

        Member result = memberService.registerMember(member);

        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals("01711223344", member.getPhone(), "Phone should be normalized to standard 11 digits");
        verify(auditService).log(eq("MEMBER_REGISTERED"), eq("MEMBER"), eq("100"), anyString(), anyString());
    }

    @Test
    @DisplayName("Should reject member with invalid Bangladesh phone format")
    void testInvalidBdPhone() {
        Member member = new Member();
        member.setMembershipNumber("LIB-2026-0006");
        member.setMemberTypeId(1);
        member.setFullNameEn("Invalid Phone User");
        member.setPhone("01234567890"); // 012 is invalid operator prefix in BD

        ValidationException ex = assertThrows(ValidationException.class, () -> memberService.registerMember(member));
        assertTrue(ex.getMessage().contains("Invalid Bangladesh mobile number"));
    }

    @Test
    @DisplayName("Should reject duplicate membership number")
    void testDuplicateMembershipNumber() {
        Member member = new Member();
        member.setMembershipNumber("LIB-2026-0001");
        member.setMemberTypeId(1);
        member.setFullNameEn("Duplicate Member");
        member.setPhone("01799887766");

        when(memberRepository.findByMembershipNumber("LIB-2026-0001")).thenReturn(Optional.of(new Member()));

        ValidationException ex = assertThrows(ValidationException.class, () -> memberService.registerMember(member));
        assertTrue(ex.getMessage().contains("already registered"));
    }

    @Test
    @DisplayName("Should change member status to SUSPENDED with reason")
    void testSuspendMember() {
        memberService.setMemberStatus(10L, MemberStatus.SUSPENDED, "Long overdue books and unpaid fines");

        verify(memberRepository).updateStatus(10L, MemberStatus.SUSPENDED);
        verify(auditService).log(eq("MEMBER_STATUS_CHANGED"), eq("MEMBER"), eq("10"), anyString(), eq("Long overdue books and unpaid fines"));
    }
}
