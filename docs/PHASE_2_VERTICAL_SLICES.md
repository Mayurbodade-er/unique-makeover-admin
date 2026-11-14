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

### 12. Files and Folder Mapping (Updated Post-Implementation)

#### Files Created:
1. `app/src/main/java/com/uniquemakeover/admin/feature/appointments/domain/model/AppointmentFilter.kt`
   - **Architectural Role:** Domain Model (Filter State)
   - **Why Created:** Encapsulates appointment status tabs (`ALL`, `UPCOMING`, `COMPLETED`, `CANCELLED`), optional `DateRange`, and text `searchQuery` independent of database or UI implementations.
2. `app/src/main/java/com/uniquemakeover/admin/feature/appointments/domain/usecase/GetAppointmentsUseCase.kt`
   - **Architectural Role:** Pure Domain UseCase
   - **Why Created:** Translates `AppointmentFilter` tab selections into database query parameters (mapping `UPCOMING` to `status = "booked", startDate = today`), sanitizes phone queries via `PhoneSanitizer`, and streams reactive appointment lists.
3. `app/src/main/java/com/uniquemakeover/admin/feature/appointments/presentation/AppointmentsActions.kt`
   - **Architectural Role:** Presentation (Action Contract)
   - **Why Created:** Bundles UI callback event handlers (tab selection, query typing, filter clearing, booking opening, appointment tapping) to keep composable function signatures clean and maintainable.
4. `app/src/main/java/com/uniquemakeover/admin/feature/appointments/presentation/components/FilterTabs.kt`
   - **Architectural Role:** Presentation (Compose Component)
   - **Why Created:** Renders luxury horizontal scrollable status tabs adhering to `desing.md` with active burgundy indicators, selected text styles, and live count badge pills.
5. `app/src/main/java/com/uniquemakeover/admin/feature/appointments/presentation/components/AppointmentCard.kt`
   - **Architectural Role:** Presentation (Compose Component)
   - **Why Created:** Formats and displays individual appointment records with scheduled datetime, staff stylist chip, client name, formatted phone, frozen service snapshot, monetary totals, and color-coded status badges.
6. `app/src/main/java/com/uniquemakeover/admin/feature/appointments/presentation/components/EmptyAppointmentsView.kt`
   - **Architectural Role:** Presentation (Compose Component)
   - **Why Created:** Renders friendly zero-state illustration and clear message when filters yield no matches, featuring a tactile "Clear Filters" CTA.
7. `app/src/test/java/com/uniquemakeover/admin/feature/appointments/domain/usecase/GetAppointmentsUseCaseTest.kt`
   - **Architectural Role:** Automated JVM Unit Test
   - **Why Created:** Verifies filter tab mappings, custom date ranges, and phone number sanitization behavior with 100% deterministic assertions.
8. `app/src/test/java/com/uniquemakeover/admin/feature/appointments/data/AppointmentDaoFilterTest.kt`
   - **Architectural Role:** Automated JVM / Robolectric Integration Test
   - **Why Created:** Validates SQLite query execution across status filters, date windows, substring search across name/phone, and verifies sub-50ms execution speed.

#### Files Modified:
1. `app/src/main/java/com/uniquemakeover/admin/core/database/dao/AppointmentDao.kt`
   - **Why Modified:** Added indexed `observeFilteredAppointments` query supporting multi-column filters and name/phone substring matching with safe Room parameter binding.
2. `app/src/main/java/com/uniquemakeover/admin/feature/appointments/domain/repository/AppointmentRepository.kt`
   - **Why Modified:** Declared `getFilteredAppointments` reactive Flow contract.
3. `app/src/main/java/com/uniquemakeover/admin/feature/appointments/data/repository/AppointmentRepositoryImpl.kt`
   - **Why Modified:** Implemented `getFilteredAppointments` delegating to `AppointmentDao.observeFilteredAppointments` on `@IoDispatcher`.
4. `app/src/main/java/com/uniquemakeover/admin/feature/appointments/presentation/AppointmentsUiState.kt`
   - **Why Modified:** Extended UI state with `appointments`, active `filter`, and live `tabCounts` while preserving slot selection fields for seamless compatibility.
5. `app/src/main/java/com/uniquemakeover/admin/feature/appointments/presentation/AppointmentsViewModel.kt`
   - **Why Modified:** Integrated `GetAppointmentsUseCase`, implemented reactive tab counts observation, debounced filter updates, and filter reset logic.
6. `app/src/main/java/com/uniquemakeover/admin/feature/appointments/presentation/AppointmentsScreen.kt`
   - **Why Modified:** Upgraded Screen #3 to the full Appointments Directory with search top bar, filter tabs, card list, empty state, and modal booking sheet integration.
7. `app/src/test/java/com/uniquemakeover/admin/feature/appointments/presentation/AppointmentsScreenTest.kt`
   - **Why Modified:** Updated UI test suite to verify top search bar, tab filtering, appointment card details, and empty state CTA interactions.
8. `app/src/test/java/com/uniquemakeover/admin/feature/appointments/presentation/AppointmentsViewModelTest.kt`
   - **Why Modified:** Added comprehensive unit tests for tab transitions, search query updates, custom date ranges, and filter reset.
9. `app/src/test/java/com/uniquemakeover/admin/feature/appointments/data/repository/AppointmentRepositoryImplTest.kt`
   - **Why Modified:** Added `observeFilteredAppointments` to test fake DAO.
10. `app/src/test/java/com/uniquemakeover/admin/feature/appointments/data/repository/SlotRepositoryImplTest.kt`
    - **Why Modified:** Added `observeFilteredAppointments` to test fake DAO.
11. `app/src/test/java/com/uniquemakeover/admin/feature/appointments/presentation/booking/BookingViewModelTest.kt`
    - **Why Modified:** Added `getFilteredAppointments` to test fake repository.

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

### 12. Files and Folder Mapping (Updated Post-Implementation)

#### Files Created:
1. `app/src/main/java/com/uniquemakeover/admin/core/util/CurrencyFormatter.kt`
   - **Architectural Role:** Core Utility (Pure Kotlin)
   - **Why Created:** Implements Indian numbering system grouping (e.g. `₹ 1,50,000`) and currency prefixing with zero locale dependencies, ensuring identical rendering across JVM unit tests and Android hardware.
2. `app/src/main/java/com/uniquemakeover/admin/feature/dashboard/domain/model/DashboardKpis.kt`
   - **Architectural Role:** Domain Model (KPI Aggregate)
   - **Why Created:** Encapsulates immutable daily operational metrics (`bookedCount`, `completedCount`, `cancelledCount`, `scheduledVolume`, `collectedRevenue`, `balanceOutstanding`) free of presentation or SQLite framework dependencies.
3. `app/src/main/java/com/uniquemakeover/admin/feature/dashboard/domain/usecase/GetDashboardKpisUseCase.kt`
   - **Architectural Role:** Pure Domain UseCase
   - **Why Created:** Implements core business logic of daily KPI calculation: queries today's appointments via `TimeProvider`, excludes cancelled bookings from volume and revenue, and clamps overpaid balances to zero.
4. `app/src/main/java/com/uniquemakeover/admin/feature/dashboard/presentation/DashboardUiState.kt`
   - **Architectural Role:** Presentation (UI State)
   - **Why Created:** Immutable presentation state model for the Dashboard screen tracking loading progress, formatted operational date, and live `DashboardKpis`.
5. `app/src/main/java/com/uniquemakeover/admin/feature/dashboard/presentation/DashboardViewModel.kt`
   - **Architectural Role:** Presentation (ViewModel)
   - **Why Created:** Reactive Hilt ViewModel managing dashboard state via `StateFlow` and continuously streaming real-time KPI computations from `GetDashboardKpisUseCase`.
6. `app/src/main/java/com/uniquemakeover/admin/feature/dashboard/presentation/components/KpiCard.kt`
   - **Architectural Role:** Presentation (Compose Component)
   - **Why Created:** Renders luxury operational cards with rounded borders, circular icon containers, semantic typography, and Indian currency values conforming to `desing.md`.
7. `app/src/main/java/com/uniquemakeover/admin/feature/dashboard/presentation/components/KpiRow.kt`
   - **Architectural Role:** Presentation (Compose Component)
   - **Why Created:** Multi-card responsive layout organizing the 6 operational KPI metrics into a 2-column grid on phones and multi-column row on tablets.
8. `app/src/main/java/com/uniquemakeover/admin/feature/dashboard/presentation/DashboardScreen.kt`
   - **Architectural Role:** Presentation (Screen & Route)
   - **Why Created:** Root Dashboard destination hosted on `Routes.Dashboard` displaying burgundy top app bar, date badge, and hosting `KpiRow`.
9. `app/src/test/java/com/uniquemakeover/admin/core/util/CurrencyFormatterTest.kt`
   - **Architectural Role:** Automated JVM Unit Test
   - **Why Created:** Verifies Indian currency formatting rules (zeros, hundreds, thousands, lakhs, crores, negatives) with 100% deterministic assertions.
10. `app/src/test/java/com/uniquemakeover/admin/feature/dashboard/domain/usecase/GetDashboardKpisUseCaseTest.kt`
    - **Architectural Role:** Automated JVM Unit Test
    - **Why Created:** Verifies mathematical KPI aggregation, cancelled booking exclusions, zero states, and overpayment due clamping.
11. `app/src/test/java/com/uniquemakeover/admin/feature/dashboard/presentation/DashboardViewModelTest.kt`
    - **Architectural Role:** Automated JVM Unit Test
    - **Why Created:** Verifies `DashboardViewModel` initialization, date tracking, and reactive UI state emissions.
12. `app/src/test/java/com/uniquemakeover/admin/feature/dashboard/presentation/DashboardKpiIntegrationTest.kt`
    - **Architectural Role:** Automated JVM / Robolectric Integration Test
    - **Why Created:** Executes full-stack integration test between in-memory Room database, repository, use case, ViewModel, and Compose UI nodes.

#### Files Modified:
1. `app/src/main/java/com/uniquemakeover/admin/core/database/dao/AppointmentDao.kt`
   - **Why Modified:** Added `observeTodayAppointmentsRaw` indexed Room query.
2. `app/src/main/java/com/uniquemakeover/admin/navigation/UniqueMakeoverNavHost.kt`
   - **Why Modified:** Connected `Routes.Dashboard` to `DashboardRoute` and extracted `UniqueMakeoverNavGraph` sub-composable to maintain Detekt method length constraints.
3. `app/src/test/java/com/uniquemakeover/admin/feature/appointments/data/repository/AppointmentRepositoryImplTest.kt`
   - **Why Modified:** Added `observeTodayAppointmentsRaw` stub to test fake DAO.
4. `app/src/test/java/com/uniquemakeover/admin/feature/appointments/data/repository/SlotRepositoryImplTest.kt`
   - **Why Modified:** Added `observeTodayAppointmentsRaw` stub to test fake DAO.

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

### 12. Files and Folder Mapping (Updated Post-Implementation)

#### Files Created:
1. `app/src/main/java/com/uniquemakeover/admin/feature/dashboard/domain/model/ScheduleItem.kt`
   - **Architectural Role:** Domain Model (Dashboard)
   - **Why Created:** Encapsulates the immutable schedule item combining the core `Appointment` entity with a computed `isPastTime: Boolean` flag for chronological timeline presentation without framework dependencies.
2. `app/src/main/java/com/uniquemakeover/admin/feature/dashboard/domain/usecase/GetTodayScheduleUseCase.kt`
   - **Architectural Role:** Domain UseCase
   - **Why Created:** Encapsulates the business logic of retrieving today's appointments ordered chronologically and computing whether each slot has elapsed relative to the current wall-clock time from `TimeProvider`.
3. `app/src/main/java/com/uniquemakeover/admin/feature/dashboard/presentation/components/ScheduleRowCard.kt`
   - **Architectural Role:** Presentation (Compose Component)
   - **Why Created:** Renders a luxury appointment card displaying 12-hour formatted time, stylist badge, client phone/name, service snapshot, financial rates/dues, and a bottom quick-action icon button strip (Done, WhatsApp, Pay, Edit, Cancel, Delete).
4. `app/src/main/java/com/uniquemakeover/admin/feature/dashboard/presentation/components/ScheduleEmptyBanner.kt`
   - **Architectural Role:** Presentation (Compose Component)
   - **Why Created:** Displays an aesthetic empty-state card adhering to `desing.md` when no appointments are scheduled for today, prompting the owner to book a new appointment.
5. `app/src/test/java/com/uniquemakeover/admin/feature/dashboard/domain/usecase/GetTodayScheduleUseCaseTest.kt`
   - **Architectural Role:** JVM Unit Test
   - **Why Created:** Verifies chronological sorting order (`time ASC, staff ASC`) and past-time evaluation using deterministic `FakeTimeProvider`.
6. `app/src/test/java/com/uniquemakeover/admin/feature/dashboard/presentation/ScheduleStreamIntegrationTest.kt`
   - **Architectural Role:** Integration Test (Compose UI)
   - **Why Created:** Validates chronological stream rendering, empty state banner display, and action callback triggers on the UI layer.

#### Files Modified:
1. `app/src/main/java/com/uniquemakeover/admin/core/database/dao/AppointmentDao.kt`
   - **Why Modified:** Added `observeTodaySchedule(today: String)` reactive query with composite index sorting on `date`, `time`, and `staff`.
2. `app/src/main/java/com/uniquemakeover/admin/feature/appointments/domain/repository/AppointmentRepository.kt`
   - **Why Modified:** Declared `getTodaySchedule(today: String): Flow<List<Appointment>>` contract.
3. `app/src/main/java/com/uniquemakeover/admin/feature/appointments/data/repository/AppointmentRepositoryImpl.kt`
   - **Why Modified:** Implemented `getTodaySchedule` by mapping database entities to domain models on `@IoDispatcher`.
4. `app/src/main/java/com/uniquemakeover/admin/feature/dashboard/presentation/DashboardUiState.kt`
   - **Why Modified:** Added `schedule: List<ScheduleItem>` state property to deliver live stream items to the UI.
5. `app/src/main/java/com/uniquemakeover/admin/feature/dashboard/presentation/DashboardViewModel.kt`
   - **Why Modified:** Injected `GetTodayScheduleUseCase` and collected today's live schedule into state flow.
6. `app/src/main/java/com/uniquemakeover/admin/feature/dashboard/presentation/DashboardScreen.kt`
   - **Why Modified:** Integrated schedule list section, empty banner, and WhatsApp implicit intent launcher via `openWhatsApp`.
7. `app/src/test/java/com/uniquemakeover/admin/feature/dashboard/presentation/DashboardViewModelTest.kt`
   - **Why Modified:** Updated mock dependencies and added assertions for schedule stream emission.
8. `app/src/test/java/com/uniquemakeover/admin/feature/dashboard/presentation/DashboardKpiIntegrationTest.kt`
   - **Why Modified:** Integrated `GetTodayScheduleUseCase` in view model instantiation and verified KPI emission.

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

### 12. Files and Folder Mapping (Updated Post-Implementation)

#### Files Created:
1. `app/src/main/java/com/uniquemakeover/admin/core/domain/model/AppointmentStatus.kt`
   - **Architectural Role:** Domain Model (Core)
   - **Why Created:** Defines the immutable status state enum (`BOOKED`, `DONE`, `CANCEL`) and sealed hierarchy `StatusUpdateResult` representing atomic outcome states independent of UI and persistence layers.
2. `app/src/main/java/com/uniquemakeover/admin/feature/appointments/domain/usecase/MarkAppointmentDoneUseCase.kt`
   - **Architectural Role:** Domain UseCase
   - **Why Created:** Implements the business logic of transitioning an appointment to `DONE`; enforces state machine transition rules preventing invalid transitions from cancelled states.
3. `app/src/main/java/com/uniquemakeover/admin/feature/appointments/domain/usecase/CancelAppointmentUseCase.kt`
   - **Architectural Role:** Domain UseCase
   - **Why Created:** Implements the business logic of cancelling an appointment; rejects direct transitions from completed states to maintain audit integrity.
4. `app/src/main/java/com/uniquemakeover/admin/feature/appointments/domain/usecase/UndoAppointmentStatusUseCase.kt`
   - **Architectural Role:** Domain UseCase
   - **Why Created:** Implements 5-second accidental tap undo logic; when reviving a cancelled appointment back to `BOOKED`, checks slot clash freedom via `SlotRepository` to prevent double-booking.
5. `app/src/main/java/com/uniquemakeover/admin/feature/dashboard/presentation/DashboardUiEffect.kt`
   - **Architectural Role:** Presentation (UI Effect)
   - **Why Created:** Models one-time transient presentation side effects (such as displaying the 5-second luxury Undo snackbar with action callback).
6. `app/src/test/java/com/uniquemakeover/admin/feature/appointments/domain/usecase/StatusLifecycleUseCaseTest.kt`
   - **Architectural Role:** JVM Unit Test
   - **Why Created:** Verifies status state machine transitions, illegal transition rejections, undo restoration, and conflict detection when slots are rebooked.
7. `app/src/test/java/com/uniquemakeover/admin/feature/appointments/presentation/AppointmentLifecycleIntegrationTest.kt`
   - **Architectural Role:** JVM Integration Test (Robolectric + Room SQLite)
   - **Why Created:** End-to-end integration test asserting real database status transitions in SQLite, verifying persistence and undo guard against conflicting bookings.

#### Files Modified:
1. `app/src/main/java/com/uniquemakeover/admin/core/database/dao/AppointmentDao.kt`
   - **Why Modified:** Added atomic `@Query("UPDATE appointments SET status = :status WHERE id = :id") suspend fun updateStatus` and multi-line formatted clash count query.
2. `app/src/main/java/com/uniquemakeover/admin/feature/appointments/domain/repository/AppointmentRepository.kt`
   - **Why Modified:** Added `getAppointmentById(id: String)` and `updateStatus(id: String, status: AppointmentStatus)` interface contracts.
3. `app/src/main/java/com/uniquemakeover/admin/feature/appointments/data/repository/AppointmentRepositoryImpl.kt`
   - **Why Modified:** Implemented `getAppointmentById` and `updateStatus` queries bridging domain and database layers on `@IoDispatcher`.
4. `app/src/main/java/com/uniquemakeover/admin/feature/dashboard/presentation/DashboardViewModel.kt`
   - **Why Modified:** Injected status lifecycle use cases; exposed `uiEffect` SharedFlow, `markDone`, `cancel`, and `undoStatus` methods.
5. `app/src/main/java/com/uniquemakeover/admin/feature/dashboard/presentation/DashboardScreen.kt`
   - **Why Modified:** Added `DashboardSnackbarHost` rendering luxury Deep Burgundy and Gold themed snackbar with `[UNDO]` action handling.
6. `app/src/test/java/com/uniquemakeover/admin/feature/dashboard/presentation/DashboardViewModelTest.kt`
   - **Why Modified:** Added unit tests verifying UI effect emission for `markDone`, `cancel`, and `undoStatus` using `UnconfinedTestDispatcher`.
7. `app/src/test/java/com/uniquemakeover/admin/feature/dashboard/presentation/DashboardKpiIntegrationTest.kt`
   - **Why Modified:** Updated test harness with injected lifecycle use cases for `DashboardViewModel`.

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

### 12. Files and Folder Mapping (Updated Post-Implementation)

#### Files Created:
1. `app/src/main/java/com/uniquemakeover/admin/core/domain/model/PaymentMode.kt`
   - **Architectural Role:** Domain Model (Core)
   - **Why Created:** Defines the payment instrument enum (`CASH`, `UPI`) with safe fallback string deserialization, completely independent of UI or persistence libraries.
2. `app/src/main/java/com/uniquemakeover/admin/core/domain/model/Payment.kt`
   - **Architectural Role:** Domain Model (Core)
   - **Why Created:** Encapsulates the immutable payment audit entity (`id`, `appointmentId`, `amount`, `mode`, `paidAt`) independent of Room SQLite entity structures.
3. `app/src/main/java/com/uniquemakeover/admin/core/domain/repository/PaymentRepository.kt`
   - **Architectural Role:** Domain Repository Interface
   - **Why Created:** Declares the repository abstraction for storing and querying individual appointment payment records.
4. `app/src/main/java/com/uniquemakeover/admin/feature/appointments/data/mapper/PaymentMapper.kt`
   - **Architectural Role:** Data Mapper
   - **Why Created:** Bidirectionally converts between pure domain `Payment` models and Room `PaymentEntity` database objects.
5. `app/src/main/java/com/uniquemakeover/admin/feature/appointments/data/repository/PaymentRepositoryImpl.kt`
   - **Architectural Role:** Data Repository Implementation
   - **Why Created:** Implements `PaymentRepository` interface; persists payment records and atomically increments `paid` amounts on `@IoDispatcher`.
6. `app/src/main/java/com/uniquemakeover/admin/feature/appointments/domain/usecase/RecordPaymentUseCase.kt`
   - **Architectural Role:** Domain UseCase
   - **Why Created:** Enforces core business validation rules: rejects non-positive payment amounts ($\le ₹0$), checks overpayments requiring explicit user confirmation when exceeding remaining balance due, and generates payment audit logs.
7. `app/src/main/java/com/uniquemakeover/admin/feature/appointments/presentation/payment/RecordPaymentUiState.kt`
   - **Architectural Role:** Presentation (State Model)
   - **Why Created:** Encapsulates immutable UI state for payment dialog including pre-filled balance due, payment mode toggles, and overpayment confirmation modal visibility.
8. `app/src/main/java/com/uniquemakeover/admin/feature/appointments/presentation/payment/RecordPaymentViewModel.kt`
   - **Architectural Role:** Presentation (ViewModel)
   - **Why Created:** Manages state transitions, filters input digits, invokes `RecordPaymentUseCase`, and controls overpayment confirmation prompts.
9. `app/src/main/java/com/uniquemakeover/admin/feature/appointments/presentation/payment/RecordPaymentDialog.kt`
   - **Architectural Role:** Presentation (Compose Component)
   - **Why Created:** Modal bottom sheet adhering to `desing.md` tokens; renders financial summaries, payment mode toggle chips (`[💵 Cash]`, `[📱 UPI]`), and overpayment confirmation dialogs.
10. `app/src/test/java/com/uniquemakeover/admin/feature/appointments/domain/usecase/RecordPaymentUseCaseTest.kt`
    - **Architectural Role:** JVM Unit Test
    - **Why Created:** Verifies positive payment processing, rejection of $\le ₹0$ amounts, and overpayment guard requiring confirmation.
11. `app/src/test/java/com/uniquemakeover/admin/feature/appointments/presentation/payment/RecordPaymentViewModelTest.kt`
    - **Architectural Role:** JVM Unit Test
    - **Why Created:** Verifies UI state prefilling, digit filtering, and overpayment prompt display logic.
12. `app/src/test/java/com/uniquemakeover/admin/feature/appointments/data/PaymentDaoTest.kt`
    - **Architectural Role:** JVM Integration Test (Robolectric + Room SQLite)
    - **Why Created:** Verifies SQLite database persistence in `payments` table and relational queries by `appointmentId`.

#### Files Modified:
1. `app/src/main/java/com/uniquemakeover/admin/core/database/dao/AppointmentDao.kt`
   - **Why Modified:** Added atomic `@Query("UPDATE appointments SET paid = paid + :amount WHERE id = :id") suspend fun addPayment`.
2. `app/src/main/java/com/uniquemakeover/admin/core/di/RepositoryModule.kt`
   - **Why Modified:** Bound `PaymentRepository` to `PaymentRepositoryImpl` as a singleton dependency.
3. `app/src/main/java/com/uniquemakeover/admin/feature/dashboard/presentation/DashboardScreen.kt`
   - **Why Modified:** Integrated `RecordPaymentDialog` into dashboard quick actions via `DashboardPaymentModal`.

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

### 12. Files and Folder Mapping (Updated Post-Implementation)

#### Files Created:
1. `app/src/main/java/com/uniquemakeover/admin/feature/appointments/domain/usecase/EditAppointmentUseCase.kt`
   - **Architectural Role:** Domain UseCase
   - **Why Created:** Encapsulates business validation for editing appointments, enforcing conflict checks with self-exclusion (`excludeId`), blocking date/time rescheduling on locked appointments (`done`/`cancelled`), recalculating rates on service changes, and preserving immutable creation metadata (`id`, `bookedAt`, `paid`).
2. `app/src/test/java/com/uniquemakeover/admin/feature/appointments/domain/usecase/EditAppointmentUseCaseTest.kt`
   - **Architectural Role:** Automated JVM Unit Test
   - **Why Created:** Validates self-exclusion, clash rejection with third-party bookings, status-locked rescheduling guards, and financial recalculations with deterministic assertions.
3. `app/src/test/java/com/uniquemakeover/admin/feature/appointments/data/AppointmentEditClashIntegrationTest.kt`
   - **Architectural Role:** In-Memory Room Integration Test
   - **Why Created:** Directly verifies SQLite query `countClashingAppointmentsExcludingId` on in-memory Room database, verifying that an appointment's own slot is selectable while conflicting bookings are flagged.
4. `app/src/test/java/com/uniquemakeover/admin/feature/appointments/presentation/booking/BookingSheetEditTest.kt`
   - **Architectural Role:** Compose UI Test
   - **Why Created:** Verifies that edit mode dynamically displays "Edit Appointment" title, "Update Appointment" button, pre-populates existing services and client name, and renders lock notice banners for completed bookings.

#### Files Modified:
1. `app/src/main/java/com/uniquemakeover/admin/core/database/dao/AppointmentDao.kt`
   - **Why Modified:** Added `@Query countClashingAppointmentsExcludingId` to exclude current appointment ID from clash count during rescheduling queries.
2. `app/src/main/java/com/uniquemakeover/admin/feature/appointments/data/repository/SlotRepositoryImpl.kt`
   - **Why Modified:** Wired `countClashingAppointmentsExcludingId` when `excludeId` is supplied in `isClashFree`.
3. `app/src/main/java/com/uniquemakeover/admin/feature/appointments/domain/usecase/GenerateAvailableSlotsUseCase.kt`
   - **Why Modified:** Added optional `excludeAppointmentId` parameter to support slot generation with self-exclusion during appointment editing.
4. `app/src/main/java/com/uniquemakeover/admin/feature/appointments/presentation/booking/BookingUiState.kt`
   - **Why Modified:** Added `editingAppointmentId: String? = null` and `isDateTimeLocked: Boolean = false` with helper `isEditMode`.
5. `app/src/main/java/com/uniquemakeover/admin/feature/appointments/presentation/booking/BookingActions.kt`
   - **Why Modified:** Added `onNoteChange` callback for editing appointment notes.
6. `app/src/main/java/com/uniquemakeover/admin/feature/appointments/presentation/booking/BookingViewModel.kt`
   - **Why Modified:** Added `loadAppointmentForEdit` method, dynamic save handling (`editAppointmentUseCase` vs `bookAppointmentUseCase`), and slot generation with self-exclusion.
7. `app/src/main/java/com/uniquemakeover/admin/feature/appointments/presentation/booking/BookingSheet.kt`
   - **Why Modified:** Added Edit mode support with dynamic headers ("Edit Appointment"), dynamic save action ("Update Appointment"), read-only lock banner, and internal notes input.
8. `app/src/main/java/com/uniquemakeover/admin/feature/dashboard/presentation/DashboardScreen.kt`
   - **Why Modified:** Wired `onEdit` in `ScheduleCardActions` to load target appointment into `BookingViewModel` and launch `BookingSheet`.
9. `app/src/test/java/com/uniquemakeover/admin/feature/appointments/presentation/booking/BookingViewModelTest.kt`
   - **Why Modified:** Added test coverage for edit mode pre-population, locked status enforcement, and update appointment workflows.

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
