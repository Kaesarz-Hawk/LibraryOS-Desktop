package com.libraryos.repository;

import com.libraryos.model.member.BorrowingPolicy;
import com.libraryos.model.member.MemberType;

import java.util.List;
import java.util.Optional;

public interface BorrowingPolicyRepository {
    List<BorrowingPolicy> findAll();
    Optional<BorrowingPolicy> findByMemberTypeId(int memberTypeId);
    void update(BorrowingPolicy policy);
    List<MemberType> findAllMemberTypes();
}

