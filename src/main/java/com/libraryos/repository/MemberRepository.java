package com.libraryos.repository;

import com.libraryos.model.member.Member;
import com.libraryos.model.member.MemberStatus;

import java.util.List;
import java.util.Optional;

public interface MemberRepository {
    List<Member> findAll();
    List<Member> search(String query, Integer memberTypeId, MemberStatus status);
    Optional<Member> findById(Long id);
    Optional<Member> findByMembershipNumber(String membershipNumber);
    Optional<Member> findByPhone(String phone);
    Optional<Member> findByInstitutionalId(String institutionalId);
    Member create(Member member);
    void update(Member member);
    void updateStatus(Long memberId, MemberStatus status);
}

