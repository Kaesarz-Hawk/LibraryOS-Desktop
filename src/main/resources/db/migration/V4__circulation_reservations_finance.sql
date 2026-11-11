-- ============================================================================
-- LibraryOS: Phase 3 — Circulation, Reservations & Finance Schema
-- Engine: InnoDB, Charset: utf8mb4, Collation: utf8mb4_unicode_ci
-- ============================================================================

-- 1. Loans (Active and Historical Circulation Records)
CREATE TABLE IF NOT EXISTS loans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    copy_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    issued_by BIGINT NOT NULL,
    issued_at DATETIME NOT NULL,
    due_date DATE NOT NULL,
    returned_at DATETIME NULL,
    returned_to BIGINT NULL,
    return_condition VARCHAR(30) NULL,
    renewal_count INT NOT NULL DEFAULT 0,
    policy_fine_per_day_snapshot DECIMAL(10,2) NOT NULL DEFAULT 5.00,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    notes TEXT,
    CONSTRAINT fk_loans_copy FOREIGN KEY (copy_id) REFERENCES book_copies (id) ON DELETE RESTRICT,
    CONSTRAINT fk_loans_member FOREIGN KEY (member_id) REFERENCES members (id) ON DELETE RESTRICT,
    CONSTRAINT fk_loans_issued_by FOREIGN KEY (issued_by) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_loans_returned_to FOREIGN KEY (returned_to) REFERENCES users (id) ON DELETE SET NULL,
    INDEX idx_loans_copy (copy_id),
    INDEX idx_loans_member (member_id),
    INDEX idx_loans_status (status),
    INDEX idx_loans_due_date (due_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Loan Renewals Audit
CREATE TABLE IF NOT EXISTS loan_renewals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    loan_id BIGINT NOT NULL,
    previous_due_date DATE NOT NULL,
    new_due_date DATE NOT NULL,
    renewed_by BIGINT NOT NULL,
    renewed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_renewals_loan FOREIGN KEY (loan_id) REFERENCES loans (id) ON DELETE CASCADE,
    CONSTRAINT fk_renewals_user FOREIGN KEY (renewed_by) REFERENCES users (id) ON DELETE RESTRICT,
    INDEX idx_renewals_loan (loan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Reservations (Hold Queue & Copy Allocation)
CREATE TABLE IF NOT EXISTS reservations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    book_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    allocated_copy_id BIGINT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'WAITING',
    requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ready_at DATETIME NULL,
    expires_at DATETIME NULL,
    closed_at DATETIME NULL,
    notes VARCHAR(500),
    CONSTRAINT fk_res_book FOREIGN KEY (book_id) REFERENCES books (id) ON DELETE CASCADE,
    CONSTRAINT fk_res_member FOREIGN KEY (member_id) REFERENCES members (id) ON DELETE CASCADE,
    CONSTRAINT fk_res_copy FOREIGN KEY (allocated_copy_id) REFERENCES book_copies (id) ON DELETE SET NULL,
    INDEX idx_res_book (book_id),
    INDEX idx_res_member (member_id),
    INDEX idx_res_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Fines (Overdue & Damage Liabilities in BDT)
CREATE TABLE IF NOT EXISTS fines (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    loan_id BIGINT NULL,
    fine_type VARCHAR(30) NOT NULL DEFAULT 'OVERDUE',
    original_amount_bdt DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    waived_amount_bdt DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    paid_amount_bdt DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(30) NOT NULL DEFAULT 'UNPAID',
    created_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_fines_member FOREIGN KEY (member_id) REFERENCES members (id) ON DELETE RESTRICT,
    CONSTRAINT fk_fines_loan FOREIGN KEY (loan_id) REFERENCES loans (id) ON DELETE SET NULL,
    CONSTRAINT fk_fines_user FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL,
    INDEX idx_fines_member (member_id),
    INDEX idx_fines_loan (loan_id),
    INDEX idx_fines_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Fine Adjustments & Waivers
CREATE TABLE IF NOT EXISTS fine_adjustments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fine_id BIGINT NOT NULL,
    adjustment_type VARCHAR(30) NOT NULL,
    amount_bdt DECIMAL(10,2) NOT NULL,
    approved_by BIGINT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_adj_fine FOREIGN KEY (fine_id) REFERENCES fines (id) ON DELETE CASCADE,
    CONSTRAINT fk_adj_user FOREIGN KEY (approved_by) REFERENCES users (id) ON DELETE RESTRICT,
    INDEX idx_adj_fine (fine_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Payments (Immutable Append-Only BDT Receipts)
CREATE TABLE IF NOT EXISTS payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    receipt_number VARCHAR(64) NOT NULL UNIQUE,
    member_id BIGINT NOT NULL,
    amount_bdt DECIMAL(10,2) NOT NULL,
    payment_method VARCHAR(30) NOT NULL DEFAULT 'CASH',
    external_reference VARCHAR(100) NULL,
    recorded_by BIGINT NOT NULL,
    paid_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_reversed BOOLEAN NOT NULL DEFAULT FALSE,
    reversal_reason VARCHAR(500) NULL,
    notes TEXT,
    CONSTRAINT fk_payments_member FOREIGN KEY (member_id) REFERENCES members (id) ON DELETE RESTRICT,
    CONSTRAINT fk_payments_recorded_by FOREIGN KEY (recorded_by) REFERENCES users (id) ON DELETE RESTRICT,
    INDEX idx_payments_receipt (receipt_number),
    INDEX idx_payments_member (member_id),
    INDEX idx_payments_ext_ref (external_reference)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. Payment Allocations (Payment-to-Fine Mapping)
CREATE TABLE IF NOT EXISTS payment_allocations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id BIGINT NOT NULL,
    fine_id BIGINT NOT NULL,
    allocated_amount_bdt DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_alloc_payment FOREIGN KEY (payment_id) REFERENCES payments (id) ON DELETE CASCADE,
    CONSTRAINT fk_alloc_fine FOREIGN KEY (fine_id) REFERENCES fines (id) ON DELETE RESTRICT,
    INDEX idx_alloc_payment (payment_id),
    INDEX idx_alloc_fine (fine_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed Sample Active Loan for Tariqul Islam (Member #1) on Book Copy #1 (Clean Code)
INSERT INTO loans (id, copy_id, member_id, issued_by, issued_at, due_date, renewal_count, policy_fine_per_day_snapshot, status)
VALUES (
    1,
    1,
    1,
    1,
    '2026-11-01 10:00:00',
    '2026-11-15',
    0,
    5.00,
    'ACTIVE'
);

-- Update Copy 1 to ISSUED
UPDATE book_copies SET status = 'ISSUED' WHERE id = 1;

INSERT INTO copy_status_history (copy_id, old_status, new_status, changed_by, reason)
VALUES (1, 'AVAILABLE', 'ISSUED', 1, 'Initial seed loan issue to Tariqul Islam');
