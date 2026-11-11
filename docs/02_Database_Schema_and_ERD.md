# LibraryOS — Database Schema & ERD Specification

## 1. Database Standards
- **Engine & Charset:** Every table MUST use `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci` to guarantee ACID transactions, foreign keys, and full Bangla (`বাংলা`) Unicode support.
- **Monetary Values:** Always use `DECIMAL(10, 2)` for currency (BDT `৳`). Never use `FLOAT` or `DOUBLE`.
- **Timestamps & Dates:** Use `DATETIME` / `TIMESTAMP` in UTC for audit events and `DATE` (`LocalDate`) for calendar-based due dates and membership expiry dates.
- **Soft Archiving:** Never issue `DELETE FROM` on `books`, `book_copies`, `members`, `users`, `loans`, `fines`, or `payments`. Use status columns (`is_archived`, `status`) to preserve historical integrity.
- **Migrations:** All DDL changes must be placed in `src/main/resources/db/migration/` using Flyway naming (`V1__init_core_schema.sql`, `V2__catalog_and_inventory.sql`, etc.).

---

## 2. Core Entity-Relationship Architecture

```text
[users] M:N [roles] M:N [permissions]
   │
   └── 1:N [audit_logs]

[publishers] 1:N [books] M:N [authors] (via book_authors)
                    │    M:N [categories] (via book_categories)
                    │
                    └── 1:N [book_copies] N:1 [shelves]
                                  │
                                  ├── 1:N [copy_status_history]
                                  └── 1:N [loans] N:1 [members] N:1 [member_types]
                                             │            │
                                             │            ├── 1:N [reservations] N:1 [books]
                                             │            └── 1:N [fines] 1:N [payment_allocations] N:1 [payments]
                                             └── 1:N [loan_renewals]
```

---

## 3. Detailed Table Specifications

### 3.1 Identity, Access & Audit (`V1__init_core_schema.sql`)
- **`users`**: `id` (BIGINT PK AUTO_INCREMENT), `username` (VARCHAR(64) UNIQUE NOT NULL), `password_hash` (VARCHAR(255) NOT NULL), `full_name` (VARCHAR(150) NOT NULL), `full_name_bn` (VARCHAR(150)), `email` (VARCHAR(150) UNIQUE), `phone` (VARCHAR(25)), `member_id` (BIGINT NULL FK -> `members.id`), `status` (`ACTIVE`, `SUSPENDED`, `DISABLED`), `failed_login_attempts` (INT DEFAULT 0), `locked_until` (DATETIME NULL), `last_login_at` (DATETIME NULL), `created_at`, `updated_at`.
- **`roles`**: `id` (INT PK), `code` (VARCHAR(50) UNIQUE NOT NULL), `name` (VARCHAR(100) NOT NULL), `description` (VARCHAR(255)).
- **`permissions`**: `id` (INT PK), `code` (VARCHAR(64) UNIQUE NOT NULL), `module` (VARCHAR(50) NOT NULL), `description` (VARCHAR(255)).
- **`user_roles`**: `(user_id, role_id)` Composite PK.
- **`role_permissions`**: `(role_id, permission_id)` Composite PK.
- **`audit_logs`**: `id` (BIGINT PK AUTO_INCREMENT), `actor_user_id` (BIGINT FK -> `users.id`), `action` (VARCHAR(100) NOT NULL), `entity_type` (VARCHAR(64) NOT NULL), `entity_id` (VARCHAR(64)), `summary` (TEXT), `reason` (VARCHAR(500)), `created_at` (DATETIME NOT NULL).
- **`app_settings`**: `setting_key` (VARCHAR(100) PK), `setting_value` (TEXT NOT NULL), `value_type` (VARCHAR(30)), `updated_by` (BIGINT), `updated_at` (DATETIME).

### 3.2 Catalog & Physical Inventory (`V2__catalog_and_inventory.sql`)
- **`publishers`**: `id` (BIGINT PK), `name` (VARCHAR(200) NOT NULL), `country` (VARCHAR(100)), `is_archived` (BOOLEAN DEFAULT FALSE).
- **`authors`**: `id` (BIGINT PK), `name_en` (VARCHAR(200) NOT NULL), `name_bn` (VARCHAR(200)), `biography` (TEXT), `is_archived` (BOOLEAN DEFAULT FALSE).
- **`categories`**: `id` (BIGINT PK), `parent_id` (BIGINT NULL FK -> `categories.id`), `name_en` (VARCHAR(150) NOT NULL), `name_bn` (VARCHAR(150)), `code` (VARCHAR(50) UNIQUE), `description` (TEXT), `is_archived` (BOOLEAN DEFAULT FALSE).
- **`books`**:
  - `id` (BIGINT PK AUTO_INCREMENT)
  - `title_en` (VARCHAR(300) NOT NULL), `title_bn` (VARCHAR(300)), `subtitle` (VARCHAR(300))
  - `isbn_10` (VARCHAR(13) UNIQUE NULL), `isbn_13` (VARCHAR(17) UNIQUE NULL)
  - `publisher_id` (BIGINT NULL FK -> `publishers.id`)
  - `publication_year` (SMALLINT), `edition` (VARCHAR(50)), `language` (VARCHAR(50) DEFAULT 'Bangla')
  - `description` (TEXT), `cover_image_path` (VARCHAR(500))
  - `is_archived` (BOOLEAN DEFAULT FALSE), `created_at`, `updated_at`.
  - *Note: Do NOT add an editable `available_copies` column here.*
- **`book_authors`**: `(book_id, author_id)` Composite PK.
- **`book_categories`**: `(book_id, category_id)` Composite PK.
- **`shelves`**: `id` (BIGINT PK), `code` (VARCHAR(50) UNIQUE NOT NULL), `section_name` (VARCHAR(100)), `rack_number` (VARCHAR(50)), `shelf_level` (VARCHAR(50)), `description` (VARCHAR(255)).
- **`book_copies`**:
  - `id` (BIGINT PK AUTO_INCREMENT)
  - `book_id` (BIGINT NOT NULL FK -> `books.id`)
  - `accession_number` (VARCHAR(64) UNIQUE NOT NULL)
  - `barcode` (VARCHAR(64) UNIQUE NOT NULL)
  - `shelf_id` (BIGINT NULL FK -> `shelves.id`)
  - `acquisition_id` (BIGINT NULL)
  - `acquisition_source` (`PURCHASE`, `DONATION`, `INSTITUTIONAL`, `OTHER`)
  - `purchase_cost` (DECIMAL(10,2) CHECK (`purchase_cost` >= 0))
  - `acquired_date` (DATE)
  - `condition_status` (`NEW`, `GOOD`, `FAIR`, `POOR`, `DAMAGED`)
  - `status` (`AVAILABLE`, `ISSUED`, `RESERVED`, `LOST`, `DAMAGED`, `UNDER_MAINTENANCE`, `WITHDRAWN`)
  - `notes` (TEXT), `created_at`, `updated_at`.
- **`copy_status_history`**: `id` (BIGINT PK), `copy_id` (BIGINT FK), `old_status` (VARCHAR(30)), `new_status` (VARCHAR(30) NOT NULL), `changed_by` (BIGINT FK -> `users.id`), `reason` (VARCHAR(500)), `changed_at` (DATETIME).

### 3.3 Members & Borrowing Policies (`V3__members_and_policies.sql`)
- **`member_types`**: `id` (INT PK), `code` (VARCHAR(40) UNIQUE NOT NULL — e.g., `STUDENT`, `TEACHER`, `STAFF`, `RESEARCHER`, `GENERAL`), `name` (VARCHAR(100) NOT NULL).
- **`borrowing_policies`**:
  - `id` (INT PK), `member_type_id` (INT UNIQUE NOT NULL FK -> `member_types.id`)
  - `max_active_loans` (INT NOT NULL DEFAULT 3)
  - `loan_duration_days` (INT NOT NULL DEFAULT 14)
  - `max_renewals` (INT NOT NULL DEFAULT 1)
  - `renewal_duration_days` (INT NOT NULL DEFAULT 7)
  - `grace_period_days` (INT NOT NULL DEFAULT 0)
  - `fine_per_day_bdt` (DECIMAL(10,2) NOT NULL DEFAULT 5.00)
  - `max_fine_per_loan_bdt` (DECIMAL(10,2) NULL)
  - `max_unpaid_fine_limit_bdt` (DECIMAL(10,2) NOT NULL DEFAULT 100.00)
  - `reservation_hold_days` (INT NOT NULL DEFAULT 3)
  - `count_holidays_in_overdue` (BOOLEAN NOT NULL DEFAULT FALSE)
  - `updated_at` (DATETIME).
- **`members`**:
  - `id` (BIGINT PK AUTO_INCREMENT)
  - `membership_number` (VARCHAR(50) UNIQUE NOT NULL)
  - `member_type_id` (INT NOT NULL FK -> `member_types.id`)
  - `full_name_en` (VARCHAR(150) NOT NULL), `full_name_bn` (VARCHAR(150))
  - `institutional_id` (VARCHAR(64) INDEX — Student Roll / Staff ID)
  - `department` (VARCHAR(120)), `program` (VARCHAR(120)), `semester_or_session` (VARCHAR(64))
  - `phone` (VARCHAR(25) NOT NULL INDEX), `email` (VARCHAR(150)), `address` (TEXT)
  - `photo_path` (VARCHAR(500))
  - `membership_start_date` (DATE NOT NULL), `membership_expiry_date` (DATE NOT NULL)
  - `status` (`ACTIVE`, `SUSPENDED`, `EXPIRED`, `ARCHIVED`)
  - `notes` (TEXT), `created_at`, `updated_at`.

### 3.4 Circulation, Reservations & Finance (`V4__circulation_reservations_finance.sql`)
- **`loans`**:
  - `id` (BIGINT PK AUTO_INCREMENT)
  - `copy_id` (BIGINT NOT NULL FK -> `book_copies.id`)
  - `member_id` (BIGINT NOT NULL FK -> `members.id`)
  - `issued_by` (BIGINT NOT NULL FK -> `users.id`)
  - `issued_at` (DATETIME NOT NULL)
  - `due_date` (DATE NOT NULL)
  - `returned_at` (DATETIME NULL)
  - `returned_to` (BIGINT NULL FK -> `users.id`)
  - `return_condition` (VARCHAR(30) NULL)
  - `renewal_count` (INT NOT NULL DEFAULT 0)
  - `policy_fine_per_day_snapshot` (DECIMAL(10,2) NOT NULL)
  - `status` (`ACTIVE`, `RETURNED`, `LOST_CLOSED`)
  - `notes` (TEXT).
- **`loan_renewals`**: `id` (BIGINT PK), `loan_id` (BIGINT FK -> `loans.id`), `previous_due_date` (DATE), `new_due_date` (DATE), `renewed_by` (BIGINT FK -> `users.id`), `renewed_at` (DATETIME).
- **`reservations`**:
  - `id` (BIGINT PK AUTO_INCREMENT)
  - `book_id` (BIGINT NOT NULL FK -> `books.id`)
  - `member_id` (BIGINT NOT NULL FK -> `members.id`)
  - `allocated_copy_id` (BIGINT NULL FK -> `book_copies.id`)
  - `status` (`WAITING`, `READY_FOR_PICKUP`, `FULFILLED`, `CANCELLED`, `EXPIRED`)
  - `requested_at` (DATETIME NOT NULL)
  - `ready_at` (DATETIME NULL), `expires_at` (DATETIME NULL)
  - `closed_at` (DATETIME NULL), `notes` (VARCHAR(500)).
- **`fines`**:
  - `id` (BIGINT PK AUTO_INCREMENT)
  - `member_id` (BIGINT NOT NULL FK -> `members.id`)
  - `loan_id` (BIGINT NULL FK -> `loans.id`)
  - `fine_type` (`OVERDUE`, `LOST_BOOK`, `DAMAGED_BOOK`, `OTHER`)
  - `original_amount_bdt` (DECIMAL(10,2) NOT NULL CHECK (`original_amount_bdt` >= 0))
  - `waived_amount_bdt` (DECIMAL(10,2) NOT NULL DEFAULT 0.00)
  - `paid_amount_bdt` (DECIMAL(10,2) NOT NULL DEFAULT 0.00)
  - `status` (`UNPAID`, `PARTIALLY_PAID`, `PAID`, `WAIVED`)
  - `created_by` (BIGINT FK -> `users.id`), `created_at` (DATETIME), `updated_at` (DATETIME).
- **`fine_adjustments`**: `id` (BIGINT PK), `fine_id` (BIGINT FK -> `fines.id`), `adjustment_type` (`WAIVER`, `SURCHARGE`, `CORRECTION`), `amount_bdt` (DECIMAL(10,2) NOT NULL), `approved_by` (BIGINT FK -> `users.id`), `reason` (VARCHAR(500) NOT NULL), `created_at` (DATETIME).
- **`payments`**:
  - `id` (BIGINT PK AUTO_INCREMENT)
  - `receipt_number` (VARCHAR(64) UNIQUE NOT NULL)
  - `member_id` (BIGINT NOT NULL FK -> `members.id`)
  - `amount_bdt` (DECIMAL(10,2) NOT NULL CHECK (`amount_bdt` > 0))
  - `payment_method` (`CASH`, `BKASH`, `NAGAD`, `ROCKET`, `BANK_TRANSFER`, `OTHER`)
  - `external_reference` (VARCHAR(100) NULL — TrxID for external MFS payments)
  - `recorded_by` (BIGINT NOT NULL FK -> `users.id`)
  - `paid_at` (DATETIME NOT NULL)
  - `is_reversed` (BOOLEAN DEFAULT FALSE), `reversal_reason` (VARCHAR(500)), `notes` (TEXT).
- **`payment_allocations`**: `id` (BIGINT PK), `payment_id` (BIGINT FK -> `payments.id`), `fine_id` (BIGINT FK -> `fines.id`), `allocated_amount_bdt` (DECIMAL(10,2) NOT NULL).