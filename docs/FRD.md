# Unique Makeover — Admin Console
## Functional Requirements Document (FRD) — v2.0
### Native Android App · Fully Offline · Admin-Only · Single Location

---

## 1. Overview & System Summary

**Unique Makeover — Admin Console** is a **fully offline, native Android application** built for the owner/operator of a single boutique luxury beauty parlour (configured for Jalgaon Jamod).

It replaces the paper appointment diary, WhatsApp-copy-paste reminder routine, and manual cash notebook with a fast, private, zero-recurring-cost operations tool. It handles:

- Daily appointment scheduling with clash prevention
- Appointment lifecycle (booked → done / cancelled) with revenue tracking
- Service catalog with photo references
- 1-tap WhatsApp reminders for tomorrow's clients
- Client CRM with lifetime metrics and private skin/hair notes
- Offline-generated UPI QR codes for advance/balance/full payments
- Full JSON backup & restore to device storage

**There is no client-facing portal, no public booking, no cloud sync, and no internet dependency.** All data lives on the device.

---

## 2. Architecture & Roles

```
┌────────────────────────────────────────────────────────────────────┐
│              UNIQUE MAKEOVER — ADMIN CONSOLE (ANDROID)             │
├────────────────────────────────────────────────────────────────────┤
│  UI LAYER (Jetpack Compose / XML)                                  │
│   • PIN Lock  • Dashboard  • Appointments  • Services              │
│   • Clients   • Reminders  • UPI QR        • Settings              │
├────────────────────────────────────────────────────────────────────┤
│  DOMAIN LAYER                                                      │
│   • Slot Generator & Clash Checker                                 │
│   • Revenue / Balance Calculator                                   │
│   • WhatsApp Template Formatter                                    │
│   • UPI URI Builder                                                │
├────────────────────────────────────────────────────────────────────┤
│  DATA LAYER (Room / SQLite)                                        │
│   • services  • appointments  • client_notes  • settings           │
│   • Single local DB, WAL mode, indexed on date & phone             │
├────────────────────────────────────────────────────────────────────┤
│  PLATFORM INTEGRATIONS (all offline)                               │
│   • ZXing local QR encoder        • Bitmap compression             │
│   • Camera + Gallery intents      • WhatsApp via wa.me intent      │
│   • Storage Access Framework      • Local notifications            │
└────────────────────────────────────────────────────────────────────┘
```

### Roles

| Role | Access | Purpose |
|---|---|---|
| **Admin (Owner/Operator)** | App launch → 4-digit PIN gate | All operations |

**No client role exists.** No public screens, no unauthenticated access.

---

## 3. Access Control

### 3.1 PIN Lock
- On every cold launch, the app presents a **4-digit numeric PIN keypad**.
- PIN is verified against a **hashed value** stored in `Settings` (default `1234` on first install).
- Incorrect PIN: shake animation + attempt counter (soft lock after 5 failed attempts for 30 seconds — local only).
- **Change PIN** available inside Settings (requires entering current PIN first).
- *(Optional enhancement)* Biometric unlock (`BiometricPrompt`) as a secondary gate.

---

## 4. Daily Operations Dashboard

### 4.1 Today's KPI Cards
- **Booked (Pending)** — count of `status = booked` for today's date
- **Completed (Done)** — count of `status = done` for today
- **Cancelled** — count of `status = cancel` for today
- **Scheduled Volume** — sum of `rate` for today's non-cancelled appointments
- **Collected Revenue** — sum of `paid` for today's non-cancelled appointments
- **Balance Outstanding** — `scheduled − collected` for today

### 4.2 Alert Banners (dismissible, locally computed)
| Banner | Trigger |
|---|---|
| WhatsApp number not configured | `settings.wa` is empty |
| UPI ID not configured | `settings.upi` is empty |
| Backup overdue | `lastBackup` older than 3 days |
| Overdue appointments | Any appointment with `date < today` and `status = booked` |
| Tomorrow's reminders pending | Appointments tomorrow where `reminded ≠ tomorrow's date` |

Tapping a banner deep-links to the relevant screen.

### 4.3 Live Schedule Stream
- Chronologically sorted list of **today's** appointments.
- Each row shows: time, client name, phone, services, staff, rate/paid/balance, status chip.
- **One-tap row actions:**
  - ✅ Mark Done (prompts to record pending balance)
  - 💬 Send WhatsApp message
  - ₹ Record Payment (cash / UPI)
  - ✏️ Reschedule / Edit
  - ❌ Cancel
  - 🗑 Delete (with confirmation)

---

## 5. Appointment Management

### 5.1 List & Filters
- Filter tabs: **All · Upcoming · Completed · Cancelled**
- Date range picker (default: today)
- Search by client name or phone (substring match)
- Sorted by date then time ascending

### 5.2 Manual Booking (Admin-created)

Fields:
| Field | Validation |
|---|---|
| Client name | Required, non-empty |
| Phone | Required, exactly 10 digits, Indian format |
| Services | Multi-select from catalog; recomputes subtotal live |
| Date | Today or future (past dates disabled) |
| Time slot | Generated from operating hours; conflicting slots disabled with **"Full"** badge; today's past slots disabled |
| Staff | Dropdown from `settings.staff` roster |
| Discount (₹) | Optional; subtracted from subtotal |
| Advance paid (₹) | Optional; recorded as `paid` at creation |
| Internal note | Optional free text |

**Computed:** `rate = subtotal − discount`

On save:
- Validate all fields
- Re-check slot clash server-side (local query)
- Insert `appointments` row with `status = booked`
- Toast "Appointment booked"

### 5.3 Slot Generation & Clash Rules
- Slots generated from `settings.open` → `settings.close` at `settings.slot` minute intervals.
- A slot is **disabled** if any existing appointment has `date = selected` AND `time = selected` AND `status ≠ cancel`.
- For **today**, any slot whose time is ≤ current device time is disabled.
- Slot generation respects any future change to open/close/slot settings.

### 5.4 Status Lifecycle
```
booked ──► done      (Mark Done)
   │  ◄── cancel     (Undo, restores to booked)
   └──► cancel       (Cancel)
```
- **Undo** available within the row menu for recently completed/cancelled records.
- **Hard delete** requires a confirmation dialog and is logged nowhere (permanent).

### 5.5 Edit Appointment
- All fields editable except `id` and `bookedAt`.
- Slot change re-runs clash check against *other* appointments.
- Editing `rate` / `paid` recalculates balance and dashboard metrics immediately.

---

## 6. Service Catalog Management

### 6.1 Operations
- **Add** service: name, rate (₹), reference photo
- **Edit** existing name / rate / photo
- **Delete** service (confirm; existing appointments retain the service string via snapshot)
- **Reset** photo to a default aesthetic fallback

### 6.2 Photo Handling
- Source options: **Camera** (`MediaStore.ACTION_IMAGE_CAPTURE`) or **Gallery** (`ACTION_PICK`)
- **Client-side compression:** scale longest edge to **320 px**, JPEG quality **78%**
- Stored as byte array in Room (`ServiceItem.imageBlob`) or as a file path in app-internal storage
- Delete photo option available

### 6.3 Seed Data
On first install, seed with example services (Haircut, Threading, Gold Facial, Keratin Spa, Manicure, Pedicure, Bridal Package) with placeholder rates — editable by owner.

---

## 7. Automated WhatsApp Reminders

### 7.1 Tomorrow's Queue
- Lists appointments with `date = tomorrow` and `status = booked`
- Shows `reminded` status per appointment (✓ if `reminded == tomorrow's date`)

### 7.2 1-Tap Remind
- Tapping **"Remind"** builds a message from the template and opens WhatsApp via:
  `https://wa.me/91<phone>?text=<urlencoded message>`
  via `Intent.ACTION_VIEW` (works offline; WhatsApp handles delivery)
- On return, marks `appointments.reminded = tomorrow's date`
- Optionally shows a confirmation snackbar with **Undo**

### 7.3 Template Placeholders
The reminder template (`settings.msg`) supports:

| Placeholder | Replaced with |
|---|---|
| `{name}` | Client name |
| `{shop}` | Salon name |
| `{service}` | Booked services |
| `{date}` | Appointment date (formatted) |
| `{time}` | 12-hour formatted time |
| `{rate}` | Total billed |
| `{due}` | Balance due |
| `{shopnum}` | Salon WhatsApp number |

### 7.4 Template Editing
- Editable in **Settings → Reminder Template** (multiline text field)
- "Reset to default" button
- Live preview with sample data

---

## 8. Client Directory & CRM

### 8.1 Consolidated Profiles
- Derived by grouping appointments by **sanitized phone** (digits only)
- One row per unique phone: name (most recent), total visits, total spend, last visit date, last service

### 8.2 Live Search
- Substring match across **name** and **phone**

### 8.3 Client Detail Screen
- Header: name, phone, lifetime metrics
- **Full appointment history** (chronological)
- **Private notes editor** — stored in `client_notes` table keyed by phone
  - e.g. hair texture, skin sensitivities, dye allergies, preferred stylist
  - Debounced autosave (or explicit Save button)

### 8.4 Quick Rebook
- From client detail, **"Rebook"** opens the booking form pre-filled with:
  - name, phone, and the client's last services
  - note carries forward if desired
- Staff/date/time left blank for admin to choose

---

## 9. Billing & Offline UPI QR System

### 9.1 UPI URI Construction
Builds a standard `upi://pay` URI:
```
upi://pay?pa=<upi_id>&pn=<shop_name>&am=<amount>&cu=INR&tn=<note>
```
- `pa` = `settings.upi`
- `pn` = `settings.shop`
- `am` = selected amount (2 decimal places)
- `cu` = `INR`
- `tn` = e.g. `"Sunita - Balance Payment"`, `"Advance Payment"`, `"Full Payment"`

If `settings.upi` is empty, the QR screen shows a **blocking banner** linking to Settings.

### 9.2 Payment Mode Presets
| Preset | Amount |
|---|---|
| **Balance Due** | `rate − paid` (default) |
| **Advance (30%)** | `round(rate × 0.30)` |
| **Full Payment** | `rate` |
| **Custom** | Admin-entered ₹ amount |

### 9.3 Offline QR Rendering
- QR generated **locally with ZXing (`com.google.zxing:core`)** as a Bitmap
- No network call, works in airplane mode
- Displayed large, screen-brightness boosted, with amount + note text below
- **Save QR as PNG** and **Share QR** (share sheet) available

### 9.4 WhatsApp UPI Request
- **"Send payment link on WhatsApp"** opens `wa.me` with:
  - message text including client name, service, amount, and the raw `upi://pay` URI (clickable in WhatsApp on Android)

### 9.5 Payment Recording (Admin)
- From appointment row → **Record Payment**
- Input: amount, mode (Cash / UPI)
- `paid += amount` (cannot exceed `rate` without a confirm dialog)
- Balance recomputed; dashboard KPIs update instantly
- *(Optional)* `payments` table to log individual payment events (amount, mode, timestamp) for audit

---

## 10. Settings & Configuration

### 10.1 Branding
- **Salon name** (text)
- **Tagline / address** (text)
- **Logo upload** — camera/gallery → compressed to **200 px**, circular crop → stored locally
- *(Optional)* **App wallpaper** — compressed to **900 px @ 72%**, applied as app background

### 10.2 Access Control
- **Change 4-digit PIN** (requires current PIN)

### 10.3 Business Operation Parameters
- **Opening time** (`HH:MM` picker)
- **Closing time** (`HH:MM` picker)
- **Slot interval** — 15 / 30 / 45 / 60 minutes (radio)
- **Staff roster** — comma-separated names, parsed into a list for booking dropdown

### 10.4 Communication Settings
- **Salon WhatsApp number** (10-digit + country code)
- **Merchant UPI ID** (e.g. `parlour@upi`)
- **Reminder template** (multiline + reset + preview)

### 10.5 Data Management
- **Export backup** → SAF `ACTION_CREATE_DOCUMENT` → `UniqueMakeover_backup_YYYY-MM-DD.json`
- **Import backup** → SAF `ACTION_OPEN_DOCUMENT` → validate → confirm overwrite
- **Show last backup date**
- *(Optional)* **Clear all data** (double confirmation, requires PIN re-entry)

---

## 11. Storage, Persistence & Data Portability

### 11.1 Local Database
- **Room / SQLite**, WAL mode enabled for fast concurrent reads
- Stored in app-internal storage (`getDatabasePath()`)
- No external storage permission needed for DB

### 11.2 Image Storage
- Compressed images stored as **BLOBs in Room** or as files in `context.filesDir/images/` with DB storing the path
- Recommended: **file paths in DB** to keep DB size manageable

### 11.3 Backup Format (JSON)
```json
{
  "version": 2,
  "exportedAt": "2026-11-13T10:00:00Z",
  "settings": { ... },
  "services": [ ... ],
  "appointments": [ ... ],
  "clientNotes": { "<phone>": "<note>" }
}
```

### 11.4 Restore Rules
- Validate schema version and required keys
- Show a diff-style summary (counts) before confirming overwrite
- On invalid file: error dialog, no changes applied
- On success: reload all screens from DB

### 11.5 File Naming Convention
`UniqueMakeover_backup_YYYY-MM-DD.json` (matches legacy spec)

---

## 12. Notifications (Optional, Local Only)

- **Daily 8:00 PM local notification** (WorkManager): *"You have N appointments tomorrow. Tap to send reminders."*
- **Backup reminder** notification if `lastBackup` older than 3 days
- All notifications generated locally — **no push infrastructure**

---

## 13. Data Schema (Room Entities)

### 13.1 `SettingsEntity`
```kotlin
@Entity(tableName = "settings")
data class SettingsEntity(
  @PrimaryKey val id: Int = 1,        // singleton row
  val shop: String,                   // "Unique Makeover"
  val tagline: String,                // "Jalgaon Jamod • Luxury Beauty Parlour"
  val adminPinHash: String,           // hashed PIN
  val logoPath: String?,              // internal storage path
  val bgPath: String?,                // optional wallpaper
  val wa: String,                     // WhatsApp number
  val upi: String,                    // UPI ID
  val staff: String,                  // comma-separated
  val open: String,                   // "HH:MM"
  val close: String,                  // "HH:MM"
  val slot: Int,                      // minutes
  val msg: String,                    // reminder template
  val lastBackup: String?,            // ISO date
)
```

### 13.2 `ServiceEntity`
```kotlin
@Entity(tableName = "services")
data class ServiceEntity(
  @PrimaryKey val id: String,         // UUID or "s1"
  val name: String,
  val rate: Int,                      // INR
  val imagePath: String?,             // internal storage path
  val sortOrder: Int = 0
)
```

### 13.3 `AppointmentEntity`
```kotlin
@Entity(
  tableName = "appointments",
  indices = [Index("date"), Index("phone"), Index(value=["date","time"])]
)
data class AppointmentEntity(
  @PrimaryKey val id: String,         // UUID
  val name: String,
  val phone: String,                  // 10-digit sanitized
  val svc: String,                    // formatted "Facial + Haircut"
  val serviceIds: String,             // JSON array of service IDs (for rebook)
  val rate: Int,
  val paid: Int,
  val staff: String,
  val date: String,                   // "YYYY-MM-DD"
  val time: String,                   // "HH:MM"
  val note: String,
  val status: String,                 // "booked" | "done" | "cancel"
  val reminded: String?,              // date string or null
  val bookedAt: Long                  // epoch millis
)
```

### 13.4 `ClientNoteEntity`
```kotlin
@Entity(tableName = "client_notes")
data class ClientNoteEntity(
  @PrimaryKey val phone: String,
  val note: String,
  val updatedAt: Long
)
```

### 13.5 `PaymentEntity` *(optional audit log)*
```kotlin
@Entity(tableName = "payments", indices = [Index("appointmentId")])
data class PaymentEntity(
  @PrimaryKey val id: String,
  val appointmentId: String,
  val amount: Int,
  val mode: String,                   // "cash" | "upi"
  val paidAt: Long
)
```

---

## 14. Screens Inventory

| # | Screen | Purpose |
|---|---|---|
| 1 | **PIN Lock** | App entry gate |
| 2 | **Dashboard** | KPIs, alerts, today's live schedule |
| 3 | **Appointments** | Filtered list + manual booking FAB |
| 4 | **Booking / Edit Sheet** | Create or edit appointment |
| 5 | **Services** | Catalog CRUD + photo uploader |
| 6 | **Clients** | CRM directory + search |
| 7 | **Client Detail** | History, notes, quick rebook |
| 8 | **Reminders** | Tomorrow's queue + 1-tap remind |
| 9 | **UPI QR** | Preset amounts, offline QR, share |
| 10 | **Settings** | Branding, PIN, hours, staff, comms, backup |

---

## 15. Non-Functional Requirements

| Area | Requirement |
|---|---|
| **Offline** | App must be **100% functional with airplane mode ON** at all times |
| **Performance** | Cold start < 2s; list scroll 60fps on mid-range devices |
| **Storage** | Handle 5,000+ appointments and 200+ services without lag |
| **Data safety** | Room transactions for multi-step writes; backup on demand |
| **Privacy** | No analytics, no crash SDKs that phone home, no INTERNET permission required *(except optional none — see note)* |
| **Permissions** | Camera (photos), Storage/SAF (backup) — **no INTERNET permission needed** |
| **Localization** | English UI; INR currency formatting; 12-hour time display |
| **Min SDK** | Android 8.0 (API 26) |
| **Target SDK** | Latest stable |
| **Orientation** | Portrait (default); no lock required |

> **Note:** WhatsApp launching via `wa.me` uses an implicit intent handled by the WhatsApp app — **your app itself needs no INTERNET permission**.

---

## 16. Out of Scope (Explicitly Removed vs. v1)

| Removed | Reason |
|---|---|
| Client portal / public browsing | Admin-only app |
| Client self-booking | Owner controls all bookings |
| Client "My Appointments" hub | No client access |
| Client self-cancellation | Admin-only cancellation |
| PWA / Service Worker / manifest | Native Android app |
| In-app install prompt | Play Store install |
| Cloud sync (Firebase REST) | Fully offline |
| `cloudUrl`, `heroImg`, `bookedBy`, `updatedAt` fields | No cloud, no client hero, single actor |
| External QR generation API | Replaced with local ZXing |
| `navigator.storage.persist()` | Browser-only |
| Canvas compression | Replaced with `Bitmap.compress()` |

---

## 17. Future Enhancements (Post-v2, optional)

- Payment audit log UI (from `PaymentEntity`)
- Multiple staff logins / per-staff views
- Local analytics (monthly revenue charts)
- CSV export alongside JSON
- Hindi / Marathi UI localization
- Encrypted backup option

---

**End of FRD v2.0 — Admin Console, Fully Offline, Native Android**

---

Would you like me to follow this up with **Room DAO interfaces**, a **Jetpack Compose screen wireflow**, or the **ZXing UPI QR + WhatsApp intent** Kotlin snippets?