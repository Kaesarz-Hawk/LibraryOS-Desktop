# LibraryOS — Architecture & Technology Stack Specification

## 1. Technology Stack Baseline

| Component | Technology | Version / Standard | Purpose |
| :--- | :--- | :--- | :--- |
| **Language** | Java | 25 LTS | Core application and business logic |
| **Desktop UI** | JavaFX | 21.x | Views, FXML/programmatic layouts, charts, dialogs |
| **Styling** | JavaFX CSS | CSS 2.1 / JavaFX | Unified design tokens, dark/light themes, typography |
| **Database** | MySQL | 8.x (`InnoDB`, `utf8mb4`) | Relational persistence with full Bangla Unicode support |
| **DB Connectivity** | JDBC + HikariCP | Latest stable | Connection pooling and parameterized SQL execution |
| **Schema Migrations** | Flyway | 10.x | Versioned, repeatable SQL schema migrations |
| **Build System** | Apache Maven | 3.9+ | Dependency management, build lifecycle, packaging |
| **Password Security** | jBCrypt / BCrypt | Cost factor 12 | Secure salted password hashing |
| **Barcode & QR** | Google ZXing | 3.5.x | Barcode (Code-128) & QR generation and decoding |
| **PDF Generation** | OpenPDF | 1.3.x / 2.0.x | Printable circulation receipts, member cards, reports |
| **PDF Reader** | Apache PDFBox | 3.0.x | Digital library PDF metadata and in-app rendering |
| **Excel / CSV** | Apache POI | 5.2.x | Bulk catalog/member import and report export |
| **Logging** | SLF4J + Logback | 2.0.x / 1.5.x | Structured application and error diagnostics |
| **Testing** | JUnit 5 + Mockito | 5.10.x | Unit, service, and repository integration testing |

---

## 2. Layered Modular Architecture

LibraryOS strictly follows a **4-tier layered architecture**. Dependencies flow downwards only:

`Presentation (JavaFX) -> Service (Business Logic & Auth) -> Repository/DAO (JDBC) -> Database (MySQL)`

### 2.1 Layer Responsibilities
1. **Presentation Layer (`controller`, `view`):**
   - Renders UI state, binds form inputs, triggers background tasks, and displays user-friendly validation or error dialogs.
   - **Prohibited:** Direct SQL queries, `Connection` objects, password hashing, or complex fine/loan calculations.
2. **Service Layer (`service`):**
   - Enforces all domain business rules (loan limits, fine calculations, reservation queues).
   - Enforces Role-Based Access Control (`PermissionGuard`) before executing sensitive operations.
   - Manages JDBC transaction boundaries (`commit` / `rollback`) for multi-step operations.
   - **Prohibited:** Importing `javafx.*` UI classes.
3. **Repository / DAO Layer (`repository`):**
   - Executes parameterized SQL queries using `PreparedStatement`.
   - Maps `ResultSet` rows to immutable or clean domain `model` entities.
   - Uses `try-with-resources` for all `PreparedStatement` and `ResultSet` objects.
4. **Core & Config Layer (`config`, `core`, `model`, `dto`):**
   - Holds application configuration, HikariCP data source setup, custom domain exceptions, validation utilities, and session context.

---

## 3. Standard Package Structure

```text
com.libraryos
├── app
│   ├── AppLauncher.java               # Non-JavaFX main wrapper for fat-jar/module launch
│   ├── LibraryOSApplication.java      # JavaFX Application subclass
│   └── AppContext.java                # Dependency wiring & current session holder
├── config
│   ├── AppConfig.java                 # Loads external config / env variables
│   ├── DatabaseConfig.java            # HikariDataSource & Flyway runner
│   └── FileStorageConfig.java         # Managed local storage paths for covers/PDFs
├── core
│   ├── exception                      # DomainException, UnauthorizedException, ValidationException
│   ├── security                       # PasswordHasher, SessionManager, PermissionGuard
│   ├── transaction                    # TransactionManager for atomic JDBC operations
│   ├── util                           # DateTimeUtil, CurrencyFormatUtil (BDT), BarcodeUtil
│   └── validation                     # Input validators (ISBN, BD phone numbers, emails)
├── model
│   ├── user                           # User, Role, Permission
│   ├── catalog                        # Book, Author, Category, Publisher
│   ├── inventory                      # BookCopy, CopyStatus, ShelfLocation, Supplier, Acquisition
│   ├── member                         # Member, MemberType, MemberStatus
│   ├── circulation                    # Loan, LoanRenewal, BorrowingPolicy
│   ├── reservation                    # Reservation, ReservationStatus
│   ├── finance                        # Fine, Payment, PaymentAllocation, FineWaiver
│   ├── digital                        # DigitalBook, Bookmark, ReadingProgress
│   └── audit                          # AuditLog, Notification
├── dto                                # Data transfer objects for complex views & reports
├── repository                         # JDBC Repositories grouped by domain
├── service                            # Business services grouped by domain
├── controller                         # JavaFX Controllers grouped by module
└── view                               # Reusable JavaFX custom components & dialog builders
```

---

## 4. Configuration & Secrets Policy

- **Zero Hardcoded Credentials:** Never commit MySQL `root` passwords or production secrets in `.java` or tracked `.properties` files.
- Provide a tracked `config/application.example.properties` template and load runtime overrides from environment variables (`LIBRARYOS_DB_URL`, `LIBRARYOS_DB_USER`, `LIBRARYOS_DB_PASS`) or a local untracked `config/application.properties` file.
- **File Storage:** Store uploaded book covers and digital PDFs in a managed user-data directory (e.g., `${user.home}/.libraryos/storage/`), keeping only relative paths and SHA-256 checksums in MySQL.