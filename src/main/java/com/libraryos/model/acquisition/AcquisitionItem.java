package com.libraryos.model.acquisition;

import java.math.BigDecimal;

public class AcquisitionItem {
    private Long id;
    private Long acquisitionId;
    private Long bookId;
    private String bookTitle;
    private String bookIsbn;
    private int quantity;
    private BigDecimal unitCostBdt = BigDecimal.ZERO;
    private int receivedQuantity;

    public AcquisitionItem() {
    }

    public AcquisitionItem(Long id, Long acquisitionId, Long bookId, int quantity, BigDecimal unitCostBdt) {
        this.id = id;
        this.acquisitionId = acquisitionId;
        this.bookId = bookId;
        this.quantity = quantity;
        this.unitCostBdt = unitCostBdt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAcquisitionId() {
        return acquisitionId;
    }

    public void setAcquisitionId(Long acquisitionId) {
        this.acquisitionId = acquisitionId;
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

    public String getBookIsbn() {
        return bookIsbn;
    }

    public void setBookIsbn(String bookIsbn) {
        this.bookIsbn = bookIsbn;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitCostBdt() {
        return unitCostBdt;
    }

    public void setUnitCostBdt(BigDecimal unitCostBdt) {
        this.unitCostBdt = unitCostBdt;
    }

    public int getReceivedQuantity() {
        return receivedQuantity;
    }

    public void setReceivedQuantity(int receivedQuantity) {
        this.receivedQuantity = receivedQuantity;
    }

    public BigDecimal getSubtotalBdt() {
        if (unitCostBdt == null) return BigDecimal.ZERO;
        return unitCostBdt.multiply(BigDecimal.valueOf(quantity));
    }
}

