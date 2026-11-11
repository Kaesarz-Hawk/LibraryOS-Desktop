# LibraryOS — JavaFX UI Design System & Screen Inventory

## 1. Visual Design Tokens (`theme.css`)
LibraryOS uses a calm, high-contrast, modern desktop aesthetic.

### 1.1 Color Palette (Dark Slate & Clean Light Support)
- **Sidebar / Shell Background:** `#0F172A` (Slate 900)
- **Primary Surface / Card Background (Light Mode):** `#FFFFFF` with `#E2E8F0` 1px border
- **App Canvas Background (Light Mode):** `#F8FAFC` (Slate 50)
- **Primary Accent:** `#2563EB` (Royal Blue 600), Hover: `#1D4ED8`
- **Text Primary:** `#0F172A`, **Text Secondary:** `#475569`, **Text Muted:** `#94A3B8`
- **Semantic Status Badges:**
  - `AVAILABLE` / `ACTIVE` / `PAID`: Background `#DCFCE7`, Text `#15803D` (Emerald)
  - `ISSUED` / `READY_FOR_PICKUP`: Background `#DBEAFE`, Text `#1D4ED8` (Blue)
  - `RESERVED` / `PARTIALLY_PAID` / `DUE_SOON`: Background `#FEF3C7`, Text `#B45309` (Amber)
  - `OVERDUE` / `LOST` / `DAMAGED` / `SUSPENDED`: Background `#FEE2E2`, Text `#B91C1C` (Crimson)

### 1.2 Typography & Bangla Rendering
- **Primary Font Stack:** `"Inter", "Segoe UI", "Noto Sans Bengali", "Kalpurush", "Nirmala UI", sans-serif`
- Ensure all JavaFX labels, text fields, tables, and OpenPDF exports include a Bangla Unicode font fallback (`Noto Sans Bengali` or `Kalpurush`) so Bangla book titles and member names never render as boxes (`□□□`).

---

## 2. Main Desktop Shell Layout
- **Minimum Window Size:** `1280 x 760` (Resizable, launches maximized or centered `1440 x 860`).
- **Left Sidebar (`250px` fixed width):**
  - App Logo & Institution Name header.
  - Scrollable navigation list filtered dynamically by `SessionManager.hasPermission(...)`.
- **Top Header Bar (`64px` fixed height):**
  - Quick global search bar (`Ctrl+K` shortcut support).
  - Active date/time & institution badge.
  - Notification bell with unread badge counter.
  - Current User Name, Role Pill Badge, and Profile/Logout menu.
- **Main Content Workspace:**
  - Consistent `24px` padding, Page Header (Title, Subtitle, Primary Action CTA button), Filter/Search toolbar, and Data Table / Card Grid.

---

## 3. Concurrency & UX Rules
1. **Never Block the JavaFX Application Thread:** All database queries, report generation, file imports, and barcode rendering must run inside a `javafx.concurrent.Task<T>` submitted to an application `ExecutorService`.
2. **Explicit UI States:** Every data view must handle 4 states cleanly:
   - **Loading State:** Non-intrusive progress indicator or skeleton overlay.
   - **Empty State:** Clear message and call-to-action when a table or search returns 0 rows.
   - **Success State:** Toast notification or status banner.
   - **Error State:** User-friendly alert dialog explaining what went wrong without dumping raw SQL stack traces.
3. **Form Validation:** Validate inputs before service submission and highlight invalid fields inline with a crimson border and helper label.