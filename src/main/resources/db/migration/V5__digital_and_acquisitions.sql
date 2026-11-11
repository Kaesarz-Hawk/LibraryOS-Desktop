-- ============================================================================
-- Migration V5: Digital Library & Acquisitions Schema
-- LibraryOS — Commercial-Grade Library Management System
-- ============================================================================

-- 1. Suppliers Table
CREATE TABLE IF NOT EXISTS suppliers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    contact_person VARCHAR(150),
    phone VARCHAR(30) NOT NULL,
    email VARCHAR(150),
    address TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Acquisitions Table (Purchase Orders / Procurement Batches)
CREATE TABLE IF NOT EXISTS acquisitions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    acquisition_number VARCHAR(64) UNIQUE NOT NULL,
    supplier_id BIGINT NOT NULL,
    invoice_number VARCHAR(100),
    order_date DATE NOT NULL,
    received_date DATE,
    total_cost_bdt DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(30) NOT NULL DEFAULT 'ORDERED',
    notes TEXT,
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    CONSTRAINT fk_acq_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id),
    CONSTRAINT fk_acq_created_by FOREIGN KEY (created_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Acquisition Line Items
CREATE TABLE IF NOT EXISTS acquisition_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    acquisition_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    unit_cost_bdt DECIMAL(10,2) NOT NULL,
    received_quantity INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_item_acquisition FOREIGN KEY (acquisition_id) REFERENCES acquisitions(id) ON DELETE CASCADE,
    CONSTRAINT fk_item_book FOREIGN KEY (book_id) REFERENCES books(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Digital Books Table (E-Books & Managed PDFs)
CREATE TABLE IF NOT EXISTS digital_books (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    book_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    mime_type VARCHAR(100) NOT NULL DEFAULT 'application/pdf',
    sha256_checksum VARCHAR(64) NOT NULL,
    total_pages INT NOT NULL DEFAULT 0,
    is_download_allowed BOOLEAN NOT NULL DEFAULT TRUE,
    uploaded_by BIGINT NOT NULL,
    uploaded_at DATETIME NOT NULL,
    updated_at DATETIME,
    CONSTRAINT fk_digital_book FOREIGN KEY (book_id) REFERENCES books(id),
    CONSTRAINT fk_digital_uploaded_by FOREIGN KEY (uploaded_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Reading Progress
CREATE TABLE IF NOT EXISTS reading_progress (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    digital_book_id BIGINT NOT NULL,
    last_page_read INT NOT NULL DEFAULT 1,
    total_pages INT NOT NULL DEFAULT 1,
    last_read_at DATETIME NOT NULL,
    CONSTRAINT fk_read_progress_member FOREIGN KEY (member_id) REFERENCES members(id),
    CONSTRAINT fk_read_progress_digital FOREIGN KEY (digital_book_id) REFERENCES digital_books(id) ON DELETE CASCADE,
    CONSTRAINT uk_reading_progress UNIQUE (member_id, digital_book_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Bookmarks
CREATE TABLE IF NOT EXISTS bookmarks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    digital_book_id BIGINT NOT NULL,
    page_number INT NOT NULL,
    title VARCHAR(200) NOT NULL,
    note TEXT,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_bookmark_member FOREIGN KEY (member_id) REFERENCES members(id),
    CONSTRAINT fk_bookmark_digital FOREIGN KEY (digital_book_id) REFERENCES digital_books(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. Reviews and Ratings
CREATE TABLE IF NOT EXISTS reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    book_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    rating INT NOT NULL,
    review_title VARCHAR(200),
    review_text TEXT,
    is_approved BOOLEAN NOT NULL DEFAULT TRUE,
    is_reported BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_review_book FOREIGN KEY (book_id) REFERENCES books(id),
    CONSTRAINT fk_review_member FOREIGN KEY (member_id) REFERENCES members(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. Notifications
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Indexes for performance
CREATE INDEX idx_digital_book_id ON digital_books(book_id);
CREATE INDEX idx_acq_supplier ON acquisitions(supplier_id);
CREATE INDEX idx_acq_status ON acquisitions(status);
CREATE INDEX idx_review_book ON reviews(book_id);
CREATE INDEX idx_notif_user_unread ON notifications(user_id, is_read);

-- Seed Suppliers
INSERT INTO suppliers (id, name, contact_person, phone, email, address, is_active, created_at)
VALUES 
(1, 'বাতিঘর প্রকাশনী ও সরবরাহকারী (Batighar Books)', 'Mohammad Ali', '+8801711223344', 'sales@batighar.com', 'Bangla Bazar, Dhaka-1100', TRUE, CURRENT_TIMESTAMP),
(2, 'রকমারি প্রাতিষ্ঠানিক সরবরাহ (Rokomari Institutional)', 'Tanvir Hossain', '+8801912345678', 'supply@rokomari.com', 'Karwan Bazar, Dhaka-1215', TRUE, CURRENT_TIMESTAMP),
(3, 'সময় প্রকাশন ও বই বিতান (Somoy Prokashon)', 'Kamrul Islam', '+8801819876543', 'info@somoyprokashon.com', 'Azimpur, Dhaka-1205', TRUE, CURRENT_TIMESTAMP);

