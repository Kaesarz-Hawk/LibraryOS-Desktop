package com.libraryos.service;

import com.libraryos.core.exception.DomainException;
import com.libraryos.core.exception.ValidationException;
import com.libraryos.core.security.PermissionGuard;
import com.libraryos.core.security.SessionManager;
import com.libraryos.core.security.UserSession;
import com.libraryos.model.acquisition.*;
import com.libraryos.model.inventory.*;
import com.libraryos.model.user.PermissionCode;
import com.libraryos.repository.AcquisitionRepository;
import com.libraryos.repository.BookCopyRepository;
import com.libraryos.repository.SupplierRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AcquisitionService {

    private static final Logger log = LoggerFactory.getLogger(AcquisitionService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SupplierRepository supplierRepository;
    private final AcquisitionRepository acquisitionRepository;
    private final BookCopyRepository bookCopyRepository;
    private final AuditService auditService;

    public AcquisitionService(SupplierRepository supplierRepository,
                              AcquisitionRepository acquisitionRepository,
                              BookCopyRepository bookCopyRepository,
                              AuditService auditService) {
        this.supplierRepository = supplierRepository;
        this.acquisitionRepository = acquisitionRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.auditService = auditService;
    }

    // ==========================================
    // Supplier Management
    // ==========================================

    public List<Supplier> getAllSuppliers() {
        PermissionGuard.requirePermission(PermissionCode.SUPPLIER_MANAGE);
        return supplierRepository.findAll();
    }

    public List<Supplier> searchSuppliers(String query) {
        PermissionGuard.requirePermission(PermissionCode.SUPPLIER_MANAGE);
        return supplierRepository.search(query);
    }

    public Optional<Supplier> getSupplierById(Long id) {
        PermissionGuard.requirePermission(PermissionCode.SUPPLIER_MANAGE);
        return supplierRepository.findById(id);
    }

    public Supplier createSupplier(Supplier supplier) {
        PermissionGuard.requirePermission(PermissionCode.SUPPLIER_MANAGE);
        validateSupplier(supplier);
        Supplier saved = supplierRepository.save(supplier);

        if (auditService != null) {
            auditService.log("SUPPLIER_CREATE", "Supplier", String.valueOf(saved.getId()),
                    "Registered new book supplier: " + saved.getName(), null);
        }
        return saved;
    }

    public boolean updateSupplier(Supplier supplier) {
        PermissionGuard.requirePermission(PermissionCode.SUPPLIER_MANAGE);
        validateSupplier(supplier);
        boolean updated = supplierRepository.update(supplier);
        if (updated && auditService != null) {
            auditService.log("SUPPLIER_UPDATE", "Supplier", String.valueOf(supplier.getId()),
                    "Updated supplier profile: " + supplier.getName(), null);
        }
        return updated;
    }

    public boolean deactivateSupplier(Long id) {
        PermissionGuard.requirePermission(PermissionCode.SUPPLIER_MANAGE);
        return supplierRepository.delete(id);
    }

    private void validateSupplier(Supplier supplier) {
        if (supplier == null) throw new ValidationException("Supplier data cannot be null");
        if (supplier.getName() == null || supplier.getName().trim().isBlank()) {
            throw new ValidationException("Supplier organization name is required");
        }
        if (supplier.getPhone() == null || supplier.getPhone().trim().isBlank()) {
            throw new ValidationException("Supplier contact phone number is required");
        }
    }

    // ==========================================
    // Acquisitions / Purchase Orders Management
    // ==========================================

    public List<Acquisition> getAllAcquisitions() {
        PermissionGuard.requirePermission(PermissionCode.SUPPLIER_MANAGE);
        return acquisitionRepository.findAll();
    }

    public List<Acquisition> getAcquisitionsByStatus(AcquisitionStatus status) {
        PermissionGuard.requirePermission(PermissionCode.SUPPLIER_MANAGE);
        return acquisitionRepository.findByStatus(status);
    }

    public Optional<Acquisition> getAcquisitionById(Long id) {
        PermissionGuard.requirePermission(PermissionCode.SUPPLIER_MANAGE);
        return acquisitionRepository.findById(id);
    }

    /**
     * Creates a new Acquisition Purchase Order with validated item lines.
     */
    public Acquisition createAcquisitionOrder(Long supplierId, String invoiceNumber, LocalDate orderDate,
                                              String notes, List<AcquisitionItem> items) {
        PermissionGuard.requirePermission(PermissionCode.SUPPLIER_MANAGE);

        if (supplierId == null) {
            throw new ValidationException("Supplier must be selected.");
        }
        if (orderDate == null) {
            orderDate = LocalDate.now();
        }
        if (items == null || items.isEmpty()) {
            throw new ValidationException("Acquisition order must contain at least one book item line.");
        }

        BigDecimal totalCost = BigDecimal.ZERO;
        for (AcquisitionItem item : items) {
            if (item.getBookId() == null) {
                throw new ValidationException("Book must be selected for every line item.");
            }
            if (item.getQuantity() <= 0) {
                throw new ValidationException("Item quantity must be greater than zero.");
            }
            if (item.getUnitCostBdt() == null || item.getUnitCostBdt().compareTo(BigDecimal.ZERO) < 0) {
                throw new ValidationException("Item unit cost cannot be negative.");
            }
            totalCost = totalCost.add(item.getSubtotalBdt());
        }

        UserSession session = SessionManager.getCurrentSession().orElse(null);
        Long actorId = session != null ? session.getUserId() : 1L;

        String acqNumber = generateAcquisitionNumber();

        Acquisition acq = new Acquisition();
        acq.setAcquisitionNumber(acqNumber);
        acq.setSupplierId(supplierId);
        acq.setInvoiceNumber(invoiceNumber != null ? invoiceNumber.trim() : null);
        acq.setOrderDate(orderDate);
        acq.setTotalCostBdt(totalCost);
        acq.setStatus(AcquisitionStatus.ORDERED);
        acq.setNotes(notes);
        acq.setCreatedBy(actorId);
        acq.setItems(items);

        Acquisition saved = acquisitionRepository.save(acq);

        if (auditService != null) {
            auditService.log("ACQUISITION_ORDER_CREATED", "Acquisition",
                    String.valueOf(saved.getId()),
                    "Created purchase order " + saved.getAcquisitionNumber() + " with " + items.size() +
                            " items, total BDT " + totalCost, null);
        }

        log.info("Acquisition order created: {} (Total: BDT {})", saved.getAcquisitionNumber(), totalCost);
        return saved;
    }

    /**
     * Receives an acquisition batch and automatically generates physical book copies in inventory.
     */
    public List<BookCopy> receiveAcquisitionBatch(Long acquisitionId, Long shelfId) {
        PermissionGuard.requirePermission(PermissionCode.SUPPLIER_MANAGE);

        Acquisition acq = acquisitionRepository.findById(acquisitionId)
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Acquisition order not found: " + acquisitionId));

        if (acq.getStatus() != AcquisitionStatus.ORDERED) {
            throw new DomainException("INVALID_STATE", "Acquisition order is already " + acq.getStatus());
        }

        List<AcquisitionItem> items = acq.getItems();
        if (items == null || items.isEmpty()) {
            items = acquisitionRepository.findItemsByAcquisitionId(acquisitionId);
        }

        List<BookCopy> generatedCopies = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (AcquisitionItem item : items) {
            int qty = item.getQuantity();
            for (int i = 0; i < qty; i++) {
                String accNum = generateUniqueAccessionNumber();
                String barcode = generateUniqueBarcode();

                BookCopy copy = new BookCopy();
                copy.setBookId(item.getBookId());
                copy.setAccessionNumber(accNum);
                copy.setBarcode(barcode);
                copy.setShelfId(shelfId);
                copy.setAcquisitionId(acq.getId());
                copy.setAcquisitionSource(AcquisitionSource.PURCHASE);
                copy.setPurchaseCost(item.getUnitCostBdt());
                copy.setAcquiredDate(today);
                copy.setConditionStatus(ConditionStatus.NEW);
                copy.setStatus(CopyStatus.AVAILABLE);
                copy.setNotes("Generated via Acquisition Order #" + acq.getAcquisitionNumber());

                BookCopy savedCopy = bookCopyRepository.create(copy);
                generatedCopies.add(savedCopy);
            }
        }

        // Update acquisition status to RECEIVED
        acquisitionRepository.updateStatus(acquisitionId, AcquisitionStatus.RECEIVED, today);

        if (auditService != null) {
            auditService.log("ACQUISITION_RECEIVED", "Acquisition",
                    String.valueOf(acq.getId()),
                    "Received order " + acq.getAcquisitionNumber() + ", generated " +
                            generatedCopies.size() + " physical copies in inventory", null);
        }

        log.info("Acquisition #{} successfully received. Generated {} inventory copies.",
                acq.getAcquisitionNumber(), generatedCopies.size());
        return generatedCopies;
    }

    public boolean cancelAcquisitionOrder(Long acquisitionId, String reason) {
        PermissionGuard.requirePermission(PermissionCode.SUPPLIER_MANAGE);

        Acquisition acq = acquisitionRepository.findById(acquisitionId)
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Acquisition order not found: " + acquisitionId));

        if (acq.getStatus() == AcquisitionStatus.RECEIVED) {
            throw new DomainException("ALREADY_RECEIVED", "Cannot cancel an order that has already been received into inventory.");
        }

        boolean updated = acquisitionRepository.updateStatus(acquisitionId, AcquisitionStatus.CANCELLED, null);
        if (updated && auditService != null) {
            auditService.log("ACQUISITION_CANCELLED", "Acquisition",
                    String.valueOf(acq.getId()), "Cancelled order " + acq.getAcquisitionNumber() + ". Reason: " + reason, null);
        }
        return updated;
    }

    private String generateAcquisitionNumber() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int rand = 1000 + RANDOM.nextInt(9000);
        return "PO-" + datePart + "-" + rand;
    }

    private String generateUniqueAccessionNumber() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        for (int attempt = 0; attempt < 20; attempt++) {
            int rand = 100000 + RANDOM.nextInt(900000);
            String candidate = "ACC-" + datePart + "-" + rand;
            if (bookCopyRepository.findByAccessionNumber(candidate).isEmpty()) {
                return candidate;
            }
        }
        return "ACC-" + System.currentTimeMillis();
    }

    private String generateUniqueBarcode() {
        for (int attempt = 0; attempt < 20; attempt++) {
            long rand = 10000000L + (long) (RANDOM.nextDouble() * 90000000L);
            String candidate = "BC" + rand;
            if (bookCopyRepository.findByBarcode(candidate).isEmpty()) {
                return candidate;
            }
        }
        return "BC" + System.currentTimeMillis();
    }
}
