package com.libraryos.model.finance;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Payment {
    private Long id;
    private String receiptNumber;
    private Long memberId;
    private String memberName;
    private String membershipNumber;
    private BigDecimal amountBdt = BigDecimal.ZERO;
    private PaymentMethod paymentMethod = PaymentMethod.CASH;
    private String externalReference; // TrxID
    private Long recordedBy;
    private String recordedByName;
    private LocalDateTime paidAt;
    private boolean isReversed = false;
    private String reversalReason;
    private String notes;

    private List<PaymentAllocation> allocations = new ArrayList<>();

    public Payment() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReceiptNumber() {
        return receiptNumber;
    }

    public void setReceiptNumber(String receiptNumber) {
        this.receiptNumber = receiptNumber;
    }

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public String getMemberName() {
        return memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }

    public String getMembershipNumber() {
        return membershipNumber;
    }

    public void setMembershipNumber(String membershipNumber) {
        this.membershipNumber = membershipNumber;
    }

    public BigDecimal getAmountBdt() {
        return amountBdt;
    }

    public void setAmountBdt(BigDecimal amountBdt) {
        this.amountBdt = amountBdt != null ? amountBdt : BigDecimal.ZERO;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod != null ? paymentMethod : PaymentMethod.CASH;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public void setExternalReference(String externalReference) {
        this.externalReference = externalReference;
    }

    public Long getRecordedBy() {
        return recordedBy;
    }

    public void setRecordedBy(Long recordedBy) {
        this.recordedBy = recordedBy;
    }

    public String getRecordedByName() {
        return recordedByName;
    }

    public void setRecordedByName(String recordedByName) {
        this.recordedByName = recordedByName;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public boolean isReversed() {
        return isReversed;
    }

    public void setReversed(boolean reversed) {
        isReversed = reversed;
    }

    public String getReversalReason() {
        return reversalReason;
    }

    public void setReversalReason(String reversalReason) {
        this.reversalReason = reversalReason;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<PaymentAllocation> getAllocations() {
        return allocations;
    }

    public void setAllocations(List<PaymentAllocation> allocations) {
        this.allocations = allocations != null ? allocations : new ArrayList<>();
    }

    public BigDecimal getAmount() {
        return amountBdt;
    }

    public String getTransactionReference() {
        return externalReference;
    }

    public String getReceivedByUsername() {
        return recordedByName;
    }

    public Long getReceivedByUserId() {
        return recordedBy;
    }

    public LocalDateTime getCreatedAt() {
        return paidAt;
    }

    public LocalDateTime getReversedAt() {
        return paidAt;
    }

    public Long getReversedByUserId() {
        return recordedBy;
    }

    @Override
    public String toString() {
        return "Payment [" + receiptNumber + " - ৳" + amountBdt + " via " + paymentMethod + "]";
    }
}
