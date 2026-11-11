package com.libraryos.model.finance;

public enum PaymentMethod {
    CASH("Cash / নগদ"),
    BKASH("bKash / বিকাশ"),
    NAGAD("Nagad / নগদ"),
    ROCKET("Rocket / রকেট"),
    BANK_TRANSFER("Bank Transfer / ব্যাংক স্থানান্তর"),
    OTHER("Other / অন্যান্য");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

