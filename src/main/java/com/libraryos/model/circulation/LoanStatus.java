package com.libraryos.model.circulation;

public enum LoanStatus {
    ACTIVE("Active / চলমান"),
    RETURNED("Returned / ফেরত গৃহীত"),
    LOST_CLOSED("Lost & Closed / হারানো নিষ্পন্ন");

    private final String displayName;

    LoanStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

