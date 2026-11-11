-- ============================================================================
-- LibraryOS: Phase 2 — Catalog & Physical Inventory Schema
-- Engine: InnoDB, Charset: utf8mb4, Collation: utf8mb4_unicode_ci
-- ============================================================================

-- 1. Publishers
CREATE TABLE IF NOT EXISTS publishers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    country VARCHAR(100) DEFAULT 'Bangladesh',
    is_archived BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_publishers_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Authors
CREATE TABLE IF NOT EXISTS authors (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name_en VARCHAR(200) NOT NULL,
    name_bn VARCHAR(200),
    biography TEXT,
    is_archived BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_authors_name_en (name_en),
    INDEX idx_authors_name_bn (name_bn)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Categories (Supports Hierarchical Parent-Child)
CREATE TABLE IF NOT EXISTS categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id BIGINT NULL,
    name_en VARCHAR(150) NOT NULL,
    name_bn VARCHAR(150),
    code VARCHAR(50) UNIQUE,
    description TEXT,
    is_archived BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cat_parent FOREIGN KEY (parent_id) REFERENCES categories (id) ON DELETE SET NULL,
    INDEX idx_categories_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Bibliographic Book Titles (Master Catalog - Never has editable available_copies column)
CREATE TABLE IF NOT EXISTS books (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title_en VARCHAR(300) NOT NULL,
    title_bn VARCHAR(300),
    subtitle VARCHAR(300),
    isbn_10 VARCHAR(13) UNIQUE NULL,
    isbn_13 VARCHAR(17) UNIQUE NULL,
    publisher_id BIGINT NULL,
    publication_year SMALLINT,
    edition VARCHAR(50),
    language VARCHAR(50) DEFAULT 'Bangla',
    description TEXT,
    cover_image_path VARCHAR(500),
    is_archived BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_books_publisher FOREIGN KEY (publisher_id) REFERENCES publishers (id) ON DELETE SET NULL,
    INDEX idx_books_title_en (title_en),
    INDEX idx_books_title_bn (title_bn),
    INDEX idx_books_isbn13 (isbn_13)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Book Authors (M:N)
CREATE TABLE IF NOT EXISTS book_authors (
    book_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    PRIMARY KEY (book_id, author_id),
    CONSTRAINT fk_ba_book FOREIGN KEY (book_id) REFERENCES books (id) ON DELETE CASCADE,
    CONSTRAINT fk_ba_author FOREIGN KEY (author_id) REFERENCES authors (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Book Categories (M:N)
CREATE TABLE IF NOT EXISTS book_categories (
    book_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    PRIMARY KEY (book_id, category_id),
    CONSTRAINT fk_bc_book FOREIGN KEY (book_id) REFERENCES books (id) ON DELETE CASCADE,
    CONSTRAINT fk_bc_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. Shelves & Racks (Physical Location Layout)
CREATE TABLE IF NOT EXISTS shelves (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    section_name VARCHAR(100),
    rack_number VARCHAR(50),
    shelf_level VARCHAR(50),
    description VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. Book Copies (Physical Inventory with Accession Number & Barcode)
CREATE TABLE IF NOT EXISTS book_copies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    book_id BIGINT NOT NULL,
    accession_number VARCHAR(64) NOT NULL UNIQUE,
    barcode VARCHAR(64) NOT NULL UNIQUE,
    shelf_id BIGINT NULL,
    acquisition_id BIGINT NULL,
    acquisition_source VARCHAR(30) DEFAULT 'PURCHASE',
    purchase_cost DECIMAL(10,2) DEFAULT 0.00,
    acquired_date DATE,
    condition_status VARCHAR(30) DEFAULT 'GOOD',
    status VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE',
    notes TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_copies_book FOREIGN KEY (book_id) REFERENCES books (id) ON DELETE RESTRICT,
    CONSTRAINT fk_copies_shelf FOREIGN KEY (shelf_id) REFERENCES shelves (id) ON DELETE SET NULL,
    INDEX idx_copies_accession (accession_number),
    INDEX idx_copies_barcode (barcode),
    INDEX idx_copies_status (status),
    INDEX idx_copies_book (book_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. Copy Status Audit History
CREATE TABLE IF NOT EXISTS copy_status_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    copy_id BIGINT NOT NULL,
    old_status VARCHAR(30),
    new_status VARCHAR(30) NOT NULL,
    changed_by BIGINT NULL,
    reason VARCHAR(500),
    changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_csh_copy FOREIGN KEY (copy_id) REFERENCES book_copies (id) ON DELETE CASCADE,
    CONSTRAINT fk_csh_user FOREIGN KEY (changed_by) REFERENCES users (id) ON DELETE SET NULL,
    INDEX idx_csh_copy (copy_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- Seed Data: Sample Categories, Publishers, Authors & Shelves
-- ============================================================================
INSERT INTO categories (code, name_en, name_bn, description) VALUES
('CS', 'Computer Science & Engineering', 'কম্পিউটার বিজ্ঞান ও প্রকৌশল', 'Programming, Algorithms, AI, Systems'),
('LIT_BN', 'Bengali Literature', 'বাংলা সাহিত্য', 'Poetry, Novels, Plays, Essays'),
('MATH', 'Mathematics & Statistics', 'গণিত ও পরিসংখ্যান', 'Calculus, Linear Algebra, Probability'),
('HIST', 'Bangladesh History & Liberation War', 'বাংলাদেশের ইতিহাস ও মুক্তিযুদ্ধ', 'Historical records, 1971 Liberation War'),
('SCI', 'General Science & Physics', 'সাধারণ বিজ্ঞান ও পদার্থবিদ্যা', 'Physics, Chemistry, Biology');

INSERT INTO publishers (name, country) VALUES
('Somoy Prokashon', 'Bangladesh'),
('Ananya Prokashani', 'Bangladesh'),
('Oitijjho', 'Bangladesh'),
('Pearson Education', 'United States'),
('McGraw-Hill', 'United States');

INSERT INTO authors (name_en, name_bn, biography) VALUES
('Humayun Ahmed', 'হুমায়ূন আহমেদ', 'Renowned Bangladeshi author, dramatist, and filmmaker.'),
('Kazi Nazrul Islam', 'কাজী নজরুল ইসলাম', 'National poet of Bangladesh, rebel poet, and essayist.'),
('Robert C. Martin', 'রবার্ট সি. মার্টিন', 'Author of Clean Code and agile software development leader.'),
('Dr. Muhammad Zafar Iqbal', 'ড. মুহম্মদ জাফর ইকবাল', 'Famous author, scientist, and educator in Bangladesh.');

INSERT INTO shelves (code, section_name, rack_number, shelf_level, description) VALUES
('R1-S1', 'Science & Engineering', 'Rack 1', 'Level 1', 'Main Computer Science Rack'),
('R1-S2', 'Science & Engineering', 'Rack 1', 'Level 2', 'Mathematics & Algorithms'),
('R2-S1', 'Bengali Literature', 'Rack 2', 'Level 1', 'Bengali Novels & Fiction'),
('R3-S1', 'History & National Archive', 'Rack 3', 'Level 1', 'Bangladesh Liberation War');

-- Seed Sample Book Titles
INSERT INTO books (id, title_en, title_bn, subtitle, isbn_10, isbn_13, publisher_id, publication_year, edition, language, description) VALUES
(1, 'Clean Code: A Handbook of Agile Software Craftsmanship', 'ক্লিন কোড', 'A Handbook of Agile Software Craftsmanship', '0132350882', '9780132350884', 4, 2008, '1st Edition', 'English', 'Standard guide to writing clean, maintainable software.'),
(2, 'Shonkhonil Karagar', 'শঙ্খনীল কারাগার', NULL, NULL, '9789844120013', 1, 1973, '1st Edition', 'Bangla', 'Acclaimed classic novel by Humayun Ahmed portraying middle-class life in Bangladesh.'),
(3, 'Agni-Bina', 'অগ্নি-বীণা', NULL, NULL, '9789844120020', 2, 1922, 'Centenary Edition', 'Bangla', 'Masterpiece poetry collection by National Poet Kazi Nazrul Islam.');

INSERT INTO book_authors (book_id, author_id) VALUES
(1, 3),
(2, 1),
(3, 2);

INSERT INTO book_categories (book_id, category_id) VALUES
(1, 1),
(2, 2),
(3, 2);

-- Seed Sample Physical Copies
INSERT INTO book_copies (id, book_id, accession_number, barcode, shelf_id, acquisition_source, purchase_cost, acquired_date, condition_status, status) VALUES
(1, 1, 'ACC-2026-0001', 'BC100001', 1, 'PURCHASE', 1200.00, '2026-01-10', 'GOOD', 'AVAILABLE'),
(2, 1, 'ACC-2026-0002', 'BC100002', 1, 'PURCHASE', 1200.00, '2026-01-10', 'GOOD', 'AVAILABLE'),
(3, 2, 'ACC-2026-0003', 'BC100003', 3, 'DONATION', 250.00, '2026-01-15', 'NEW', 'AVAILABLE'),
(4, 2, 'ACC-2026-0004', 'BC100004', 3, 'PURCHASE', 250.00, '2026-01-15', 'GOOD', 'AVAILABLE'),
(5, 3, 'ACC-2026-0005', 'BC100005', 3, 'PURCHASE', 300.00, '2026-01-20', 'NEW', 'AVAILABLE');

INSERT INTO copy_status_history (copy_id, old_status, new_status, changed_by, reason) VALUES
(1, NULL, 'AVAILABLE', 1, 'Initial accession cataloging'),
(2, NULL, 'AVAILABLE', 1, 'Initial accession cataloging'),
(3, NULL, 'AVAILABLE', 1, 'Initial accession cataloging'),
(4, NULL, 'AVAILABLE', 1, 'Initial accession cataloging'),
(5, NULL, 'AVAILABLE', 1, 'Initial accession cataloging');

