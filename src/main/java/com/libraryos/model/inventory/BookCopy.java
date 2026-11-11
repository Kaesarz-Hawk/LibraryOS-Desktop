package com.libraryos.model.inventory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class BookCopy {
    private Long id;
    private Long bookId;
    private String bookTitle;
    private String accessionNumber;
    private String barcode;
    private Long shelfId;
    private String shelfCode;
    private Long acquisitionId;
    private AcquisitionSource acquisitionSource = AcquisitionSource.PURCHASE;
    private BigDecimal purchaseCost = BigDecimal.ZERO;
    private LocalDate acquiredDate = LocalDate.now();
    private ConditionStatus conditionStatus = ConditionStatus.GOOD;
    private CopyStatus status = CopyStatus.AVAILABLE;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public BookCopy() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public Long getShelfId() {
        return shelfId;
    }

    public void setShelfId(Long shelfId) {
        this.shelfId = shelfId;
    }

    public String getShelfCode() {
        return shelfCode;
    }

    public String getShelfLocation() {
        return shelfCode;
    }

    public void setShelfCode(String shelfCode) {
        this.shelfCode = shelfCode;
    }

    public void setShelfLocation(String shelfLocation) {
        this.shelfCode = shelfLocation;
    }

    public Long getAcquisitionId() {
        return acquisitionId;
    }

    public void setAcquisitionId(Long acquisitionId) {
        this.acquisitionId = acquisitionId;
    }

    public AcquisitionSource getAcquisitionSource() {
        return acquisitionSource;
    }

    public void setAcquisitionSource(AcquisitionSource acquisitionSource) {
        this.acquisitionSource = acquisitionSource != null ? acquisitionSource : AcquisitionSource.PURCHASE;
    }

    public BigDecimal getPurchaseCost() {
        return purchaseCost;
    }

    public void setPurchaseCost(BigDecimal purchaseCost) {
        this.purchaseCost = purchaseCost != null ? purchaseCost : BigDecimal.ZERO;
    }

    public LocalDate getAcquiredDate() {
        return acquiredDate;
    }

    public void setAcquiredDate(LocalDate acquiredDate) {
        this.acquiredDate = acquiredDate;
    }

    public ConditionStatus getConditionStatus() {
        return conditionStatus;
    }

    public void setConditionStatus(ConditionStatus conditionStatus) {
        this.conditionStatus = conditionStatus != null ? conditionStatus : ConditionStatus.GOOD;
    }

    public CopyStatus getStatus() {
        return status;
    }

    public void setStatus(CopyStatus status) {
        this.status = status != null ? status : CopyStatus.AVAILABLE;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isAvailable() {
        return status == CopyStatus.AVAILABLE;
    }

    @Override
    public String toString() {
        return accessionNumber + " [" + barcode + "]";
    }
}

