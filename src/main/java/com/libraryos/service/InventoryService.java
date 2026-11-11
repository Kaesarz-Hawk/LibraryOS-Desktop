package com.libraryos.service;

import com.libraryos.core.exception.ValidationException;
import com.libraryos.core.security.PermissionGuard;
import com.libraryos.core.security.SessionManager;
import com.libraryos.model.inventory.*;
import com.libraryos.model.user.PermissionCode;
import com.libraryos.repository.BookCopyRepository;
import com.libraryos.repository.ShelfRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final BookCopyRepository copyRepository;
    private final ShelfRepository shelfRepository;
    private final AuditService auditService;

    public InventoryService(BookCopyRepository copyRepository,
                            ShelfRepository shelfRepository,
                            AuditService auditService) {
        this.copyRepository = copyRepository;
        this.shelfRepository = shelfRepository;
        this.auditService = auditService;
    }

    public List<BookCopy> getCopiesForBook(Long bookId) {
        PermissionGuard.requirePermission(PermissionCode.BOOK_VIEW);
        return copyRepository.findByBookId(bookId);
    }

    public Optional<BookCopy> getCopyById(Long id) {
        PermissionGuard.requirePermission(PermissionCode.BOOK_VIEW);
        return copyRepository.findById(id);
    }

    public Optional<BookCopy> getCopyByBarcode(String barcode) {
        PermissionGuard.requirePermission(PermissionCode.BOOK_VIEW);
        return copyRepository.findByBarcode(barcode);
    }

    public Optional<BookCopy> getCopyByAccessionNumber(String accessionNumber) {
        PermissionGuard.requirePermission(PermissionCode.BOOK_VIEW);
        return copyRepository.findByAccessionNumber(accessionNumber);
    }

    public List<BookCopy> searchCopies(String query, CopyStatus status, Long shelfId) {
        PermissionGuard.requirePermission(PermissionCode.COPY_MANAGE);
        return copyRepository.search(query, status, shelfId);
    }

    public BookCopy addCopy(BookCopy copy) {
        PermissionGuard.requirePermission(PermissionCode.COPY_MANAGE);
        validateCopy(copy);

        // Check duplicate accession number
        copyRepository.findByAccessionNumber(copy.getAccessionNumber()).ifPresent(existing -> {
            throw new ValidationException("Accession number '" + copy.getAccessionNumber() + "' is already registered.");
        });

        // Check duplicate barcode
        copyRepository.findByBarcode(copy.getBarcode()).ifPresent(existing -> {
            throw new ValidationException("Barcode '" + copy.getBarcode() + "' is already registered to accession: " + existing.getAccessionNumber());
        });

        BookCopy created = copyRepository.create(copy);
        auditService.log("COPY_ADDED", "BOOK_COPY", String.valueOf(created.getId()),
                "Added physical copy Acc: " + created.getAccessionNumber() + ", Barcode: " + created.getBarcode(), "Physical accession");
        log.info("Book copy registered: #{} - Acc: {}", created.getId(), created.getAccessionNumber());
        return created;
    }

    public void updateCopy(BookCopy copy) {
        PermissionGuard.requirePermission(PermissionCode.COPY_MANAGE);
        if (copy.getId() == null) {
            throw new ValidationException("Copy ID must be specified for update.");
        }
        copyRepository.update(copy);
        auditService.log("COPY_UPDATED", "BOOK_COPY", String.valueOf(copy.getId()),
                "Updated physical copy Acc: " + copy.getAccessionNumber(), "Inventory adjustment");
    }

    public void changeCopyStatus(Long copyId, CopyStatus newStatus, String reason) {
        PermissionGuard.requirePermission(PermissionCode.COPY_MANAGE);
        if (copyId == null || newStatus == null) {
            throw new ValidationException("Copy ID and target status are required.");
        }
        Long actorId = SessionManager.getCurrentUserId();
        copyRepository.updateStatus(copyId, newStatus, actorId, reason);
        auditService.log("COPY_STATUS_CHANGED", "BOOK_COPY", String.valueOf(copyId),
                "Changed status to " + newStatus.name() + " (" + reason + ")", reason);
        log.info("Copy #{} status transitioned to {}", copyId, newStatus);
    }

    public List<CopyStatusHistory> getCopyStatusHistory(Long copyId) {
        PermissionGuard.requirePermission(PermissionCode.COPY_MANAGE);
        return copyRepository.getStatusHistory(copyId);
    }

    private void validateCopy(BookCopy copy) {
        List<String> errors = new ArrayList<>();
        if (copy.getBookId() == null) {
            errors.add("Book bibliographic reference is required.");
        }
        if (copy.getAccessionNumber() == null || copy.getAccessionNumber().isBlank()) {
            errors.add("Accession Number is required.");
        }
        if (copy.getBarcode() == null || copy.getBarcode().isBlank()) {
            errors.add("Barcode identifier is required.");
        }
        if (copy.getPurchaseCost() != null && copy.getPurchaseCost().compareTo(java.math.BigDecimal.ZERO) < 0) {
            errors.add("Purchase cost cannot be negative.");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    // Shelves
    public List<Shelf> getAllShelves() {
        return shelfRepository.findAll();
    }

    public Shelf createShelf(Shelf shelf) {
        PermissionGuard.requirePermission(PermissionCode.COPY_MANAGE);
        if (shelf.getCode() == null || shelf.getCode().isBlank()) {
            throw new ValidationException("Shelf code is required.");
        }
        Shelf created = shelfRepository.create(shelf);
        auditService.log("SHELF_CREATED", "SHELF", String.valueOf(created.getId()),
                "Created shelf location: " + created.getCode(), "Inventory physical layout");
        return created;
    }
}

