# LibraryOS — Roles & Permissions Matrix (RBAC)

## 1. Security Enforcement Rules
1. **Dual-Layer Enforcement:** UI screens must hide or disable unauthorized navigation items and buttons, **AND** every sensitive `Service` method must invoke `PermissionGuard.requirePermission(PermissionCode)` before executing business logic.
2. **Member Isolation:** A user logged in with the `MEMBER` role may only query or mutate records where `member_id == currentSession.getMemberId()`. Any attempt to view another member's loans, reservations, or fines must throw an `UnauthorizedException`.
3. **No Self-Privilege Escalation:** Users cannot edit their own role assignments or grant permissions they do not possess.

---

## 2. Permission Matrix by Role

| Permission Code | Module | Super Admin | Library Admin | Librarian | Assistant Librarian | Student / Member |
| :--- | :--- | :---: | :---: | :---: | :---: | :---: |
| `DASHBOARD_STAFF_VIEW` | Dashboard | ✅ | ✅ | ✅ | ✅ | ❌ |
| `DASHBOARD_MEMBER_VIEW` | Dashboard | ❌ | ❌ | ❌ | ❌ | ✅ |
| `BOOK_VIEW` | Catalog | ✅ | ✅ | ✅ | ✅ | ✅ |
| `BOOK_CREATE` | Catalog | ✅ | ✅ | ✅ | ❌ | ❌ |
| `BOOK_UPDATE` | Catalog | ✅ | ✅ | ✅ | ❌ | ❌ |
| `BOOK_ARCHIVE` | Catalog | ✅ | ✅ | ❌ | ❌ | ❌ |
| `COPY_MANAGE` | Inventory | ✅ | ✅ | ✅ | ✅ | ❌ |
| `MEMBER_VIEW` | Members | ✅ | ✅ | ✅ | ✅ | Own Only |
| `MEMBER_CREATE` | Members | ✅ | ✅ | ✅ | ❌ | ❌ |
| `MEMBER_UPDATE` | Members | ✅ | ✅ | ✅ | ❌ | Own Profile |
| `MEMBER_SUSPEND` | Members | ✅ | ✅ | ❌ | ❌ | ❌ |
| `LOAN_ISSUE` | Circulation | ✅ | ✅ | ✅ | ✅ | ❌ |
| `LOAN_RETURN` | Circulation | ✅ | ✅ | ✅ | ✅ | ❌ |
| `LOAN_RENEW` | Circulation | ✅ | ✅ | ✅ | ✅ | ❌ |
| `RESERVATION_SELF` | Reservations | ❌ | ❌ | ❌ | ❌ | ✅ |
| `RESERVATION_MANAGE` | Reservations | ✅ | ✅ | ✅ | ✅ | ❌ |
| `FINE_VIEW` | Finance | ✅ | ✅ | ✅ | ✅ | Own Only |
| `FINE_WAIVE` | Finance | ✅ | ✅ | ❌ | ❌ | ❌ |
| `PAYMENT_RECORD` | Finance | ✅ | ✅ | ✅ | ❌ | ❌ |
| `BARCODE_GENERATE` | Barcode/QR | ✅ | ✅ | ✅ | ✅ | ❌ |
| `DIGITAL_READ` | Digital Library | ✅ | ✅ | ✅ | ✅ | ✅ |
| `DIGITAL_MANAGE` | Digital Library | ✅ | ✅ | ✅ | ❌ | ❌ |
| `SUPPLIER_MANAGE` | Acquisitions | ✅ | ✅ | ❌ | ❌ | ❌ |
| `REPORT_VIEW` | Reports | ✅ | ✅ | ✅ | ❌ | ❌ |
| `REPORT_EXPORT` | Reports | ✅ | ✅ | ✅ | ❌ | ❌ |
| `USER_MANAGE` | Administration | ✅ | ✅ (Staff/Mem) | ❌ | ❌ | ❌ |
| `ROLE_MANAGE` | Administration | ✅ | ❌ | ❌ | ❌ | ❌ |
| `SETTINGS_MANAGE` | Administration | ✅ | ✅ (Policies) | ❌ | ❌ | ❌ |
| `AUDIT_VIEW` | Administration | ✅ | ✅ | ❌ | ❌ | ❌ |
| `BACKUP_MANAGE` | Administration | ✅ | ❌ | ❌ | ❌ | ❌ |