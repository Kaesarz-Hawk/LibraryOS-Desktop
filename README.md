# LibraryOS — BUBT CSE Edition

[![Java](https://img.shields.io/badge/Java-21%20LTS-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![JavaFX](https://img.shields.io/badge/JavaFX-21-4285F4?style=for-the-badge&logo=java&logoColor=white)](https://openjfx.io/)
[![MySQL](https://img.shields.io/badge/MySQL-8.x-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Flyway](https://img.shields.io/badge/Flyway-Migrations-CC0202?style=for-the-badge&logo=flyway&logoColor=white)](https://flywaydb.org/)
[![Maven](https://img.shields.io/badge/Maven-Build-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![Git LFS](https://img.shields.io/badge/Git%20LFS-216MB%2B%20Binaries-F05032?style=for-the-badge&logo=git&logoColor=white)](https://git-lfs.com/)
[![Platform](https://img.shields.io/badge/Platform-Windows%2010%2F11-0078D6?style=for-the-badge&logo=windows&logoColor=white)](https://www.microsoft.com/windows)

**LibraryOS (BUBT CSE Edition)** is an enterprise-grade desktop Library Management System (LMS) engineered for the **Bangladesh University of Business and Technology (BUBT) Central Library / বিইউবিটি কেন্দ্রীয় গ্রন্থাগার**. Built with Java 21 LTS, JavaFX, and MySQL 8.x, the platform combines ACID-compliant circulation, physical copy tracking, automated fine calculations, and a BUBT CSE course-aware AI Assistant inside a custom Microsoft Fluent x Apple macOS Dark-Plum Glass interface.

---

## 📑 Table of Contents

1. [Key Features](#-key-features)
2. [System Requirements](#-system-requirements)
3. [How to Download (Important Git LFS Guide)](#-how-to-download-important-git-lfs-guide)
4. [Database Setup & Configuration](#-database-setup--configuration)
5. [How to Run the Application](#-how-to-run-the-application)
6. [Pre-Configured 300-Book Catalog](#-pre-configured-300-book-catalog)
7. [Project Architecture & Directory Structure](#-project-architecture--directory-structure)
8. [Role-Based Access Control (RBAC) & Security](#-role-based-access-control-rbac--security)
9. [Developer & Institutional Credits](#-developer--institutional-credits)

---

## 🌟 Key Features

### 1. Core Circulation & Multi-Copy Inventory

- **Title vs. Physical Copy Separation:** Separates bibliographic metadata (`books`) from individual physical copies (`book_copies`), tracking unique accession numbers, barcodes, shelf locations, acquisition costs, and physical condition (`AVAILABLE`, `ISSUED`, `RESERVED`, `LOST`, `DAMAGED`, `UNDER_MAINTENANCE`).
- **Atomic Circulation Workflows:** Issue, return, and renewal operations execute inside strict MySQL transactions to prevent double-issuing and race conditions.
- **FIFO Reservation Queues:** Automated waiting lists with configurable holding expirations that advance to the next member when copies are returned.
- **Deterministic Fine & Payment Engine:** Calculates overdue fines based on configurable grace periods and daily rates in BDT (`৳`). Supports partial payments, authorized fine waivers, and append-only payment ledgers (Cash, bKash, Nagad, Rocket, Bank Transfer).

### 2. Tailored for BUBT & Bangladesh Localization

- **Curriculum-Mapped Dataset:** Ships with 300 verified books across 3 datasets, mapped directly to BUBT CSE course codes (`CSE 111`, `CSE 231`, etc.) with bilingual Bangla and English metadata.
- **Zero-Clipping Bangla Typography:** Configured with `Segoe UI`, `Hind Siliguri`, and `Nirmala UI` font stacks with custom line heights so Bangla vowel marks (কার/ফলা) render cleanly.
- **Institutional Workflows:** Supports Student/Staff IDs, department/program tags, semester/intake tracking, BD phone validation, and configurable Friday/Ramadan library schedules.

### 3. Desktop UI & Theme Engine

- **2-Mode Theme Switcher (`☀️ Light` / `🌙 Dark`):**
  - **Dark-Plum Glass Mode:** Deep matte obsidian-plum base (`#130F1D`), radial glow (`#2A1842`), translucent charcoal-violet containers (`#1C162B`), and glowing violet focus rings (`#A855F7`).
  - **High-Contrast Light Mode:** Soft purple table selection highlights (`#EDE9FE`) with crisp dark typography (`#1E1B4B`).
- **Desktop Ergonomics:** Global quick search (`Ctrl + K`), 280px wide bilingual sidebar, high-DPI scaled typography, and non-blocking background task execution.

### 4. 10x AI Assistant & Operations

- **BUBT CSE Course-Aware AI Advisor:** Answers real-time stock queries, recommends textbooks by course code, checks shelf locations, and renders clean Markdown responses.
- **Barcode, QR & Reporting:** Generates copy barcodes and member QR cards (ZXing) and exports operational/financial reports to PDF (OpenPDF), Excel (Apache POI), and CSV.

---

## 💻 System Requirements

| Component            | Minimum Requirement                   | Recommended                          |
| :------------------- | :------------------------------------ | :----------------------------------- |
| **Operating System** | Windows 10 (64-bit)                   | Windows 11 (64-bit)                  |
| **Processor**        | Dual-Core 2.0 GHz                     | Quad-Core / AMD Ryzen 5 or Intel i5+ |
| **Memory (RAM)**     | 4 GB RAM                              | 8 GB+ RAM                            |
| **Disk Space**       | 600 MB free space                     | 1 GB SSD space                       |
| **Java Runtime**     | JDK 21 LTS _(Bundled inside `dist/`)_ | JDK 21 LTS or JDK 25 LTS             |
| **Database**         | MySQL Server 8.0+                     | MySQL Server 8.x (InnoDB, `utf8mb4`) |
| **Version Control**  | Git + **Git LFS**                     | Git 2.40+ with Git LFS enabled       |

---

## 📥 How to Download (Important Git LFS Guide)

> ⚠️ **CRITICAL DOWNLOAD NOTICE — DO NOT USE "DOWNLOAD ZIP"**  
> This repository stores the complete standalone Windows distribution (`dist/`), custom runtime image (`112 MB+`), `.jar` libraries, and `LibraryOS.exe` (totaling **216 MB+ of binary assets**) using **Git Large File Storage (Git LFS)**.
>
> Clicking GitHub's green **Code -> Download ZIP** button will **NOT** download the actual executables—it will only download a `~842 KB` archive filled with tiny text pointer files, and `LibraryOS.exe` will not run.

Follow the steps below to download the full working project (~500 MB unpacked):

### Step 1: Install Git and Git LFS

1. Download and install **[Git for Windows](https://git-scm.com/download/win)** (Git LFS is included by default in the installer).
2. Open **Command Prompt (`cmd`)**, **PowerShell**, or **Git Bash** and initialize Git LFS once on your machine:
   ```cmd
   git lfs install
   ```

### Step 2: Clone the Repository with All Binaries

Run the following commands to clone the project and pull all large runtime binaries:

```cmd
git clone [https://github.com/Kaesarz-Hawk/LibraryOS-Desktop.git](https://github.com/Kaesarz-Hawk/LibraryOS-Desktop.git)
cd LibraryOS-Desktop
git lfs pull
```

### Step 3: Verify Your Download

Check that the binaries were pulled properly:

- Verify that `LibraryOS.exe` in the root folder and `dist/LibraryOS/runtime/lib/modules` (~112 MB) exist as full-size files rather than 1 KB text files.
- If any binary file is only 1 KB, run `git lfs checkout` inside the project folder.

---

## 🗄️ Database Setup & Configuration

### Step 1: Create the MySQL Database

Log in to your local MySQL Server (via MySQL Workbench, CLI, or XAMPP/Laragon) and run:

```sql
CREATE DATABASE IF NOT EXISTS libraryos_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

### Step 2: Configure Database Credentials

1. Locate `config/application.properties` (or copy `config/application.example.properties` to `config/application.properties`).
2. Update your MySQL connection settings:
   ```properties
   db.url=jdbc:mysql://localhost:3306/libraryos_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8
   db.username=root
   db.password=your_mysql_password
   ```

### Step 3: Automatic Schema & Data Seeding (Flyway)

You do **not** need to import SQL files manually. On first launch, **Flyway** automatically executes migrations `V1` through `V7` located in `src/main/resources/db/migration/`:

- `V1__init_core_schema.sql` — Users, roles, permissions, settings, and audit logs
- `V2__catalog_and_inventory.sql` — Books, authors, categories, shelves, and physical copies
- `V3__members_and_policies.sql` — Members and configurable borrowing policies
- `V4__circulation_reservations_finance.sql` — Loans, renewals, reservations, fines, and payments
- `V5__digital_and_acquisitions.sql` — Digital PDF library, suppliers, and acquisitions
- `V6__seed_300_master_books.sql` — Seeds the complete 300-book bilingual catalog
- `V7__seed_friends_circulation_finance_acquisitions.sql` — Seeds realistic member, circulation, fine, and procurement records

---

## 🚀 How to Run the Application

Choose any of the 3 methods below after configuring your MySQL database:

### Method 1: Native Windows Executable (Fastest — No Maven Needed)

Double-click **`LibraryOS.exe`** (or **`LibraryOS - Shortcut.lnk`**) in the root folder, or run from Command Prompt:

```cmd
LibraryOS.exe
```

### Method 2: Windows Batch Launcher

Run the included batch script from the project root:

```cmd
run.bat
```

### Method 3: Build & Run from Source via Maven (For Developers)

If you modify the Java source code or CSS themes, compile and launch using Maven:

```cmd
mvn clean compile
mvn javafx:run
```

To run the automated unit and integration test suite (JUnit 5 + Mockito):

```cmd
mvn test
```

---

## 📚 Pre-Configured 300-Book Catalog

Standardized, pipe-delimited (`|`) datasets are included in `data/books/` for bulk import or re-seeding:

- **`books_part1_001_to_100.txt`** — _IDs 1–100:_ 100 Popular Bangla & English Literature Books (e.g., Humayun Ahmed / হুমায়ূন আহমেদ, Rabindranath Tagore, Muhammed Zafar Iqbal).
- **`books_part2_101_to_200.txt`** — _IDs 101–200:_ Global Top 100 World Literature & Science Books.
- **`books_part3_201_to_300.txt`** — _IDs 201–300:_ BUBT CSE Curriculum-Mapped Top 100 Textbooks (Data Structures, Algorithms, Operating Systems, Database Systems, Artificial Intelligence, Software Engineering).

Each record conforms to the 15-field schema: Book ID, English Title, Bangla Title, Subtitle/Course Code, ISBN-13, ISBN-10, Publisher, Year, Edition, Language, Bilingual Authors, Bilingual Categories, Summary, Price (BDT), and Stock Count.

---

## 🏗️ Project Architecture & Directory Structure

LibraryOS follows a **Strict Layered Modular Architecture** where UI controllers never execute direct SQL and all business rules live in transactional services:

```text
LibraryOS-Desktop/
├── .github/                  # Java modernization hooks & workflow configurations
├── config/                   # Externalized application & database configuration
├── data/books/               # 300 standardized bilingual book dataset files (.txt)
├── dist/LibraryOS/           # Standalone Windows runtime image & bundled binaries (LFS)
├── docs/                     # Architecture, ERD, RBAC matrix, and user manuals
├── src/
│   ├── main/
│   │   ├── csharp/           # Native Windows executable launcher source (Launcher.cs)
│   │   ├── java/com/libraryos/
│   │   │   ├── app/          # LibraryOSApplication, AppLauncher & DI AppContext
│   │   │   ├── config/       # DatabaseConfig (HikariCP), AppConfig & FileStorageConfig
│   │   │   ├── controller/   # JavaFX UI Controllers (Dashboard, Catalog, Circulation, etc.)
│   │   │   ├── core/         # BCrypt security, PermissionGuard, TransactionManager & utils
│   │   │   ├── dto/          # Data Transfer Objects for KPI metrics & reporting
│   │   │   ├── model/        # Domain entities (Catalog, Inventory, Circulation, Finance, RBAC)
│   │   │   ├── repository/   # DAO interfaces & parameterized JDBC implementations
│   │   │   ├── service/      # Transactional business logic, AI Assistant & PDF/Excel exports
│   │   │   └── view/         # Modular JavaFX Views, Dialogs & ThemeManager
│   │   └── resources/
│   │       ├── css/          # Light Mode (theme.css) & Dark-Plum Glass (theme-dark.css)
│   │       ├── db/migration/ # Flyway SQL schema & seed migrations (V1 to V7)
│   │       └── logback.xml   # SLF4J / Logback diagnostic logging configuration
│   └── test/                 # JUnit 5 & Mockito unit and integration test suites
├── target/                   # Compiled classes, Surefire test reports & packaged JARs (LFS)
├── .gitattributes            # Git LFS binary tracking rules
├── .gitignore                # Git tracking configuration
├── LibraryOS.exe             # Native Windows desktop launcher
├── pom.xml                   # Apache Maven build & dependency specification
├── README.md                 # Project documentation
└── run.bat                   # Quick-start Windows batch launcher
```

---

## 🛡️ Role-Based Access Control (RBAC) & Security

- **5 Built-In Roles:** `Super Admin`, `Library Admin`, `Librarian`, `Assistant Librarian`, and `Student / Member`.
- **Service-Layer Authorization:** Every sensitive operation (`BOOK_CREATE`, `LOAN_ISSUE`, `FINE_WAIVE`, `PAYMENT_RECORD`, `SETTINGS_MANAGE`, `BACKUP_MANAGE`) is verified by `PermissionGuard` in the service layer.
- **Cryptographic Security:** Passwords are hashed using **BCrypt** (`PasswordHasher`), and 100% of database queries use parameterized `PreparedStatement` calls to prevent SQL injection.
- **Immutable Audit Trail:** Critical actions (`LOGIN_SUCCESS`, `SYSTEM_INITIALIZED`, loan transactions, fine waivers, policy edits) are permanently logged in `audit_logs` with actor IDs and timestamps.

---

## 👨‍💻 Developer & Institutional Credits

- **Developer:** **Kawsar Hoque** (`Kaesarz-Hawk`) — Department of Computer Science & Engineering (CSE), BUBT
- **Institution:** **Bangladesh University of Business and Technology (BUBT) Central Library / বিইউবিটি কেন্দ্রীয় গ্রন্থাগার**, Dhaka, Bangladesh
- **Default Active Profile:** `Kawsar (Super Admin)`
