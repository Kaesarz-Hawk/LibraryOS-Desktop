package com.libraryos.model.reservation;

import java.time.LocalDateTime;

public class Reservation {
    private Long id;
    private Long bookId;
    private String bookTitle;
    private Long memberId;
    private String memberName;
    private String membershipNumber;
    private Long allocatedCopyId;
    private String allocatedBarcode;
    private ReservationStatus status = ReservationStatus.WAITING;
    private LocalDateTime requestedAt;
    private LocalDateTime readyAt;
    private LocalDateTime expiresAt;
    private LocalDateTime closedAt;
    private String notes;

    public Reservation() {
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

    public Long getAllocatedCopyId() {
        return allocatedCopyId;
    }

    public void setAllocatedCopyId(Long allocatedCopyId) {
        this.allocatedCopyId = allocatedCopyId;
    }

    public String getAllocatedBarcode() {
        return allocatedBarcode;
    }

    public void setAllocatedBarcode(String allocatedBarcode) {
        this.allocatedBarcode = allocatedBarcode;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status != null ? status : ReservationStatus.WAITING;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public LocalDateTime getReadyAt() {
        return readyAt;
    }

    public void setReadyAt(LocalDateTime readyAt) {
        this.readyAt = readyAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public String toString() {
        return "Reservation #" + id + " [" + bookTitle + " -> " + membershipNumber + " (" + status + ")]";
    }
}

