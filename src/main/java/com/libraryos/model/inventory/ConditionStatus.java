package com.libraryos.model.inventory;

public enum ConditionStatus {
    NEW("New / নতুন"),
    GOOD("Good / ভালো"),
    FAIR("Fair / চলনসই"),
    POOR("Poor / জীর্ণ"),
    DAMAGED("Damaged / নষ্ট");

    private final String displayName;

    ConditionStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

