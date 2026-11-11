package com.libraryos.model.digital;

import java.time.LocalDateTime;

public class Bookmark {
    private Long id;
    private Long memberId;
    private Long digitalBookId;
    private int pageNumber;
    private String title;
    private String note;
    private LocalDateTime createdAt;

    public Bookmark() {
    }

    public Bookmark(Long memberId, Long digitalBookId, int pageNumber, String title, String note) {
        this.memberId = memberId;
        this.digitalBookId = digitalBookId;
        this.pageNumber = pageNumber;
        this.title = title;
        this.note = note;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public Long getDigitalBookId() {
        return digitalBookId;
    }

    public void setDigitalBookId(Long digitalBookId) {
        this.digitalBookId = digitalBookId;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

