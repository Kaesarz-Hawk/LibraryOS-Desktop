package com.libraryos.repository;

import com.libraryos.model.digital.ReadingProgress;

import java.util.List;
import java.util.Optional;

public interface ReadingProgressRepository {
    Optional<ReadingProgress> findByMemberAndBook(Long memberId, Long digitalBookId);
    List<ReadingProgress> findByMemberId(Long memberId);
    void saveOrUpdate(ReadingProgress progress);
}

