package com.libraryos.model.inventory;

public enum CopyStatus {
    AVAILABLE("Available / মজুত"),
    ISSUED("Issued / ধারকৃত"),
    RESERVED("Reserved / সংরক্ষিত"),
    LOST("Lost / হারিয়ে গেছে"),
    DAMAGED("Damaged / ক্ষতিগ্রস্ত"),
    UNDER_MAINTENANCE("Under Maintenance / মেরামতে"),
    WITHDRAWN("Withdrawn / প্রত্যাহারকৃত");

    private final String displayName;

    CopyStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

