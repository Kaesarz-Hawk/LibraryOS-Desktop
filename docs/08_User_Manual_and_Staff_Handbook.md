# LibraryOS — User Manual & Staff Operations Handbook

## 1. Introduction & Daily Operations Flow

Welcome to **LibraryOS**, the comprehensive desktop Library Management System designed for institutional libraries in Bangladesh. This handbook outlines daily operational procedures for library staff, assistants, and administrators.

---

## 2. Circulation Desk Workflows

### 2.1 Issuing a Book to a Member
1. Open **Circulation Desk** (`⇄ Circulation Desk / লেনদেন`).
2. Select the **Issue Book / বই প্রদান** tab.
3. Enter or scan the **Member Identifier** (Membership Number, Institutional ID, or Phone).
4. Enter or scan the **Copy Barcode** or **Accession Number** (`ACC-...` / `BC...`).
5. The system automatically validates:
   - Member account status (`ACTIVE` vs `SUSPENDED`).
   - Maximum active loans limit per member type (e.g. 3 books for Students).
   - Outstanding unpaid fine limits (blocked if unpaid fines exceed ৳ 100.00).
   - Reservation queues (blocked if copy is held for another waiting member).
6. Click **"Confirm Issue / বই প্রদান নিশ্চিত করুন"** or press Enter.

### 2.2 Returning a Book
1. Select the **Return Book / বই গ্রহণ** tab.
2. Scan the **Book Copy Barcode** or enter the **Accession Number**.
3. The system instantly previews:
   - Loan issue date and due date.
   - Overdue days (excluding weekly Friday holidays).
   - Calculated overdue fine amount in BDT (`৳`).
4. Select the return condition (`GOOD`, `DAMAGED`, `FAIR`).
5. Click **"Confirm Return / বই জমা নিন"**.
6. If an overdue fine is incurred, a fine record is automatically generated for the member. If a waiting reservation exists on this title, the copy is automatically allocated to the next member in the FIFO queue and marked `READY_FOR_PICKUP`.

### 2.3 Loan Renewal
1. Select the **Active Loans & Overdue** tab.
2. Select the loan and click **"Renew Loan / নবায়ন করুন"**.
3. Renewals extend the due date by the policy period (e.g. 7 days for students) provided the book is not overdue, the maximum renewal allowance is not exceeded, and no other member has a pending reservation on the title.

---

## 3. Fines, Waivers & Financial Management

### 3.1 Collecting Fine Payments & Issuing Receipts
1. Open **Fines & Payments** (`৳ Fines & Payments / জরিমানা`).
2. Search for the member to view outstanding unpaid balances.
3. Click **"Collect Payment / ফি গ্রহণ"**.
4. Enter the amount to collect (supports partial payments or full settlement).
5. Select the payment method:
   - **Cash**: Direct recording.
   - **bKash / Nagad / Rocket**: Enter the 10-character or 8-character TrxID. The system verifies TrxID syntax and prevents duplicate entry.
6. Click **"Confirm Payment"**.
7. An official payment receipt voucher (`REC-YYYYMMDD-XXXXXX`) with allocated fine breakdown and verification QR code is generated. Click **"Print Receipt Voucher (PDF)"** to print.

### 3.2 Fine Waivers
1. Select an unpaid fine and click **"Waive Fine / মওকুফ"**.
2. Enter the waived amount and **mandatory institutional reason** (e.g. "Medical emergency certificate", "Authorized by Principal").
3. The waiver is recorded in the financial ledger and an immutable security entry is logged in the **Institutional Audit Trail**.

---

## 4. Digital Library & E-Books

### 4.1 Reading E-Books In-App
1. Open **Digital Library** (`💻 Digital Library / ডিজিটাল লাইব্রেরি`).
2. Select an e-book and click **"Open Reader / পড়ুন"** or double-click the row.
3. Navigate pages with `◄ Prev` and `Next ►` or jump directly by entering a page number.
4. Zoom in (`+`) or Zoom out (`-`) for comfortable reading.
5. Reading progress is automatically saved to your profile so you can resume later.

### 4.2 Bookmarks & Notes
1. While reading, click **"🔖 Add Bookmark"**.
2. Enter a note or title (e.g. "Chapter 3 Revision Formula").
3. Click any bookmark in the right sidebar to instantly jump to that page.

### 4.3 Uploading New Digital PDF Resources (Librarians/Admins)
1. Click **"+ Upload E-Book / নতুন ই-বুক"**.
2. Select the bibliographic book title from the catalog.
3. Choose the `.pdf` file. The system checks header magic bytes, calculates a SHA-256 integrity checksum, counts total pages, and stores the file in managed storage.

---

## 5. Procurement, Suppliers & Inventory Acquisitions

### 5.1 Creating a Purchase Order
1. Open **Acquisitions & Suppliers** (`📦 Acquisitions & Suppliers / সংগ্রহ`).
2. Click **"+ New Acquisition PO / নতুন সংগ্রহ অর্ডার"**.
3. Select the publisher/supplier, enter invoice reference, and add book line items with quantities and BDT unit prices.
4. Click **"Create Order"**.

### 5.2 Receiving Shipments & Automatic Copy Generation
1. When physical books arrive, select the order and click **"Receive Order & Generate Copies / পণ্য গ্রহণ ও কপি তৈরি"**.
2. Choose the initial target shelf.
3. Click **"Receive & Generate Copies"**.
4. The system automatically creates physical `BookCopy` records with unique Accession numbers (`ACC-YYYYMMDD-XXXXXX`) and Code-128 barcodes (`BCXXXXXXXX`), updating library inventory instantaneously!

---

## 6. AI Assistant (`🤖 AI Assistant / এআই সহায়ক`)

1. Click the **"🤖 AI Assistant"** button in the top header bar at any time.
2. Ask questions in English or বাংলা:
   - *"What is the overdue fine rate?"*
   - *"দেরিতে বই জমা দিলে জরিমানা কত?"*
   - *"How many books can a student borrow?"*
   - *"Search books about Java"*
   - *"Suggest some popular books"*
3. The AI provides instant advisory guidance without interrupting active workflows.

