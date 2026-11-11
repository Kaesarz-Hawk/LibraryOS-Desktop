# LibraryOS — Core Business Rules & Critical Workflows

## 1. Atomic Book Issue Workflow (`CirculationService.issueBook`)
Must execute inside a single JDBC transaction (`autoCommit = false`):
1. **Permission Check:** Require `LOAN_ISSUE`.
2. **Member Validation:**
   - Load `Member` and verify `status == ACTIVE` and `membership_expiry_date >= LocalDate.now()`.
   - Load `BorrowingPolicy` for the member's `member_type_id`.
   - Count active loans (`status == ACTIVE`) for the member; reject if `activeLoans >= policy.maxActiveLoans`.
   - Sum unpaid fine balances for the member; reject if `unpaidTotal > policy.maxUnpaidFineLimitBdt`.
3. **Copy Locking & Validation:**
   - Execute `SELECT ... FROM book_copies WHERE id = ? FOR UPDATE` to prevent race conditions.
   - Verify no active loan exists for `copy_id`.
   - If `copy.status == AVAILABLE`, verify there isn't an active `READY_FOR_PICKUP` reservation for another member.
   - If `copy.status == RESERVED`, verify the active `READY_FOR_PICKUP` reservation belongs to this exact member (and mark that reservation `FULFILLED`).
   - Reject if copy is `ISSUED`, `LOST`, `DAMAGED`, `UNDER_MAINTENANCE`, or `WITHDRAWN`.
4. **Due Date Calculation:**
   - `dueDate = LocalDate.now().plusDays(policy.loanDurationDays)`.
   - If `dueDate` falls on a configured library weekly holiday (e.g., Friday in Bangladesh) or special holiday, advance `dueDate` to the next open library day.
5. **State Persistence:**
   - Insert `loans` record with `policy_fine_per_day_snapshot = policy.finePerDayBdt`.
   - Update `book_copies.status = ISSUED` and insert `copy_status_history`.
   - Insert `audit_logs` entry (`LOAN_ISSUED`).
6. **Commit Transaction.**

---

## 2. Atomic Book Return Workflow (`CirculationService.returnBook`)
Must execute inside a single JDBC transaction:
1. **Permission Check:** Require `LOAN_RETURN`.
2. **Lock Active Loan & Copy:**
   - Locate the `ACTIVE` loan for the scanned/selected `copy_id` using `FOR UPDATE`. Reject if no active loan exists.
3. **Deterministic Overdue Fine Calculation (`FineCalculationService`):**
   - Let `returnDate = LocalDate.now()` and `dueDate = loan.getDueDate()`.
   - If `returnDate <= dueDate.plusDays(policy.gracePeriodDays)`, overdue days = `0`.
   - Otherwise, calculate chargeable days between `dueDate` (exclusive) and `returnDate` (inclusive):
     - If `policy.countHolidaysInOverdue == false`, subtract configured weekly holidays and special closure dates falling within that range.
   - `rawFine = chargeableDays * loan.getPolicyFinePerDaySnapshot()`.
   - If `policy.maxFinePerLoanBdt != null`, `finalFine = min(rawFine, policy.maxFinePerLoanBdt)`.
   - If `finalFine > 0`, insert a `fines` record (`OVERDUE`, `UNPAID`) linked to `loan_id` and `member_id`.
4. **Copy Next Status & Reservation Queue Progression:**
   - If returned condition is `DAMAGED` or `LOST`, set copy status accordingly and optionally create a `DAMAGED_BOOK` / `LOST_BOOK` fine.
   - Else, check `reservations` for `book_id` with `status = WAITING` ordered by `requested_at ASC`:
     - If a waiting reservation exists, transition that reservation to `READY_FOR_PICKUP`, set `allocated_copy_id = copy.id`, set `expires_at = now + policy.reservationHoldDays`, set `book_copies.status = RESERVED`, and create an in-app notification for that member.
     - If no waiting reservation exists, set `book_copies.status = AVAILABLE`.
5. **Close Loan & Audit:**
   - Update `loans` (`status = RETURNED`, `returned_at = now`, `returned_to = actorId`).
   - Record `copy_status_history` and `audit_logs` (`LOAN_RETURNED`).
6. **Commit Transaction.**

---

## 3. Financial & Payment Rules
- **Immutable Ledger:** Payments are append-only. Never delete a payment row. If a clerk makes an error, an authorized admin executes `reversePayment(paymentId, reason)` which marks `is_reversed = true`, restores the unpaid balance on the linked `fines`, and logs the reversal in `audit_logs`.
- **Partial Payments:** A single payment can pay part of a fine (`PARTIALLY_PAID`) or span multiple unpaid fines via `payment_allocations`.
- **External Mobile Financial Services (bKash / Nagad / Rocket):** Recording a payment via bKash/Nagad/Rocket records the external `TrxID` (`external_reference`) verified by the librarian. Ensure `external_reference` is unique when provided to prevent double-posting the same transaction ID.

---

## 4. Bangladesh Localization Standards
- **Currency:** Format all monetary amounts with `৳` (`BDT`), e.g., `৳ 150.00`.
- **Phone Validation:** Validate Bangladesh mobile numbers matching `^(?:\+?88)?01[3-9]\d{8}$`.
- **Bilingual Support:** Book titles, author names, categories, and member names support both English and Bangla (`বাংলা`). Search queries must match both `*_en` and `*_bn` columns.
- **Working Calendar:** Default weekly holiday is configurable (e.g., Friday or Friday+Saturday) along with institution-specific public holidays and Ramadan hours.