# Unique Makeover — Admin Console
# Phase 1 Vertical Slices Specification
### Core Booking Heartbeat · Native Android · Fully Offline · Clean Architecture
**Document Version:** 1.0  
**Target Roles:** Veteran System Design Android Architect & QA Engineering Lead  
**Source References:** `docs/FRD.md` (§5.2, §5.3, §10.3), `docs/ARD.md` (§6.3, §6.8, ADR-0006), `docs/USER_STORIES.md`, `vertical-slice-indexmap.md`

---

## Phase 1 Executive Summary

Phase 1 constructs the **Core Booking Heartbeat** of *Unique Makeover, Jalgaon Jamod*. Building directly on top of the Phase 0 Walking Skeleton (Database, Seed Services, and PIN Gate), Phase 1 empowers the salon owner to:
1. Configure salon operating hours, slot intervals, and staff roster.
2. Dynamically generate valid operating time slots, disable booked slots with "Full" badges, and invalidate past time slots for today.
3. Select multiple beauty services, apply discounts, compute subtotal and balance due in real time, validate 10-digit customer mobile numbers, and atomically commit the booking to SQLite.

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                                 PHASE 1 VERTICAL SLICES                                 │
├─────────────────────────────┬─────────────────────────────┬─────────────────────────────┤
│ SLICE #04                   │ SLICE #05                   │ SLICE #06                   │
│ Operating Hours & Staff     │ Dynamic Slot Engine & Clash │ Multi-Service Booking Sheet │
│ US-SET-02 (Hours/Roster)    │ US-APT-02 (Slot Generation) │ US-APT-03 (Booking Commit)  │
└─────────────────────────────┴─────────────────────────────┴─────────────────────────────┘
```

---

## Slice #04: Business Operating Hours, Slot Duration & Staff Roster (US-SET-02)

### 1. Business Goal
Allow the salon owner to tailor operational timings (open/close times, slot intervals of 15/30/45/60 mins, and attending staff names) to match the actual boutique beauty parlour routine in Jalgaon Jamod, establishing the rule parameters for all appointment slot generation.

### 2. Actors
* **Salon Owner (Sunita):** Configures daily opening/closing hours, time step increments, and staff list.

### 3. User Story
> **As** the salon owner (Sunita),  
> **I want** to set my daily opening time, closing time, slot interval, and staff roster,  
> **So that** slot generation and staff assignments match our actual salon schedule.

### 4. Acceptance Criteria
* **AC-SET-02.1 (Valid Operating Parameters Persistence):**
  * **Given** the owner is in Settings $\rightarrow$ Business Operations,
  * **When** setting Open Time to `09:30 AM`, Close Time to `08:30 PM`, Slot Interval to `30 min`, and Staff to `"Sunita, Pooja, Priya"`,
  * **Then** the parameters are validated, persisted to the singleton `settings` row, and a confirmation toast displays *"Operations settings saved"*.
* **AC-SET-02.2 (Closing Time Boundary Validation):**
  * **Given** Open Time is set to `10:00 AM`,
  * **When** the owner attempts to set Closing Time to `09:00 AM` or `10:00 AM`,
  * **Then** the Save action is blocked, the Close Time field flags red, and an inline error shows *"Closing time must be strictly after opening time"*.
* **AC-SET-02.3 (Staff Roster Dropdown Population):**
  * **Given** staff roster is saved as `"Sunita, Rekha"`,
  * **When** opening the appointment booking form,
  * **Then** the Staff dropdown list presents `"Sunita"` and `"Rekha"` as the selectable options.

### 5. Edge Cases & Technical Resilience
* **EC-SET-02.1 (Slot Interval Exceeds Operating Window):**
  * *Scenario:* Salon open window is 45 minutes (e.g. 10:00 AM to 10:45 AM) and owner selects 60 min slot.
  * *System Behavior:* `UpdateOperationsUseCase` validates `(closeMinutes - openMinutes) >= slotInterval`. If violated, validation rejects with *"Operating window must be at least as long as one slot interval"*.
* **EC-SET-02.2 (Malformed Staff Input with Trailing Commas & Duplicates):**
  * *Scenario:* Owner inputs `"Sunita, Pooja, , sunita, Priya,  "`.
  * *System Behavior:* Domain parser sanitizes the input: trims whitespace, removes empty tokens, deduplicates case-insensitively, and produces canonical list `["Sunita", "Pooja", "Priya"]`.
* **EC-SET-02.3 (Narrowing Hours with Existing Appointments Outside New Window):**
  * *Scenario:* Owner shifts closing time from 8:00 PM to 6:00 PM, but an appointment is already booked for 7:00 PM next week.
  * *System Behavior:* System checks future bookings via `AppointmentDao.countAppointmentsAfterTime(newCloseTime)`. Displays a warning dialog: *"1 existing appointment is booked after 6:00 PM. Existing bookings will remain intact, but new slots will end at 6:00 PM. Proceed?"*.

### 6. UI/UX Specification
* **Screen:** `SettingsScreen` $\rightarrow$ `OperationsSection.kt` (Route: `Routes.Settings`).
* **Components:**
  * **Time Pickers:** Material 3 `TimePickerField` (12-hour AM/PM dialog or wheel with localized formatting).
  * **Slot Interval Selector:** Segmented radio chips: `[15 min]`, `[30 min]`, `[45 min]`, `[60 min]`.
  * **Staff Roster:** Flow row of removable dismissible chips (`InputChip`) with an inline `+ Add Staff` text field and keyboard IME Done action.
* **Ergonomics & Feedback:** Single-tap selection with immediate visual toggle state and light haptic feedback.

### 7. Database Schema Reference
Updates the singleton row (`id = 1`) in the `settings` table defined in Slice #01:
* Columns modified: `open TEXT` ("HH:MM" 24-hr format, e.g. `"10:00"`), `close TEXT` ("HH:MM", e.g. `"20:00"`), `slot INTEGER` (minutes: 15, 30, 45, 60), `staff TEXT` (comma-separated string).

### 8. API / Internal Architecture Contracts

```kotlin
// Domain Model (core/domain/model/OperationsConfig.kt)
data class OperationsConfig(
    val openTime: String,      // "HH:MM" e.g. "10:00"
    val closeTime: String,     // "HH:MM" e.g. "20:00"
    val slotDurationMinutes: Int, // 15, 30, 45, 60
    val staffList: List<String>
)

// Repository Interface (core/domain/repository/SettingsRepository.kt)
interface SettingsRepository {
    fun getSettings(): Flow<Settings>
    suspend fun getOperationsConfig(): OperationsConfig
    suspend fun updateOperationsConfig(config: OperationsConfig): DataResult<Unit>
}

// UseCase Contract (feature/settings/domain/usecase/UpdateOperationsUseCase.kt)
class UpdateOperationsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(
        openTime: String,
        closeTime: String,
        slotDurationMinutes: Int,
        rawStaffInput: String
    ): DataResult<Unit>
}
```

### 9. Unit Tests
* **`UpdateOperationsUseCaseTest.kt`:**
  * Asserts rejection when `closeTime <= openTime`.
  * Asserts rejection when `slotDurationMinutes <= 0`.
  * Tests sanitization and deduplication of staff names (`"Sunita, Pooja, sunita"` $\rightarrow$ `["Sunita", "Pooja"]`).
* **`SlotTimeCalculatorTest.kt`:**
  * Tests string parsing of `"10:00"` to minutes from midnight (`600`).
  * Tests 12-hour display formatting (`"10:00"` $\rightarrow$ `"10:00 AM"`, `"14:30"` $\rightarrow$ `"02:30 PM"`).

### 10. Integration Tests (Not Required)
* Pure Kotlin unit tests and Robolectric settings DAO tests provide 100% coverage; no hardware or Android OS intent integration is involved.

### 11. Internal Contract / Boundary Tests
* **`SettingsDaoTest.kt`:** Verifies singleton row update in Room SQLite; verifies that updating `open`/`close` does not overwrite branding or PIN fields.

### 12. Files and Folder Mapping (Updated Post-Implementation)

#### Files Created:
1. `app/src/main/java/com/uniquemakeover/admin/core/domain/model/OperationsConfig.kt`
   - **Architectural Role:** Pure Domain Model
   - **Why Created:** Encapsulates parlour operating parameters (`openTime`, `closeTime`, `slotDurationMinutes`, `staffList`) independent of frameworks.
2. `app/src/main/java/com/uniquemakeover/admin/core/domain/model/Settings.kt`
   - **Architectural Role:** Pure Domain Model
   - **Why Created:** Full domain model representing salon metadata, credentials marker, operating hours, and communication configurations.
3. `app/src/main/java/com/uniquemakeover/admin/core/domain/repository/SettingsRepository.kt`
   - **Architectural Role:** Pure Domain Repository Interface
   - **Why Created:** Defines the repository contract for retrieving and persisting settings and operational timing configurations.
4. `app/src/main/java/com/uniquemakeover/admin/core/util/SlotTimeCalculator.kt`
   - **Architectural Role:** Core Utility
   - **Why Created:** Provides deterministic conversions between 24-hour time strings (`"10:00"`), minutes from midnight (`600`), and 12-hour AM/PM representations (`"10:00 AM"`).
5. `app/src/main/java/com/uniquemakeover/admin/feature/settings/data/mapper/SettingsMapper.kt`
   - **Architectural Role:** Data Layer (Mapper)
   - **Why Created:** Bidirectionally converts between Room `SettingsEntity` and domain models `Settings` and `OperationsConfig`.
6. `app/src/main/java/com/uniquemakeover/admin/feature/settings/data/repository/SettingsRepositoryImpl.kt`
   - **Architectural Role:** Data Layer (Repository Implementation)
   - **Why Created:** Bridges `SettingsDao` with `SettingsRepository` using `@IoDispatcher`, handling default entity creation and preserving PIN/branding on operations updates.
7. `app/src/main/java/com/uniquemakeover/admin/feature/settings/domain/usecase/GetOperationsConfigUseCase.kt`
   - **Architectural Role:** Pure Domain UseCase
   - **Why Created:** Single-responsibility use case that retrieves current operating parameters for slot generation and settings forms.
8. `app/src/main/java/com/uniquemakeover/admin/feature/settings/domain/usecase/UpdateOperationsUseCase.kt`
   - **Architectural Role:** Pure Domain UseCase
   - **Why Created:** Validates timing rules (`closeTime > openTime`, `slotDuration > 0`, operating window $\ge$ slot duration) and sanitizes/deduplicates staff inputs before persistence.
9. `app/src/main/java/com/uniquemakeover/admin/core/ui/components/TimePickerField.kt`
   - **Architectural Role:** Core Presentation Component
   - **Why Created:** Material 3 dialog and input field for selecting and formatting operating times in 12-hour AM/PM format adhering to `desing.md`.
10. `app/src/main/java/com/uniquemakeover/admin/feature/settings/presentation/sections/OperationsSection.kt`
    - **Architectural Role:** Presentation (Compose Component)
    - **Why Created:** Interactive operations configuration form featuring time pickers, segmented slot duration chips (15/30/45/60 min), and dynamic staff roster management.
11. `app/src/main/java/com/uniquemakeover/admin/core/ui/components/AppScaffold.kt`
    - **Architectural Role:** Core Presentation (Universal Scaffold Component)
    - **Why Created:** Implements the luxury 5-item responsive navigation shell (`Dashboard`, `Appointments`, `Services`, `Reminders`, `Settings`) adhering strictly to `desing.md` (§2); renders Material 3 `NavigationBar` on phones (< 600dp) and `NavigationRail` on foldables/tablets (>= 600dp) with brand palette tokens (`BrandPrimary`, `BlushSurface`, `SurfaceCard`).
12. `app/src/main/java/com/uniquemakeover/admin/feature/settings/presentation/SettingsUiState.kt`
    - **Architectural Role:** Presentation (UI State)
    - **Why Created:** Immutable presentation state model for the Settings screen tracking loading status, active `OperationsConfig`, and user feedback messages (`UiText`).
13. `app/src/main/java/com/uniquemakeover/admin/feature/settings/presentation/SettingsViewModel.kt`
    - **Architectural Role:** Presentation (ViewModel)
    - **Why Created:** Hilt-injected ViewModel managing UI state via `StateFlow` and orchestrating operating hours and staff roster mutations through `GetOperationsConfigUseCase` and `UpdateOperationsUseCase`.
14. `app/src/main/java/com/uniquemakeover/admin/feature/settings/presentation/SettingsScreen.kt`
    - **Architectural Role:** Presentation (Compose Screen & Route)
    - **Why Created:** Primary Settings destination hosted on `Routes.Settings`, providing luxury app bar, snackbar messaging, and hosting `OperationsSection` with reactive state.
15. `app/src/test/java/com/uniquemakeover/admin/core/util/SlotTimeCalculatorTest.kt`
    - **Architectural Role:** Automated JVM Unit Test
    - **Why Created:** Validates minute calculation, formatting, and edge bounds (midnight, noon, 24h clock conversions).
16. `app/src/test/java/com/uniquemakeover/admin/feature/settings/domain/usecase/UpdateOperationsUseCaseTest.kt`
    - **Architectural Role:** Automated JVM Unit Test
    - **Why Created:** Tests validation boundaries (`close <= open`, `slot <= 0`, window too small) and staff sanitization.
17. `app/src/test/java/com/uniquemakeover/admin/feature/settings/data/SettingsDaoTest.kt`
    - **Architectural Role:** Automated JVM / Robolectric Test
    - **Why Created:** Verifies singleton row updates and ensures PIN hash and branding fields are preserved.
18. `app/src/test/java/com/uniquemakeover/admin/feature/settings/presentation/SettingsViewModelTest.kt`
    - **Architectural Role:** Automated JVM Unit Test
    - **Why Created:** Verifies `SettingsViewModel` reactive state flow, successful operations configuration persistence, validation error propagation, and message clearing.
19. `app/src/test/java/com/uniquemakeover/admin/feature/settings/presentation/SettingsScreenTest.kt`
    - **Architectural Role:** Automated Compose UI Test
    - **Why Created:** Tests `SettingsScreen` UI rendering of headers, time pickers, chip groups, and save button interaction.
20. `app/src/test/java/com/uniquemakeover/admin/core/ui/components/AppScaffoldTest.kt`
    - **Architectural Role:** Automated Compose UI Test
    - **Why Created:** Verifies responsive rendering of 5 navigation items on phone (< 600dp), navigation rail on tablet (>= 600dp), and tab navigation callbacks.

#### Files Modified:
1. `app/src/main/java/com/uniquemakeover/admin/core/di/RepositoryModule.kt`
   - **Why Modified:** Bound `SettingsRepositoryImpl` to `SettingsRepository`.
2. `app/src/main/java/com/uniquemakeover/admin/navigation/Routes.kt`
   - **Why Modified:** Registered navigation constants for `Dashboard`, `Appointments`, `Reminders`, and `Settings`.
3. `app/src/main/java/com/uniquemakeover/admin/navigation/UniqueMakeoverNavHost.kt`
   - **Why Modified:** Integrated `AppScaffold` with responsive bottom bar / rail navigation, wired `SettingsRoute` to `Routes.Settings`, and introduced clean placeholder destinations for unbuilt tabs.

---

## Slice #05: Conflict-Free Manual Booking & Dynamic Slot Generation (US-APT-02)

### 1. Business Goal
Prevent embarrassing double-booking incidents and impossible past-time scheduling by dynamically calculating all available slots from operating hours, querying booked appointments from SQLite in $< 15$ ms, and disabling occupied or past slots with prominent visual indicators.

### 2. Actors
* **Salon Owner (Sunita):** Selects appointment date and time slot when taking client calls or walk-ins.

### 3. User Story
> **As** the salon owner (Sunita),  
> **I want** the booking form to dynamically show available time slots and disable already-booked times,  
> **So that** I never accidentally double-book a time slot or book an appointment in the past.

### 4. Acceptance Criteria
* **AC-APT-02.1 (Dynamic Slot Generation from Config):**
  * **Given** salon config with Open = `10:00 AM`, Close = `01:00 PM`, and Slot = `30 min`,
  * **When** opening the booking sheet for tomorrow,
  * **Then** exactly 6 slots are generated: `10:00 AM`, `10:30 AM`, `11:00 AM`, `11:30 AM`, `12:00 PM`, `12:30 PM`.
* **AC-APT-02.2 (Conflict Detection with "Full" Badge):**
  * **Given** an appointment exists on 2026-11-20 at `11:30 AM` with status `"booked"`,
  * **When** viewing the slot grid for 2026-11-20,
  * **Then** the `11:30 AM` chip displays a red/orange **"Full"** badge, alpha is reduced to 0.38, and click events are disabled.
* **AC-APT-02.3 (Cancelled Booking Releases Slot):**
  * **Given** an appointment at `11:30 AM` on 2026-11-20 is transitioned to `"cancel"`,
  * **When** viewing the slot grid for 2026-11-20,
  * **Then** the `11:30 AM` chip becomes active, selectable, and has no "Full" badge.
* **AC-APT-02.4 (Today's Past Slots Invalidation):**
  * **Given** the current device time is `02:15 PM` on today's date,
  * **When** viewing slots for today,
  * **Then** all slots prior to `02:30 PM` (e.g. 10:00 AM to 02:00 PM) are disabled with a grayed-out past indicator.

### 5. Edge Cases & Technical Resilience
* **EC-APT-02.1 (Last Slot Boundary Calculation):**
  * *Scenario:* Salon closes at 08:00 PM with 30-minute slots.
  * *System Behavior:* The last generated slot is `07:30 PM`. 08:00 PM is never generated because a service cannot be performed after doors close. Formula: `lastSlot = closeTime - slotDuration`.
* **EC-APT-02.2 (Device Clock Shifted / Time Provider Determinism - ADR-0006):**
  * *Scenario:* Salon owner's phone clock is manipulated or drifts.
  * *System Behavior:* All time checks inject an injectable `TimeProvider` interface (`timeProvider.now()`, `timeProvider.today()`). This decouples system wall-clock from domain logic and guarantees 100% deterministic unit tests.
* **EC-APT-02.3 (Date Change Invalidation of Previously Selected Slot):**
  * *Scenario:* Owner picks 11:00 AM on Monday, then toggles date to Tuesday where 11:00 AM is already "Full".
  * *System Behavior:* `BookingViewModel` re-validates the selected slot against the newly emitted slot list; if unavailable on Tuesday, selection resets to `null` with prompt: *"Selected time is full on this date. Please choose an open slot"*.

### 6. UI/UX Specification
* **Component:** `SlotGrid.kt` within `BookingSheet.kt`.
* **Layout:** Adaptive grid (3 columns on phone, 4-6 columns on tablet).
* **Chip Anatomy:**
  * **Available Slot:** White/Surface container, high-contrast dark text, subtle border. On tap: Animates to solid Luxury Gold (`#D4AF37`) with check icon.
  * **Booked Slot ("Full"):** Gray container, strikethrough or badge labeled **"Full"** in crimson red, non-clickable.
  * **Past Slot:** Low contrast, disabled opacity (38%), non-clickable.
* **Haptics:** Light tick on selecting an open slot; error reject vibration if attempting to tap a disabled slot.

### 7. Database Schema & Query Optimization
Uses composite index from Slice #01 for instant lookup:
```sql
-- Room Clash Query in AppointmentDao.kt
@Query("""
    SELECT COUNT(*) FROM appointments 
    WHERE date = :date 
      AND time = :time 
      AND status != 'cancel'
""")
suspend fun countClashingAppointments(date: String, time: String): Int
```

### 8. API / Internal Architecture Contracts

```kotlin
// Domain Model (core/domain/model/Slot.kt)
data class Slot(
    val time24: String,       // "14:30"
    val time12: String,       // "02:30 PM"
    val isAvailable: Boolean,
    val reason: UnavailableReason? = null
)

enum class UnavailableReason {
    ALREADY_BOOKED,
    PAST_TIME
}

// Repository Interface (feature/appointments/domain/repository/SlotRepository.kt)
interface SlotRepository {
    suspend fun getAvailableSlots(date: String): List<Slot>
    suspend fun isClashFree(date: String, time: String, excludeId: String? = null): Boolean
}

// UseCase Contract (feature/appointments/domain/usecase/GenerateAvailableSlotsUseCase.kt)
class GenerateAvailableSlotsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val appointmentDao: AppointmentDao,
    private val timeProvider: TimeProvider
) {
    suspend operator fun invoke(selectedDate: String): List<Slot>
}
```

### 9. Unit Tests
* **`GenerateAvailableSlotsUseCaseTest.kt`:**
  * Tests slot count: 10:00 to 14:00 with 30m slots returns 8 slots.
  * Tests clash mapping: Occupied slot returns `isAvailable = false`, `reason = ALREADY_BOOKED`.
  * Tests past-slot pruning: Injects `FakeTimeProvider(now = "12:15")`; asserts slots before 12:30 are marked `PAST_TIME` for today's date, but remain available for tomorrow.
* **`SlotRepositoryImplTest.kt`:** Tests clash query against mocked DAO.

### 10. Integration Tests (Required)
* **`AppointmentDaoClashTest.kt` (In-Memory Room):**
  * Inserts an appointment at `2026-11-20 15:00` with `status = "booked"`.
  * Verifies `countClashingAppointments("2026-11-20", "15:00") == 1`.
  * Updates status to `"cancel"`; verifies `countClashingAppointments("2026-11-20", "15:00") == 0`.
  * Inserts 50 appointments; verifies query completes in $< 10$ ms via index `index_appointments_date_time`.

### 11. Internal Contract / Boundary Tests
* **`SlotGridTest.kt` (Compose UI Test):** Verifies that clicking a slot marked with `UnavailableReason.ALREADY_BOOKED` emits no click event.

### 12. Files and Folder Mapping (Updated Post-Implementation)

#### Files Created:
1. `app/src/main/java/com/uniquemakeover/admin/core/domain/model/Slot.kt`
   - **Architectural Role:** Pure Domain Model
   - **Why Created:** Encapsulates the immutable time slot entity (`time24`, `time12`, `isAvailable`, `reason`) and `UnavailableReason` (`ALREADY_BOOKED`, `PAST_TIME`) independent of frameworks or SQLite tables.
2. `app/src/main/java/com/uniquemakeover/admin/core/util/TimeProvider.kt`
   - **Architectural Role:** Core Utility Contract & Default Implementation
   - **Why Created:** Decouples system wall-clock time (`LocalTime`, `LocalDate`) behind an injectable contract (`TimeProvider`, `DefaultTimeProvider`) to ensure 100% deterministic, drift-resistant time calculations and test harnesses.
3. `app/src/main/java/com/uniquemakeover/admin/feature/appointments/domain/repository/SlotRepository.kt`
   - **Architectural Role:** Domain Repository Interface
   - **Why Created:** Defines the Clean Architecture contract for querying clash-free slot availability without leaking Room DAOs into the domain layer.
4. `app/src/main/java/com/uniquemakeover/admin/feature/appointments/data/repository/SlotRepositoryImpl.kt`
   - **Architectural Role:** Data Repository Implementation
   - **Why Created:** Implements `SlotRepository` interface; bridges domain use cases with Room `AppointmentDao.countClashingAppointments()` on the `@IoDispatcher`.
5. `app/src/main/java/com/uniquemakeover/admin/feature/appointments/domain/usecase/GenerateAvailableSlotsUseCase.kt`
   - **Architectural Role:** Domain UseCase
   - **Why Created:** Implements the core business logic of generating daily time slots from operating hours, querying occupied slots from SQLite, evaluating past-time invalidation using injected `TimeProvider`, and enforcing boundary limits (`closeTime - slotDuration`).
6. `app/src/main/java/com/uniquemakeover/admin/feature/appointments/presentation/booking/components/SlotGrid.kt`
   - **Architectural Role:** Presentation (Compose Component)
   - **Why Created:** Renders the responsive time chip grid adhering to `desing.md`; adapts dynamically from 3 columns on phones to 4-6 columns on tablets with luxury gold selection state, red "Full" badge, and disabled past-time chips meeting $\ge 48\text{dp}$ touch targets.
7. `app/src/main/java/com/uniquemakeover/admin/feature/appointments/presentation/AppointmentsUiState.kt`
   - **Architectural Role:** Presentation (UI State)
   - **Why Created:** Represents immutable state for appointment slot selection and daily schedule view tracking selected date, slots, active selection, and loading state.
8. `app/src/main/java/com/uniquemakeover/admin/feature/appointments/presentation/AppointmentsViewModel.kt`
   - **Architectural Role:** Presentation (ViewModel)
   - **Why Created:** Hilt-injected ViewModel orchestrating dynamic slot generation via `GenerateAvailableSlotsUseCase` and managing date and slot selections with `StateFlow`.
9. `app/src/main/java/com/uniquemakeover/admin/feature/appointments/presentation/AppointmentsScreen.kt`
   - **Architectural Role:** Presentation (Screen & Route)
   - **Why Created:** Appointments destination screen hosted on `Routes.Appointments`, featuring luxury burgundy app bar, date selection chips, and hosting `SlotGrid`.
10. `app/src/test/java/com/uniquemakeover/admin/testutil/FakeTimeProvider.kt`
    - **Architectural Role:** Automated Test Double
    - **Why Created:** Provides controllable time and date provider for deterministic JVM testing of past-time invalidation.
11. `app/src/test/java/com/uniquemakeover/admin/feature/appointments/domain/usecase/GenerateAvailableSlotsUseCaseTest.kt`
    - **Architectural Role:** Automated JVM Unit Test
    - **Why Created:** Verifies slot count calculations, past-time pruning with `FakeTimeProvider`, clash mapping logic, and last slot boundary calculation.
12. `app/src/test/java/com/uniquemakeover/admin/feature/appointments/data/repository/SlotRepositoryImplTest.kt`
    - **Architectural Role:** Automated JVM Unit Test
    - **Why Created:** Validates `SlotRepositoryImpl` clash checking against `AppointmentDao`.
13. `app/src/test/java/com/uniquemakeover/admin/feature/appointments/data/AppointmentDaoClashTest.kt`
    - **Architectural Role:** Automated Integration Test (Room SQLite In-Memory)
    - **Why Created:** Verifies Room clash query detection, cancelled status slot release, `excludeId` handling, and indexed lookup performance ($< 10\text{ ms}$).
14. `app/src/test/java/com/uniquemakeover/admin/feature/appointments/presentation/booking/components/SlotGridTest.kt`
    - **Architectural Role:** Automated Compose UI Test
    - **Why Created:** Tests `SlotGrid` tap events, "Full" badge non-clickable state, "Past" chip state, and tablet grid responsiveness.
15. `app/src/test/java/com/uniquemakeover/admin/feature/appointments/presentation/AppointmentsViewModelTest.kt`
    - **Architectural Role:** Automated JVM Unit Test
    - **Why Created:** Tests ViewModel reactive state flow, slot loading on date change, and slot selection logic.
16. `app/src/test/java/com/uniquemakeover/admin/feature/appointments/presentation/AppointmentsScreenTest.kt`
    - **Architectural Role:** Automated Compose UI Test
    - **Why Created:** Tests `AppointmentsScreen` top bar, header rendering, and slot grid user interactions.

#### Files Modified:
1. `app/src/main/java/com/uniquemakeover/admin/core/di/AppModule.kt`
   - **Why Modified:** Bound `TimeProvider` to `DefaultTimeProvider`.
2. `app/src/main/java/com/uniquemakeover/admin/core/di/RepositoryModule.kt`
   - **Why Modified:** Bound `SlotRepositoryImpl` to `SlotRepository`.
3. `app/src/main/java/com/uniquemakeover/admin/core/database/dao/AppointmentDao.kt`
   - **Why Modified:** Enhanced `countClashingAppointments` composite query to exclude both `'cancel'` and `'cancelled'` statuses.
4. `app/src/main/java/com/uniquemakeover/admin/navigation/UniqueMakeoverNavHost.kt`
   - **Why Modified:** Wired `AppointmentsRoute` to `Routes.Appointments`.

---

## Slice #06: Multi-Service Selection, Real-Time Subtotal, Discount & Advance (US-APT-03)

### 1. Business Goal
Complete the primary revenue-generating action of the application: provide a fast, error-proof manual booking sheet that computes billing subtotals live across multiple services, validates discounts, records partial advances, enforces valid 10-digit Indian mobile numbers, and commits the booking atomically to SQLite.

### 2. Actors
* **Salon Owner (Sunita):** Creates customer appointments during in-person visits or over phone calls.

### 3. User Story
> **As** the salon owner (Sunita),  
> **I want** to select multiple services from my catalog, apply an optional discount, and record an advance payment during booking,  
> **So that** the total billing amount and balance due are calculated automatically and accurately.

### 4. Acceptance Criteria
* **AC-APT-03.1 (Live Billing Computation):**
  * **Given** the owner selects "Gold Facial" (₹800) and "Keratin Spa" (₹1,500),
  * **When** entering Discount = ₹300 and Advance Paid = ₹500,
  * **Then** Subtotal displays `₹ 2,300`, Final Rate displays `₹ 2,000`, and Balance Due displays `₹ 1,500` live without lag.
* **AC-APT-03.2 (Phone Number Validation):**
  * **Given** the owner enters `98221` (5 digits) or `1234567890` (invalid Indian telecom prefix),
  * **When** tapping "Save Appointment",
  * **Then** submission is blocked, the phone field outlines in red, and an error message displays *"Enter valid 10-digit Indian mobile number (starts with 6, 7, 8, or 9)"*.
* **AC-APT-03.3 (Advance Exceeding Final Rate Rejection):**
  * **Given** Final Rate is computed as `₹ 1,200`,
  * **When** entering Advance Paid as `₹ 1,500`,
  * **Then** the advance field shows inline error *"Advance cannot exceed total bill amount"*, and Save remains disabled.
* **AC-APT-03.4 (Atomic Database Commit):**
  * **Given** a valid booking form with selected slot, services, client name, and phone,
  * **When** tapping "Save Appointment",
  * **Then** a final clash re-check executes, a new row is inserted into `appointments` table with `status = "booked"`, the sheet dismisses, and a toast confirms *"Appointment booked successfully"*.

### 5. Edge Cases & Technical Resilience
* **EC-APT-03.1 (Discount Exceeds Subtotal):**
  * *Scenario:* Subtotal is ₹1,000; user enters ₹1,200 discount.
  * *System Behavior:* `ValidateBookingDraftUseCase` catches `discount > subtotal`. Blocks save and shows error: *"Discount cannot exceed subtotal (₹1,000)"*.
* **EC-APT-03.2 (Deselecting Service When Advance Exceeds New Lower Rate):**
  * *Scenario:* Services total ₹3,000, advance entered is ₹2,000. User deselects a ₹1,500 service (new subtotal ₹1,500).
  * *System Behavior:* ViewModel detects `advance (2000) > newRate (1500)`; immediately clamps or flags advance with red error, prompting owner to adjust the payment figure.
* **EC-APT-03.3 (Concurrency Race Condition at Save Instant):**
  * *Scenario:* Two booking forms attempt to claim the same time slot at the exact same millisecond.
  * *System Behavior:* `BookAppointmentUseCase` executes inside a Room `@Transaction`. It re-runs `isClashFree()` inside the transaction immediately before inserting. If taken, it throws `SlotClashException` and surfaces: *"Slot was just booked by another client. Please select another time"*.
* **EC-APT-03.4 (Zero-Rate / Complimentary Services):**
  * *Scenario:* Free touch-up is booked with Rate = ₹0 and Paid = ₹0.
  * *System Behavior:* Mathematical checks allow ₹0 totals; ensures no division-by-zero or negative due amounts.

### 6. UI/UX Specification
* **Screen:** `BookingSheet.kt` (Modal Bottom Sheet or Fullscreen Dialog on small phones).
* **Form Anatomy:**
  * **Client Details:** Name (`OutlinedTextField`), Mobile Number (10-digit numeric keyboard with `+91` prefix icon).
  * **Service Multi-Select:** Horizontal scrolling chip flow (`FilterChip`) displaying service name and price (e.g. `[✓ Gold Facial ₹800]`).
  * **Date & Slot Grid:** Date picker row + `SlotGrid` component from Slice #05.
  * **Staff Dropdown:** Exposed dropdown menu populated with `OperationsConfig.staffList`.
  * **Financial Card:**
    * Subtotal row: `₹ 2,300`
    * Discount field: `₹ [   ]`
    * Total Billable Rate: Bold Large `₹ 2,000`
    * Advance Paid field: `₹ [   ]`
    * Outstanding Due: Amber Badge `₹ 1,500`
  * **Bottom Bar:** Sticky [Save Appointment] full-width luxury button (`PrimaryButton.kt`).
* **Haptics:** Success vibration on save; error buzz on validation failure.

### 7. Database Schema & Snapshot Safety
Inserts into `appointments` table (from Slice #01):
```kotlin
@Entity(
    tableName = "appointments",
    indices = [
        Index("date"),
        Index("phone"),
        Index(value = ["date", "time"])
    ]
)
data class AppointmentEntity(
    @PrimaryKey val id: String,         // java.util.UUID.randomUUID().toString()
    val name: String,
    val phone: String,                  // 10-digit sanitized digits only
    val svc: String,                    // Frozen snapshot: "Gold Facial + Keratin Spa"
    val serviceIds: String,             // JSON array: "[\"s3\",\"s4\"]"
    val rate: Int,                      // Billed rate after discount
    val paid: Int,                      // Advance paid
    val staff: String,
    val date: String,                   // "YYYY-MM-DD"
    val time: String,                   // "HH:MM" 24-hr format
    val note: String,
    val status: String,                 // "booked"
    val reminded: String?,              // null
    val bookedAt: Long                  // System.currentTimeMillis()
)
```

### 8. API / Internal Architecture Contracts

```kotlin
// Booking Form State Draft (feature/appointments/domain/model/BookingDraft.kt)
data class BookingDraft(
    val name: String = "",
    val phone: String = "",
    val selectedServices: List<Service> = emptyList(),
    val date: String = "",
    val time: String = "",
    val staff: String = "",
    val discount: Int = 0,
    val advancePaid: Int = 0,
    val internalNote: String = ""
) {
    val subtotal: Int get() = selectedServices.sumOf { it.rate }
    val finalRate: Int get() = (subtotal - discount).coerceAtLeast(0)
    val balanceDue: Int get() = (finalRate - advancePaid).coerceAtLeast(0)
}

// Validation Result Contract
sealed interface BookingValidationResult {
    object Valid : BookingValidationResult
    data class Invalid(
        val nameError: UiText? = null,
        val phoneError: UiText? = null,
        val serviceError: UiText? = null,
        val slotError: UiText? = null,
        val financialError: UiText? = null
    ) : BookingValidationResult
}

// UseCase Contracts
class ValidateBookingDraftUseCase @Inject constructor(
    private val phoneSanitizer: PhoneSanitizer
) {
    operator fun invoke(draft: BookingDraft): BookingValidationResult
}

class BookAppointmentUseCase @Inject constructor(
    private val appointmentRepository: AppointmentRepository,
    private val slotRepository: SlotRepository
) {
    suspend operator fun invoke(draft: BookingDraft): DataResult<String>
}
```

### 9. Unit Tests
* **`ValidateBookingDraftUseCaseTest.kt`:**
  * Tests blank name rejection.
  * Tests phone validation: Rejects `12345`, `abcdefghij`, `0000000000`; accepts `9822001122`, `7888123456`.
  * Tests empty service selection rejection.
  * Tests discount validation: Rejects `discount > subtotal`.
  * Tests advance validation: Rejects `advance > finalRate`.
* **`BookingViewModelTest.kt`:**
  * Verifies live subtotal recalculation when toggling service selections.
  * Verifies discount subtraction and balance due computation.

### 10. Integration Tests (Required)
* **`BookingSheetTest.kt` (Compose UI + Room In-Memory):**
  * Launches `BookingSheet` in Compose test rule with in-memory database.
  * Selects 2 services from seeded catalog.
  * Enters Name = "Pooja Patil", Phone = "9876543210".
  * Selects available 10:00 AM slot.
  * Taps [Save Appointment].
  * Asserts sheet dismisses and asserts row is present in `appointmentDao.getAllAppointments()`.
* **`AppointmentRepositoryTransactionTest.kt` (Room Transaction):**
  * Verifies that inserting an appointment executes inside a transaction and commits successfully.

### 11. Internal Contract / Boundary Tests
* **`PhoneSanitizerTest.kt`:**
  * Verifies `+91 98220-01122` $\rightarrow$ `9822001122`.
  * Verifies `09822001122` $\rightarrow$ `9822001122`.
  * Verifies invalid length returns `null`.

### 12. Files and Folder Mapping

```
[CREATED]
app/src/main/java/com/uniquemakeover/admin/core/util/PhoneSanitizer.kt
app/src/main/java/com/uniquemakeover/admin/feature/appointments/domain/model/BookingDraft.kt
app/src/main/java/com/uniquemakeover/admin/feature/appointments/domain/model/Appointment.kt
app/src/main/java/com/uniquemakeover/admin/feature/appointments/domain/repository/AppointmentRepository.kt
app/src/main/java/com/uniquemakeover/admin/feature/appointments/data/mapper/AppointmentMapper.kt
app/src/main/java/com/uniquemakeover/admin/feature/appointments/data/repository/AppointmentRepositoryImpl.kt
app/src/main/java/com/uniquemakeover/admin/feature/appointments/domain/usecase/ValidateBookingDraftUseCase.kt
app/src/main/java/com/uniquemakeover/admin/feature/appointments/domain/usecase/BookAppointmentUseCase.kt
app/src/main/java/com/uniquemakeover/admin/feature/appointments/presentation/booking/BookingSheet.kt
app/src/main/java/com/uniquemakeover/admin/feature/appointments/presentation/booking/BookingViewModel.kt
app/src/main/java/com/uniquemakeover/admin/feature/appointments/presentation/booking/BookingUiState.kt
app/src/main/java/com/uniquemakeover/admin/feature/appointments/presentation/booking/components/ServiceMultiSelect.kt
app/src/main/java/com/uniquemakeover/admin/feature/appointments/presentation/booking/components/StaffDropdown.kt
app/src/main/java/com/uniquemakeover/admin/feature/appointments/presentation/booking/components/PaymentInputRow.kt
app/src/main/java/com/uniquemakeover/admin/core/ui/components/PrimaryButton.kt
app/src/test/java/com/uniquemakeover/admin/core/util/PhoneSanitizerTest.kt
app/src/test/java/com/uniquemakeover/admin/feature/appointments/domain/usecase/ValidateBookingDraftUseCaseTest.kt
app/src/test/java/com/uniquemakeover/admin/feature/appointments/presentation/booking/BookingViewModelTest.kt
app/src/androidTest/java/com/uniquemakeover/admin/feature/appointments/presentation/BookingSheetTest.kt
app/src/androidTest/java/com/uniquemakeover/admin/feature/appointments/data/AppointmentRepositoryTransactionTest.kt
[MODIFIED]
app/src/main/java/com/uniquemakeover/admin/core/database/DatabaseModule.kt (Binds AppointmentRepository)
```

---

## Phase 1 Test Execution Matrix & QA Acceptance Gate

To declare Phase 1 complete and authorize progression to **Phase 2 (Operations & Financial Cycle)**, QA must verify that all 14 test suites pass with 100% green status:

```bash
# 1. Run All JVM Unit Tests (< 20 seconds)
./gradlew testDebugUnitTest

# 2. Run All Phase 1 Android Runtime Integration Tests
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=\
com.uniquemakeover.admin.feature.settings.data.SettingsDaoTest,\
com.uniquemakeover.admin.feature.appointments.data.AppointmentDaoClashTest,\
com.uniquemakeover.admin.feature.appointments.data.AppointmentRepositoryTransactionTest,\
com.uniquemakeover.admin.feature.appointments.presentation.BookingSheetTest

# 3. Verify Detekt Clean Architecture Rules (no android.* in domain, no Room in presentation)
./gradlew detekt

# 4. Strict Offline Invariant Check
grep -rn "INTERNET" app/src/main/AndroidManifest.xml && exit 1
```

---
**End of Phase 1 Vertical Slices Specification — Unique Makeover Admin Console**
