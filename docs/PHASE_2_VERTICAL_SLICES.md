# Unique Makeover — Admin Console
# Phase 2 Vertical Slices Specification
### Operations & Financial Cycle · Native Android · Fully Offline · Clean Architecture
**Document Version:** 1.0  
**Target Roles:** Veteran System Design Android Architect & QA Engineering Lead  
**Source References:** `docs/FRD.md` (§4.1, §4.3, §5.1, §5.4, §5.5, §9.5), `docs/ARD.md` (§6.2, §6.3), `docs/USER_STORIES.md`, `vertical-slice-indexmap.md`

---

## Phase 2 Executive Summary

Phase 2 builds the **Operations & Financial Cycle** of *Unique Makeover, Jalgaon Jamod*. While Phase 1 established the ability to create bookings, Phase 2 delivers the operational interface through which the salon owner manages daily footfall, tracks cash and UPI collections in real time, executes appointment lifecycle transitions (Booked $\leftrightarrow$ Done $\leftrightarrow$ Cancel), records payments, and performs conflict-aware rescheduling.

```
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                     PHASE 2 VERTICAL SLICES                                      │
├──────────────────────────┬──────────────────────────┬──────────────────────────┬─────────────────┤
│ SLICE #07                │ SLICE #08                │ SLICE #09                │ SLICE #10       │
│ Appointment Directory    │ Live Dashboard KPIs      │ Today's Live Schedule    │ Status Lifecycle│
│ US-APT-01 (Directory)    │ US-DASH-01 (Metrics)     │ US-DASH-03 (Timeline)    │ US-APT-05 (Undo)│
├──────────────────────────┼──────────────────────────┼──────────────────────────┴─────────────────┤
│ SLICE #11                │ SLICE #12                │ SLICE #13                                  │
│ In-line Payment Record   │ Conflict-Aware Reschedule│ Hard Deletion Guardrail                    │
│ US-APT-07 (Payments)     │ US-APT-04 (Self-Exclude) │ US-APT-06 (Cascade/Wipe)                   │
└──────────────────────────┴──────────────────────────┴────────────────────────────────────────────┘
```

---

## Slice #07: Filtered Appointment Directory & Substring Search (US-APT-01)

### 1. Business Goal
Provide an indexed, sub-50ms searchable directory of all historical, current, and upcoming appointments so the salon owner can locate past client visits or review upcoming schedules across arbitrary date ranges.

### 2. Actors
* **Salon Owner (Sunita):** Browses appointments, filters by status, or searches by client name/phone during enquiries.

### 3. User Story
> **As** the salon owner (Sunita),  
> **I want** to browse, filter, and search all historical and upcoming appointments,  
> **So that** I can look up any client's past visits or plan for future weeks.

### 4. Acceptance Criteria
* **AC-APT-01.1 (Status Tab Filtering):**
  * **Given** appointments across multiple statuses,
  * **When** selecting the "Upcoming" filter tab,
  * **Then** only appointments with `date >= today` and `status == "booked"` are displayed, sorted chronologically (`date ASC, time ASC`).
* **AC-APT-01.2 (Instant Substring Search on Name and Phone):**
  * **Given** an appointment for "Sunita Sharma" with phone `9876543210`,
  * **When** typing "sharma" or "4321" in the search box,
  * **Then** matching appointments appear immediately within $< 50$ ms without blocking UI scrolling.
* **AC-APT-01.3 (Date Range Filter):**
  * **Given** appointments spanning multiple months,
  * **When** filtering by a custom date range (e.g. 01-Nov to 15-Nov),
  * **Then** only appointments with dates within the inclusive range are rendered.

### 5. Edge Cases & Technical Resilience
* **EC-APT-01.1 (SQL Wildcard & Character Injection):** Search query containing `%`, `_`, `'`, or SQLite control characters is sanitized and bound safely via Room `@Query`, preventing wild table dumps or syntax crashes.
* **EC-APT-01.2 (Search Input with Country Prefix / Spaces):** User pastes `+91 98765 43210`. `PhoneSanitizer` extracts raw digits so phone substring matching succeeds seamlessly.
* **EC-APT-01.3 (Large Dataset Scrolling at 60 FPS):** With 5,000+ appointment records, Compose `LazyColumn` virtualizes list rows; indices `index_appointments_date` and `index_appointments_phone` ensure SQLite query plans avoid full-table scans.

### 6. UI/UX Specification
* **Screen:** `AppointmentsScreen.kt` (Route: `Routes.Appointments`).
* **Layout:**
  * **Top Bar:** Search `OutlinedTextField` with leading search icon and trailing clear `(X)` button.
  * **Filter Tabs:** Scrollable `PrimaryTabRow` with 4 tabs: **All · Upcoming · Completed · Cancelled** with count badges.
  * **Date Range Chip:** Compact filter chip (e.g. `[Today ▼]`, `[This Week]`, `[Custom Range]`).
  * **List Anatomy:** Cards showing client name, phone, scheduled date & time, assigned staff chip, formatted services snapshot, rate/paid/balance summary, and status badge.
* **Empty State:** `EmptyState.kt` with calendar illustration: *"No appointments found for the selected filter"*.

### 7. Database Schema & Query Optimization
```sql
-- Room Query in AppointmentDao.kt
@Query("""
    SELECT * FROM appointments 
    WHERE (:status IS NULL OR status = :status)
      AND (:startDate IS NULL OR date >= :startDate)
      AND (:endDate IS NULL OR date <= :endDate)
      AND (:query = '' OR name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%')
    ORDER BY date ASC, time ASC
""")
fun observeFilteredAppointments(
    status: String?,
    startDate: String?,
    endDate: String?,
    query: String
): Flow<List<AppointmentEntity>>
```

### 8. API / Internal Architecture Contracts

```kotlin
// Filter Domain Model (feature/appointments/domain/model/AppointmentFilter.kt)
data class AppointmentFilter(
    val tab: FilterTab = FilterTab.UPCOMING,
    val dateRange: DateRange? = null,
    val searchQuery: String = ""
)

enum class FilterTab { ALL, UPCOMING, COMPLETED, CANCELLED }

// UseCase Contract (feature/appointments/domain/usecase/GetAppointmentsUseCase.kt)
class GetAppointmentsUseCase @Inject constructor(
    private val appointmentRepository: AppointmentRepository
) {
    operator fun invoke(filter: AppointmentFilter): Flow<List<Appointment>>
}

// UI State Contract (feature/appointments/presentation/list/AppointmentsUiState.kt)
data class AppointmentsUiState(
    val isLoading: Boolean = false,
    val appointments: List<Appointment> = emptyList(),
    val filter: AppointmentFilter = AppointmentFilter(),
    val errorMessage: UiText? = null
)
```

### 9. Unit Tests
* **`GetAppointmentsUseCaseTest.kt`:** Tests filter mappings (e.g. `FilterTab.UPCOMING` maps to `status = "booked", startDate = today`).
* **`AppointmentsViewModelTest.kt`:** Uses Turbine to verify `searchQuery` updates trigger debounced flow emissions.

### 10. Integration Tests (Not Required)
* Dao query unit tests and Compose UI tests provide full coverage; no external hardware or OS boundary is crossed.

### 11. Internal Contract / Boundary Tests
* **`AppointmentDaoFilterTest.kt` (In-Memory Room):** Tests SQL query across 100 seeded records validating status tabs, date ranges, and substring name/phone matches.

### 12. Files and Folder Mapping

```
[CREATED]
app/src/main/java/com/uniquemakeover/admin/feature/appointments/domain/model/AppointmentFilter.kt
app/src/main/java/com/uniquemakeover/admin/feature/appointments/domain/usecase/GetAppointmentsUseCase.kt
app/src/main/java/com/uniquemakeover/admin/feature/appointments/presentation/list/AppointmentsScreen.kt
app/src/main/java/com/uniquemakeover/admin/feature/appointments/presentation/list/AppointmentsViewModel.kt
app/src/main/java/com/uniquemakeover/admin/feature/appointments/presentation/list/AppointmentsUiState.kt
app/src/main/java/com/uniquemakeover/admin/feature/appointments/presentation/list/components/FilterTabs.kt
app/src/main/java/com/uniquemakeover/admin/feature/appointments/presentation/list/components/AppointmentCard.kt
app/src/test/java/com/uniquemakeover/admin/feature/appointments/domain/usecase/GetAppointmentsUseCaseTest.kt
app/src/test/java/com/uniquemakeover/admin/feature/appointments/presentation/list/AppointmentsViewModelTest.kt
app/src/androidTest/java/com/uniquemakeover/admin/feature/appointments/data/AppointmentDaoFilterTest.kt
[MODIFIED]
app/src/main/java/com/uniquemakeover/admin/core/database/dao/AppointmentDao.kt (Adds observeFilteredAppointments)
```

---

## Slice #08: Real-Time Operational & Financial KPIs (US-DASH-01)

### 1. Business Goal
Provide an executive operational dashboard displaying today's appointment counts and live monetary metrics (Volume, Collected Cash/UPI, Outstanding Dues) so the salon owner knows exact daily parlour workload and cash flow without manual calculations.

### 2. Actors
* **Salon Owner (Sunita):** Views the home screen throughout the business day.

### 3. User Story
> **As** the salon owner (Sunita),  
> **I want** to view high-level appointment counts and financial totals for today on the home screen,  
> **So that** I can instantly gauge today's workload, cash flow, and outstanding dues at a glance.

### 4. Acceptance Criteria
* **AC-DASH-01.1 (Mathematical Computation Integrity):**
  * **Given** today's appointments: Apt A (Rate ₹1,000, Paid ₹1,000, status "done"), Apt B (Rate ₹2,000, Paid ₹500, status "booked"), Apt C (Rate ₹1,500, Paid ₹0, status "cancel"),
  * **When** viewing the Dashboard,
  * **Then** KPIs display: Booked: 1, Completed: 1, Cancelled: 1, Scheduled Volume: `₹ 3,000`, Collected Revenue: `₹ 1,500`, Balance Outstanding: `₹ 1,500`.
* **AC-DASH-01.2 (Cancelled Bookings Exclusion):**
  * **Given** an appointment marked as `"cancel"`,
  * **Then** its rate and paid amounts are strictly excluded from Scheduled Volume, Collected Revenue, and Balance Outstanding.
* **AC-DASH-01.3 (Zero-State Display):**
  * **Given** zero appointments exist for today,
  * **Then** count cards display `0` and financial cards display `₹ 0`.

### 5. Edge Cases & Technical Resilience
* **EC-DASH-01.1 (Midnight Day Transition While Dashboard Open):** Device clock rolls from 23:59 to 00:00 while app remains open. Injected `TimeProvider` triggers a recalculation; KPIs reset cleanly to the new day's metrics.
* **EC-DASH-01.2 (High-Season Revenue Overflow):** High bridal season produces daily volume exceeding ₹1,00,000. Indian grouping currency formatter formats numbers properly (`₹ 1,25,000`); Compose text scales dynamically without breaking card layout.
* **EC-DASH-01.3 (Overpayment / Client Credit Balance):** A client pays more than the scheduled billable amount (`paid > rate`). Balance Due is clamped to `₹ 0` or displays a distinct credit badge, preventing confusing negative dues.

### 6. UI/UX Specification
* **Screen:** `DashboardScreen.kt` (Route: `Routes.Dashboard`).
* **KPI Grid Anatomy:** 2x3 or 3x2 Material 3 card grid:
  1. *Booked (Pending):* Count badge with calendar icon.
  2. *Completed (Done):* Count badge with green check icon.
  3. *Cancelled:* Count badge with muted gray icon.
  4. *Scheduled Volume:* ₹ sum of non-cancelled rates.
  5. *Collected Revenue:* Bold green ₹ sum of non-cancelled paid amounts.
  6. *Balance Outstanding:* Bold amber ₹ due amount.
* **Typography & Styling:** Currency figures styled in `headlineSmall` (`Poppins`/`Inter` Semi-Bold) with standard Indian comma formatting.

### 7. Database Schema & Query Optimization
```sql
-- Room Query in AppointmentDao.kt
@Query("SELECT * FROM appointments WHERE date = :today")
fun observeTodayAppointmentsRaw(today: String): Flow<List<AppointmentEntity>>
```

### 8. API / Internal Architecture Contracts

```kotlin
// Domain Model (feature/dashboard/domain/model/DashboardKpis.kt)
data class DashboardKpis(
    val bookedCount: Int = 0,
    val completedCount: Int = 0,
    val cancelledCount: Int = 0,
    val scheduledVolume: Int = 0,
    val collectedRevenue: Int = 0,
    val balanceOutstanding: Int = 0
)

// UseCase Contract (feature/dashboard/domain/usecase/GetDashboardKpisUseCase.kt)
class GetDashboardKpisUseCase @Inject constructor(
    private val appointmentRepository: AppointmentRepository,
    private val timeProvider: TimeProvider
) {
    operator fun invoke(): Flow<DashboardKpis>
}
```

### 9. Unit Tests
* **`GetDashboardKpisUseCaseTest.kt`:**
  * Tests pure mathematical summation across varying appointment lists.
  * Asserts cancelled appointments do not contribute to volume or revenue.
  * Injects `FakeTimeProvider` to test date boundary transitions.

### 10. Integration Tests (Required)
* **`DashboardKpiIntegrationTest.kt` (Compose UI + In-Memory Room):**
  * Inserts 3 appointments into in-memory Room database.
  * Launches `DashboardScreen`.
  * Asserts Compose nodes display exact KPI figures ("₹ 3,000", "₹ 1,500").
  * Updates an appointment in Room; asserts Compose KPI card reactively updates.

### 11. Internal Contract / Boundary Tests
* **`CurrencyFormatterTest.kt`:** Validates Indian currency formatting rules (e.g. `1000` $\rightarrow$ `"₹ 1,000"`, `150000` $\rightarrow$ `"₹ 1,50,000"`).

### 12. Files and Folder Mapping

```
[CREATED]
app/src/main/java/com/uniquemakeover/admin/core/util/CurrencyFormatter.kt
app/src/main/java/com/uniquemakeover/admin/feature/dashboard/domain/model/DashboardKpis.kt
app/src/main/java/com/uniquemakeover/admin/feature/dashboard/domain/usecase/GetDashboardKpisUseCase.kt
app/src/main/java/com/uniquemakeover/admin/feature/dashboard/presentation/DashboardScreen.kt
app/src/main/java/com/uniquemakeover/admin/feature/dashboard/presentation/DashboardViewModel.kt
app/src/main/java/com/uniquemakeover/admin/feature/dashboard/presentation/DashboardUiState.kt
app/src/main/java/com/uniquemakeover/admin/feature/dashboard/presentation/components/KpiCard.kt
app/src/main/java/com/uniquemakeover/admin/feature/dashboard/presentation/components/KpiRow.kt
app/src/test/java/com/uniquemakeover/admin/core/util/CurrencyFormatterTest.kt
app/src/test/java/com/uniquemakeover/admin/feature/dashboard/domain/usecase/GetDashboardKpisUseCaseTest.kt
app/src/test/java/com/uniquemakeover/admin/feature/dashboard/presentation/DashboardViewModelTest.kt
app/src/androidTest/java/com/uniquemakeover/admin/feature/dashboard/presentation/DashboardKpiIntegrationTest.kt
[MODIFIED]
app/src/main/java/com/uniquemakeover/admin/core/database/dao/AppointmentDao.kt (Adds observeTodayAppointmentsRaw)
```

---

## Slice #09: Today's Live Schedule Stream & Quick Actions (US-DASH-03)

### 1. Business Goal
Provide an immediate, chronological live schedule stream directly on the dashboard home screen so the salon owner can monitor incoming clients and trigger one-tap operational actions (Mark Done, WhatsApp, Record Payment, Edit, Cancel, Delete) without navigating into deep menus.

### 2. Actors
* **Salon Owner (Sunita):** Manages salon walk-ins and active appointments from the salon reception desk.

### 3. User Story
> **As** the salon owner (Sunita),  
> **I want** a chronological stream of today's appointments with one-tap action buttons directly on the dashboard,  
> **So that** I can manage clients walking in without navigating through deep sub-menus.

### 4. Acceptance Criteria
* **AC-DASH-03.1 (Strict Chronological Sorting):**
  * **Given** today has appointments at `02:00 PM`, `10:00 AM`, and `11:30 AM`,
  * **When** viewing the dashboard schedule stream,
  * **Then** cards render in order: 10:00 AM $\rightarrow$ 11:30 AM $\rightarrow$ 02:00 PM.
* **AC-DASH-03.2 (Row Action Surface):**
  * **Given** an appointment card in today's schedule,
  * **When** viewing the card,
  * **Then** quick action buttons (✅ Done, 💬 WhatsApp, ₹ Pay, ✏️ Edit, ❌ Cancel, 🗑 Delete) are directly accessible.
* **AC-DASH-03.3 (Empty Today Stream):**
  * **Given** no appointments are scheduled for today,
  * **Then** a relaxed aesthetic banner displays: *"No appointments scheduled for today. Tap '+' to create a booking"*.

### 5. Edge Cases & Technical Resilience
* **EC-DASH-03.1 (Multiple Appointments at Identical Time with Different Stylists):** Two clients booked at 11:00 AM with Stylist A and Stylist B. Secondary sort order resolves by `staff ASC, bookedAt ASC`; both cards render clearly with stylist chips.
* **EC-DASH-03.2 (Long Client Name & Multi-Service Text Wrap):** Client name is 35 characters with 5 services booked. UI layout uses responsive text wrapping; action buttons stay anchored to the right without getting pushed off-screen.
* **EC-DASH-03.3 (Rapid Tap Flooding on Action Buttons):** Tapping "Mark Done" or "Record Payment" 4 times rapidly is debounced via `throttleFirst(500ms)` in the ViewModel, preventing duplicate dialogs.

### 6. UI/UX Specification
* **Component:** `ScheduleRowCard.kt` within `DashboardScreen.kt`.
* **Card Anatomy:**
  * **Left Column:** Time block (Large 12-hr time e.g. `10:30 AM`), Status chip (`Booked`, `Done`, `Cancelled`).
  * **Center Column:** Client Name (`titleMedium`), Phone, Assigned Staff pill, Formatted services text (`bodySmall`).
  * **Right Column:** Rate / Paid / Due summary (e.g. `₹ 1,500 / Due ₹ 500`).
  * **Bottom Strip:** Quick action icon buttons with tooltips.

### 7. Database Schema & Query Optimization
```sql
-- Room Query in AppointmentDao.kt
@Query("SELECT * FROM appointments WHERE date = :today ORDER BY time ASC, staff ASC")
fun observeTodaySchedule(today: String): Flow<List<AppointmentEntity>>
```

### 8. API / Internal Architecture Contracts

```kotlin
// Domain Model (feature/dashboard/domain/model/ScheduleItem.kt)
data class ScheduleItem(
    val appointment: Appointment,
    val isPastTime: Boolean
)

// UseCase Contract (feature/dashboard/domain/usecase/GetTodayScheduleUseCase.kt)
class GetTodayScheduleUseCase @Inject constructor(
    private val appointmentRepository: AppointmentRepository,
    private val timeProvider: TimeProvider
) {
    operator fun invoke(): Flow<List<ScheduleItem>>
}
```

### 9. Unit Tests
* **`GetTodayScheduleUseCaseTest.kt`:** Tests chronological ordering and `isPastTime` calculation using `FakeTimeProvider`.

### 10. Integration Tests (Required)
* **`ScheduleStreamIntegrationTest.kt` (Compose UI + Flow):**
  * Launches dashboard with mock schedule flow.
  * Verifies 10:00 AM card appears above 02:00 PM card.
  * Taps "Mark Done" icon; asserts callback triggers with correct appointment ID.

### 11. Internal Contract / Boundary Tests
* **Navigation Contract Test:** Verifies tapping "Edit" on a schedule card navigates to `Routes.BookingSheet(appointmentId = id)`.

### 12. Files and Folder Mapping

```
[CREATED]
app/src/main/java/com/uniquemakeover/admin/feature/dashboard/domain/model/ScheduleItem.kt
app/src/main/java/com/uniquemakeover/admin/feature/dashboard/domain/usecase/GetTodayScheduleUseCase.kt
app/src/main/java/com/uniquemakeover/admin/feature/dashboard/presentation/components/ScheduleRowCard.kt
app/src/test/java/com/uniquemakeover/admin/feature/dashboard/domain/usecase/GetTodayScheduleUseCaseTest.kt
app/src/androidTest/java/com/uniquemakeover/admin/feature/dashboard/presentation/ScheduleStreamIntegrationTest.kt
[MODIFIED]
app/src/main/java/com/uniquemakeover/admin/core/database/dao/AppointmentDao.kt (Adds observeTodaySchedule)
app/src/main/java/com/uniquemakeover/admin/feature/dashboard/presentation/DashboardScreen.kt (Integrates Schedule stream)
```

---

## Slice #10: Status Lifecycle Transitions & Accidental Tap Undo (US-APT-05)

### 1. Business Goal
Allow the salon owner to transition appointments between Booked, Done, and Cancelled with a single tap, while providing an instant 5-second **Undo** safety net to revert accidental taps without needing to re-type or re-book records.

### 2. Actors
* **Salon Owner (Sunita):** Updates client status as they arrive, finish treatments, or cancel.

### 3. User Story
> **As** the salon owner (Sunita),  
> **I want** to transition appointments between Booked, Done, and Cancelled, with a quick Undo option,  
> **So that** my daily schedule stays accurate and accidental status taps can be corrected instantly without re-typing.

### 4. Acceptance Criteria
* **AC-APT-05.1 (Mark Done Transition):**
  * **Given** an appointment with `status = "booked"`,
  * **When** tapping "Mark Done",
  * **Then** status updates to `"done"` in SQLite, card reflects a green status chip, and a snackbar displays *"Appointment marked as Done [Undo]"*.
* **AC-APT-05.2 (5-Second Undo Action):**
  * **Given** the Undo snackbar is visible on screen,
  * **When** the owner taps "Undo" within 5 seconds,
  * **Then** the appointment status reverts back to `"booked"` in SQLite, and financial KPIs adjust back immediately.
* **AC-APT-05.3 (Cancel Frees Slot with Undo Guard):**
  * **Given** Appointment A was cancelled and Appointment B subsequently booked that slot,
  * **When** the owner taps "Undo" on Appointment A,
  * **Then** the system detects slot conflict, keeps Appointment A cancelled, and displays *"Slot is now occupied by another client. Please reschedule."*.

### 5. Edge Cases & Technical Resilience
* **EC-APT-05.1 (Rapid Multi-State Tapping):** User rapidly taps Done $\rightarrow$ Cancel $\rightarrow$ Undo. The state machine enforces legal transitions (`booked` $\leftrightarrow$ `done`, `booked` $\leftrightarrow$ `cancel`); direct transition between `done` and `cancel` is disallowed without returning to `booked`.
* **EC-APT-05.2 (Screen Navigation While Snackbar is Displayed):** User marks appointment Done and immediately navigates to Settings. Snackbar dismisses on navigation exit; the `"done"` status remains committed safely.
* **EC-APT-05.3 (Balance Prompt on Mark Done):** If appointment has an outstanding balance, marking Done prompts: *"Client has pending balance of ₹500. Settle balance now?"* with [Settle Cash], [Settle UPI], or [Skip / Keep Unpaid].

### 6. UI/UX Specification
* **Component:** Animated status chip on `ScheduleRowCard` / `AppointmentCard`.
* **Transitions:**
  * Active $\rightarrow$ Done: Smooth color transition to Emerald Green (`#2E7D32`) with checkmark animation.
  * Active $\rightarrow$ Cancel: Smooth transition to muted gray with strikethrough.
* **Snackbar:** Custom Material 3 `SnackbarHost` in `AppScaffold.kt` with high-contrast luxury gold action button `[UNDO]`.

### 7. Database Schema & State Updates
Updates `status` column in `appointments` table:
```kotlin
enum class AppointmentStatus(val value: String) {
    BOOKED("booked"),
    DONE("done"),
    CANCEL("cancel")
}
```

### 8. API / Internal Architecture Contracts

```kotlin
// Domain Model (core/domain/model/AppointmentStatus.kt)
sealed interface StatusUpdateResult {
    object Success : StatusUpdateResult
    data class SlotConflict(val message: UiText) : StatusUpdateResult
}

// UseCase Contracts
class MarkAppointmentDoneUseCase @Inject constructor(
    private val appointmentRepository: AppointmentRepository
) {
    suspend operator fun invoke(appointmentId: String): DataResult<Unit>
}

class CancelAppointmentUseCase @Inject constructor(
    private val appointmentRepository: AppointmentRepository
) {
    suspend operator fun invoke(appointmentId: String): DataResult<Unit>
}

class UndoAppointmentStatusUseCase @Inject constructor(
    private val appointmentRepository: AppointmentRepository,
    private val slotRepository: SlotRepository
) {
    suspend operator fun invoke(appointmentId: String, previousStatus: AppointmentStatus): StatusUpdateResult
}
```

### 9. Unit Tests
* **`StatusLifecycleUseCaseTest.kt`:**
  * Tests status transitions: `booked -> done`, `booked -> cancel`.
  * Tests undo conflict detection: Returns `StatusUpdateResult.SlotConflict` if slot was re-booked.

### 10. Integration Tests (Required)
* **`AppointmentLifecycleIntegrationTest.kt` (In-Memory Room + Compose):**
  * Marks an appointment Done.
  * Asserts database entity status is `"done"`.
  * Triggers Undo callback; asserts database entity status reverts to `"booked"`.

### 11. Internal Contract / Boundary Tests
* **State Machine Contract Test:** Verifies invalid transitions (e.g. `cancel -> done` directly) are rejected by the domain layer.

### 12. Files and Folder Mapping

```
[CREATED]
app/src/main/java/com/uniquemakeover/admin/core/domain/model/AppointmentStatus.kt
app/src/main/java/com/uniquemakeover/admin/feature/appointments/domain/usecase/MarkAppointmentDoneUseCase.kt
app/src/main/java/com/uniquemakeover/admin/feature/appointments/domain/usecase/CancelAppointmentUseCase.kt
app/src/main/java/com/uniquemakeover/admin/feature/appointments/domain/usecase/UndoAppointmentStatusUseCase.kt
app/src/main/java/com/uniquemakeover/admin/core/ui/components/AppScaffold.kt
app/src/test/java/com/uniquemakeover/admin/feature/appointments/domain/usecase/StatusLifecycleUseCaseTest.kt
app/src/androidTest/java/com/uniquemakeover/admin/feature/appointments/presentation/AppointmentLifecycleIntegrationTest.kt
[MODIFIED]
app/src/main/java/com/uniquemakeover/admin/core/database/dao/AppointmentDao.kt (Adds updateStatus query)
```

---

## Slice #11: In-line Payment Recording & Overpayment Guard (US-APT-07)

### 1. Business Goal
Enable quick recording of incremental customer payments (cash or UPI) directly from the schedule card or appointment row, ensuring parlour revenue records remain accurate while preventing accidental overpayments.

### 2. Actors
* **Salon Owner (Sunita):** Accepts cash or confirms received UPI payments from salon walk-in clients.

### 3. User Story
> **As** the salon owner (Sunita),  
> **I want** to record incremental cash or UPI payments against any appointment,  
> **So that** I can track partial payments, advances, and final settlements accurately.

### 4. Acceptance Criteria
* **AC-APT-07.1 (Record Outstanding Balance):**
  * **Given** an appointment with Rate = ₹1,500 and Paid = ₹500 (Balance Due = ₹1,000),
  * **When** opening Record Payment, entering ₹1,000 Cash, and tapping Save,
  * **Then** Paid updates to `₹ 1,500`, Balance Due becomes `₹ 0`, and the payment card reflects full settlement.
* **AC-APT-07.2 (Overpayment Confirmation Dialog):**
  * **Given** an appointment with Balance Due = ₹300,
  * **When** entering ₹500,
  * **Then** an explicit confirmation prompt appears: *"Amount entered (₹500) exceeds balance due (₹300). Confirm extra payment/tip?"*, requiring confirmation before saving.
* **AC-APT-07.3 (Audit Log Persistence):**
  * **Given** a recorded payment of ₹1,000 Cash,
  * **Then** an audit entry is inserted into `payments` table with `amount = 1000, mode = "cash", timestamp = now`.

### 5. Edge Cases & Technical Resilience
* **EC-APT-07.1 (Zero or Negative Payment Amount):** Owner types ₹0 or negative values. Save button remains disabled; inline error displays *"Payment must be greater than ₹0"*.
* **EC-APT-07.2 (Payment on Cancelled Appointment):** Client settles a late cancellation fee. System prompts: *"This appointment is Cancelled. Record payment anyway?"*; revenue updates correctly in collected totals.
* **EC-APT-07.3 (Concurrent Payment Recording):** Executed inside Room `@Transaction` (`paid = paid + :amount`), preventing lost update race conditions if two updates occur rapidly.

### 6. UI/UX Specification
* **Screen:** `RecordPaymentDialog.kt` (Modal Bottom Sheet / Dialog).
* **Anatomy:**
  * Header: Client Name, Total Billed Rate, Already Paid, and Remaining Balance Due.
  * Payment Mode Toggle: Segmented control: `[💵 Cash]` or `[📱 UPI]`.
  * Amount Field: Pre-filled with the exact remaining balance due; includes clear button.
  * Action Buttons: [Cancel] and [Record Payment] (`PrimaryButton.kt`).

### 7. Database Schema Reference
Updates `appointments` table and inserts into `payments` table (from Slice #01):
```kotlin
@Entity(tableName = "payments", indices = [Index("appointmentId")])
data class PaymentEntity(
    @PrimaryKey val id: String,         // UUID
    val appointmentId: String,
    val amount: Int,
    val mode: String,                   // "cash" | "upi"
    val paidAt: Long                    // Epoch millis
)
```

### 8. API / Internal Architecture Contracts

```kotlin
// Domain Model (core/domain/model/PaymentMode.kt)
enum class PaymentMode(val value: String) {
    CASH("cash"),
    UPI("upi")
}

// UseCase Contract (feature/appointments/domain/usecase/RecordPaymentUseCase.kt)
class RecordPaymentUseCase @Inject constructor(
    private val appointmentRepository: AppointmentRepository,
    private val paymentRepository: PaymentRepository
) {
    suspend operator fun invoke(
        appointmentId: String,
        amount: Int,
        mode: PaymentMode,
        confirmedOverpayment: Boolean = false
    ): DataResult<Unit>
}
```

### 9. Unit Tests
* **`RecordPaymentUseCaseTest.kt`:**
  * Tests normal payment: Increments `paid` by exact amount.
  * Tests overpayment guard: Returns `DataResult.Error(OVERPAYMENT_CONFIRMATION_REQUIRED)` when `amount > balanceDue` and `confirmedOverpayment == false`.
  * Tests rejection of non-positive payments (`amount <= 0`).

### 10. Integration Tests (Not Required)
* Transaction tests in Slice #06 cover Room transaction mechanisms; pure ViewModel/UseCase unit tests provide complete coverage.

### 11. Internal Contract / Boundary Tests
* **`PaymentDaoTest.kt` (In-Memory Room):** Verifies payment insertion and query by `appointmentId`.

### 12. Files and Folder Mapping

```
[CREATED]
app/src/main/java/com/uniquemakeover/admin/core/domain/model/PaymentMode.kt
app/src/main/java/com/uniquemakeover/admin/core/domain/model/Payment.kt
app/src/main/java/com/uniquemakeover/admin/core/domain/repository/PaymentRepository.kt
app/src/main/java/com/uniquemakeover/admin/feature/appointments/data/repository/PaymentRepositoryImpl.kt
app/src/main/java/com/uniquemakeover/admin/feature/appointments/domain/usecase/RecordPaymentUseCase.kt
app/src/main/java/com/uniquemakeover/admin/feature/appointments/presentation/payment/RecordPaymentDialog.kt
app/src/main/java/com/uniquemakeover/admin/feature/appointments/presentation/payment/RecordPaymentViewModel.kt
app/src/test/java/com/uniquemakeover/admin/feature/appointments/domain/usecase/RecordPaymentUseCaseTest.kt
app/src/test/java/com/uniquemakeover/admin/feature/appointments/presentation/payment/RecordPaymentViewModelTest.kt
app/src/androidTest/java/com/uniquemakeover/admin/feature/appointments/data/PaymentDaoTest.kt
[MODIFIED]
app/src/main/java/com/uniquemakeover/admin/core/database/DatabaseModule.kt (Binds PaymentRepository)
```

---

## Slice #12: Appointment Rescheduling & Conflict-Aware Editing (US-APT-04)

### 1. Business Goal
Allow the salon owner to reschedule dates, change time slots, modify service selections, or assign different stylists for an existing booking without triggering false clash errors against the appointment's own current slot.

### 2. Actors
* **Salon Owner (Sunita):** Reschedules or adjusts appointments upon client phone requests.

### 3. User Story
> **As** the salon owner (Sunita),  
> **I want** to edit existing appointment details including time, date, services, and staff,  
> **So that** I can accommodate client rescheduling requests without false conflict errors.

### 4. Acceptance Criteria
* **AC-APT-04.1 (Self-Exclusion from Clash Check):**
  * **Given** Appointment #101 is scheduled at 11:00 AM on 2026-11-25,
  * **When** opening Appointment #101 in the Edit sheet for that same date,
  * **Then** the 11:00 AM slot is active and selectable (not marked "Full" by its own existing record).
* **AC-APT-04.2 (Clash Prevention Against Other Bookings):**
  * **Given** Appointment #102 is already booked at 04:00 PM on 2026-11-25,
  * **When** editing Appointment #101 and attempting to switch to 04:00 PM,
  * **Then** the 04:00 PM chip is marked "Full" and cannot be chosen.
* **AC-APT-04.3 (Financial Recalculation on Service Changes):**
  * **Given** an appointment with Paid = ₹500 and Rate was ₹1,000,
  * **When** adding an extra service of ₹500 (new Rate = ₹1,500),
  * **Then** saving updates Balance Due to `₹ 1,000` and refreshes Dashboard KPIs immediately.

### 5. Edge Cases & Technical Resilience
* **EC-APT-04.1 (Editing Completed or Cancelled Appointments):** Date and time selectors are locked/read-only with caption: *"To reschedule, first restore appointment status to Booked"*. Notes and staff remain editable.
* **EC-APT-04.2 (Rescheduling to Past Date):** Date picker disables past dates (`date < today`).
* **EC-APT-04.3 (Immutable Creation Metadata):** `id` and `bookedAt` timestamps remain strictly immutable during updates.

### 6. UI/UX Specification
* **Screen:** `BookingSheet.kt` in Edit Mode (Title: *"Edit Appointment"*).
* Pre-populates all existing appointment values (Client Name, Phone, Date, Time, Staff, Services, Discount, Notes).
* Save button reads: **[Update Appointment]**.

### 7. Database Schema & Query Optimization
```sql
-- Room Query in AppointmentDao.kt
@Query("""
    SELECT COUNT(*) FROM appointments 
    WHERE date = :date 
      AND time = :time 
      AND status != 'cancel' 
      AND id != :excludeId
""")
suspend fun countClashingAppointmentsExcludingId(date: String, time: String, excludeId: String): Int
```

### 8. API / Internal Architecture Contracts

```kotlin
// UseCase Contract (feature/appointments/domain/usecase/EditAppointmentUseCase.kt)
class EditAppointmentUseCase @Inject constructor(
    private val appointmentRepository: AppointmentRepository,
    private val slotRepository: SlotRepository
) {
    suspend operator fun invoke(
        appointmentId: String,
        updatedDraft: BookingDraft
    ): DataResult<Unit>
}
```

### 9. Unit Tests
* **`EditAppointmentUseCaseTest.kt`:**
  * Tests self-exclusion: Passing `excludeId == currentAppointmentId` passes clash check on the same slot.
  * Tests clash rejection when target slot belongs to another active appointment.
  * Verifies recalculation of `rate` and `balanceDue`.

### 10. Integration Tests (Required)
* **`AppointmentEditClashIntegrationTest.kt` (In-Memory Room):**
  * Inserts Apt A (`date = D, time = 10:00`) and Apt B (`date = D, time = 11:00`).
  * Calls `countClashingAppointmentsExcludingId(D, "10:00", excludeId = AptA.id)` $\rightarrow$ Returns 0.
  * Calls `countClashingAppointmentsExcludingId(D, "11:00", excludeId = AptA.id)` $\rightarrow$ Returns 1 (clashes with Apt B).

### 11. Internal Contract / Boundary Tests
* **`BookingSheetEditTest.kt` (Compose UI Test):** Verifies form pre-populates existing client name and services.

### 12. Files and Folder Mapping

```
[CREATED]
app/src/main/java/com/uniquemakeover/admin/feature/appointments/domain/usecase/EditAppointmentUseCase.kt
app/src/test/java/com/uniquemakeover/admin/feature/appointments/domain/usecase/EditAppointmentUseCaseTest.kt
app/src/androidTest/java/com/uniquemakeover/admin/feature/appointments/data/AppointmentEditClashIntegrationTest.kt
[MODIFIED]
app/src/main/java/com/uniquemakeover/admin/core/database/dao/AppointmentDao.kt (Adds countClashingAppointmentsExcludingId)
app/src/main/java/com/uniquemakeover/admin/feature/appointments/presentation/booking/BookingSheet.kt (Adds Edit mode support)
app/src/main/java/com/uniquemakeover/admin/feature/appointments/presentation/booking/BookingViewModel.kt (Adds loadAppointmentForEdit)
```

---

## Slice #13: Hard Deletion of Appointment with Guardrail (US-APT-06)

### 1. Business Goal
Provide a secure mechanism to permanently purge erroneous or test appointment records from the SQLite database while preventing accidental deletion through an explicit two-button confirmation dialog.

### 2. Actors
* **Salon Owner (Sunita):** Purges accidental test entries or erroneous appointment bookings.

### 3. User Story
> **As** the salon owner (Sunita),  
> **I want** to permanently delete a test or erroneous appointment record with a confirmation prompt,  
> **So that** my records remain clean while preventing accidental data loss.

### 4. Acceptance Criteria
* **AC-APT-06.1 (Explicit Confirmation Dialog):**
  * **Given** the owner taps the trash icon on an appointment row,
  * **When** the confirmation dialog appears and the owner taps "Cancel",
  * **Then** the appointment remains untouched in SQLite and the dialog dismisses.
* **AC-APT-06.2 (Permanent Deletion & Cascade):**
  * **Given** the owner confirms deletion by tapping destructive button [Delete],
  * **Then** the record is deleted from `appointments` table inside a Room `@Transaction`, any associated entries in `payments` table are deleted, and the record disappears from the UI immediately.

### 5. Edge Cases & Technical Resilience
* **EC-APT-06.1 (Rapid Double Tapping on Delete Button):** Dialog dismisses on first touch; `deleteById` idempotent SQL executes safely without throwing `EntityNotFoundException`.
* **EC-APT-06.2 (Deleting Appointment with Historical Payments):** Dialog explicitly warns: *"This appointment has recorded payments totaling ₹1,000. Deleting will adjust historical revenue metrics. Proceed?"*.

### 6. UI/UX Specification
* **Component:** `ConfirmDialog.kt` (Material 3 AlertDialog).
* **Anatomy:**
  * Title: *"Delete Appointment?"*
  * Message: *"This will permanently delete the appointment for Sunita Sharma on 20-Nov. This action cannot be undone."*
  * Dismiss Button: *"Cancel"*
  * Confirm Button: Red Destructive Container *"Delete"*

### 7. Database Schema & Query Optimization
```sql
-- Room Query in AppointmentDao.kt & PaymentDao.kt
@Query("DELETE FROM appointments WHERE id = :id")
suspend fun deleteById(id: String)

@Query("DELETE FROM payments WHERE appointmentId = :appointmentId")
suspend fun deletePaymentsByAppointmentId(appointmentId: String)
```

### 8. API / Internal Architecture Contracts

```kotlin
// UseCase Contract (feature/appointments/domain/usecase/DeleteAppointmentUseCase.kt)
class DeleteAppointmentUseCase @Inject constructor(
    private val appointmentRepository: AppointmentRepository,
    private val paymentRepository: PaymentRepository
) {
    suspend operator fun invoke(appointmentId: String): DataResult<Unit>
}
```

### 9. Unit Tests
* **`DeleteAppointmentUseCaseTest.kt`:** Verifies that deleting delegates to repository and handles exceptions.

### 10. Integration Tests (Required)
* **`AppointmentCascadeDeleteTest.kt` (In-Memory Room):**
  * Inserts an appointment and 2 related payments.
  * Executes `deleteById()` in `@Transaction`.
  * Asserts appointment record is gone and payment audit records are purged.

### 11. Internal Contract / Boundary Tests
* **`ConfirmDialogTest.kt` (Compose UI Test):** Verifies Cancel leaves data intact; Confirm triggers deletion callback.

### 12. Files and Folder Mapping

```
[CREATED]
app/src/main/java/com/uniquemakeover/admin/core/ui/components/ConfirmDialog.kt
app/src/main/java/com/uniquemakeover/admin/feature/appointments/domain/usecase/DeleteAppointmentUseCase.kt
app/src/test/java/com/uniquemakeover/admin/feature/appointments/domain/usecase/DeleteAppointmentUseCaseTest.kt
app/src/androidTest/java/com/uniquemakeover/admin/feature/appointments/data/AppointmentCascadeDeleteTest.kt
[MODIFIED]
app/src/main/java/com/uniquemakeover/admin/core/database/dao/AppointmentDao.kt (Adds deleteById)
app/src/main/java/com/uniquemakeover/admin/feature/appointments/data/repository/AppointmentRepositoryImpl.kt (Implements deleteById)
```

---

## Phase 2 Test Execution Matrix & QA Acceptance Gate

To declare Phase 2 complete and authorize progression to **Phase 3 (Catalog & Visual Media)**, QA must verify that all 18 test suites pass with 100% green status:

```bash
# 1. Run All JVM Unit Tests (< 25 seconds)
./gradlew testDebugUnitTest

# 2. Run All Phase 2 Android Runtime Integration Tests
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=\
com.uniquemakeover.admin.feature.appointments.data.AppointmentDaoFilterTest,\
com.uniquemakeover.admin.feature.dashboard.presentation.DashboardKpiIntegrationTest,\
com.uniquemakeover.admin.feature.dashboard.presentation.ScheduleStreamIntegrationTest,\
com.uniquemakeover.admin.feature.appointments.presentation.AppointmentLifecycleIntegrationTest,\
com.uniquemakeover.admin.feature.appointments.data.AppointmentEditClashIntegrationTest,\
com.uniquemakeover.admin.feature.appointments.data.AppointmentCascadeDeleteTest

# 3. Verify Detekt Architecture Rules (Zero Room in Presentation, Zero android.* in Domain)
./gradlew detekt

# 4. Strict Offline Invariant Check
grep -rn "INTERNET" app/src/main/AndroidManifest.xml && exit 1
```

---
**End of Phase 2 Vertical Slices Specification — Unique Makeover Admin Console**
