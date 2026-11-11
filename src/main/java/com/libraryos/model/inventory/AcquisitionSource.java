package com.libraryos.model.inventory;

public enum AcquisitionSource {
    PURCHASE("Purchase / ক্রয়কৃত"),
    DONATION("Donation / দানপ্রাপ্ত"),
    INSTITUTIONAL("Institutional / প্রাতিষ্ঠানিক"),
    OTHER("Other / অন্যান্য");

    private final String displayName;

    AcquisitionSource(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

