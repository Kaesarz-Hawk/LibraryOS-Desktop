# LibraryOS — BUBT CSE Edition

[![Java](https://img.shields.io/badge/Java-21%20LTS-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![JavaFX](https://img.shields.io/badge/JavaFX-21-4285F4?style=for-the-badge&logo=java&logoColor=white)](https://openjfx.io/)
[![MySQL](https://img.shields.io/badge/MySQL-8.x-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Flyway](https://img.shields.io/badge/Flyway-Migrations-CC0202?style=for-the-badge&logo=flyway&logoColor=white)](https://flywaydb.org/)
[![Maven](https://img.shields.io/badge/Maven-Build-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)](https://maven.apache.org/)

**LibraryOS (BUBT CSE Edition)** is an enterprise-grade desktop Library Management System (LMS) engineered for the **Bangladesh University of Business and Technology (BUBT) Central Library**. Built with modern Java 21, JavaFX, and MySQL, the application bridges core library workflows (atomic circulation, multi-copy inventory, fines, reservations) with a modern Fluent/macOS Dark-Plum Glass interface and an integrated course-aware AI Assistant.

---

## 🌟 Key Features

### 1. Robust Core Circulation & Inventory
- **Atomic Transactions:** Zero-race-condition issuing and returning powered by strict MySQL ACID transactions.
- **Copy vs. Title Model:** Distinguishes catalog bibliographic titles from physical inventory (`book_copies`) tracking individual accession numbers, barcodes, conditions, and shelf locations.
- **Fair Queue Reservations:** FIFO reservation queues that progress upon item returns or reservation expirations.
- **Deterministic Fines:** Configurable overdue fine policies with partial payments, waivers, and auditable adjustments in BDT (৳).

### 2. Tailored for BUBT & Bangladesh Localization
- **Curriculum-Mapped Dataset:** Pre-loaded with 300 verified books across 3 datasets, including BUBT CSE courses (e.g., CSE 111, CSE 231) and bilingual Bangla-English metadata.
- **Native Typography:** Engineered with font fallbacks (`Segoe UI`, `Hind Siliguri`, `Nirmala UI`) ensuring Bangla vowel marks (কার/ফলা) never clip.
- **Academic Identification:** Native support for student IDs, staff IDs, department tags, intake/session tracking, and local holiday/Ramadan scheduling.

### 3. Desktop Experience & Theme Engine
- **Dark-Plum Glass Aesthetic:** Deep obsidian-plum backgrounds (`#130F1D`), radial ambient violet glow (`#2A1842`), and specular borders.
- **Optimized Light Mode:** High-contrast purple row highlights (`#EDE9FE`) and readable dark text (`#1E1B4B`).
- **Productivity Shortcuts:** Top global quick search (`Ctrl + K`), empty-state diagnostics, and zero-freeze asynchronous UI background execution.

### 4. 10x AI Assistant & Audit Trail
- **Course-Aware AI Advisor:** Real-time stock queries, syllabus-mapped book lookups, and formatted Markdown answers.
- **Immutable Security:** BCrypt password hashing, Granular RBAC (`Super Admin`, `Library Admin`, `Librarian`, `Student`), and non-repudiable audit logs.

---

## 🏗️ Architecture & Tech Stack

The project adheres to a **Strict Layered Modular Architecture**:

```
com.libraryos
├── app          # Application entry point & AppContext
├── config       # DB connection pool (HikariCP), Flyway & Storage configuration
├── core         # Security (BCrypt), exceptions, validators & utilities
├── model        # Domain entities (Books, Copies, Loans, Fines, Members)
├── repository   # Parameterized JDBC DAO persistence layer
├── service      # Business logic & transactional boundaries
├── controller   # JavaFX controllers & UI event handlers
└── resources    # FXML layouts, CSS design tokens, icons & fonts
```

- **Runtime:** Java 21 LTS
- **UI Toolkit:** JavaFX + Modular JavaFX CSS
- **Database:** MySQL 8.x with Flyway versioned migrations
- **Build System:** Apache Maven

---

## 📁 Pre-Configured Book Catalog Datasets

The repository includes standardized, pipe-delimited (`|`) book metadata located under `data/books/`:
- `books_part1_001_to_100.txt`: 100 Popular Bangla & English Literature Books
- `books_part2_101_to_200.txt`: 100 World Literature & General Science Books
- `books_part3_201_to_300.txt`: 100 BUBT CSE Curriculum-Mapped Textbooks

---

## 🚀 Getting Started

### Prerequisites
- **JDK 21** or later installed and configured on your `PATH`.
- **MySQL Server 8.x** running locally or across the network.
- **Apache Maven** (or use the built-in IDE wrapper).

### 1. Database Setup
Create an empty MySQL database:
```sql
CREATE DATABASE libraryos_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Update your database credentials in `config/database.properties` or your environment variables:
```properties
db.url=jdbc:mysql://localhost:3306/libraryos_db?useSSL=false&serverTimezone=UTC
db.user=your_db_username
db.password=your_db_password
```
*(Flyway will automatically execute migrations and seed the initial schema upon launch).*

### 2. Build the Project
Open your terminal in the project root:
```bash
mvn clean compile
```

### 3. Run the Application
You can run it via Maven:
```bash
mvn javafx:run
```
Or execute the Windows launcher directly:
```cmd
run.bat
```

---

## 🛡️ Security & Integrity Highlights
- **Prepared Statements:** 100% of queries use parameterized statements to prevent SQL injection.
- **Separation of Concerns:** Business validation is fully enforced in the service layer, independent of UI controllers.
- **File Integrity:** Digital library resources and book covers are stored outside source directories with validated relative path handling.

---

## 👨‍💻 Developer & Institutional Credits

- **Developer:** Kawsar Hoque (Dept. of Computer Science & Engineering, BUBT)
- **Institution:** Bangladesh University of Business and Technology (BUBT) Central Library
- **Super Admin Account:** Default local administrative profile `Kawsar (Super Admin)`