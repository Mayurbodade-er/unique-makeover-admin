# Unique Makeover — Admin Console
# User Stories Specification Document
### Native Android App · Fully Offline · Admin-Only · Single Location (Jalgaon Jamod)
**Document Version:** 2.0 (Comprehensive Specification with Edge Cases & Failure Scenarios)  
**Target Roles:** Veteran Business Analyst, Product Designer, Android System Architect  
**Traceability References:** `docs/FRD.md` (v2.0) & `docs/ARD.md` (v1.0)

---

## Document Overview & Persona Definition

### Primary Persona
* **Name:** Sunita (Salon Owner / Operator)
* **Role:** Sole Administrator & Master Beautician at *Unique Makeover*, Jalgaon Jamod.
* **Context:** Fast-paced salon environment, transitioning from paper notebooks and manual WhatsApp typing to a dedicated Android tablet/phone. Needs quick single-tap operations, instant responsiveness, high readability under salon lighting, absolute data privacy, and 100% offline autonomy (zero cloud dependencies, no recurring fees).

---

## Epic 1: Access Control & Authentication (FRD §3, ARD §6.1)

### US-AUTH-01: App Cold-Start PIN Verification Gate
**As** the salon owner (Sunita),  
**I want** the application to prompt for a 4-digit PIN on every cold start before revealing any salon data,  
**So that** clients, helpers, or unauthorized persons holding the device cannot view confidential client notes, contact numbers, or revenue metrics.

#### Context & Product Design
* **Ergonomics:** Large, high-contrast numeric keypad with tactile haptic feedback on touch.
* **UI States:** 4 discrete PIN dot indicators (empty, filled).
* **Default Setup:** Seeded with default PIN `1234` on first installation.

#### Technical & Architectural Specifications
* **Architecture:** Presentation (`PinLockScreen` + `PinLockViewModel`) → Domain (`VerifyPinUseCase`) → Data (`PinRepositoryImpl` + `PinLockManager`).
* **Security:** Master PIN hash + per-install cryptographic salt stored in `EncryptedSharedPreferences` backed by Android Keystore (`PBKDF2WithHmacSHA256`, 100k iterations) as per ADR-0003.
* **Invariant:** Direct hardware back button exits the app; no top-level navigation route accessible without successful verification.

#### Acceptance Criteria
* **AC-AUTH-01.1 (Valid PIN Entry):**
  * **Given** the app is launched cold and displays the PIN Lock screen,
  * **When** the user taps the 4 digits matching the configured PIN,
  * **Then** the PIN dots fill sequentially, and upon entering the 4th correct digit, the system immediately navigates to `Routes.Dashboard` and clears the auth backstack.
* **AC-AUTH-01.2 (Incorrect PIN Animation):**
  * **Given** the PIN Lock screen is visible,
  * **When** an incorrect 4-digit combination is entered,
  * **Then** the PIN dots clear immediately, a horizontal shake animation triggers on the dots, a short haptic error buzz is produced, and an error caption displays *"Incorrect PIN. Try again."*.
* **AC-AUTH-01.3 (Cold Start Default PIN):**
  * **Given** a fresh install of the app with no prior configuration,
  * **When** entering `1234`,
  * **Then** authentication succeeds and navigates to the Dashboard.

#### Edge Cases & Failure Scenarios
* **EC-AUTH-01.1 (Rapid Multi-Touch / Tap Bouncing):**
  * **Scenario:** User rapidly taps the same digit multiple times or two numbers simultaneously with oily/wet fingers.
  * **System Behavior:** `PinLockViewModel` enforces a debounce window (75 ms) between registered keystrokes and limits entered digit buffer strictly to 4 characters. Extra taps beyond 4 digits are discarded.
* **EC-AUTH-01.2 (App Backgrounding During Partial PIN Entry):**
  * **Scenario:** User enters 2 digits, switches to an incoming phone call or camera, and resumes the app after 2 minutes.
  * **System Behavior:** `onStop()` / `onPause()` in `MainActivity` clears transient in-memory PIN state. Upon resume, the user is presented with 4 empty PIN dots to prevent session-hijacking of partially entered credentials.
* **EC-AUTH-01.3 (Configuration Change / Screen Rotation):**
  * **Scenario:** Device is rotated from portrait to landscape while 3 digits are entered.
  * **System Behavior:** ViewModel retains entered state cleanly via `SavedStateHandle` or `UiState` Flow; input does not reset, and keypad layout dynamically reflows without clipping.
* **EC-AUTH-01.4 (Android Keystore Decryption Failure):**
  * **Scenario:** Hardware Keystore corrupted or throws `KeyPermanentlyInvalidatedException` (e.g. after OS biometric update).
  * **System Behavior:** `PinLockManager` catches security exceptions gracefully, logs error internally, and provides a fallback recovery prompt rather than crashing on launch.

---

### US-AUTH-02: Brute-Force Protection & Lockout Rate Limiting
**As** the salon owner,  
**I want** the PIN input to lock temporarily after repeated failed attempts,  
**So that** someone cannot brute-force guess my 4-digit PIN by typing combinations consecutively.

#### Context & Product Design
* **Visual Lockout State:** Numeric keypad is disabled (alpha = 0.38, non-clickable).
* **Live Countdown:** Dynamic countdown timer displays: *"Too many failed attempts. Try again in XXs"*.

#### Technical & Architectural Specifications
* **Architecture:** `LockoutPolicy` pure Kotlin domain logic (`(failCount, lastFailElapsedRealtime) -> LockoutState`).
* **Persistence:** Failed attempt counter and lockout timestamp persisted in `DataStore<Preferences>` via `PinLockManager` using `SystemClock.elapsedRealtime()` to prevent device system clock tampering.

#### Acceptance Criteria
* **AC-AUTH-02.1 (Soft Lockout Trigger):**
  * **Given** 4 consecutive incorrect PIN attempts have already been made,
  * **When** the 5th incorrect PIN attempt is entered,
  * **Then** the keypad becomes disabled, a 30-second lockout timer starts, and the remaining seconds count down tick-by-tick.
* **AC-AUTH-02.2 (Lockout Expiry):**
  * **Given** the app is currently in a 30-second lockout state,
  * **When** the timer reaches 0 seconds,
  * **Then** the keypad re-enables, the failed attempt counter resets, and the user can enter PIN digits again.
* **AC-AUTH-02.3 (Lockout Persistence Across App Restart):**
  * **Given** the app is locked with 20 seconds remaining,
  * **When** the owner closes and relaunches the app after 5 seconds,
  * **Then** the PIN Lock screen remains locked with approximately 15 seconds remaining.

#### Edge Cases & Failure Scenarios
* **EC-AUTH-02.1 (System Clock Manipulation / Time Rollover):**
  * **Scenario:** An unauthorized user enters 5 wrong PINs and manually advances Android system clock forward by 1 hour in device Settings to bypass lockout.
  * **System Behavior:** Lockout relies on `SystemClock.elapsedRealtime()` (monotonic uptime since boot) combined with secure boot-cycle counter, rendering wall-clock changes completely ineffective.
* **EC-AUTH-02.2 (Device Reboot During Active Lockout):**
  * **Scenario:** The device is powered off and rebooted while 25 seconds remain on lockout.
  * **System Behavior:** `PinLockManager` detects boot cycle difference via recorded boot timestamp. Upon reboot, the policy enforces the full 30-second penalty as a security guardrail.
* **EC-AUTH-02.3 (Touch Event Flooding While Disabled):**
  * **Scenario:** Continuous rapid tapping on disabled keypad numbers during lockout.
  * **System Behavior:** `Modifier.clickable(enabled = false)` drops touch events at the Compose gesture detection layer; no event queue backlog or lagging clicks trigger when the timer expires.

---

### US-AUTH-03: Admin PIN Modification
**As** the salon owner,  
**I want** to change my 4-digit PIN from the Settings menu after verifying my existing PIN,  
**So that** I can maintain operational security or update the factory default PIN.

#### Context & Product Design
* Two-step modal workflow: First prompts *"Enter Current PIN"*, followed by *"Enter New 4-digit PIN"* and *"Confirm New PIN"*.

#### Technical & Architectural Specifications
* **Domain:** `ChangePinUseCase(currentPin, newPin, confirmPin)`.
* **Repository:** `PinRepository.updatePin(newPin)` generates new salt, calculates PBKDF2 hash, updates Keystore and synchronizes `SettingsEntity.adminPinHash`.

#### Acceptance Criteria
* **AC-AUTH-03.1 (Successful PIN Update):**
  * **Given** the owner is in Settings → Access Control and enters the correct existing PIN,
  * **When** entering a new 4-digit PIN and matching confirmation PIN,
  * **Then** the database and Keystore update with the new salted hash, a toast confirms *"PIN changed successfully"*, and subsequent logins require the new PIN.
* **AC-AUTH-03.2 (Mismatched Confirmation PIN):**
  * **Given** the owner enters the correct current PIN and a new PIN `5678`,
  * **When** entering `5679` in the confirmation step,
  * **Then** an inline error displays *"PINs do not match"* and the PIN is not updated.
* **AC-AUTH-03.3 (Incorrect Current PIN):**
  * **Given** the owner enters an incorrect current PIN in step 1,
  * **Then** an error is shown and the workflow does not proceed to the new PIN input.

#### Edge Cases & Failure Scenarios
* **EC-AUTH-03.1 (Setting New PIN Identical to Current PIN):**
  * **Scenario:** The owner enters their current PIN as the new PIN.
  * **System Behavior:** UI displays an informational inline notice: *"New PIN must be different from current PIN"* and keeps the Save button disabled.
* **EC-AUTH-03.2 (Weak Trivial PIN Entry):**
  * **Scenario:** Owner attempts to set PIN to `0000`, `1111`, or `1234`.
  * **System Behavior:** App warns with a dialog: *"Weak PIN detected. Are you sure you want to use this PIN?"* with options to [Choose Stronger] or [Keep Anyway].
* **EC-AUTH-03.3 (Process Kill Mid-Workflow):**
  * **Scenario:** Android OS reclaims memory while the owner is on step 2 (Confirm New PIN).
  * **System Behavior:** Because changes are not committed until full verification succeeds, the existing PIN remains active in Keystore without corruption.

---

## Epic 2: Daily Operations & Dashboard (FRD §4, ARD §6.2)

### US-DASH-01: Real-Time Operational & Financial KPIs
**As** the salon owner,  
**I want** to view high-level appointment counts and financial totals for today on the home screen,  
**So that** I can instantly gauge today's workload, cash flow, and outstanding dues at a glance.

#### Context & Product Design
* **Visual Layout:** 6 compact metric cards in a grid:
  1. *Booked (Pending)* (Count)
  2. *Completed (Done)* (Count)
  3. *Cancelled* (Count)
  4. *Scheduled Volume* (₹ sum of non-cancelled rates)
  5. *Collected Revenue* (₹ sum of non-cancelled paid)
  6. *Balance Outstanding* (₹ scheduled − collected)
* **Currency Formatting:** Indian numbering format (e.g., `₹ 12,500`).

#### Technical & Architectural Specifications
* **Architecture:** `DashboardViewModel` observes reactive Room Flow from `GetDashboardKpisUseCase` combining `AppointmentDao.getAppointmentsByDate(today)`.
* **Calculation Invariant:** Pure domain mapping inside `DashboardKpis`. Cancelled appointments (`status = "cancel"`) are excluded from Volume, Collected, and Balance.

#### Acceptance Criteria
* **AC-DASH-01.1 (Real-Time Reactive Calculation):**
  * **Given** today has 3 appointments: Apt A (Rate ₹1,000, Paid ₹1,000, status "done"), Apt B (Rate ₹2,000, Paid ₹500, status "booked"), Apt C (Rate ₹1,500, Paid ₹0, status "cancel"),
  * **When** viewing the Dashboard,
  * **Then** KPIs display: Booked: 1, Completed: 1, Cancelled: 1, Scheduled Volume: ₹3,000, Collected Revenue: ₹1,500, Balance Outstanding: ₹1,500.
* **AC-DASH-01.2 (Zero Appointments Empty State):**
  * **Given** there are no appointments recorded for today,
  * **Then** count cards display `0` and monetary values display `₹ 0`.

#### Edge Cases & Failure Scenarios
* **EC-DASH-01.1 (Midnight Day Transition While Dashboard Open):**
  * **Scenario:** Salon operates late into the night. Time transitions from 23:59 to 00:00 with the app open in foreground.
  * **System Behavior:** Injected `TimeProvider` or a midnight broadcast receiver triggers a recalculation; KPIs reset cleanly to the new day's appointments without requiring app restart.
* **EC-DASH-01.2 (Appointments with Zero Rate / Free Services):**
  * **Scenario:** A bridal touch-up or complimentary service is booked with Rate = ₹0 and Paid = ₹0.
  * **System Behavior:** Appointment counts accurately increment in Booked/Done; mathematical calculations handle ₹0 without division errors or negative formatting.
* **EC-DASH-01.3 (Large Numbers & UI Overflow):**
  * **Scenario:** High-season bridal bookings produce daily revenue exceeding ₹1,00,000.
  * **System Behavior:** Currency formatter utilizes standard Indian grouping (`₹ 1,50,000`); Compose KPI text containers utilize `TextOverflow.Ellipsis` with auto-scaling font size to prevent layout breakage on small screens.
* **EC-DASH-01.4 (Overpayment / Negative Outstanding Balance):**
  * **Scenario:** A client pays a generous tip or advance resulting in `paid > rate`.
  * **System Behavior:** `Balance Outstanding` calculates as ₹0 or displays a distinct credit badge (e.g. `+ ₹200 Tip/Credit`) rather than displaying a misleading negative due amount.

---

### US-DASH-02: Actionable System Alert Banners & Deep Linking
**As** the salon owner,  
**I want** prominent alert banners on the dashboard for missing configurations, overdue backups, and pending actions,  
**So that** I never forget to send client reminders or take database backups.

#### Context & Product Design
* Dismissible cards styled with Material 3 warning/info colors.
* Tapping a banner deep-links directly to the relevant action screen.
* Computed dynamically on each dashboard load.

#### Technical & Architectural Specifications
* **Domain:** `GetAlertBannersUseCase` evaluating 5 strict triggers:
  1. `WaNotConfigured`: `settings.wa.isBlank()` → Links to `Routes.Settings(section = "communication")`
  2. `UpiNotConfigured`: `settings.upi.isBlank()` → Links to `Routes.Settings(section = "communication")`
  3. `BackupOverdue`: `lastBackup == null` OR `lastBackup < (today - 3 days)` → Links to `Routes.Settings(section = "data")`
  4. `OverdueAppointments`: Any appointment where `date < today` AND `status == "booked"` → Links to `Routes.Appointments(filter = "overdue")`
  5. `RemindersPending`: Appointments where `date == tomorrow` AND `status == "booked"` AND `reminded != tomorrow` → Links to `Routes.Reminders`

#### Acceptance Criteria
* **AC-DASH-02.1 (Overdue Backup Banner):**
  * **Given** the last database backup date was 4 days ago,
  * **When** the owner views the Dashboard,
  * **Then** an alert banner *"Backup Overdue: Last backup was 4 days ago. Tap to safeguard data."* appears, and tapping it opens the Data Management section in Settings.
* **AC-DASH-02.2 (Pending Reminders Banner):**
  * **Given** 3 appointments exist for tomorrow with `reminded = null`,
  * **When** viewing the Dashboard,
  * **Then** an alert banner *"3 Reminders Pending for Tomorrow"* is displayed, and tapping it navigates directly to the Reminders screen.
* **AC-DASH-02.3 (Resolution Auto-Dismissal):**
  * **Given** the WhatsApp alert banner is active,
  * **When** the owner navigates to Settings and saves a valid WhatsApp number,
  * **Then** returning to the Dashboard immediately removes the WhatsApp banner.

#### Edge Cases & Failure Scenarios
* **EC-DASH-02.1 (All 5 Banners Triggered Simultaneously):**
  * **Scenario:** On fresh setup, UPI, WhatsApp, Backup, and Reminders are all simultaneously unconfigured or overdue.
  * **System Behavior:** Banners are rendered inside a vertical scrolling carousel or ordered list with priority weighting (`BackupOverdue` > `RemindersPending` > `UpiNotConfigured`); dashboard metrics remain accessible below.
* **EC-DASH-02.2 (Banner Deep-Link to Deleted Resource):**
  * **Scenario:** Owner taps "Overdue Appointments" banner, but in the meantime an appointment was deleted via another sheet.
  * **System Behavior:** Destination screen gracefully re-queries SQLite; if empty, it displays an informative empty state (*"No overdue appointments"*) rather than crashing or showing stale data.
* **EC-DASH-02.3 (Repeated Dismissal of Critical Alerts):**
  * **Scenario:** Owner dismisses the "Backup Overdue" banner without taking a backup.
  * **System Behavior:** Dismissal suppresses the banner for the current session only; on the next cold start or day rollover, the banner re-appears if still unresolved.

---

### US-DASH-03: Today's Live Schedule Stream & Quick Actions
**As** the salon owner,  
**I want** a chronological stream of today's appointments with one-tap action buttons directly on the dashboard,  
**So that** I can manage clients walking in without navigating through deep sub-menus.

#### Context & Product Design
* Each row displays: Slot time (12-hr format `hh:mm a`), Client Name, Phone, Formatted Services, Assigned Staff, Rate / Paid / Due amounts, Status Badge.
* Action Bar / Quick Context Menu on each card:
  * ✅ Mark Done
  * 💬 WhatsApp
  * ₹ Record Payment
  * ✏️ Edit
  * ❌ Cancel
  * 🗑 Delete

#### Technical & Architectural Specifications
* Observed via `AppointmentDao.observeTodayAppointments()` ordered by `time ASC`.
* Direct integration with `MarkAppointmentDoneUseCase`, `WhatsAppLauncher`, and `RecordPaymentDialog`.

#### Acceptance Criteria
* **AC-DASH-03.1 (Chronological Order):**
  * **Given** today has appointments at 02:00 PM, 10:00 AM, and 11:30 AM,
  * **When** viewing the schedule list,
  * **Then** items appear in order: 10:00 AM, 11:30 AM, 02:00 PM.
* **AC-DASH-03.2 (Immediate Balance Prompt on Mark Done):**
  * **Given** an appointment has ₹500 unpaid balance,
  * **When** tapping the "Mark Done" (✅) icon on the schedule card,
  * **Then** a dialog appears: *"Client has pending balance of ₹500. Record payment now?"* with options [Settle Full Cash], [Settle UPI], or [Skip / Keep Unpaid].

#### Edge Cases & Failure Scenarios
* **EC-DASH-03.1 (Concurrent Appointments at Same Time with Different Staff):**
  * **Scenario:** 2 different clients have appointments booked at 11:00 AM (one with Stylist A, one with Stylist B).
  * **System Behavior:** List sorting secondary key sorts by `staff ASC` then `bookedAt ASC`. Both cards render clearly with prominent staff badges.
* **EC-DASH-03.2 (Long Client Name or 8+ Selected Services):**
  * **Scenario:** Client has a very long name (e.g. "Shrimati Rajlaxmi Chhatrapati-Patil") and booked 6 services.
  * **System Behavior:** UI container uses multi-line flexible text wrapping with chip flow rows; action buttons remain pinned to the right edge and do not get pushed off-screen.
* **EC-DASH-03.3 (Rapid Spam Tapping on Inline Action Buttons):**
  * **Scenario:** User rapidly taps "Mark Done" or "WhatsApp" 4 times in half a second.
  * **System Behavior:** ViewModels apply coroutine `throttleFirst(500ms)` preventing multiple dialog popups or duplicate Intent launches.

---

## Epic 3: Appointment Management & Conflict-Free Booking (FRD §5, ARD §6.3)

### US-APT-01: Filtered Appointment Directory & Substring Search
**As** the salon owner,  
**I want** to browse, filter, and search all historical and upcoming appointments,  
**So that** I can look up any client's past visits or plan for future weeks.

#### Context & Product Design
* Top sticky bar with 4 status tabs: **All · Upcoming · Completed · Cancelled**.
* Date Range Filter chip (Defaults to current date; expandable to This Week, This Month, Custom Range).
* Search text field matching client name or 10-digit phone number with instant query filtering.

#### Technical & Architectural Specifications
* **Query:** Room query with `LIKE %:query%` indexed on `phone` and `date`.
* **State Management:** `AppointmentsUiState` emits filtered list without blocking main thread.

#### Acceptance Criteria
* **AC-APT-01.1 (Status Filtering):**
  * **Given** 10 total appointments across statuses,
  * **When** selecting the "Upcoming" filter tab,
  * **Then** only appointments with `date >= today` and `status == "booked"` are displayed.
* **AC-APT-01.2 (Search by Substring Phone or Name):**
  * **Given** appointments for "Sunita Sharma" (9876543210) and "Pooja Patil" (9123456789),
  * **When** typing "9876" in the search box,
  * **Then** only Sunita Sharma's appointment is shown.
* **AC-APT-01.3 (Empty Filter State):**
  * **Given** no appointments match the selected filter,
  * **Then** a friendly empty state illustration with *"No appointments found"* and a *"Clear Filters"* button is rendered.

#### Edge Cases & Failure Scenarios
* **EC-APT-01.1 (Special Characters / SQL Injection Probing in Search):**
  * **Scenario:** User enters characters like `' OR '1'='1`, `%`, `_`, or quotes in the search box.
  * **System Behavior:** Room parameter binding escapes queries securely; SQLite wildcard characters `%` and `_` are escaped so they do not return unexpected full-table dumps.
* **EC-APT-01.2 (Search Query Formatted with Spaces or Country Code):**
  * **Scenario:** User types `+91 98765 43210` or `09876543210` in search.
  * **System Behavior:** `PhoneSanitizer` cleans the input digits in memory to `9876543210`, immediately locating the client record.
* **EC-APT-01.3 (Inverted Custom Date Range Selection):**
  * **Scenario:** User sets Date Range with Start Date = 25-Nov and End Date = 20-Nov.
  * **System Behavior:** UI date picker validates `endDate >= startDate`; if violated, the system automatically swaps the dates or shows an error tooltip: *"End date cannot precede start date"*.

---

### US-APT-02: Conflict-Free Manual Booking & Dynamic Slot Generation
**As** the salon owner,  
**I want** the booking form to dynamically show available time slots and disable already-booked times,  
**So that** I never accidentally double-book a time slot or book an appointment in the past.

#### Context & Product Design
* **Date Selection:** Date picker defaulting to today (past dates grayed out and non-selectable).
* **Slot Grid:** Grid of time chips generated based on salon operating hours (e.g., 10:00 AM to 08:00 PM in 30-minute intervals).
* **Disabled States:** Booked slots appear grayed out with a red/orange badge labeled **"Full"** and cannot be tapped.
* Past time slots for *today's date* are automatically disabled.

#### Technical & Architectural Specifications
* **Domain:** `GenerateAvailableSlotsUseCase` computes slots from `SettingsRepository.getOperationsConfig()` (`open`, `close`, `slot`) using `TimeProvider`.
* **Clash Checking:** SQL indexed lookup `SELECT COUNT(*) FROM appointments WHERE date = :date AND time = :time AND status != 'cancel'`.
* **Atomic Insert:** `BookAppointmentUseCase` re-verifies clash in a Room transaction before inserting to prevent race conditions.

#### Acceptance Criteria
* **AC-APT-02.1 (Clash Detection):**
  * **Given** an appointment already exists at 03:00 PM on 2026-11-20 with status "booked",
  * **When** the owner opens the booking form for 2026-11-20,
  * **Then** the 03:00 PM chip displays the "Full" badge and is disabled.
* **AC-APT-02.2 (Cancelled Slot Frees Up):**
  * **Given** the 03:00 PM appointment on 2026-11-20 is marked as "cancel",
  * **When** the booking form for 2026-11-20 is opened,
  * **Then** the 03:00 PM chip becomes selectable and shows no "Full" badge.
* **AC-APT-02.3 (Today's Past Slot Invalidation):**
  * **Given** current device time is 02:15 PM,
  * **When** booking an appointment for today,
  * **Then** all slots up to and including 02:00 PM are disabled, and 02:30 PM is the first available slot.

#### Edge Cases & Failure Scenarios
* **EC-APT-02.1 (Booking at Exact Salon Closing Time):**
  * **Scenario:** Salon closes at 08:00 PM with 30-minute slots. A slot is calculated at 08:00 PM.
  * **System Behavior:** Slot calculator calculates last bookable slot as `closing_time - slot_interval` (07:30 PM). 08:00 PM is never generated because a service cannot be completed after doors close.
* **EC-APT-02.2 (Device Clock Shifted into Past):**
  * **Scenario:** User changes phone time backwards to 9:00 AM when true time is 3:00 PM.
  * **System Behavior:** The app evaluates against local `TimeProvider`. If past-slot checks rely on device clock, the transaction validation also verifies monotonic time checks or warns if timestamps drift backwards.
* **EC-APT-02.3 (Changing Date While Conflicting Slot Selected):**
  * **Scenario:** User selects 11:00 AM on Monday, then switches date picker to Tuesday where 11:00 AM is already "Full".
  * **System Behavior:** `BookingViewModel` re-validates the currently selected time slot on date change; if unavailable on the newly selected date, selection resets to `null` with a prompt: *"Please choose an open time slot"*.

---

### US-APT-03: Multi-Service Selection, Real-Time Subtotal, Discount & Advance
**As** the salon owner,  
**I want** to select multiple services from my catalog, apply an optional discount, and record an advance payment during booking,  
**So that** the total billing amount and balance due are calculated automatically and accurately.

#### Context & Product Design
* Multi-select chip group of active services from catalog showing name and price (e.g. `[+ Facial ₹800]`, `[+ Haircut ₹300]`).
* Dynamic live calculation:
  * `Subtotal = sum(selected services)`
  * `Rate = Subtotal - Discount`
  * `Balance Due = Rate - Advance Paid`
* Input validations: Client phone must be sanitized to exactly 10 digits; Name cannot be blank; at least 1 service must be selected.

#### Technical & Architectural Specifications
* **Domain:** `ValidateBookingDraftUseCase` checks phone regex (`^[6-9]\d{9}$`), positive integers for discount/advance, and ensures `advance <= rate`.
* **Entity Mapping:** Formatted service names stored in `AppointmentEntity.svc` (e.g. `"Facial + Haircut"`) and JSON array of IDs in `AppointmentEntity.serviceIds` (for rebooking).

#### Acceptance Criteria
* **AC-APT-03.1 (Live Calculation):**
  * **Given** the owner selects "Keratin Spa" (₹2,000) and "Threading" (₹100),
  * **When** entering Discount ₹200 and Advance ₹500,
  * **Then** Subtotal displays ₹2,100, Final Rate displays ₹1,900, and Balance Due displays ₹1,400.
* **AC-APT-03.2 (Phone Number Validation):**
  * **Given** the owner types `98765` (5 digits) or `ABC` in the phone field,
  * **When** tapping "Save Appointment",
  * **Then** the form highlights the phone field with *"Enter valid 10-digit mobile number"* and prevents submission.
* **AC-APT-03.3 (Advance Exceeding Rate Warning):**
  * **Given** Final Rate is ₹1,000,
  * **When** entering Advance ₹1,200,
  * **Then** an inline error displays *"Advance cannot exceed total bill amount"*.

#### Edge Cases & Failure Scenarios
* **EC-APT-03.1 (Discount Exceeding Subtotal):**
  * **Scenario:** Selected services total ₹800. User accidentally enters Discount of ₹1,000.
  * **System Behavior:** `ValidateBookingDraftUseCase` catches error; field highlights in red: *"Discount cannot exceed subtotal (₹800)"*. Form submission button remains disabled.
* **EC-APT-03.2 (Service Deselection Invalidating Prior Advance):**
  * **Scenario:** User selects Services totaling ₹2,000, enters Advance ₹1,500. User then deselects a ₹1,000 service (new subtotal ₹1,000).
  * **System Behavior:** Subtotal drops to ₹1,000; the form detects that existing Advance (₹1,500) now exceeds new Rate (₹1,000), flags the advance field, and prompts the owner to adjust it.
* **EC-APT-03.3 (Leading Zeroes and Non-Numeric Symbols in Money Fields):**
  * **Scenario:** User pastes `₹500` or `00500` or `.50` into discount field.
  * **System Behavior:** Custom `AmountInput` visual transformation strips non-digit characters and normalizes leading zeroes to valid integer rupees.

---

### US-APT-04: Appointment Rescheduling & Conflict-Aware Editing (Self-Exclusion)
**As** the salon owner,  
**I want** to edit existing appointment details including time, date, services, and staff,  
**So that** I can accommodate client rescheduling requests without false conflict errors.

#### Context & Product Design
* Edit sheet pre-populates with current appointment data.
* If editing an appointment currently at 11:00 AM on 2026-11-25, the 11:00 AM slot must remain selectable for *this* appointment (not marked "Full" by its own existing record).

#### Technical & Architectural Specifications
* **Domain:** `EditAppointmentUseCase` delegates to `AppointmentDao.checkClashExcludingId(date, time, excludeAppointmentId)`.
* **Integrity:** `bookedAt` creation timestamp remains immutable.

#### Acceptance Criteria
* **AC-APT-04.1 (Self-Exclusion from Clash Check):**
  * **Given** Appointment #101 is scheduled at 11:00 AM on 2026-11-25,
  * **When** opening Appointment #101 in the Edit sheet for that same date,
  * **Then** 11:00 AM is active and selectable (not marked "Full").
* **AC-APT-04.2 (Clash Prevention on Reschedule):**
  * **Given** Appointment #102 is already booked at 04:00 PM on 2026-11-25,
  * **When** editing Appointment #101 and attempting to switch to 04:00 PM,
  * **Then** the 04:00 PM chip is marked "Full" and cannot be chosen.
* **AC-APT-04.3 (Financial Recalculation on Service Edit):**
  * **Given** an appointment has Paid = ₹500 and Rate was ₹1,000,
  * **When** the owner adds an extra service of ₹500 (new Rate = ₹1,500),
  * **Then** saving updates Balance Due to ₹1,000 and updates Dashboard metrics immediately.

#### Edge Cases & Failure Scenarios
* **EC-APT-04.1 (Editing Completed / Cancelled Appointments):**
  * **Scenario:** Owner attempts to edit date/time of an appointment that is already marked "done" or "cancel".
  * **System Behavior:** Date and time slot selectors are locked/read-only with an explanation: *"To reschedule, first restore appointment status to Booked"*. Service notes and staff notes remain editable.
* **EC-APT-04.2 (Rescheduling to a Past Date):**
  * **Scenario:** Owner accidentally picks yesterday's date in the date picker during edit.
  * **System Behavior:** Date picker disables dates `< today`; validation rejects past date inputs.
* **EC-APT-04.3 (Concurrent Edit on Multiple Devices After Backup Restore):**
  * **Scenario:** An appointment ID is modified on two instances and imported.
  * **System Behavior:** Single-device architecture ensures local Room SQLite acts as the single source of truth; updates execute sequentially via Room primary key.

---

### US-APT-05: Status Lifecycle Transitions & Accidental Tap Undo
**As** the salon owner,  
**I want** to transition appointments between Booked, Done, and Cancelled, with a quick Undo option,  
**So that** my daily schedule stays accurate and accidental status taps can be corrected instantly without re-typing.

#### Context & Product Design
* Status transitions:
  * `booked -> done` (Prompts for outstanding balance settlement)
  * `booked -> cancel` (Releases slot)
  * `done -> booked` (Undo action)
  * `cancel -> booked` (Restores to active, provided slot hasn't been taken)
* Transient snackbar with **"Undo"** action displayed for 5 seconds after status change.

#### Technical & Architectural Specifications
* **Domain:** `MarkAppointmentDoneUseCase`, `CancelAppointmentUseCase`, `UndoAppointmentStatusUseCase`.
* **State Machine Guard:** If undoing a cancellation, the system re-validates that the time slot is still free before restoring.

#### Acceptance Criteria
* **AC-APT-05.1 (Mark Done Transition):**
  * **Given** an appointment with status "booked",
  * **When** the owner taps "Mark Done",
  * **Then** status updates to "done", the card changes visual style (green chip), and a snackbar displays *"Appointment marked as Done [Undo]"*.
* **AC-APT-05.2 (Undo Action):**
  * **Given** the snackbar is visible with the "Undo" button,
  * **When** the owner taps "Undo" within 5 seconds,
  * **Then** the status reverts back to "booked" and financial KPIs revert accordingly.
* **AC-APT-05.3 (Undo Cancel Slot Clash):**
  * **Given** Appointment A was cancelled, and Appointment B subsequently booked the same slot,
  * **When** attempting to Undo the cancellation of Appointment A,
  * **Then** an error dialog warns *"Slot has already been taken by another appointment. Please reschedule."* and keeps Appointment A cancelled.

#### Edge Cases & Failure Scenarios
* **EC-APT-05.1 (Rapid Multi-State Toggling):**
  * **Scenario:** User rapidly taps Done -> Cancel -> Undo in quick succession.
  * **System Behavior:** Status mutations run sequentially inside Room coroutine transactions. State transitions strictly follow the state machine (`booked` <-> `done`, `booked` <-> `cancel`); direct `done` <-> `cancel` is disallowed without passing through `booked`.
* **EC-APT-05.2 (Screen Navigation While Snackbar is Displayed):**
  * **Scenario:** User marks appointment Done, sees the Undo snackbar, and immediately taps to the Settings screen.
  * **System Behavior:** Snackbar state does not leak across navigation destinations; status remains "Done" unless explicitly undone before leaving the screen.
* **EC-APT-05.3 (Undoing a Mark Done with Auto-Settled Balance):**
  * **Scenario:** User marked Done and chose to settle the remaining ₹500 balance, then taps Undo.
  * **System Behavior:** Undo status reverts appointment to "booked" and optionally prompts: *"Revert recorded settlement payment of ₹500 as well?"* to preserve financial book integrity.

---

### US-APT-06: Hard Deletion of Appointment with Guardrail
**As** the salon owner,  
**I want** to permanently delete a test or erroneous appointment record with a confirmation prompt,  
**So that** my records remain clean while preventing accidental data loss.

#### Context & Product Design
* Delete action requires confirming a Material 3 alert dialog: *"Delete Appointment? This cannot be undone."* with destructive red button [Delete].

#### Technical & Architectural Specifications
* `DeleteAppointmentUseCase` invokes `AppointmentDao.deleteById(id)`.
* Deletion is atomic and cascades or deletes associated payment entries if audit table is enabled.

#### Acceptance Criteria
* **AC-APT-06.1 (Confirmation Dialog):**
  * **Given** the owner taps the trash icon on an appointment row,
  * **When** the confirmation dialog appears and the owner taps "Cancel",
  * **Then** the appointment remains untouched in the database.
* **AC-APT-06.2 (Permanent Deletion):**
  * **Given** the owner confirms deletion by tapping "Delete",
  * **Then** the record is deleted from SQLite, removed from the UI list, and all KPIs update.

#### Edge Cases & Failure Scenarios
* **EC-APT-06.1 (Double Tapping "Delete" in Dialog):**
  * **Scenario:** User rapidly double-clicks the destructive [Delete] button.
  * **System Behavior:** Dialog dismisses on first click, and `deleteById` idempotent SQL executes safely without throwing `EntityNotFoundException` on second invoke.
* **EC-APT-06.2 (Deleting Appointment with Recorded Historical Payments):**
  * **Scenario:** An appointment with ₹2,000 recorded in the payments table is targeted for deletion.
  * **System Behavior:** Dialog warns: *"This appointment has recorded payments totaling ₹2,000. Deleting will adjust historical revenue reports. Proceed?"*.
* **EC-APT-06.3 (Active Background Intent While Deleting):**
  * **Scenario:** WhatsApp was opened to message the client, and in split-screen/pip the appointment is deleted.
  * **System Behavior:** External intent continues unaffected; on return, app updates list gracefully.

---

### US-APT-07: In-line Payment Recording & Overpayment Guard
**As** the salon owner,  
**I want** to record incremental cash or UPI payments against any appointment,  
**So that** I can track partial payments, advances, and final settlements accurately.

#### Context & Product Design
* Dedicated bottom sheet / dialog:
  * Shows current Total Rate, Already Paid, and Remaining Balance.
  * Number field defaulting to the exact remaining balance.
  * Payment Mode toggle: **Cash** or **UPI**.
* If entered amount exceeds the outstanding balance, a warning prompt asks to confirm overpayment.

#### Technical & Architectural Specifications
* **Domain:** `RecordPaymentUseCase(appointmentId, amount, mode)`.
* Updates `AppointmentEntity.paid += amount`.
* Optionally writes to `PaymentEntity(appointmentId, amount, mode, timestamp)`.

#### Acceptance Criteria
* **AC-APT-07.1 (Record Outstanding Balance):**
  * **Given** an appointment has Rate ₹1,500 and Paid ₹500 (Balance ₹1,000),
  * **When** the owner opens Record Payment, enters ₹1,000 Cash, and submits,
  * **Then** Paid becomes ₹1,500, Balance becomes ₹0, and the status chip reflects full settlement.
* **AC-APT-07.2 (Overpayment Confirmation Dialog):**
  * **Given** Remaining Balance is ₹300,
  * **When** the owner enters ₹500,
  * **Then** a warning appears: *"Amount entered (₹500) exceeds balance due (₹300). Confirm extra payment/tip?"*, requiring explicit confirmation before saving.

#### Edge Cases & Failure Scenarios
* **EC-APT-07.1 (Zero or Negative Payment Entry):**
  * **Scenario:** Owner enters ₹0 or negative values in the payment dialog.
  * **System Behavior:** Save button remains disabled; inline note indicates: *"Payment amount must be greater than ₹0"*.
* **EC-APT-07.2 (Payment on a Cancelled Appointment):**
  * **Scenario:** Client pays cancellation fee or settles dues for a cancelled slot.
  * **System Behavior:** Allowed with an explicit prompt: *"This appointment is Cancelled. Record payment anyway?"*; revenue updates correctly in collected totals.
* **EC-APT-07.3 (Multiple Rapid Partial Payments):**
  * **Scenario:** Two ₹200 payments entered within 2 seconds of each other.
  * **System Behavior:** Executed inside `@Transaction` ensuring SQLite atomic increments (`paid = paid + :amount`) without lost update race conditions.

---

## Epic 4: Service Catalog & Photo Management (FRD §6, ARD §6.4)

### US-SVC-01: Service Catalog Browsing & Seed Data Initialization
**As** the salon owner,  
**I want** the app to come pre-loaded with standard salon services upon first install,  
**So that** I can begin booking appointments immediately without entering dozens of services from scratch.

#### Context & Product Design
* Grid or list of services showing reference thumbnail, service name, and price in INR.
* Default catalog seeded on Room first-run callback: Haircut, Threading, Gold Facial, Keratin Spa, Manicure, Pedicure, Bridal Package.

#### Technical & Architectural Specifications
* Handled via `RoomDatabase.Callback` in `SeedCallback.kt`.
* Inserts default `ServiceEntity` items with unique IDs (`s1`..`s7`) and placeholder rates.

#### Acceptance Criteria
* **AC-SVC-01.1 (First-Run Seed Verification):**
  * **Given** a brand-new installation of the app,
  * **When** navigating to the Services screen for the first time,
  * **Then** at least 7 default services (including Gold Facial, Haircut, Bridal Package) are pre-populated and visible.
* **AC-SVC-01.2 (Subsequent Launches No-Op):**
  * **Given** the database has already been seeded,
  * **When** restarting the app,
  * **Then** the seed callback does not duplicate or overwrite edited services.

#### Edge Cases & Failure Scenarios
* **EC-SVC-01.1 (User Deletes All Seed Services):**
  * **Scenario:** Owner manually deletes all pre-loaded services because they offer completely different packages.
  * **System Behavior:** Seed callback checks database version/flag and does NOT re-inject deleted seed items on next cold start. Empty catalog state prompts: *"No services found. Tap '+' to create custom services"*.
* **EC-SVC-01.2 (Process Termination During Seed Insertion):**
  * **Scenario:** Phone dies precisely when `onCreate` SQLite seed transaction is halfway through writing the 7 services.
  * **System Behavior:** SQLite wraps seeding in a single atomic transaction; partial writes roll back automatically, and subsequent launch cleanly re-seeds all 7 items.
* **EC-SVC-01.3 (Empty Catalog Booking Attempt):**
  * **Scenario:** Owner tries to book an appointment when 0 services exist.
  * **System Behavior:** Booking sheet displays a warning banner: *"Please add services to catalog before booking"* with a direct link to the Services screen.

---

### US-SVC-02: Service Catalog CRUD with Appointment Snapshot Safety
**As** the salon owner,  
**I want** to add, edit, and delete services in my catalog without altering or breaking existing appointments,  
**So that** my historical appointment records reflect the exact price and name charged at the time of booking.

#### Context & Product Design
* Add/Edit Service sheet with fields: Service Name (text), Rate (numeric ₹), Reference Photo selector.
* Deleting a service requires confirmation.

#### Technical & Architectural Specifications
* **Domain:** `AddServiceUseCase`, `UpdateServiceUseCase`, `DeleteServiceUseCase`.
* **Snapshot Safety:** `AppointmentEntity.svc` stores a frozen text snapshot (e.g. `"Gold Facial ₹800"`); altering or deleting the service in `ServiceEntity` does NOT cascade changes to existing appointments.

#### Acceptance Criteria
* **AC-SVC-02.1 (Price Change Does Not Alter Past Bookings):**
  * **Given** an existing appointment booked with "Gold Facial" at ₹800,
  * **When** the owner edits "Gold Facial" in the Service Catalog to ₹1,000,
  * **Then** the existing appointment retains its original ₹800 rate and billing calculations.
* **AC-SVC-02.2 (Delete Service Retains Appointment History):**
  * **Given** an appointment references "Keratin Spa",
  * **When** "Keratin Spa" is deleted from the catalog,
  * **Then** past and upcoming appointments containing "Keratin Spa" still display the service name clearly.

#### Edge Cases & Failure Scenarios
* **EC-SVC-02.1 (Duplicate Service Name Entry):**
  * **Scenario:** Owner attempts to add "Haircut" when "Haircut" already exists.
  * **System Behavior:** `AddServiceUseCase` checks case-insensitive name uniqueness; warns: *"A service named 'Haircut' already exists. Please choose a distinct name (e.g. Haircut - Men)"*.
* **EC-SVC-02.2 (Blank or Extreme Service Rate):**
  * **Scenario:** Owner leaves Rate blank or enters ₹9,99,999.
  * **System Behavior:** Rate field enforces positive integer range (₹1 to ₹1,00,000); blank entry highlights field: *"Please enter service rate"*.
* **EC-SVC-02.3 (Deleting a Service Present in Tomorrow's Bookings):**
  * **Scenario:** Owner deletes "Bridal Makeup" which is scheduled for tomorrow.
  * **System Behavior:** Dialog warns: *"This service is assigned to 2 upcoming appointments. Existing bookings will retain this service snapshot, but it will be unavailable for new bookings. Confirm deletion?"*.

---

### US-SVC-03: Service Reference Photo Acquisition & Local Compression
**As** the salon owner,  
**I want** to capture or select a hairstyle/makeup photo for each service, with automatic image compression,  
**So that** the catalog looks visually appealing while keeping app storage lightweight and fast on low-memory devices.

#### Context & Product Design
* Photo source chooser: **Camera** or **Gallery**.
* "Reset Photo" button to revert to default aesthetic vector icon.

#### Technical & Architectural Specifications
* **Image Processing:** `ImageCompressor` scales the image longest edge to **320 px** and compresses to **JPEG @ 78% quality**.
* **Storage Pattern (ADR-0004):** Image file saved to internal storage `context.filesDir/images/{uuid}.jpg`. Database `ServiceEntity.imagePath` stores relative file path.
* Zero external internet transmission.

#### Acceptance Criteria
* **AC-SVC-03.1 (Camera Image Acquisition):**
  * **Given** the owner creates a new service,
  * **When** choosing "Take Photo" and capturing an image via the camera,
  * **Then** the image is scaled to max 320 px longest edge, compressed to JPEG 78%, saved in internal app storage, and rendered as the service card thumbnail.
* **AC-SVC-03.2 (Disk Space Optimization):**
  * **Given** an original camera photo of size 5 MB,
  * **When** processed by the app,
  * **Then** the saved image file on disk does not exceed 50 KB.
* **AC-SVC-03.3 (Reset to Default Icon):**
  * **Given** a service with a custom photo,
  * **When** tapping "Reset to Default Photo",
  * **Then** the stored JPEG file is deleted from internal storage, `imagePath` set to `null`, and the fallback icon is displayed.

#### Edge Cases & Failure Scenarios
* **EC-SVC-03.1 (Camera Permission Denied by User):**
  * **Scenario:** Owner taps "Take Photo" and chooses "Don't allow" on Android camera runtime permission prompt.
  * **System Behavior:** App falls back gracefully without crashing, displays an inline snackbar: *"Camera permission is required to capture photos directly. You can still pick photos from Gallery"*, and offers the Gallery picker.
* **EC-SVC-03.2 (Ultra-High Resolution Image Selected):**
  * **Scenario:** Owner selects a 108 MP RAW/JPEG photo from an external SD card.
  * **System Behavior:** `ImageCompressor` uses `BitmapFactory.Options.inSampleSize` sub-sampling during decoding to prevent `OutOfMemoryError` (OOM) before downscaling to 320 px.
* **EC-SVC-03.3 (Corrupted or Non-Image File Chosen):**
  * **Scenario:** User selects a damaged file or renamed PDF from file manager.
  * **System Behavior:** Bitmap decoder catches `null` result, logs warning, displays toast *"Unable to load selected image"*, and preserves prior thumbnail.
* **EC-SVC-03.4 (Device Storage Completely Exhausted):**
  * **Scenario:** Phone has < 1 MB storage remaining when attempting to save compressed JPEG.
  * **System Behavior:** File write throws `IOException`; app catches exception, aborts file write, and informs user: *"Device storage is full. Free up space to save photos"*.

---

## Epic 5: WhatsApp Reminders Queue (FRD §7, ARD §6.6)

### US-REM-01: Tomorrow's Reminder Queue Monitoring
**As** the salon owner,  
**I want** a dedicated screen showing all booked clients for tomorrow with their reminder status,  
**So that** I can review who is coming in and know who has already been notified.

#### Context & Product Design
* Dedicated "Reminders" tab or screen.
* Shows date header for tomorrow with total count.
* List items show: Client Name, Scheduled Time, Booked Services, Balance Due, and a status chip:
  * Gray chip: `Not Sent`
  * Green chip with checkmark: `Reminded` (if sent today for tomorrow)
* Prominent 1-tap **"Remind"** button per row.

#### Technical & Architectural Specifications
* **Domain:** `GetTomorrowQueueUseCase` queries `AppointmentDao.getAppointmentsByDate(tomorrow)` where `status == 'booked'`.
* Reminded state determined by checking `AppointmentEntity.reminded == tomorrowDateString`.

#### Acceptance Criteria
* **AC-REM-01.1 (Tomorrow Queue Scope):**
  * **Given** today is 2026-11-14, with appointments on 2026-11-14 (today), 2026-11-15 (tomorrow), and 2026-11-16 (day after),
  * **When** opening the Reminders screen,
  * **Then** only appointments scheduled for 2026-11-15 with status "booked" are listed.
* **AC-REM-01.2 (Reminded Flag Display):**
  * **Given** an appointment has `reminded = '2026-11-15'`,
  * **When** viewing the queue,
  * **Then** its status chip displays "Reminded" with a green checkmark.

#### Edge Cases & Failure Scenarios
* **EC-REM-01.1 (Zero Appointments for Tomorrow):**
  * **Scenario:** No appointments booked for tomorrow.
  * **System Behavior:** Screen displays an aesthetic illustration with message: *"No appointments scheduled for tomorrow. Enjoy your day!"*.
* **EC-REM-01.2 (Appointments Added Late at Night):**
  * **Scenario:** A new appointment is booked for tomorrow at 11:30 PM.
  * **System Behavior:** Room Flow emits automatically; the new client appears instantly in the queue with "Not Sent" status.
* **EC-REM-01.3 (Cancelled Appointments Excluded from Queue):**
  * **Scenario:** Client was booked for tomorrow, but cancelled in the afternoon.
  * **System Behavior:** Cancelled appointment is immediately filtered out of tomorrow's reminder queue so owner does not mistakenly message them.

---

### US-REM-02: 1-Tap Offline WhatsApp Dispatch & Optimistic Status Tracking
**As** the salon owner,  
**I want** to tap a single button to open WhatsApp with a pre-filled personalized reminder message,  
**So that** I don't have to copy-paste numbers or type long messages manually.

#### Context & Product Design
* Tapping **"Remind"** builds the personalized text, copies or url-encodes it, and launches the official WhatsApp application directly to that client's chat.
* On returning to the app, the row immediately marks as "Reminded", accompanied by a transient snackbar with **"Undo"**.

#### Technical & Architectural Specifications
* **Platform Intent:** Dispatched via `Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/91<phone>?text=<encoded_msg>"))`.
* **Zero Permission:** Does NOT require `android.permission.INTERNET` because the OS delegates HTTP/URL routing to the installed WhatsApp app package.
* **Domain:** `MarkRemindedUseCase(appointmentId, tomorrowDate)` updates Room; `UndoMarkRemindedUseCase` restores state if tapped.

#### Acceptance Criteria
* **AC-REM-02.1 (WhatsApp Intent Launch):**
  * **Given** a client "Pooja" with phone `9876543210`,
  * **When** the owner taps "Remind",
  * **Then** the Android system fires an `ACTION_VIEW` intent targeting `https://wa.me/919876543210?text=...`, opening WhatsApp directly to Pooja's chat with the formatted message ready in the input field.
* **AC-REM-02.2 (Return State & Optimistic Update):**
  * **Given** WhatsApp was launched,
  * **When** the owner switches back to the Unique Makeover app,
  * **Then** Pooja's appointment reflects "Reminded", and a snackbar displays *"Reminder marked as sent [Undo]"*.
* **AC-REM-02.3 (Undo Sent Reminder):**
  * **Given** the reminder was marked sent,
  * **When** the owner taps "Undo" in the snackbar,
  * **Then** `AppointmentEntity.reminded` is reset to `null` and the chip reverts to "Not Sent".

#### Edge Cases & Failure Scenarios
* **EC-REM-02.1 (WhatsApp Not Installed on Device):**
  * **Scenario:** The tablet or phone does not have WhatsApp or WhatsApp Business installed.
  * **System Behavior:** System catches `ActivityNotFoundException`; displays a helpful dialog: *"WhatsApp is not installed on this device. Would you like to copy the message to clipboard?"* with [Copy Message] option.
* **EC-REM-02.2 (User Aborts / Cancels Inside WhatsApp Without Sending):**
  * **Scenario:** Owner taps "Remind", WhatsApp opens, but owner presses Back without tapping send in WhatsApp.
  * **System Behavior:** Because Android intent boundaries do not return private third-party chat status, the app updates status optimistically but provides the 5-second **Undo** snackbar and an inline context menu to "Mark as Not Sent".
* **EC-REM-02.3 (Client Has Non-Standard Phone Number):**
  * **Scenario:** Phone number in database has 9 digits or contains formatting artifacts from legacy data.
  * **System Behavior:** `WhatsAppLauncher` verifies `PhoneSanitizer.isValid(phone)`; if invalid, blocks intent and flags row: *"Invalid mobile number. Please edit appointment to correct"*.

---

### US-REM-03: Dynamic Template Token Substitution & Customization
**As** the salon owner,  
**I want** to customize the WhatsApp reminder template using dynamic placeholders and test it with a live preview,  
**So that** my messages sound friendly, local, and contain exact appointment information.

#### Context & Product Design
* Multiline template editor located in Settings → Communication.
* Supported Placeholders pill buttons for easy insertion:
  * `{name}`, `{shop}`, `{service}`, `{date}`, `{time}`, `{rate}`, `{due}`, `{shopnum}`
* "Reset to Default" button.
* Live Preview card beneath the editor showing how a real message looks with sample data.

#### Technical & Architectural Specifications
* **Domain:** `FormatReminderTemplateUseCase` replaces all 8 placeholder tokens using strict string substitution.
* Sanitizes special characters and properly encodes for URI transmission.

#### Acceptance Criteria
* **AC-REM-03.1 (All 8 Placeholders Replaced):**
  * **Given** template: *"Hello {name}, your appointment at {shop} for {service} is on {date} at {time}. Total: ₹{rate}, Due: ₹{due}. Call {shopnum}."*,
  * **When** formatted for client Priya (Date: 15-Nov, Time: 03:00 PM, Service: Facial, Rate: 800, Due: 300, Shop: Unique Makeover, ShopNum: 9999999999),
  * **Then** the output contains no unparsed `{...}` curly braces and reads: *"Hello Priya, your appointment at Unique Makeover for Facial is on 15-Nov at 03:00 PM. Total: ₹800, Due: ₹300. Call 9999999999."*.
* **AC-REM-03.2 (Reset Template):**
  * **Given** the owner made custom edits to the template,
  * **When** tapping "Reset to Default",
  * **Then** the text field restores the factory default reminder template and updates the live preview.

#### Edge Cases & Failure Scenarios
* **EC-REM-03.1 (Malformed or Unknown Placeholders):**
  * **Scenario:** Owner types typos in tokens like `{client_name}` or `{tim}` or unclosed `{name`.
  * **System Behavior:** Unknown tokens remain literal strings without crashing; the template editor flags unclosed braces with a warning indicator.
* **EC-REM-03.2 (Special Characters / Emojis in Template):**
  * **Scenario:** Template includes emojis (✨💇‍♀️), quotes, Marathi script, and ampersands (`&`).
  * **System Behavior:** `Uri.encode()` properly percent-encodes all Unicode and query parameter delimiters so WhatsApp parses the full text without truncation at the first `&` character.
* **EC-REM-03.3 (Blank Template Saved):**
  * **Scenario:** Owner deletes all text in the template and taps Save.
  * **System Behavior:** Validation blocks save with message: *"Template cannot be empty"*, offering [Restore Default Template].

---

## Epic 6: Client CRM & Private Treatment Notes (FRD §8, ARD §6.5)

### US-CRM-01: Client Directory & Automated History Aggregation
**As** the salon owner,  
**I want** the app to automatically build a client directory by grouping appointments by phone number,  
**So that** I don't have to manually maintain a separate client address book.

#### Context & Product Design
* Clean list of unique clients.
* Each card displays: Most recent client name, sanitized phone number, total visits count, lifetime spend (₹), date of last visit, and last services availed.

#### Technical & Architectural Specifications
* **Data Layer:** `ClientProfileRepositoryImpl` derives profiles using SQLite aggregate query:
  ```sql
  SELECT phone, name, COUNT(*) as visits, SUM(rate) as spend, MAX(date) as lastVisit
  FROM appointments WHERE status != 'cancel' GROUP BY phone ORDER BY lastVisit DESC
  ```
* No duplicate records for the same client phone number.

#### Acceptance Criteria
* **AC-CRM-01.1 (Aggregate Computation):**
  * **Given** client "Neha" (phone `9822001122`) has 3 appointments (Rate ₹500, ₹1,200, ₹800),
  * **When** viewing the Client Directory,
  * **Then** Neha appears as a single row showing: Visits: 3, Total Spend: ₹2,500, and the date of her latest appointment.
* **AC-CRM-01.2 (Cancelled Excluded from Spend):**
  * **Given** a client has 1 completed visit of ₹1,000 and 1 cancelled booking of ₹2,000,
  * **Then** visits count is 1 and spend is ₹1,000.

#### Edge Cases & Failure Scenarios
* **EC-CRM-01.1 (Name Variations Across Bookings):**
  * **Scenario:** Same phone `9822001122` was entered as "Neha" on visit 1 and "Neha Kulkarni" on visit 2.
  * **System Behavior:** SQLite query selects the name associated with the most recent `bookedAt` timestamp (`MAX(bookedAt)`), keeping profile display current.
* **EC-CRM-01.2 (Clients with Large Historical Visit Volume):**
  * **Scenario:** A loyal client has 100+ appointments over 3 years.
  * **System Behavior:** SQLite aggregation runs on indexed columns (`phone`, `status`); queries return in < 50 ms without blocking the UI thread.
* **EC-CRM-01.3 (Clients with Only Cancelled Bookings):**
  * **Scenario:** A registered number has only 2 cancelled bookings and 0 completed visits.
  * **System Behavior:** Client still appears in directory so contact history isn't lost, showing 0 Completed Visits and ₹0 Spend with a distinct badge.

---

### US-CRM-02: Instant Client Search by Name or Mobile
**As** the salon owner,  
**I want** to search my client directory by typing any part of a name or phone number,  
**So that** I can find a client's records in seconds while they are on the phone.

#### Context & Product Design
* Search input bar with debounce (150ms).
* Instant substring match on both Name and 10-digit Phone.

#### Technical & Architectural Specifications
* `GetClientProfilesUseCase(searchQuery)` applies case-insensitive substring matching.

#### Acceptance Criteria
* **AC-CRM-02.1 (Search by Partial Name):**
  * **Given** clients "Radhika Apte" and "Anjali Sharma",
  * **When** typing "radh" in search,
  * **Then** "Radhika Apte" is shown and "Anjali Sharma" is hidden.
* **AC-CRM-02.2 (Search by Partial Phone):**
  * **Given** client with phone `9822123456`,
  * **When** typing "1234",
  * **Then** that client appears in the results.

#### Edge Cases & Failure Scenarios
* **EC-CRM-02.1 (Rapid Query Modification & Coroutine Race):**
  * **Scenario:** User rapidly types "Pooja", hits backspace 3 times, and types "Priya".
  * **System Behavior:** Flow uses `restartableOrLatest` / `collectLatest`; previous query emissions are cancelled immediately, guaranteeing that stale search results never overwrite current results.
* **EC-CRM-02.2 (Non-Latin Characters / Regional Script Input):**
  * **Scenario:** User enters Marathi Devanagari script (e.g. "पूजा") in search.
  * **System Behavior:** SQLite Unicode text matching handles UTF-8 collation smoothly without throwing formatting errors.
* **EC-CRM-02.3 (Leading / Trailing Whitespace in Search):**
  * **Scenario:** User copies and pastes `" 9822123456  "` with accidental spaces.
  * **System Behavior:** Query automatically trims leading/trailing whitespace before filtering.

---

### US-CRM-03: Detailed Client History & Lifetime Metrics
**As** the salon owner,  
**I want** to open a client's profile and see their complete chronological visit history,  
**So that** I know their service preferences, spending habits, and visit frequency over time.

#### Context & Product Design
* **Header:** Client Name, Phone, Quick Call button, Quick WhatsApp button, Total Spend, Total Visits.
* **Timeline/List:** Chronological list of every past visit showing date, services taken, staff member who attended, amount charged, and status.

#### Technical & Architectural Specifications
* Presentation (`ClientDetailScreen`) observes `GetClientHistoryUseCase(phone)` querying `AppointmentDao.getAppointmentsByPhone(phone)`.

#### Acceptance Criteria
* **AC-CRM-03.1 (Complete Chronological History):**
  * **Given** a client has visited 5 times over the past 6 months,
  * **When** opening the Client Detail screen,
  * **Then** all 5 visits are listed in reverse chronological order (newest first) with full service and payment details.

#### Edge Cases & Failure Scenarios
* **EC-CRM-03.1 (Services in History Deleted from Catalog):**
  * **Scenario:** Client received "Bridal Spa v1" 6 months ago, which was subsequently deleted from the catalog.
  * **System Behavior:** Appointment entity preserves service text snapshot; historical card renders `"Bridal Spa v1"` normally with no missing data flags.
* **EC-CRM-03.2 (Zero Appointments for Number):**
  * **Scenario:** Client Detail is accessed via a direct link for a newly created note before appointments are booked.
  * **System Behavior:** History tab displays: *"No appointment history yet. Tap 'Rebook' to create an appointment"*.
* **EC-CRM-03.3 (Initiating Phone Call Without SIM Card):**
  * **Scenario:** User taps Quick Call button on a tablet with no cellular dialer installed.
  * **System Behavior:** App catches `ActivityNotFoundException` when launching `ACTION_DIAL`, showing a toast: *"No calling app available on this device"*.

---

### US-CRM-04: Private Client Skin & Hair Notes with Debounced Autosave
**As** the salon owner,  
**I want** to record private medical/beauty notes for each client (e.g. hair dye allergies, skin sensitivities, preferred stylist) that autosave without pressing a button,  
**So that** I can provide personalized care and avoid allergic reactions without worrying about losing typed notes.

#### Context & Product Design
* Dedicated multiline text area on the Client Detail screen labeled *"Private Salon Notes (Skin, Hair, Allergies)"*.
* Subtle status indicator in the corner: *"Saving..."* → *"Saved ✓"*.

#### Technical & Architectural Specifications
* **Architecture:** `SaveClientNoteUseCase` triggered via `FlowExt.debounce(500ms)` from `ClientDetailViewModel`.
* **Persistence:** Saved into dedicated `ClientNoteEntity(phone, note, updatedAt)` table keyed by phone number.

#### Acceptance Criteria
* **AC-CRM-04.1 (Autosave Debounce):**
  * **Given** the owner is typing in a client's notes: *"Allergic to ammonia bleach. Prefers herbal facial."*,
  * **When** typing stops for 500 ms,
  * **Then** the note is written to SQLite, and the indicator shows "Saved ✓".
* **AC-CRM-04.2 (Note Persistence Across Sessions):**
  * **Given** a note is saved for client phone `9822001122`,
  * **When** the app is restarted and the client's profile is reopened,
  * **Then** the saved note text is restored exactly as typed.

#### Edge Cases & Failure Scenarios
* **EC-CRM-04.1 (Navigating Away While Typing Mid-Debounce):**
  * **Scenario:** Owner types a sentence and immediately presses Android Back button before the 500 ms debounce timer fires.
  * **System Behavior:** `ViewModel.onCleared()` / `DisposableEffect` flushes the pending uncommitted note string to Room synchronously via `NonCancellable` coroutine context, guaranteeing zero data loss.
* **EC-CRM-04.2 (Very Large Treatment Notes):**
  * **Scenario:** Owner types extensive multi-year chemical history (e.g. 10,000 words).
  * **System Behavior:** Room `TEXT` column supports megabytes of UTF-8 data; Compose text field uses virtualization and vertical scrolling without memory stutter.
* **EC-CRM-04.3 (Concurrent Edit on Shared Client Phone):**
  * **Scenario:** A household shares one phone number for two sisters.
  * **System Behavior:** Single note field stores aggregated client records; UI allows multi-line paragraphs with timestamps if owner chooses.

---

### US-CRM-05: 1-Tap Quick Rebooking Pre-Fill
**As** the salon owner,  
**I want** a "Rebook" button on the client's profile that opens the booking form pre-populated with their details and last services,  
**So that** repeat bookings take less than 10 seconds to create.

#### Context & Product Design
* Prominent "Quick Rebook" action button on Client Detail screen.
* Clicking opens `BookingSheet` pre-filled with: Client Name, Phone, and the services availed in their last visit.
* Date, Time slot, and Staff fields are left open for new selection.

#### Technical & Architectural Specifications
* **Domain:** `BuildRebookDraftUseCase(phone)` fetches latest appointment, parses `serviceIds`, and initializes `BookingDraft`.

#### Acceptance Criteria
* **AC-CRM-05.1 (Pre-Fill Accuracy):**
  * **Given** client "Kavita" whose last visit was for "Hair Spa" and "Threading",
  * **When** tapping "Quick Rebook" from Kavita's profile,
  * **Then** the booking sheet opens with Name="Kavita", Phone="...", and "Hair Spa" + "Threading" already selected, with Subtotal calculated.

#### Edge Cases & Failure Scenarios
* **EC-CRM-05.1 (Historical Services No Longer in Catalog):**
  * **Scenario:** Kavita's last appointment had a promotional service that has since been deleted from the catalog.
  * **System Behavior:** `BuildRebookDraftUseCase` matches active catalog IDs; orphaned service IDs are filtered out cleanly, active services remain selected, and an inline note informs: *"Some previous promotional services are no longer in the catalog"*.
* **EC-CRM-05.2 (Client Has Zero Prior Completed Bookings):**
  * **Scenario:** Tapping Rebook on a client who only had a cancelled appointment.
  * **System Behavior:** Pre-populates Name and Phone; services selection defaults to empty for the owner to pick.
* **EC-CRM-05.3 (Pricing Changes Since Last Visit):**
  * **Scenario:** A service cost ₹500 during last visit, but catalog rate is now ₹600.
  * **System Behavior:** Rebooking draft uses the current active catalog rate (₹600), computing the new subtotal accurately.

---

## Epic 7: Offline UPI QR Code Generation & Payment Requests (FRD §9, ARD §6.7)

### US-UPI-01: Dynamic Offline UPI QR Code Generation using ZXing Core
**As** the salon owner,  
**I want** to generate a standard UPI QR code on my screen without an internet connection,  
**So that** clients can scan and pay me directly via PhonePe, Google Pay, or Paytm even during internet outages.

#### Context & Product Design
* Fullscreen/Modal QR view with automatic screen brightness boost for effortless scanning by client camera.
* Displays UPI Payee Name, UPI ID, Amount, and Transaction Note prominently below the QR.
* Works 100% in Airplane Mode.

#### Technical & Architectural Specifications
* **URI Format:** `upi://pay?pa=<upi_id>&pn=<shop_name>&am=<amount>&cu=INR&tn=<note>` (FRD §9.1).
* **QR Generation:** Uses embedded `com.google.zxing:core` (`QRCodeWriter`) to render a local `Bitmap` (512x512 px) in memory (ADR-0007). Zero network calls or external web APIs.

#### Acceptance Criteria
* **AC-UPI-01.1 (Offline QR Render):**
  * **Given** the device is in Airplane Mode (no WiFi, no mobile data),
  * **When** the owner opens the UPI QR screen for an amount of ₹850,
  * **Then** a high-resolution QR bitmap generates locally within 100 ms and displays on the screen.
* **AC-UPI-01.2 (UPI URI Conformance):**
  * **Given** salon UPI ID `uniquemakeover@upi`, Shop Name `Unique Makeover`, Amount `850.00`, and Note `Pooja - Balance Payment`,
  * **When** scanned by any UPI banking app,
  * **Then** the decoded string matches `upi://pay?pa=uniquemakeover@upi&pn=Unique%20Makeover&am=850.00&cu=INR&tn=Pooja%20-%20Balance%20Payment`.

#### Edge Cases & Failure Scenarios
* **EC-UPI-01.1 (Special Characters in Salon Name or Transaction Note):**
  * **Scenario:** Shop name contains ampersands or quotes (e.g. `Unique & Chic "Makeover"`).
  * **System Behavior:** URI parameters are strictly URL-encoded (`%26`, `%22`); UPI client apps parse payee parameters without protocol truncation.
* **EC-UPI-01.2 (Screen Brightness Restoration on Screen Exit):**
  * **Scenario:** User opens QR (screen brightness boosts to 100%), then exits screen or receives phone call.
  * **System Behavior:** Screen brightness override is scoped to window attributes of `UpiQrScreen`; exiting restores device brightness to prior user setting immediately.
* **EC-UPI-01.3 (Zero Amount in QR Request):**
  * **Scenario:** Owner attempts to generate QR for ₹0.
  * **System Behavior:** Generation is blocked; UI prompts: *"Please specify a payment amount greater than ₹0"*.

---

### US-UPI-02: Payment Amount Preset Calculations
**As** the salon owner,  
**I want** 1-tap presets for Balance Due, 30% Advance, Full Payment, and Custom Amount on the QR screen,  
**So that** I don't have to calculate percentages or manually enter amounts for standard payments.

#### Context & Product Design
* 4 segmented buttons:
  * **Balance Due** (default, pre-selected)
  * **Advance (30%)** (computed as `round(rate * 0.30)`)
  * **Full Payment** (`rate`)
  * **Custom** (enables editable number field)
* Tapping any preset regenerates the QR code instantly.

#### Technical & Architectural Specifications
* **Domain:** `ComputePresetAmountUseCase(rate, paid, preset)` handles pure integer rounding math.

#### Acceptance Criteria
* **AC-UPI-02.1 (Preset Calculations):**
  * **Given** an appointment with Total Rate = ₹1,500 and Paid = ₹0,
  * **When** selecting presets:
    * "Advance (30%)" → Amount sets to ₹450 and QR updates.
    * "Full Payment" → Amount sets to ₹1,500 and QR updates.
    * "Balance Due" → Amount sets to ₹1,500 and QR updates.
* **AC-UPI-02.2 (Custom Amount Input):**
  * **Given** the owner selects "Custom",
  * **When** typing ₹700,
  * **Then** the QR updates to request exactly ₹700.00.

#### Edge Cases & Failure Scenarios
* **EC-UPI-02.1 (Fractional Rounding on 30% Advance):**
  * **Scenario:** Total Rate is ₹355. 30% calculation is ₹106.50.
  * **System Behavior:** Domain logic rounds to nearest integer rupee (`round(355 * 0.30) = 107`), generating clean whole-rupee QR amounts for cash register compatibility.
* **EC-UPI-02.2 (Client Has Already Paid More Than 30% Advance):**
  * **Scenario:** Rate is ₹1,000, Paid is ₹400. Owner taps "Advance (30%)" (₹300).
  * **System Behavior:** Preset chip shows "Advance already covered" or is disabled, leaving "Balance Due" (₹600) selected.
* **EC-UPI-02.3 (Excessive Custom Amount Input):**
  * **Scenario:** Owner enters ₹10,00,000 in Custom field.
  * **System Behavior:** Field clamps input to maximum transaction limit of ₹1,00,000 in accordance with NPCI peer-to-merchant transaction ceilings.

---

### US-UPI-03: WhatsApp Direct UPI Payment Link Dispatch
**As** the salon owner,  
**I want** to send a clickable UPI payment request link to a client via WhatsApp,  
**So that** clients booking from home can pay their advance deposit easily on their phone.

#### Context & Product Design
* Button: *"Send Payment Link on WhatsApp"*.
* Opens WhatsApp chat with pre-filled message containing client name, services, requested amount, and the clickable `upi://pay` URI string.

#### Technical & Architectural Specifications
* Dispatched via `WhatsAppLauncher` with encoded message text containing the raw `upi://pay` URI scheme (clickable in Android WhatsApp clients).

#### Acceptance Criteria
* **AC-UPI-03.1 (WhatsApp Message Composition):**
  * **Given** client "Neha" requesting ₹300 advance for Bridal Booking,
  * **When** tapping "Send Payment Link on WhatsApp",
  * **Then** WhatsApp opens to Neha's chat with message: *"Hello Neha, please pay advance ₹300 for your booking at Unique Makeover using this link: upi://pay?pa=... Tap to pay via GPay/PhonePe/Paytm."*.

#### Edge Cases & Failure Scenarios
* **EC-UPI-03.1 (UPI Link Formatting in Strict Third-Party WhatsApp Clients):**
  * **Scenario:** Client's WhatsApp does not make raw `upi://pay` clickable in text chat.
  * **System Behavior:** Formatted text provides both the raw URI and clear fallback text: *"UPI ID: uniquemakeover@upi | Amount: ₹300"*, so client can copy-paste UPI ID manually if their messaging client disables deep links.
* **EC-UPI-03.2 (Missing Client Phone Number):**
  * **Scenario:** Standalone QR generated without an associated client appointment.
  * **System Behavior:** "Send Payment Link on WhatsApp" opens Android system contact picker to select recipient.

---

### US-UPI-04: QR Code Image File Export & Native Share Sheet
**As** the salon owner,  
**I want** to save the generated QR code as a PNG image or share it via the Android share sheet,  
**So that** I can print it for the salon front desk or send it via other local sharing apps.

#### Context & Product Design
* Actions: [Save to Gallery] and [Share QR].
* Saving displays a toast *"QR saved to Pictures/UniqueMakeover"*.

#### Technical & Architectural Specifications
* **Save:** `QrPngExporter` writes bitmap to `MediaStore.Images.Media` without requiring legacy storage permissions on Android 10+.
* **Share:** `QrShareIntentBuilder` caches PNG in app cache and creates an `ACTION_SEND` intent with a secure `FileProvider` URI.

#### Acceptance Criteria
* **AC-UPI-04.1 (Save PNG to MediaStore):**
  * **Given** a generated QR code on screen,
  * **When** the owner taps "Save to Gallery",
  * **Then** a PNG file is written to device Pictures, and an Android toast confirms successful save.
* **AC-UPI-04.2 (Native Share Sheet):**
  * **Given** the owner taps "Share QR",
  * **Then** the Android system share sheet appears with image preview and available apps.

#### Edge Cases & Failure Scenarios
* **EC-UPI-04.1 (Storage Permission on Legacy Android 8.0/9.0 Devices):**
  * **Scenario:** App is running on Android 8.0 (API 26) where `WRITE_EXTERNAL_STORAGE` was traditionally required for `MediaStore`.
  * **System Behavior:** `QrPngExporter` uses internal app cache directory or requests permission gracefully with explanation on older OS versions.
* **EC-UPI-04.2 (Cache Directory Cleaned by System During Share):**
  * **Scenario:** Cache file removed by OS memory pressure while share sheet is open.
  * **System Behavior:** Bitmap is generated into persistent temp file `cache/qr_share.png` and kept until intent completion.

---

### US-UPI-05: Missing UPI Configuration Blocking Alert
**As** the salon owner,  
**I want** the app to block QR generation and warn me if my UPI ID is not configured,  
**So that** I never display an invalid or broken QR code to a paying client.

#### Context & Product Design
* Fullscreen blocking banner replacing the QR canvas when `settings.upi` is empty.
* Message: *"Merchant UPI ID Not Configured. Please set your UPI ID in Settings to enable offline QR payments."*.
* Direct button: [Configure UPI in Settings].

#### Technical & Architectural Specifications
* UI state emits `UpiQrUiState.MissingConfiguration` which suppresses `QRCodeWriter` and deep-links to `Routes.Settings`.

#### Acceptance Criteria
* **AC-UPI-05.1 (Blocking Screen on Empty UPI):**
  * **Given** the salon UPI ID in Settings is blank `""`,
  * **When** navigating to the UPI QR screen,
  * **Then** no QR code is generated, the blocking banner is displayed, and tapping [Configure UPI] deep-links to Settings.

#### Edge Cases & Failure Scenarios
* **EC-UPI-05.1 (UPI Cleared While QR Screen Is Open):**
  * **Scenario:** User navigates to Settings, deletes UPI ID, and resumes QR Screen from backstack.
  * **System Behavior:** ViewModel re-collects `SettingsRepository.getSettings()` Flow; screen transitions dynamically to `MissingConfiguration` state.

---

## Epic 8: Salon Settings, Branding & Business Operations (FRD §10, ARD §6.8)

### US-SET-01: Salon Branding Setup (Name, Tagline, Circular Logo & Wallpaper)
**As** the salon owner,  
**I want** to configure my salon name, address tagline, circular logo, and background wallpaper,  
**So that** the app reflects my luxury brand aesthetic (*Unique Makeover, Jalgaon Jamod*).

#### Context & Product Design
* **Branding Fields:**
  * Salon Name (e.g. "Unique Makeover")
  * Tagline / Address (e.g. "Jalgaon Jamod • Luxury Beauty Parlour")
  * Logo Picker (Camera/Gallery) → Automatically cropped to circle and scaled to **200 px**.
  * Optional Wallpaper Picker → Scaled to **900 px @ 72%** compression.

#### Technical & Architectural Specifications
* Handled via `UpdateBrandingUseCase` and `LogoImageStore`.
* Stored in `SettingsEntity` singleton row (`id = 1`). Image files stored in internal app storage.

#### Acceptance Criteria
* **AC-SET-01.1 (Branding Update Reflected in Header):**
  * **Given** the owner changes salon name to "Unique Makeover Studio",
  * **When** saving Settings,
  * **Then** the top app bar across all screens immediately updates to display "Unique Makeover Studio".
* **AC-SET-01.2 (Logo Compression & Circular Crop):**
  * **Given** an image selected from the gallery for salon logo,
  * **When** processed by the app,
  * **Then** the image is scaled to 200x200 px circular format and saved locally.

#### Edge Cases & Failure Scenarios
* **EC-SET-01.1 (Extremely Long Salon Name or Tagline):**
  * **Scenario:** Owner enters 80 characters for Salon Name (e.g. "Unique Makeover Luxury Bridal Hair & Skin Beauty Studio Jalgaon Jamod").
  * **System Behavior:** App header renders text with single-line truncation + ellipsis, or wraps up to 2 lines cleanly without clipping the navigation icons.
* **EC-SET-01.2 (Selecting Non-Square Image for Circular Logo):**
  * **Scenario:** Owner uploads a wide panoramic 16:9 photo as logo.
  * **System Behavior:** Center-crop transformation squares the center region before circular masking, preventing distortion.
* **EC-SET-01.3 (Wallpaper Image Hindering Text Readability):**
  * **Scenario:** A very bright, high-contrast wallpaper image is uploaded.
  * **System Behavior:** Compose theme renders a subtle 40% dark/light scrim overlay over the custom wallpaper to preserve WCAG AA text contrast ratios.

---

### US-SET-02: Business Operating Hours, Slot Duration & Staff Roster
**As** the salon owner,  
**I want** to set my daily opening time, closing time, slot interval, and staff roster,  
**So that** slot generation and staff assignments match our actual salon schedule.

#### Context & Product Design
* **Time Pickers:** Opening Time (default `10:00 AM`) and Closing Time (default `08:00 PM`).
* **Slot Duration Selector:** Radio chips for `15 min`, `30 min`, `45 min`, `60 min` (default `30 min`).
* **Staff Roster:** Comma-separated text input (e.g. `"Sunita, Pooja, Priya"`) or chip management list.

#### Technical & Architectural Specifications
* **Domain:** `UpdateOperationsUseCase` validates that Opening Time is strictly earlier than Closing Time, and parses staff names into a clean list.

#### Acceptance Criteria
* **AC-SET-02.1 (Valid Operating Hours Configuration):**
  * **Given** Open = `09:00 AM`, Close = `07:00 PM`, Slot = `30 min`,
  * **When** saving,
  * **Then** booking screens generate slots starting at 09:00 AM and ending at 06:30 PM in 30-minute steps.
* **AC-SET-02.2 (Invalid Hours Validation):**
  * **Given** Open = `08:00 PM` and Close = `10:00 AM`,
  * **When** tapping Save,
  * **Then** an inline error displays *"Closing time must be after opening time"* and changes are rejected.
* **AC-SET-02.3 (Staff Dropdown Population):**
  * **Given** staff roster configured as `"Sunita, Rekha"`,
  * **When** creating an appointment,
  * **Then** the staff dropdown presents "Sunita" and "Rekha" as selectable options.

#### Edge Cases & Failure Scenarios
* **EC-SET-02.1 (Slot Duration Larger Than Operating Window):**
  * **Scenario:** Operating window is 1 hour (10:00 AM to 11:00 AM), user selects 60 min slot.
  * **System Behavior:** Generates exactly one slot at 10:00 AM. If slot interval exceeds total duration, validation rejects setting.
* **EC-SET-02.2 (Staff Roster with Trailing Commas and Duplicate Names):**
  * **Scenario:** Owner types `"Sunita, Pooja, , sunita, Priya,"`.
  * **System Behavior:** Parser sanitizes list: trims whitespace, removes empty entries, deduplicates case-insensitively, resulting in `["Sunita", "Pooja", "Priya"]`.
* **EC-SET-02.3 (Altering Hours When Appointments Exist Outside New Window):**
  * **Scenario:** Owner changes closing time from 8 PM to 6 PM, but an appointment was already booked for 7 PM.
  * **System Behavior:** Settings displays a warning dialog: *"1 existing appointment is scheduled outside these new hours. Existing appointment will remain scheduled, but new bookings will be restricted to the updated hours. Proceed?"*.

---

### US-SET-03: Communication Settings (WhatsApp & UPI Configuration)
**As** the salon owner,  
**I want** to save my salon's official WhatsApp contact number and merchant UPI ID,  
**So that** all payment QR codes and outgoing customer reminders use my verified details.

#### Context & Product Design
* Dedicated text inputs with validation:
  * WhatsApp Phone Number (10 digits, Indian prefix auto-formatted).
  * Merchant UPI ID (regex validation `^[\w.\-_]{2,256}@[a-zA-Z]{2,64}$`).

#### Technical & Architectural Specifications
* **Domain:** `UpdateCommunicationUseCase` validates inputs and persists to `SettingsEntity.wa` and `SettingsEntity.upi`.

#### Acceptance Criteria
* **AC-SET-03.1 (UPI ID Format Validation):**
  * **Given** the owner enters `invalid-upi-id` (missing `@bank`),
  * **When** tapping Save,
  * **Then** the field shows *"Please enter a valid UPI ID (e.g. name@bank)"*.
* **AC-SET-03.2 (Valid Configuration Dismisses Dashboard Alerts):**
  * **Given** valid WhatsApp and UPI IDs are saved,
  * **Then** any corresponding missing configuration alerts on the Dashboard disappear.

#### Edge Cases & Failure Scenarios
* **EC-SET-03.1 (UPI ID with Accidental Leading/Trailing Spaces):**
  * **Scenario:** User pastes `"  uniquemakeover@okaxis  "`.
  * **System Behavior:** Auto-trims all leading and trailing whitespace before validation and persistence.
* **EC-SET-03.2 (WhatsApp Number with Country Code `+91` Prefixed):**
  * **Scenario:** User enters `+91 9876543210` or `09876543210`.
  * **System Behavior:** Phone sanitizer strips non-digit characters and normalizes string to exact 10 digits (`9876543210`).

---

## Epic 9: Data Portability, Backup & System Recovery (FRD §10.5, §11, ARD §6.8)

### US-DAT-01: Full Database JSON Export via Storage Access Framework (SAF)
**As** the salon owner,  
**I want** to export my entire app database to a single JSON backup file on my phone or SD card,  
**So that** I can safeguard my business records or transfer them to a new phone without internet.

#### Context & Product Design
* Button: **"Export Backup"**.
* Launches native Android file picker to choose destination folder.
* Default filename formatted as: `UniqueMakeover_backup_YYYY-MM-DD.json`.
* Progress indicator during export, followed by success snackbar displaying file size.

#### Technical & Architectural Specifications
* **SAF Integration:** Uses `ActivityResultContracts.CreateDocument("application/json")`.
* **Serialization:** `kotlinx.serialization` serializes `BackupSnapshot` (Schema v2) containing settings, services catalog, all appointments, and client private notes (ADR-0005).
* Timestamp and version tag included in metadata:
  ```json
  { "version": 2, "exportedAt": "2026-11-14T...", "settings": {...}, "services": [...], "appointments": [...], "clientNotes": {...} }
  ```

#### Acceptance Criteria
* **AC-DAT-01.1 (Standardized File Creation):**
  * **Given** today is 2026-11-14,
  * **When** the owner initiates Export Backup,
  * **Then** the file picker prompts with suggested name `UniqueMakeover_backup_2026-11-14.json`.
* **AC-DAT-01.2 (Complete Data Capture):**
  * **Given** the app contains 50 appointments, 10 services, and 15 client notes,
  * **When** the backup is exported,
  * **Then** the generated JSON file contains all 50 appointments, 10 services, and 15 client notes, and `settings.lastBackup` is updated to current timestamp.

#### Edge Cases & Failure Scenarios
* **EC-DAT-01.1 (User Cancels System File Picker):**
  * **Scenario:** Owner taps "Export Backup", system document picker opens, but owner taps Back/Cancel.
  * **System Behavior:** `ActivityResultCallback` receives `null` Uri; export aborts silently without error toast or state corruption; `lastBackup` timestamp is not updated.
* **EC-DAT-01.2 (Target Destination Runs Out of Storage Mid-Write):**
  * **Scenario:** Target SD card or internal storage runs out of disk space during writing of a large JSON file.
  * **System Behavior:** File stream catches `IOException`, deletes partial corrupted file, and shows an alert: *"Backup failed: Storage is full"*.
* **EC-DAT-01.3 (Concurrent Database Writes During Export):**
  * **Scenario:** An appointment status is updated while the backup serialization coroutine is reading tables.
  * **System Behavior:** Room database reads execute on a snapshot transaction isolation, ensuring JSON data consistency without locks or race conditions.

---

### US-DAT-02: Atomic Backup Import with Schema Validation, Diff Preview & Rollback
**As** the salon owner,  
**I want** the app to validate any backup file and show a summary diff before overwriting data,  
**So that** I never accidentally wipe my data with an invalid or corrupted file.

#### Context & Product Design
* Button: **"Import Backup"** launching `OpenDocument`.
* **Validation & Diff Dialog:** Displays file date and counts:
  * *"Backup contains: X Services, Y Appointments, Z Client Notes. Overwrite current data?"*
  * [Cancel] and [Confirm & Overwrite].
* If file is corrupted or schema version is invalid, displays an error dialog and makes zero changes.

#### Technical & Architectural Specifications
* **Atomic Transaction (ADR-0005):** `ImportBackupUseCase` executes inside `RoomDatabase.withTransaction { ... }`.
* If parsing fails or any insertion error occurs, the entire transaction rolls back automatically, leaving the current database in its original state.

#### Acceptance Criteria
* **AC-DAT-02.1 (Diff Summary Confirmation):**
  * **Given** a valid backup file containing 45 appointments,
  * **When** the owner selects the file to import,
  * **Then** a confirmation dialog shows *"Backup contains: 45 appointments. Replace current database?"* before executing any database write.
* **AC-DAT-02.2 (Atomic Overwrite on Confirmation):**
  * **Given** the owner confirms the import dialog,
  * **Then** the database is cleared and repopulated in a single transaction, the UI refreshes all screens, and a toast confirms *"Data restored successfully"*.
* **AC-DAT-02.3 (Corrupted File Rollback & Safety):**
  * **Given** an invalid or truncated JSON file,
  * **When** the owner attempts to import it,
  * **Then** an error dialog *"Invalid backup file format"* displays, the transaction aborts, and all existing salon data remains 100% intact.

#### Edge Cases & Failure Scenarios
* **EC-DAT-02.1 (Importing File with Unsupported Schema Version):**
  * **Scenario:** Owner attempts to import a JSON file with `"version": 99` from an incompatible future release.
  * **System Behavior:** `BackupReader` checks version header before clearing tables; rejects file with message: *"Unsupported backup version (v99). Please update the app to restore this file"*. Current database is left untouched.
* **EC-DAT-02.2 (Importing Completely Arbitrary / Non-JSON File):**
  * **Scenario:** User selects a photo, MP3, or random document renamed to `.json`.
  * **System Behavior:** JSON parser throws `SerializationException`; handled cleanly with dialog: *"Corrupted or incompatible file. No changes were made to your data"*.
* **EC-DAT-02.3 (Power Loss / Crash Mid-Import):**
  * **Scenario:** Phone battery dies halfway through writing imported tables.
  * **System Behavior:** `RoomDatabase.withTransaction` ensures atomic SQLite commit; upon restart, SQLite WAL rolls back the uncommitted transaction, leaving prior database state intact.

---

### US-DAT-03: Overdue Backup Detection & Warning
**As** the salon owner,  
**I want** to see how long ago my last backup was taken and receive a reminder if it exceeds 3 days,  
**So that** I remember to plug in a pen drive or export a backup regularly to prevent data loss.

#### Context & Product Design
* Sub-label in Settings → Data Management showing: *"Last backup: YYYY-MM-DD"* or *"Never"*.
* If `lastBackup` is older than 3 days, the label highlights in orange with a warning badge.

#### Technical & Architectural Specifications
* Domain: `GetLastBackupInfoUseCase` computes day difference between `TimeProvider.now()` and `settings.lastBackup`.

#### Acceptance Criteria
* **AC-DAT-03.1 (Overdue Calculation):**
  * **Given** the last backup was performed 5 days ago,
  * **When** viewing Settings,
  * **Then** the backup status displays *"Last backup was 5 days ago (Overdue)"* in warning color.

#### Edge Cases & Failure Scenarios
* **EC-DAT-03.1 (Never Backed Up Since Fresh Install):**
  * **Scenario:** App has been in use for 4 days with zero backups taken (`lastBackup = null`).
  * **System Behavior:** Label displays: *"Last backup: Never (Overdue)"*, and Dashboard alert banner is prominently active.
* **EC-DAT-03.2 (Device Clock Shifted Backwards Earlier Than Last Backup):**
  * **Scenario:** Last backup was 2026-11-14; user manually sets clock to 2026-11-10.
  * **System Behavior:** Day difference is negative; logic gracefully handles negative duration and avoids showing negative numbers (e.g. shows *"Last backup was today/recently"*).

---

### US-DAT-04: Factory Reset / Clear All Local Data with Guardrail
**As** the salon owner,  
**I want** to factory-reset the application only after entering my admin PIN and confirming twice,  
**So that** I can clear test data before official launch without risking accidental erasure during daily use.

#### Context & Product Design
* Destructive option located at the bottom of Settings.
* Two-step barrier:
  1. Warning Dialog: *"Erase All Salon Data? All appointments, services, notes, and metrics will be wiped permanently."*
  2. Security Challenge: Requires typing the current 4-digit admin PIN.

#### Technical & Architectural Specifications
* **Domain:** `ClearAllDataUseCase` verifies PIN via `VerifyPinUseCase`, then calls `RoomDatabase.clearAllTables()`, resets `Settings` to defaults, and re-executes `SeedCallback`.

#### Acceptance Criteria
* **AC-DAT-04.1 (PIN Required for Erase):**
  * **Given** the owner clicks "Clear All Data" and confirms the first warning,
  * **When** prompted for PIN, entering an incorrect PIN rejects the request with *"Incorrect PIN. Data not cleared."*.
* **AC-DAT-04.2 (Reset Execution):**
  * **Given** the correct PIN is entered,
  * **Then** all custom appointments and notes are deleted, default seed services are restored, and the user is redirected to the initial PIN lock screen.

#### Edge Cases & Failure Scenarios
* **EC-DAT-04.1 (App Backgrounded During Second PIN Challenge):**
  * **Scenario:** Owner confirms step 1, but receives a phone call during step 2 PIN prompt.
  * **System Behavior:** On app resume, the destructive dialog is dismissed; process must be initiated from step 1 again.
* **EC-DAT-04.2 (Image Files on Disk Cleanup):**
  * **Scenario:** Multiple custom service photos and salon logo files exist in `context.filesDir/images/`.
  * **System Behavior:** `ClearAllDataUseCase` deletes SQLite records and recurses through the internal images directory, purging orphaned image files to free storage completely.

---

## Epic 10: Background Automation, Device Integrations & System Non-Functional Requirements (FRD §12, §15, ARD §6.9, §10, §14)

### US-SYS-01: Daily 8:00 PM Local Reminder Notification
**As** the salon owner,  
**I want** my phone to notify me at 8:00 PM every evening if I have appointments scheduled for tomorrow,  
**So that** I am prompted to send WhatsApp reminders without having to remember on my own.

#### Context & Product Design
* Standard Android local notification at 8:00 PM:
  * Title: *"Tomorrow's Appointments"*
  * Body: *"You have N clients scheduled for tomorrow. Tap to send reminders."*
* Tapping the notification opens the app directly to the Reminders screen.

#### Technical & Architectural Specifications
* **Implementation:** Android `WorkManager` with `DailyReminderWorker` (`@HiltWorker`) scheduled as a periodic work request.
* **Local Invariant:** Generated entirely by the Android OS locally; zero reliance on Firebase Cloud Messaging (FCM) or push notification servers.

#### Acceptance Criteria
* **AC-SYS-01.1 (8 PM Notification Trigger):**
  * **Given** 4 appointments exist for tomorrow with status "booked",
  * **When** the device clock reaches 8:00 PM,
  * **Then** a local notification appears: *"You have 4 clients scheduled for tomorrow. Tap to send reminders."*.
* **AC-SYS-01.2 (Zero Appointments Silent):**
  * **Given** there are 0 appointments scheduled for tomorrow,
  * **When** the 8:00 PM worker runs,
  * **Then** no notification is posted to the user's notification shade.

#### Edge Cases & Failure Scenarios
* **EC-SYS-01.1 (Device Powered Off at 8:00 PM):**
  * **Scenario:** Tablet is turned off at 7:30 PM and turned on at 8:30 PM.
  * **System Behavior:** `WorkManager` detects the missed execution window; upon boot completion (`ACTION_BOOT_COMPLETED`), the worker fires immediately and posts the notification.
* **EC-SYS-01.2 (Android 13+ Notification Permission Denied):**
  * **Scenario:** User running Android 13 (API 33) denies `POST_NOTIFICATIONS` runtime permission.
  * **System Behavior:** App catches permission restriction cleanly without crashing; an in-app banner in Settings suggests: *"Enable notifications to receive daily 8 PM reminder alerts"*.
* **EC-SYS-01.3 (All Tomorrow Appointments Already Reminded Before 8 PM):**
  * **Scenario:** Owner proactively sent reminders at 4 PM; all tomorrow clients have `reminded != null`.
  * **System Behavior:** Worker checks `reminded` status; if 100% of tomorrow's appointments are already reminded, it suppresses the notification.

---

### US-SYS-02: Zero-Network Strict Offline Enforcement & No-Internet Invariant
**As** the salon owner,  
**I want** complete assurance that the app never touches the internet or transmits private client details outside my phone,  
**So that** client confidentiality, phone numbers, and financial earnings remain 100% private to my salon.

#### Context & Product Design
* App functions with identical speed and feature completeness with Airplane Mode ON.
* No loading spinners waiting on network; zero ads; zero analytics trackers.

#### Technical & Architectural Specifications
* **Manifest Invariant:** `AndroidManifest.xml` contains **NO `<uses-permission android:name="android.permission.INTERNET" />`**.
* No network client libraries (no Retrofit, OkHttp, Ktor, Volley, Firebase, or Google Play Services).
* CI gate strictly verifies: `grep -rn "INTERNET" app/src/main/AndroidManifest.xml && exit 1`.

#### Acceptance Criteria
* **AC-SYS-02.1 (Full Airplane Mode Functionality):**
  * **Given** the Android device has Airplane Mode enabled with all network interfaces disabled,
  * **When** performing all operations (PIN unlock, booking, editing, QR generation, backup export/import, note taking),
  * **Then** all operations succeed with zero network errors or missing functionality.
* **AC-SYS-02.2 (Absence of Internet Permission):**
  * **Given** the compiled APK,
  * **When** inspecting Android package permissions via `PackageManager.getPackageInfo()`,
  * **Then** `INTERNET` permission is absent from the requested permissions list.

#### Edge Cases & Failure Scenarios
* **EC-SYS-02.1 (Third-Party Library Internal Network Call Attempt):**
  * **Scenario:** A transitive dependency attempts an HTTP request.
  * **System Behavior:** Operating system security sandbox halts call instantly with `SecurityException: Permission Denial` due to absence of `<uses-permission android:name="android.permission.INTERNET" />`.
* **EC-SYS-02.2 (Captive Portal / Public WiFi Popups):**
  * **Scenario:** Phone connects to a public mall WiFi requiring browser login.
  * **System Behavior:** App is completely unaffected and continues all operations uninterrupted without displaying captive network errors.

---

### US-SYS-03: Room SQLite Database Concurrency, Indices & Cold-Start Performance (<2s)
**As** the salon owner,  
**I want** the app to open in under 2 seconds and scroll through thousands of appointments at a smooth 60 frames per second,  
**So that** the app never freezes or lags during busy salon hours.

#### Context & Product Design
* Instant responsiveness on touch.
* Smooth scrolling list without stuttering or dropped frames.

#### Technical & Architectural Specifications
* **Database Optimization:**
  * Room SQLite configured with Write-Ahead Logging (`WAL` mode) for non-blocking concurrent reads and writes.
  * Explicit composite index `Index(value = ["date", "time"])` for clash queries.
  * Single column indices on `date` and `phone` for list filtering and CRM profile aggregation.
* **Dependency Injection:** Hilt lazy bindings; no eager background work blocking `Application.onCreate()`.
* **Threading:** Coroutine dispatchers injected (`@IoDispatcher`, `@DefaultDispatcher`). Pure domain use cases run off the main thread.

#### Acceptance Criteria
* **AC-SYS-03.1 (Cold-Start Speed):**
  * **Given** a mid-range Android device (API 26+) with a database of 5,000 appointments,
  * **When** launching the application cold,
  * **Then** the PIN Lock screen renders interactively in less than 2.0 seconds.
* **AC-SYS-03.2 (Scroll Performance with 5,000+ Records):**
  * **Given** an appointment history of 5,000 records,
  * **When** flinging or scrolling through the appointment list,
  * **Then** UI renders at 60 fps without main thread frame drops or `Application Not Responding` (ANR) warnings.

#### Edge Cases & Failure Scenarios
* **EC-SYS-03.1 (SQLite Database File Corruption on Low-End Storage):**
  * **Scenario:** Device forcibly powers down during NAND flash write, corrupting the SQLite file.
  * **System Behavior:** SQLite WAL mode minimizes corruptions; Room fallback invokes database recreation callback or prompts to restore from most recent JSON backup file.
* **EC-SYS-03.2 (Simultaneous Database Contention During 8 PM Worker):**
  * **Scenario:** WorkManager runs 8:00 PM query at the exact millisecond the owner is saving a new appointment.
  * **System Behavior:** WAL (Write-Ahead Logging) allows concurrent readers and writers on separate SQLite connections without thread deadlocks or `SQLiteDatabaseLockedException`.
* **EC-SYS-03.3 (Memory Pressure on Low RAM Device / 2GB RAM):**
  * **Scenario:** App is running on an entry-level Android tablet under low RAM conditions.
  * **System Behavior:** Compose lazy lists recycle item views efficiently; Coil image loader restricts image memory cache to 15% of available heap, preventing OutOfMemory crashes.

---

## User Stories Summary Matrix & Cross-Reference

| Story ID | Epic Area | FRD Ref | ARD Ref | Primary Screen | Key Architectural Component |
|---|---|---|---|---|---|
| **US-AUTH-01** | Access Control | §3.1 | §6.1, ADR-0003 | #1 PIN Lock | `VerifyPinUseCase`, `PinLockManager`, Keystore |
| **US-AUTH-02** | Access Control | §3.1 | §6.1, §10 | #1 PIN Lock | `LockoutPolicy`, `DataStore<Preferences>` |
| **US-AUTH-03** | Access Control | §3.1 | §6.1, §6.8 | #10 Settings | `ChangePinUseCase`, `PinRepository` |
| **US-DASH-01** | Operations | §4.1 | §6.2 | #2 Dashboard | `GetDashboardKpisUseCase`, Room Flow |
| **US-DASH-02** | Operations | §4.2 | §6.2 | #2 Dashboard | `GetAlertBannersUseCase`, Navigation DeepLink |
| **US-DASH-03** | Operations | §4.3 | §6.2 | #2 Dashboard | `GetTodayScheduleUseCase`, Inline Actions |
| **US-APT-01** | Appointments | §5.1 | §6.3 | #3 Appointments | `GetAppointmentsUseCase`, Indexed Room Queries |
| **US-APT-02** | Appointments | §5.2, §5.3 | §6.3, ADR-0006 | #4 Booking Sheet | `GenerateAvailableSlotsUseCase`, Clash Check |
| **US-APT-03** | Appointments | §5.2 | §6.3 | #4 Booking Sheet | `ValidateBookingDraftUseCase`, Calculation Math |
| **US-APT-04** | Appointments | §5.5 | §6.3 | #4 Booking Sheet | `EditAppointmentUseCase`, Self-Exclusion Clash |
| **US-APT-05** | Appointments | §5.4 | §6.3 | #3 & #4 | `MarkAppointmentDoneUseCase`, State Machine |
| **US-APT-06** | Appointments | §5.4 | §6.3 | #3 Appointments | `DeleteAppointmentUseCase`, Room `@Transaction` |
| **US-APT-07** | Appointments | §9.5 | §6.3 | #3 & #4 Dialog | `RecordPaymentUseCase`, `PaymentEntity` |
| **US-SVC-01** | Services | §6.1, §6.3 | §6.4 | #5 Services | `SeedCallback`, Room SQLite |
| **US-SVC-02** | Services | §6.1 | §6.4 | #5 Services | Snapshot Safety, `ServiceRepository` |
| **US-SVC-03** | Services | §6.2 | §6.4, ADR-0004 | #5 Services | `ImageCompressor` (320px, 78%), Local Disk |
| **US-REM-01** | Reminders | §7.1 | §6.6 | #8 Reminders | `GetTomorrowQueueUseCase`, `TimeProvider` |
| **US-REM-02** | Reminders | §7.2 | §6.6 | #8 Reminders | `WhatsAppLauncher`, `ACTION_VIEW` Intent |
| **US-REM-03** | Reminders | §7.3, §7.4 | §6.6 | #10 Settings | `FormatReminderTemplateUseCase` (8 tokens) |
| **US-CRM-01** | Client CRM | §8.1 | §6.5 | #6 Clients | SQL `GROUP BY phone` Aggregates |
| **US-CRM-02** | Client CRM | §8.2 | §6.5 | #6 Clients | `GetClientProfilesUseCase`, Substring Search |
| **US-CRM-03** | Client CRM | §8.3 | §6.5 | #7 Client Detail | `GetClientHistoryUseCase`, Chronological Query |
| **US-CRM-04** | Client CRM | §8.3 | §6.5 | #7 Client Detail | `SaveClientNoteUseCase`, 500ms Debounce |
| **US-CRM-05** | Client CRM | §8.4 | §6.5 | #7 Client Detail | `BuildRebookDraftUseCase`, Pre-Fill Mapper |
| **US-UPI-01** | Offline UPI | §9.1, §9.3 | §6.7, ADR-0007 | #9 UPI QR | `ZxingQrEncoder` (ZXing Core), 512px Bitmap |
| **US-UPI-02** | Offline UPI | §9.2 | §6.7 | #9 UPI QR | `ComputePresetAmountUseCase` (Balance/30%/Full) |
| **US-UPI-03** | Offline UPI | §9.4 | §6.7 | #9 UPI QR | `BuildUpiWhatsAppMessageUseCase`, Intent |
| **US-UPI-04** | Offline UPI | §9.3 | §6.7 | #9 UPI QR | `QrPngExporter` (MediaStore), `FileProvider` |
| **US-UPI-05** | Offline UPI | §9.1 | §6.7 | #9 UPI QR | `UpiBlockingBanner`, UI State Guard |
| **US-SET-01** | Settings | §10.1 | §6.8, ADR-0004 | #10 Settings | `UpdateBrandingUseCase`, Logo (200px crop) |
| **US-SET-02** | Settings | §10.3 | §6.8 | #10 Settings | `UpdateOperationsUseCase`, Hours/Staff Parser |
| **US-SET-03** | Settings | §10.4 | §6.8 | #10 Settings | `UpdateCommunicationUseCase`, Regex Validator |
| **US-DAT-01** | Data Backup | §10.5, §11.3 | §6.8 | #10 Settings | `ExportBackupUseCase`, SAF `CreateDocument` |
| **US-DAT-02** | Data Backup | §10.5, §11.4 | §6.8, ADR-0005 | #10 Settings | `ImportBackupUseCase`, Atomic `@Transaction` |
| **US-DAT-03** | Data Backup | §10.5, §12 | §6.8 | #10 Settings | `GetLastBackupInfoUseCase`, Overdue Checker |
| **US-DAT-04** | Data Backup | §10.5 | §6.8 | #10 Settings | `ClearAllDataUseCase`, Double PIN Challenge |
| **US-SYS-01** | System / NFR | §12 | §6.9 | Background | `DailyReminderWorker`, Local `WorkManager` |
| **US-SYS-02** | System / NFR | §15 | §1, §10, §14 | System-Wide | No `INTERNET` Permission, Zero Network SDKs |
| **US-SYS-03** | System / NFR | §15 | §1, §5, §10 | System-Wide | Room WAL Mode, Indexed Queries, <2s Launch |

---
**End of User Stories Specification v2.0 — Unique Makeover Admin Console**
