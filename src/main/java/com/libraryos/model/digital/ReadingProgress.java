package com.libraryos.model.digital;

import java.time.LocalDateTime;

public class ReadingProgress {
    private Long id;
    private Long memberId;
    private String memberName;
    private Long digitalBookId;
    private String digitalBookTitle;
    private int lastPageRead = 1;
    private int totalPages = 1;
    private LocalDateTime lastReadAt;

    public ReadingProgress() {
    }

    public ReadingProgress(Long memberId, Long digitalBookId, int lastPageRead, int totalPages) {
        this.memberId = memberId;
        this.digitalBookId = digitalBookId;
        this.lastPageRead = lastPageRead;
        this.totalPages = totalPages;
        this.lastReadAt = LocalDateTime.now();
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

    public String getMemberName() {
        return memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }

    public Long getDigitalBookId() {
        return digitalBookId;
    }

    public void setDigitalBookId(Long digitalBookId) {
        this.digitalBookId = digitalBookId;
    }

    public String getDigitalBookTitle() {
        return digitalBookTitle;
    }

    public void setDigitalBookTitle(String digitalBookTitle) {
        this.digitalBookTitle = digitalBookTitle;
    }

    public int getLastPageRead() {
        return lastPageRead;
    }

    public void setLastPageRead(int lastPageRead) {
        this.lastPageRead = lastPageRead;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public LocalDateTime getLastReadAt() {
        return lastReadAt;
    }

    public void setLastReadAt(LocalDateTime lastReadAt) {
        this.lastReadAt = lastReadAt;
    }

    public double getProgressPercentage() {
        if (totalPages <= 0) return 0.0;
        return Math.min(100.0, (double) lastPageRead / totalPages * 100.0);
    }
}

