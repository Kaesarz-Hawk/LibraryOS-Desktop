# LibraryOS — Enterprise Deployment & Administration Guide

## 1. System Requirements & Architecture Overview

LibraryOS is a commercial-grade, multi-tier desktop library management system architected for educational institutions, public libraries, and research organizations in Bangladesh.

### Minimum Hardware Specifications
- **Client Workstation**: Intel Core i3 (7th Gen+) or AMD Ryzen 3, 4 GB RAM (8 GB recommended), 500 MB free disk space for application files.
- **Dedicated Database Server** (Optional for multi-workstation setups): 4 vCPU, 8 GB RAM, 50 GB SSD storage running Windows Server or Ubuntu 22.04 LTS.

### Software Prerequisites
- **Java Runtime**: Eclipse Temurin or Oracle JDK / JRE 25 LTS (64-bit).
- **Relational Database**: MySQL Server 8.0 or 8.4 LTS (or MariaDB 10.11+).
- **Operating System**: Windows 10/11 (64-bit), Windows Server 2019/2022, or Linux (X11/Wayland).

---

## 2. Production MySQL 8 Configuration

### 2.1 UTF-8 Bangla (`বাংলা`) Collation Configuration
To ensure full fidelity for Bengali characters (`ইউনিকোড বাংলা`) across all book metadata, author names, and member details, configure your MySQL `my.ini` (Windows) or `my.cnf` (Linux):

```ini
[mysqld]
# Default character set and collation for Bangladesh localization
character-set-server = utf8mb4
collation-server     = utf8mb4_unicode_ci

# Transaction safety and performance
default-storage-engine = InnoDB
innodb_buffer_pool_size = 1G
innodb_log_file_size = 256M
innodb_flush_log_at_trx_commit = 1

# Connection pooling optimization for HikariCP
max_connections = 150
wait_timeout = 28800
interactive_timeout = 28800
```

Restart the MySQL service after updating configuration.

### 2.2 Database & Service Account Provisioning
Connect to MySQL as `root` and execute the following commands to provision the database and dedicated service user:

```sql
CREATE DATABASE IF NOT EXISTS libraryos_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER 'libraryos_user'@'%' IDENTIFIED BY 'StrongPass_2026!#BD';

GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, REFERENCES, INDEX, ALTER, LOCK TABLES 
ON libraryos_db.* TO 'libraryos_user'@'%';

FLUSH PRIVILEGES;
```

---

## 3. Application Configuration & Environment Variables

Configure connection settings either via environment variables or an external properties file located at `~/.libraryos/config.properties`:

```properties
# Database Connectivity
db.host=127.0.0.1
db.port=3306
db.name=libraryos_db
db.username=libraryos_user
db.password=StrongPass_2026!#BD

# HikariCP Connection Pool Tuning
db.pool.maximum_pool_size=10
db.pool.minimum_idle=3
db.pool.idle_timeout_ms=30000
db.pool.max_lifetime_ms=1800000
db.pool.connection_timeout_ms=10000

# Storage Directories
storage.base_dir=${user.home}/.libraryos/storage
storage.backups_dir=${user.home}/.libraryos/backups

# Localization & Institution Profile
institution.name=Dhaka Central Institutional Library
institution.currency=BDT
institution.currency_symbol=৳
institution.weekly_holiday=FRIDAY
```

---

## 4. Flyway Database Migrations & Versioning

All schema evolution in LibraryOS is managed strictly through Flyway versioned migrations. Upon application launch, Flyway executes automatically and applies pending migrations sequentially:

| Migration File | Module Description |
| :--- | :--- |
| `V1__init_core_schema.sql` | Identity & Access (`users`, `roles`, `permissions`, `audit_logs`, `app_settings` + seed admin). |
| `V2__catalog_and_inventory.sql` | Books, physical copies, barcodes, shelves, authors, categories, publishers. |
| `V3__members_and_policies.sql` | Member directory, membership types, borrowing policies. |
| `V4__circulation_reservations_finance.sql` | Circulation loans, renewals, FIFO hold queues, fines, waivers, payments. |
| `V5__digital_and_acquisitions.sql` | Digital library PDFs, reading progress, bookmarks, suppliers, acquisitions, reviews. |

---

## 5. Automated Backups & Disaster Recovery

### 5.1 On-Demand SQL Snapshots via UI
Navigate to **System Administration > Backup & Restore** and click **"Create Instant Backup / ব্যাকআপ নিন"**. A complete SQL snapshot is saved in `~/.libraryos/backups/`.

### 5.2 Scheduled Automated Database Backup (Windows Task Scheduler)
Create a batch script `C:\LibraryOS\backup_daily.bat`:

```bat
@echo off
set TIMESTAMP=%date:~10,4%%date:~4,2%%date:~7,2%_%time:~0,2%%time:~3,2%%time:~6,2%
set TIMESTAMP=%TIMESTAMP: =0%
set BACKUP_FILE=C:\Users\Administrator\.libraryos\backups\libraryos_auto_%TIMESTAMP%.sql

"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqldump.exe" -u libraryos_user -pStrongPass_2026!#BD --single-transaction --routines --triggers libraryos_db > "%BACKUP_FILE%"
```

Schedule this script in Windows Task Scheduler to run daily at midnight.

### 5.3 Point-In-Time Restoration Procedure
1. From Admin Desk UI: Select backup snapshot from table and click **"Restore Database / রিস্টোর"**.
2. Or via CLI:
   ```bash
   mysql -u libraryos_user -p libraryos_db < ~/.libraryos/backups/libraryos_backup_20261104.sql
   ```

---

## 6. Initial Super Administrator Account

On initial installation, LibraryOS provisions a secured default administrator account:
- **Username**: `admin`
- **Initial Password**: `admin123` *(Must be changed immediately upon first login via Admin Desk)*
- **Primary Role**: `SUPER_ADMIN` (Possesses all granular system permissions).
