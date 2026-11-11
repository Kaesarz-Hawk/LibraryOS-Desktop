# LibraryOS — Phased Implementation Roadmap & Tracker

> **Instructions for Antigravity:** Update the checkboxes (`[x]`) in this file as each milestone is implemented, verified with `mvn test`, and confirmed working.

## Phase 1 — Foundation & Authentication
- [x] Initialize `pom.xml` with Java 25 LTS, JavaFX 21, MySQL, HikariCP, Flyway, jBCrypt, SLF4J/Logback, ZXing, OpenPDF, POI, JUnit 5, Mockito.
- [x] Implement `AppConfig`, `DatabaseConfig` (HikariCP), and Flyway startup migration runner.
- [x] Create `V1__init_core_schema.sql` (`users`, `roles`, `permissions`, `user_roles`, `role_permissions`, `audit_logs`, `app_settings` + seed data).
- [x] Implement `PasswordHasher`, `SessionManager`, `PermissionGuard`, and `AuthService`.
- [x] Build JavaFX `theme.css`, `LoginView`, and `MainShellView` (permission-aware sidebar, top bar, content switcher).
- [x] Build initial `DashboardView` connected to real database count queries.
- [x] Write unit tests for `AuthService` and `PermissionGuard`.

## Phase 2 — Catalog, Physical Inventory & Members
- [x] Create `V2__catalog_and_inventory.sql` (`publishers`, `authors`, `categories`, `books`, `book_authors`, `book_categories`, `shelves`, `book_copies`, `copy_status_history`).
- [x] Create `V3__members_and_policies.sql` (`member_types`, `borrowing_policies`, `members` + seed default Bangladesh institutional policies).
- [x] Implement Repositories & Services for Books, Authors, Categories, Shelves, and Physical Book Copies.
- [x] Build **Books Catalog UI** (search, filter, add/edit/archive book title, Bangla/English metadata).
- [x] Build **Physical Copies / Inventory UI** (accession numbers, barcodes, status history, shelf assignment).
- [x] Implement Member Repository & Service (registration, BD phone validation, suspension, search).
- [x] Build **Members UI** (member directory, profile dialog, active loans/fines summary, CSV/Excel import).
- [x] Write unit and repository tests for Catalog, Copy uniqueness, and Member validation.

## Phase 3 — Circulation & Borrowing Policies
- [x] Create `V4__circulation_reservations_finance.sql` (`loans`, `loan_renewals`, `reservations`, `fines`, `fine_adjustments`, `payments`, `payment_allocations`).
- [x] Implement `TransactionManager` for atomic multi-DAO JDBC transactions.
- [x] Implement `CirculationService` (atomic issue, atomic return, renewal checks, holiday-aware due dates).
- [x] Build **Circulation Desk UI** (fast barcode/accession scan issue tab, return tab with live fine preview, active loans & overdue filter tab).
- [x] Build **Borrowing Policy Settings UI** per member type.
- [x] Write unit tests covering due-date calculation, double-issue prevention, and renewal rules.

## Phase 4 — Reservations & Finance (Fines & Payments)
- [x] Implement `FineCalculationService` (grace period, holiday exclusion, daily rate snapshot, max fine cap).
- [x] Implement `ReservationService` (FIFO queue, automatic copy allocation on return, hold expiry progression).
- [x] Implement `FineService` and `PaymentService` (partial payments, waivers with mandatory reason & approval, append-only BDT receipts).
- [x] Build **Reservations UI** and **Fines & Payments UI**.
- [x] Write comprehensive unit tests for `FineCalculationService`, partial payments, and FIFO reservation progression.

## Phase 5 — Barcodes, Receipts, Reports, Audit & Backup
- [x] Implement ZXing Barcode/QR generation service and printable label/card PDF generator (OpenPDF).
- [x] Implement PDF/Excel/CSV Report exports (Catalog, Overdue, Fine Collection, Circulation Trends).
- [x] Build **Barcode & QR Utility UI**, **Reports & Analytics UI**, **Audit Logs Viewer UI**, and **Backup/Restore UI**.

## Phase 6 — Digital Library & Acquisitions
- [x] Create `V5__digital_and_acquisitions.sql` (`digital_books`, `reading_progress`, `bookmarks`, `suppliers`, `acquisitions`, `notifications`, `reviews`).
- [x] Build **Digital Library UI** (managed PDF upload, in-app PDFBox reader, bookmarks) and **Suppliers & Acquisitions UI**.
- [x] Implement `DigitalLibraryService` (PDF header validation, SHA-256 calculation, PDFBox page rendering, reading progress tracking, bookmarks).
- [x] Implement `AcquisitionService` (supplier directory, purchase orders in BDT, atomic batch inventory receiving with automatic accession & barcode generation).
- [x] Implement `ReviewService` and `NotificationService` (ratings, reviews moderation, system event alerts).
- [x] Write comprehensive unit & integration tests (83 passing tests with 100% pass rate).

## Phase 7 — AI & Advanced Integrations
- [x] Implement `RecommendationService` (content-based category/author affinity and circulation/rating ranking).
- [x] Implement `MfsVerificationService` (bKash, Nagad, and Rocket TrxID format validation and duplicate payment prevention).
- [x] Implement `NotificationGatewayService` (Bangladeshi phone normalization `+8801...`, localized SMS and in-app alert dispatch).
- [x] Implement `AiAssistantService` (bilingual English/Bangla natural language queries, catalog search, borrowing policy explanations, and recommendations).
- [x] Build **AI Assistant UI** (`AiAssistantDialog` floating chat, prompt chips, and top-bar quick launcher).
- [x] Write comprehensive unit tests for AI, recommendations, MFS verification, and notification gateways (98 passing tests).

## Phase 8 — Release, Packaging & Enterprise Deployment
- [x] Configure production packaging strategy with `javafx-maven-plugin` and `AppLauncher` fat-jar runner.
- [x] Author comprehensive institutional IT admin and deployment guide (`docs/07_Enterprise_Deployment_and_Admin_Guide.md`).
- [x] Author staff operations handbook and user manual (`docs/08_User_Manual_and_Staff_Handbook.md`).
- [x] Verify complete system test suite: 98 unit and integration tests passing with 0 failures and 0 errors.