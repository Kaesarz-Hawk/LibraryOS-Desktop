package com.libraryos.model.user;

public enum PermissionCode {
    // Dashboard
    DASHBOARD_STAFF_VIEW("Dashboard", "View staff operational metrics and alerts"),
    DASHBOARD_MEMBER_VIEW("Dashboard", "View personal borrowing status and notifications"),

    // Catalog
    BOOK_VIEW("Catalog", "View and search book catalog"),
    BOOK_CREATE("Catalog", "Add new book titles"),
    BOOK_UPDATE("Catalog", "Edit existing book titles"),
    BOOK_ARCHIVE("Catalog", "Archive book titles"),

    // Inventory
    COPY_MANAGE("Inventory", "Add, modify, and track physical book copies"),

    // Members
    MEMBER_VIEW("Members", "View member profiles and contact details"),
    MEMBER_CREATE("Members", "Register new library members"),
    MEMBER_UPDATE("Members", "Update member profile details"),
    MEMBER_SUSPEND("Members", "Suspend or reactivate member accounts"),

    // Circulation
    LOAN_ISSUE("Circulation", "Issue book copies to members"),
    LOAN_RETURN("Circulation", "Process returned book copies and calculate fines"),
    LOAN_RENEW("Circulation", "Renew active book loans"),

    // Reservations
    RESERVATION_SELF("Reservations", "Place hold reservations for own account"),
    RESERVATION_MANAGE("Reservations", "Manage all reservation queues and allocations"),

    // Finance
    FINE_VIEW("Finance", "View fine records and overdue balances"),
    FINE_WAIVE("Finance", "Waive or adjust unpaid overdue fines"),
    PAYMENT_RECORD("Finance", "Collect fine payments and issue receipts"),

    // Barcode / QR
    BARCODE_GENERATE("Barcode/QR", "Generate and print barcode/QR labels and member cards"),

    // Digital Library
    DIGITAL_READ("Digital Library", "Read and access digital PDF resources"),
    DIGITAL_MANAGE("Digital Library", "Upload and organize digital PDF resources"),

    // Acquisitions
    SUPPLIER_MANAGE("Acquisitions", "Manage suppliers and purchase acquisitions"),

    // Reports
    REPORT_VIEW("Reports", "View operational reports and statistical charts"),
    REPORT_EXPORT("Reports", "Export reports to PDF, Excel, and CSV"),

    // Administration
    USER_MANAGE("Administration", "Create and manage system user accounts"),
    ROLE_MANAGE("Administration", "Configure system roles and permission sets"),
    SETTINGS_MANAGE("Administration", "Update institution profile and borrowing policies"),
    AUDIT_VIEW("Administration", "Inspect system audit and security logs"),
    BACKUP_MANAGE("Administration", "Perform and verify system database backups");

    private final String module;
    private final String description;

    PermissionCode(String module, String description) {
        this.module = module;
        this.description = description;
    }

    public String getModule() {
        return module;
    }

    public String getDescription() {
        return description;
    }
}
