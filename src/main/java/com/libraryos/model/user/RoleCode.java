package com.libraryos.model.user;

public enum RoleCode {
    SUPER_ADMIN("Super Admin"),
    LIBRARY_ADMIN("Library Administrator"),
    LIBRARIAN("Librarian"),
    ASSISTANT_LIBRARIAN("Assistant Librarian"),
    MEMBER("Student / Member");

    private final String displayName;

    RoleCode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
