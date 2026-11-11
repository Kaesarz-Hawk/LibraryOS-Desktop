package com.libraryos.repository;

import com.libraryos.model.digital.Bookmark;

import java.util.List;

public interface BookmarkRepository {
    List<Bookmark> findByMemberAndBook(Long memberId, Long digitalBookId);
    Bookmark save(Bookmark bookmark);
    boolean delete(Long id, Long memberId);
}

