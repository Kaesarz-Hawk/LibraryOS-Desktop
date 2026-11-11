package com.libraryos.model.acquisition;

public enum AcquisitionStatus {
    ORDERED("Ordered / অর্ডারকৃত"),
    RECEIVED("Received / প্রাপ্ত ও ইনভেন্টরিতে যুক্ত"),
    CANCELLED("Cancelled / বাতিলকৃত");

    private final String displayName;

    AcquisitionStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

