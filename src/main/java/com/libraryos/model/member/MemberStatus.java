package com.libraryos.model.member;

public enum MemberStatus {
    ACTIVE("Active / সক্রিয়"),
    SUSPENDED("Suspended / স্থগিত"),
    EXPIRED("Expired / মেয়াদোত্তীর্ণ"),
    ARCHIVED("Archived / সংরক্ষিত");

    private final String displayName;

    MemberStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

