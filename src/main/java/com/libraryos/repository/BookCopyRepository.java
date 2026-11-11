package com.libraryos.repository;

import com.libraryos.model.inventory.BookCopy;
import com.libraryos.model.inventory.ConditionStatus;
import com.libraryos.model.inventory.CopyStatus;
import com.libraryos.model.inventory.CopyStatusHistory;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public interface BookCopyRepository {
    List<BookCopy> findByBookId(Long bookId);
    Optional<BookCopy> findById(Long id);
    Optional<BookCopy> findById(Long id, Connection conn);
    Optional<BookCopy> findByIdForUpdate(Long id, Connection conn);
    Optional<BookCopy> findByBarcode(String barcode);
    Optional<BookCopy> findByBarcode(String barcode, Connection conn);
    Optional<BookCopy> findByBarcodeForUpdate(String barcode, Connection conn);
    Optional<BookCopy> findByAccessionNumber(String accessionNumber);
    BookCopy create(BookCopy copy);
    void update(BookCopy copy);
    void updateStatus(Long copyId, CopyStatus newStatus, Long actorUserId, String reason);
    void updateStatus(Long copyId, CopyStatus newStatus, Long actorUserId, String reason, Connection conn);
    void updateCondition(Long copyId, ConditionStatus condition, Connection conn);
    List<CopyStatusHistory> getStatusHistory(Long copyId);
    List<BookCopy> search(String query, CopyStatus status, Long shelfId);
}
