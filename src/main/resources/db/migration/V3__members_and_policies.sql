-- ============================================================================
-- LibraryOS: Phase 2 — Members & Borrowing Policies Schema
-- Engine: InnoDB, Charset: utf8mb4, Collation: utf8mb4_unicode_ci
-- ============================================================================

-- 1. Member Types
CREATE TABLE IF NOT EXISTS member_types (
    id INT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Institutional Borrowing Policies (1:1 with Member Types)
CREATE TABLE IF NOT EXISTS borrowing_policies (
    id INT AUTO_INCREMENT PRIMARY KEY,
    member_type_id INT NOT NULL UNIQUE,
    max_active_loans INT NOT NULL DEFAULT 3,
    loan_duration_days INT NOT NULL DEFAULT 14,
    max_renewals INT NOT NULL DEFAULT 1,
    renewal_duration_days INT NOT NULL DEFAULT 7,
    grace_period_days INT NOT NULL DEFAULT 0,
    fine_per_day_bdt DECIMAL(10,2) NOT NULL DEFAULT 5.00,
    max_fine_per_loan_bdt DECIMAL(10,2) NULL,
    max_unpaid_fine_limit_bdt DECIMAL(10,2) NOT NULL DEFAULT 100.00,
    reservation_hold_days INT NOT NULL DEFAULT 3,
    count_holidays_in_overdue BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_bp_member_type FOREIGN KEY (member_type_id) REFERENCES member_types (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Library Members Directory
CREATE TABLE IF NOT EXISTS members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    membership_number VARCHAR(50) NOT NULL UNIQUE,
    member_type_id INT NOT NULL,
    full_name_en VARCHAR(150) NOT NULL,
    full_name_bn VARCHAR(150),
    institutional_id VARCHAR(64) NULL,
    department VARCHAR(120),
    program VARCHAR(120),
    semester_or_session VARCHAR(64),
    phone VARCHAR(25) NOT NULL,
    email VARCHAR(150),
    address TEXT,
    photo_path VARCHAR(500),
    membership_start_date DATE NOT NULL,
    membership_expiry_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    notes TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_members_type FOREIGN KEY (member_type_id) REFERENCES member_types (id) ON DELETE RESTRICT,
    INDEX idx_members_number (membership_number),
    INDEX idx_members_inst_id (institutional_id),
    INDEX idx_members_phone (phone),
    INDEX idx_members_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- Seed Data: Member Types & Bangladesh Borrowing Policies
-- ============================================================================
INSERT INTO member_types (code, name, description) VALUES
('STUDENT', 'Student / শিক্ষার্থী', 'Undergraduate and Graduate enrolled students'),
('TEACHER', 'Faculty / শিক্ষক', 'Professors, Lecturers, and Teaching Staff'),
('STAFF', 'Administrative Staff / কর্মকর্তা-কর্মচারী', 'Institutional officers and staff members'),
('RESEARCHER', 'Researcher / গবেষক', 'Postdoctoral fellows and project researchers'),
('GENERAL', 'General Patron / সাধারণ পাঠক', 'Community and external registered patrons');

-- Seed Borrowing Policies for each member type
INSERT INTO borrowing_policies (member_type_id, max_active_loans, loan_duration_days, max_renewals, renewal_duration_days, grace_period_days, fine_per_day_bdt, max_unpaid_fine_limit_bdt, reservation_hold_days, count_holidays_in_overdue)
SELECT id, 3, 14, 1, 7, 0, 5.00, 100.00, 3, FALSE FROM member_types WHERE code = 'STUDENT';

INSERT INTO borrowing_policies (member_type_id, max_active_loans, loan_duration_days, max_renewals, renewal_duration_days, grace_period_days, fine_per_day_bdt, max_unpaid_fine_limit_bdt, reservation_hold_days, count_holidays_in_overdue)
SELECT id, 10, 30, 2, 14, 2, 0.00, 500.00, 5, FALSE FROM member_types WHERE code = 'TEACHER';

INSERT INTO borrowing_policies (member_type_id, max_active_loans, loan_duration_days, max_renewals, renewal_duration_days, grace_period_days, fine_per_day_bdt, max_unpaid_fine_limit_bdt, reservation_hold_days, count_holidays_in_overdue)
SELECT id, 5, 21, 1, 7, 1, 2.00, 200.00, 3, FALSE FROM member_types WHERE code = 'STAFF';

INSERT INTO borrowing_policies (member_type_id, max_active_loans, loan_duration_days, max_renewals, renewal_duration_days, grace_period_days, fine_per_day_bdt, max_unpaid_fine_limit_bdt, reservation_hold_days, count_holidays_in_overdue)
SELECT id, 8, 30, 2, 14, 1, 5.00, 300.00, 5, FALSE FROM member_types WHERE code = 'RESEARCHER';

INSERT INTO borrowing_policies (member_type_id, max_active_loans, loan_duration_days, max_renewals, renewal_duration_days, grace_period_days, fine_per_day_bdt, max_unpaid_fine_limit_bdt, reservation_hold_days, count_holidays_in_overdue)
SELECT id, 2, 10, 1, 5, 0, 10.00, 100.00, 2, FALSE FROM member_types WHERE code = 'GENERAL';

-- Seed Sample Members
INSERT INTO members (membership_number, member_type_id, full_name_en, full_name_bn, institutional_id, department, program, semester_or_session, phone, email, membership_start_date, membership_expiry_date, status) VALUES
('LIB-2026-0001', (SELECT id FROM member_types WHERE code = 'STUDENT'), 'Tariqul Islam', 'তারিকুল ইসলাম', 'CSE-2022-042', 'Computer Science & Engineering', 'B.Sc in CSE', 'Spring 2026', '01712345678', 'tariqul@university.edu.bd', '2026-01-01', '2027-01-01', 'ACTIVE'),
('LIB-2026-0002', (SELECT id FROM member_types WHERE code = 'TEACHER'), 'Dr. Ayesha Siddiqua', 'ড. আয়েশা সিদ্দিকা', 'FAC-2018-09', 'Physics', 'Faculty', 'N/A', '01812345679', 'ayesha.s@university.edu.bd', '2026-01-01', '2028-01-01', 'ACTIVE'),
('LIB-2026-0003', (SELECT id FROM member_types WHERE code = 'STAFF'), 'Md. Kabir Hossain', 'মো. কবির হোসেন', 'STAFF-104', 'Library Administration', 'Staff', 'N/A', '01912345680', 'kabir@university.edu.bd', '2026-01-01', '2027-01-01', 'ACTIVE');

