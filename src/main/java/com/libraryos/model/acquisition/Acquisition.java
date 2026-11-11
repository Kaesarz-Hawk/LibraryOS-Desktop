package com.libraryos.model.acquisition;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Acquisition {
    private Long id;
    private String acquisitionNumber;
    private Long supplierId;
    private String supplierName;
    private String invoiceNumber;
    private LocalDate orderDate;
    private LocalDate receivedDate;
    private BigDecimal totalCostBdt = BigDecimal.ZERO;
    private AcquisitionStatus status = AcquisitionStatus.ORDERED;
    private String notes;
    private Long createdBy;
    private String creatorUsername;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<AcquisitionItem> items = new ArrayList<>();

    public Acquisition() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAcquisitionNumber() {
        return acquisitionNumber;
    }

    public void setAcquisitionNumber(String acquisitionNumber) {
        this.acquisitionNumber = acquisitionNumber;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDate orderDate) {
        this.orderDate = orderDate;
    }

    public LocalDate getReceivedDate() {
        return receivedDate;
    }

    public void setReceivedDate(LocalDate receivedDate) {
        this.receivedDate = receivedDate;
    }

    public BigDecimal getTotalCostBdt() {
        return totalCostBdt;
    }

    public void setTotalCostBdt(BigDecimal totalCostBdt) {
        this.totalCostBdt = totalCostBdt;
    }

    public AcquisitionStatus getStatus() {
        return status;
    }

    public void setStatus(AcquisitionStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatorUsername() {
        return creatorUsername;
    }

    public void setCreatorUsername(String creatorUsername) {
        this.creatorUsername = creatorUsername;
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

    public List<AcquisitionItem> getItems() {
        return items;
    }

    public void setItems(List<AcquisitionItem> items) {
        this.items = items;
    }

    public int getTotalQuantity() {
        if (items == null) return 0;
        return items.stream().mapToInt(AcquisitionItem::getQuantity).sum();
    }
}

