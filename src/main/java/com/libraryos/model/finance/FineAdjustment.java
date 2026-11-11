package com.libraryos.model.finance;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class FineAdjustment {
    private Long id;
    private Long fineId;
    private AdjustmentType adjustmentType = AdjustmentType.WAIVER;
    private BigDecimal amountBdt = BigDecimal.ZERO;
    private Long approvedBy;
    private String approvedByName;
    private String reason;
    private LocalDateTime createdAt;

    public FineAdjustment() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFineId() {
        return fineId;
    }

    public void setFineId(Long fineId) {
        this.fineId = fineId;
    }

    public AdjustmentType getAdjustmentType() {
        return adjustmentType;
    }

    public void setAdjustmentType(AdjustmentType adjustmentType) {
        this.adjustmentType = adjustmentType != null ? adjustmentType : AdjustmentType.WAIVER;
    }

    public BigDecimal getAmountBdt() {
        return amountBdt;
    }

    public void setAmountBdt(BigDecimal amountBdt) {
        this.amountBdt = amountBdt != null ? amountBdt : BigDecimal.ZERO;
    }

    public Long getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(Long approvedBy) {
        this.approvedBy = approvedBy;
    }

    public String getApprovedByName() {
        return approvedByName;
    }

    public void setApprovedByName(String approvedByName) {
        this.approvedByName = approvedByName;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

