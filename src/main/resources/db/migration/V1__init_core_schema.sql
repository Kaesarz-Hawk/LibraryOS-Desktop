-- ============================================================================
-- LibraryOS: Phase 1 — Core Schema & Identity/Access Baseline
-- Engine: InnoDB, Charset: utf8mb4, Collation: utf8mb4_unicode_ci
-- ============================================================================

-- 1. Roles
CREATE TABLE IF NOT EXISTS roles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Permissions
CREATE TABLE IF NOT EXISTS permissions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    module VARCHAR(50) NOT NULL,
    description VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Users
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    full_name_bn VARCHAR(150),
    email VARCHAR(150) UNIQUE,
    phone VARCHAR(25),
    member_id BIGINT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    failed_login_attempts INT NOT NULL DEFAULT 0,
    locked_until DATETIME NULL,
    last_login_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_users_username (username),
    INDEX idx_users_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. User Roles (M:N)
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_id INT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Role Permissions (M:N)
CREATE TABLE IF NOT EXISTS role_permissions (
    role_id INT NOT NULL,
    permission_id INT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_rp_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE,
    CONSTRAINT fk_rp_permission FOREIGN KEY (permission_id) REFERENCES permissions (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Audit Logs
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_user_id BIGINT NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(64) NOT NULL,
    entity_id VARCHAR(64),
    summary TEXT,
    reason VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_action (action),
    INDEX idx_audit_actor (actor_user_id),
    INDEX idx_audit_created (created_at),
    CONSTRAINT fk_audit_user FOREIGN KEY (actor_user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. Application Settings
CREATE TABLE IF NOT EXISTS app_settings (
    setting_key VARCHAR(100) PRIMARY KEY,
    setting_value TEXT NOT NULL,
    value_type VARCHAR(30) DEFAULT 'STRING',
    updated_by BIGINT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- Seed Data: Core Roles
-- ============================================================================
INSERT INTO roles (code, name, description) VALUES
('SUPER_ADMIN', 'Super Admin', 'Full system access and institution management'),
('LIBRARY_ADMIN', 'Library Administrator', 'Administrative operations and staff supervision'),
('LIBRARIAN', 'Librarian', 'Catalog, inventory, circulation and reports management'),
('ASSISTANT_LIBRARIAN', 'Assistant Librarian', 'Circulation desk and member operations'),
('MEMBER', 'Student / Member', 'OPAC book catalog search, self reservations, and loan tracking');

-- ============================================================================
-- Seed Data: Granular Permissions (30 codes)
-- ============================================================================
INSERT INTO permissions (code, module, description) VALUES
('DASHBOARD_STAFF_VIEW', 'Dashboard', 'View staff operational metrics and alerts'),
('DASHBOARD_MEMBER_VIEW', 'Dashboard', 'View personal borrowing status and notifications'),
('BOOK_VIEW', 'Catalog', 'View and search book catalog'),
('BOOK_CREATE', 'Catalog', 'Add new book titles'),
('BOOK_UPDATE', 'Catalog', 'Edit existing book titles'),
('BOOK_ARCHIVE', 'Catalog', 'Archive book titles'),
('COPY_MANAGE', 'Inventory', 'Add, modify, and track physical book copies'),
('MEMBER_VIEW', 'Members', 'View member profiles and contact details'),
('MEMBER_CREATE', 'Members', 'Register new library members'),
('MEMBER_UPDATE', 'Members', 'Update member profile details'),
('MEMBER_SUSPEND', 'Members', 'Suspend or reactivate member accounts'),
('LOAN_ISSUE', 'Circulation', 'Issue book copies to members'),
('LOAN_RETURN', 'Circulation', 'Process returned book copies and calculate fines'),
('LOAN_RENEW', 'Circulation', 'Renew active book loans'),
('RESERVATION_SELF', 'Reservations', 'Place hold reservations for own account'),
('RESERVATION_MANAGE', 'Reservations', 'Manage all reservation queues and allocations'),
('FINE_VIEW', 'Finance', 'View fine records and overdue balances'),
('FINE_WAIVE', 'Finance', 'Waive or adjust unpaid overdue fines'),
('PAYMENT_RECORD', 'Finance', 'Collect fine payments and issue receipts'),
('BARCODE_GENERATE', 'Barcode/QR', 'Generate and print barcode/QR labels and member cards'),
('DIGITAL_READ', 'Digital Library', 'Read and access digital PDF resources'),
('DIGITAL_MANAGE', 'Digital Library', 'Upload and organize digital PDF resources'),
('SUPPLIER_MANAGE', 'Acquisitions', 'Manage suppliers and purchase acquisitions'),
('REPORT_VIEW', 'Reports', 'View operational reports and statistical charts'),
('REPORT_EXPORT', 'Reports', 'Export reports to PDF, Excel, and CSV'),
('USER_MANAGE', 'Administration', 'Create and manage system user accounts'),
('ROLE_MANAGE', 'Administration', 'Configure system roles and permission sets'),
('SETTINGS_MANAGE', 'Administration', 'Update institution profile and borrowing policies'),
('AUDIT_VIEW', 'Administration', 'Inspect system audit and security logs'),
('BACKUP_MANAGE', 'Administration', 'Perform and verify system database backups');

-- ============================================================================
-- Seed Data: Role-Permission Mappings
-- ============================================================================

-- 1. SUPER_ADMIN gets all permissions except member self views
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'SUPER_ADMIN'
  AND p.code NOT IN ('DASHBOARD_MEMBER_VIEW', 'RESERVATION_SELF');

-- 2. LIBRARY_ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'LIBRARY_ADMIN'
  AND p.code IN (
    'DASHBOARD_STAFF_VIEW', 'BOOK_VIEW', 'BOOK_CREATE', 'BOOK_UPDATE', 'BOOK_ARCHIVE',
    'COPY_MANAGE', 'MEMBER_VIEW', 'MEMBER_CREATE', 'MEMBER_UPDATE', 'MEMBER_SUSPEND',
    'LOAN_ISSUE', 'LOAN_RETURN', 'LOAN_RENEW', 'RESERVATION_MANAGE',
    'FINE_VIEW', 'FINE_WAIVE', 'PAYMENT_RECORD', 'BARCODE_GENERATE',
    'DIGITAL_READ', 'DIGITAL_MANAGE', 'SUPPLIER_MANAGE', 'REPORT_VIEW', 'REPORT_EXPORT',
    'USER_MANAGE', 'SETTINGS_MANAGE', 'AUDIT_VIEW'
  );

-- 3. LIBRARIAN
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'LIBRARIAN'
  AND p.code IN (
    'DASHBOARD_STAFF_VIEW', 'BOOK_VIEW', 'BOOK_CREATE', 'BOOK_UPDATE',
    'COPY_MANAGE', 'MEMBER_VIEW', 'MEMBER_CREATE', 'MEMBER_UPDATE',
    'LOAN_ISSUE', 'LOAN_RETURN', 'LOAN_RENEW', 'RESERVATION_MANAGE',
    'FINE_VIEW', 'PAYMENT_RECORD', 'BARCODE_GENERATE',
    'DIGITAL_READ', 'DIGITAL_MANAGE', 'REPORT_VIEW', 'REPORT_EXPORT'
  );

-- 4. ASSISTANT_LIBRARIAN
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'ASSISTANT_LIBRARIAN'
  AND p.code IN (
    'DASHBOARD_STAFF_VIEW', 'BOOK_VIEW', 'COPY_MANAGE', 'MEMBER_VIEW',
    'LOAN_ISSUE', 'LOAN_RETURN', 'LOAN_RENEW', 'RESERVATION_MANAGE',
    'FINE_VIEW', 'BARCODE_GENERATE', 'DIGITAL_READ'
  );

-- 5. MEMBER
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'MEMBER'
  AND p.code IN (
    'DASHBOARD_MEMBER_VIEW', 'BOOK_VIEW', 'MEMBER_VIEW', 'MEMBER_UPDATE',
    'RESERVATION_SELF', 'FINE_VIEW', 'DIGITAL_READ'
  );

-- ============================================================================
-- Seed Data: Initial Super Admin Account (admin / Admin@1234)
-- ============================================================================
INSERT INTO users (username, password_hash, full_name, full_name_bn, email, phone, status)
VALUES (
    'admin',
    '$2a$12$sYPMyymGHrHzAimrWnLhButQj/df.Epix.St3ucDZyaG0nNsFFt1.',
    'System Administrator',
    'সিস্টেম অ্যাডমিনিস্ট্রেটর',
    'admin@libraryos.local',
    '01700000000',
    'ACTIVE'
);

-- Assign SUPER_ADMIN role to initial admin
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'admin' AND r.code = 'SUPER_ADMIN';

-- Initial Audit Log entry
INSERT INTO audit_logs (actor_user_id, action, entity_type, entity_id, summary, reason)
SELECT u.id, 'SYSTEM_INITIALIZED', 'SYSTEM', '1', 'LibraryOS database schema initialized with Super Admin account', 'Phase 1 Core Setup'
FROM users u WHERE u.username = 'admin';

-- ============================================================================
-- Seed Data: Default Application Settings
-- ============================================================================
INSERT INTO app_settings (setting_key, setting_value, value_type) VALUES
('institution.name', 'Dhaka Central Model Library', 'STRING'),
('institution.name_bn', 'ঢাকা সেন্ট্রাল মডেল লাইব্রেরি', 'STRING'),
('institution.code', 'DCML-01', 'STRING'),
('institution.currency', '৳', 'STRING'),
('institution.country', 'Bangladesh', 'STRING'),
('library.weekly_holiday', 'FRIDAY', 'STRING'),
('auth.max_failed_attempts', '5', 'INTEGER'),
('auth.lockout_minutes', '15', 'INTEGER'),
('app.version', '1.0.0', 'STRING');

