# Unique Makeover — Admin Console
# Phase 4 Vertical Slices Specification
### Client CRM & Treatment Notes · Native Android · Fully Offline · Clean Architecture
**Document Version:** 1.0  
**Target Roles:** Veteran System Design Android Architect & QA Engineering Lead  
**Source References:** `docs/FRD.md` (§8.1–§8.4, §13.4), `docs/ARD.md` (§6.5), `docs/USER_STORIES.md`, `vertical-slice-indexmap.md`

---

## Phase 4 Executive Summary

Phase 4 builds the **Client CRM & Private Treatment Notes** subsystem of *Unique Makeover, Jalgaon Jamod*. Rather than forcing the salon owner to maintain a separate address book, Phase 4 dynamically synthesizes client profiles from appointment history using SQLite aggregate queries (`GROUP BY phone`). 

Furthermore, Phase 4 introduces private, confidential skin and hair notes (allergy warnings, hair dye sensitivity, scalp conditions) stored in a dedicated `client_notes` table with **500ms debounced autosave**, complete reverse-chronological visit timelines, and a **1-tap quick rebooking engine** that pre-populates previous beauty packages into the booking sheet in under 10 seconds.

```
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                     PHASE 4 VERTICAL SLICES                                      │
├──────────────────────────┬──────────────────────────┬──────────────────────────┬─────────────────┤
│ SLICE #16                │ SLICE #17                │ SLICE #18                │ SLICE #19       │
│ Client CRM Aggregation   │ Instant Substring Search │ Detailed Visit History   │ Private Notes   │
│ US-CRM-01 (Directory)    │ US-CRM-02 (Fast Lookup)  │ US-CRM-03 (Timeline)     │ US-CRM-04 (Auto)│
├──────────────────────────┴──────────────────────────┴──────────────────────────┴─────────────────┤
│ SLICE #20                                                                                        │
│ 1-Tap Quick Rebooking Pre-Fill                                                                   │
│ US-CRM-05 (Rebook Engine)                                                                        │
└──────────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## Slice #16: Client Directory & Automated History Aggregation (US-CRM-01)

### 1. Business Goal
Automatically consolidate all appointment records into unique client profiles grouped by 10-digit mobile number, computing total completed visits, lifetime revenue, and last visit dates without requiring manual customer address book entry.

### 2. Actors
* **Salon Owner (Sunita):** Views parlour client base and identifies high-value VIP clients.

### 3. User Story
> **As** the salon owner (Sunita),  
> **I want** the app to automatically build a client directory by grouping appointments by phone number,  
> **So that** I don't have to manually maintain a separate client address book.

### 4. Acceptance Criteria
* **AC-CRM-01.1 (Aggregate Computation Integrity):**
  * **Given** client "Neha" (phone `9822001122`) has 3 appointments: Apt 1 (Rate ₹500, status "done"), Apt 2 (Rate ₹1,200, status "done"), Apt 3 (Rate ₹800, status "booked"),
  * **When** opening the Client Directory,
  * **Then** Neha appears as a single row displaying: Visits: `3`, Total Lifetime Spend: `₹ 2,500`, and the date of her latest appointment.
* **AC-CRM-01.2 (Cancelled Bookings Exclusion from Spend):**
  * **Given** a client with 1 completed visit of ₹1,000 and 1 cancelled appointment of ₹2,000,
  * **When** viewing her profile summary,
  * **Then** completed visits count is `1` and total spend displays `₹ 1,000`.
* **AC-CRM-01.3 (Deduplication by Sanitized Phone):**
  * **Given** appointments booked under `"9822001122"` and `"+91 98220 01122"`,
  * **When** aggregating the directory,
  * **Then** they merge cleanly into a single client profile under canonical phone `9822001122`.

### 5. Edge Cases & Technical Resilience
* **EC-CRM-01.1 (Client Name Variations Across Visits):**
  * *Scenario:* Phone `9822001122` was entered as "Neha" on visit 1, and "Neha Kulkarni" on visit 2.
  * *System Behavior:* The SQL query selects the name associated with the latest `bookedAt` timestamp (`MAX(bookedAt)`), keeping the display name fresh and accurate.
* **EC-CRM-01.2 (Clients with 100+ Historical Appointments):**
  * *Scenario:* A loyal client has 100+ visits over 3 years.
  * *System Behavior:* Aggregation runs on indexed columns (`phone`, `status`, `bookedAt`); query resolves in $< 30\text{ ms}$ on mid-range devices without UI frame drops.
* **EC-CRM-01.3 (Clients with Only Cancelled Bookings):**
  * *Scenario:* A registered number has only 2 cancelled bookings and 0 completed visits.
  * *System Behavior:* Client still appears in directory so contact history isn't lost, displaying `0 Visits` and `₹ 0 Spend` with a distinct neutral badge.

### 6. UI/UX Specification
* **Screen:** `ClientsScreen.kt` (Route: `Routes.Clients`).
* **Layout:** Vertical scrolling list (`LazyColumn`) with alphabetical jump index on right rail.
* **Card Anatomy (`ClientRow.kt`):**
  * Left: Circular avatar with client initials in luxury gold background.
  * Center: Client Name (`titleMedium`), 10-digit Phone (`bodyMedium`), Last Service taken chip.
  * Right: Lifetime Spend pill (`₹ 4,500` in emerald green) and Total Visits badge (`bodySmall`).
  * Tap action: Transitions smoothly to `ClientDetailScreen`.

### 7. Database Schema & Query Optimization
Uses composite indices on `appointments` table (from Slice #01):
```sql
-- Room Query in AppointmentDao.kt
@Query("""
    SELECT 
        phone,
        name,
        COUNT(*) as totalVisits,
        SUM(CASE WHEN status != 'cancel' THEN rate ELSE 0 END) as totalSpend,
        MAX(date) as lastVisitDate,
        svc as lastServices
    FROM appointments
    GROUP BY phone
    ORDER BY MAX(bookedAt) DESC
""")
fun observeClientProfiles(): Flow<List<ClientProfileAggregate>>
```

### 8. API / Internal Architecture Contracts

```kotlin
// Data Transfer Object for Room Aggregate
data class ClientProfileAggregate(
    val phone: String,
    val name: String,
    val totalVisits: Int,
    val totalSpend: Int,
    val lastVisitDate: String,
    val lastServices: String
)

// Domain Model (feature/clients/domain/model/ClientProfile.kt)
data class ClientProfile(
    val phone: String,
    val name: String,
    val totalVisits: Int,
    val totalSpend: Int,
    val lastVisitDate: String,
    val lastServices: String
)

// Repository Interface (feature/clients/domain/repository/ClientProfileRepository.kt)
interface ClientProfileRepository {
    fun getClientProfiles(): Flow<List<ClientProfile>>
    fun searchClientProfiles(query: String): Flow<List<ClientProfile>>
}

// UseCase Contract (feature/clients/domain/usecase/GetClientProfilesUseCase.kt)
class GetClientProfilesUseCase @Inject constructor(
    private val clientProfileRepository: ClientProfileRepository
) {
    operator fun invoke(): Flow<List<ClientProfile>> = clientProfileRepository.getClientProfiles()
}
```

### 9. Unit Tests
* **`GetClientProfilesUseCaseTest.kt`:** Tests profile mapping, zero spend handling on cancelled appointments, and list sorting by latest visit date.

### 10. Integration Tests (Required)
* **`ClientProfileRepositoryImplTest.kt` (In-Memory Room):**
  * Inserts 5 appointments across 2 unique phone numbers with mixed statuses (`booked`, `done`, `cancel`).
  * Executes aggregate query.
  * Asserts exactly 2 unique `ClientProfile` records are returned with exact calculated spend sums and visit counts.

### 11. Internal Contract / Boundary Tests
* **`ClientProfileMapperTest.kt`:** Verifies conversion from database aggregate tuple to immutable domain `ClientProfile`.

### 12. Files and Folder Mapping (Updated Post-Implementation)

#### Files Created:
1. `app/src/main/java/com/uniquemakeover/admin/core/database/model/ClientProfileAggregate.kt`
   - **Architectural Role:** Database Query Projection DTO
   - **Why Created:** Encapsulates the raw Room aggregation tuple (`phone`, `name`, `totalVisits`, `totalSpend`, `lastVisitDate`, `lastServices`, `maxBookedAt`) returned by the composite indexed appointment query.
2. `app/src/main/java/com/uniquemakeover/admin/feature/clients/domain/model/ClientProfile.kt`
   - **Architectural Role:** Pure Kotlin Domain Model
   - **Why Created:** Represents immutable client summary entity independent of database, framework, or UI annotations.
3. `app/src/main/java/com/uniquemakeover/admin/feature/clients/domain/repository/ClientProfileRepository.kt`
   - **Architectural Role:** Domain Repository Contract
   - **Why Created:** Defines pure Kotlin abstraction for retrieving client directory streams and searching client profiles without exposing Room or Android APIs.
4. `app/src/main/java/com/uniquemakeover/admin/feature/clients/domain/usecase/GetClientProfilesUseCase.kt`
   - **Architectural Role:** Domain UseCase
   - **Why Created:** Encapsulates business logic for retrieving the consolidated client directory stream with single-responsibility `operator fun invoke()`.
5. `app/src/main/java/com/uniquemakeover/admin/feature/clients/data/mapper/ClientProfileMapper.kt`
   - **Architectural Role:** Data Mapper
   - **Why Created:** Decouples Room query projection `ClientProfileAggregate` from domain `ClientProfile`, handling null-safe fallbacks.
6. `app/src/main/java/com/uniquemakeover/admin/feature/clients/data/repository/ClientProfileRepositoryImpl.kt`
   - **Architectural Role:** Data Repository Implementation
   - **Why Created:** Bridges Room `AppointmentDao.observeClientProfiles()` on `@IoDispatcher`, grouping appointments by sanitized canonical phone numbers (`PhoneSanitizer`) and sorting by most recent interaction.
7. `app/src/main/java/com/uniquemakeover/admin/feature/clients/presentation/list/ClientsUiState.kt`
   - **Architectural Role:** Presentation UI State
   - **Why Created:** Immutable UI state model capturing directory list, search query, loading indicator, and error messaging.
8. `app/src/main/java/com/uniquemakeover/admin/feature/clients/presentation/list/ClientsViewModel.kt`
   - **Architectural Role:** Presentation ViewModel
   - **Why Created:** Manages client directory reactive streams, debounced/in-line search filtering, and UI state lifecycle using `StateFlow`.
9. `app/src/main/java/com/uniquemakeover/admin/feature/clients/presentation/list/components/ClientRow.kt`
   - **Architectural Role:** Presentation Compose Component
   - **Why Created:** Renders luxury gold initials avatar, client name, formatted phone, lifetime spend pill in emerald green, and visit count badge adhering to `desing.md` with $\ge 48\text{dp}$ touch target.
10. `app/src/main/java/com/uniquemakeover/admin/feature/clients/presentation/list/ClientsScreen.kt`
    - **Architectural Role:** Presentation Screen & Route
    - **Why Created:** Renders full client directory list with search bar, client count badge, and empty state illustrations.
11. `app/src/test/java/com/uniquemakeover/admin/feature/clients/domain/usecase/GetClientProfilesUseCaseTest.kt`
    - **Architectural Role:** Domain Unit Test
    - **Why Created:** Verifies usecase delegates directly to repository and returns mapped profiles or empty list.
12. `app/src/test/java/com/uniquemakeover/admin/feature/clients/data/mapper/ClientProfileMapperTest.kt`
    - **Architectural Role:** Data Unit Test
    - **Why Created:** Verifies null safety and mapping accuracy from database projection to domain model.
13. `app/src/test/java/com/uniquemakeover/admin/feature/clients/data/ClientProfileRepositoryImplTest.kt`
    - **Architectural Role:** In-Memory Room Integration Test (Robolectric)
    - **Why Created:** Verifies AC-CRM-01.1 (aggregates), AC-CRM-01.2 (cancelled spend exclusion), AC-CRM-01.3 (phone sanitization deduplication), EC-CRM-01.1 (latest name freshness), and EC-CRM-01.3 (only cancelled appointments).
14. `app/src/test/java/com/uniquemakeover/admin/feature/clients/presentation/list/ClientsViewModelTest.kt`
    - **Architectural Role:** Presentation Unit Test
    - **Why Created:** Verifies profile loading, search query filtering by name and phone substring, and error handling.

#### Files Modified:
1. `app/src/main/java/com/uniquemakeover/admin/core/database/dao/AppointmentDao.kt`
   - **Why Modified:** Added `observeClientProfiles(): Flow<List<ClientProfileAggregate>>` indexed aggregation query with correlated subqueries resolving name freshness and services in $< 30$ ms.
2. `app/src/main/java/com/uniquemakeover/admin/core/di/RepositoryModule.kt`
   - **Why Modified:** Bound `ClientProfileRepository` interface to `ClientProfileRepositoryImpl`.
3. `app/src/main/java/com/uniquemakeover/admin/navigation/Routes.kt`
   - **Why Modified:** Added `Routes.Clients = "clients"`.
4. `app/src/main/java/com/uniquemakeover/admin/core/ui/components/AppScaffold.kt`
   - **Why Modified:** Added Clients navigation destination with `Icons.Outlined.People` into `MainNavItems`.
5. `app/src/main/java/com/uniquemakeover/admin/navigation/UniqueMakeoverNavHost.kt`
   - **Why Modified:** Registered `composable(Routes.Clients)` route hosting `ClientsRoute`.
6. `app/src/test/java/com/uniquemakeover/admin/feature/appointments/data/repository/AppointmentRepositoryImplTest.kt`
   - **Why Modified:** Implemented `observeClientProfiles()` on `FakeAppointmentDao`.
7. `app/src/test/java/com/uniquemakeover/admin/feature/appointments/data/repository/SlotRepositoryImplTest.kt`
   - **Why Modified:** Implemented `observeClientProfiles()` on `FakeAppointmentDao`.

---

## Slice #17: Instant Client Search by Name or Mobile (US-CRM-02)

### 1. Business Goal
Enable the salon owner to search the consolidated client database instantly by typing any substring of a client's name or 10-digit mobile number, surfacing records in $< 30\text{ ms}$ while speaking with a customer on the phone.

### 2. Actors
* **Salon Owner (Sunita):** Searches for a client during phone booking enquiries.

### 3. User Story
> **As** the salon owner (Sunita),  
> **I want** to search my client directory by typing any part of a name or phone number,  
> **So that** I can find a client's records in seconds while they are on the phone.

### 4. Acceptance Criteria
* **AC-CRM-02.1 (Search by Partial Name):**
  * **Given** registered clients "Radhika Apte" and "Anjali Sharma",
  * **When** typing `"radh"` into the search box,
  * **Then** "Radhika Apte" is shown and "Anjali Sharma" is hidden within $< 50\text{ ms}$.
* **AC-CRM-02.2 (Search by Partial Phone Substring):**
  * **Given** a client with phone `9822123456`,
  * **When** typing `"1234"` or `"3456"`,
  * **Then** that client appears in the search results immediately.
* **AC-CRM-02.3 (Debounce & Immediate Clear):**
  * **Given** an active search query,
  * **When** tapping the `(X)` clear button,
  * **Then** the search field clears instantly and the complete client directory re-renders without flickering.

### 5. Edge Cases & Technical Resilience
* **EC-CRM-02.1 (Rapid Query Typing with Frequent Backspaces):**
  * *Scenario:* User types "Pooja", hits backspace 3 times, and types "Priya".
  * *System Behavior:* ViewModel utilizes Kotlin `Flow.debounce(150ms)` and `collectLatest`; previous query emissions are cancelled in-flight, preventing race conditions or stale results from overwriting current results.
* **EC-CRM-02.2 (Regional Marathi Devanagari Text Input):**
  * *Scenario:* User types "पूजा" or non-Latin Unicode characters.
  * *System Behavior:* SQLite UTF-8 case-insensitive text collation matches Devanagari characters safely without throwing collation exceptions.
* **EC-CRM-02.3 (Accidental Whitespace Padding):**
  * *Scenario:* User copies and pastes `"  9822123456  "`.
  * *System Behavior:* Query auto-trims leading/trailing whitespace prior to filtering.

### 6. UI/UX Specification
* **Component:** Search header in `ClientsScreen.kt`.
* **Anatomy:**
  * Material 3 `SearchBar` or `OutlinedTextField` with rounded corners (28 dp pill).
  * Leading search icon; trailing animated clear icon button.
  * Real-time query result count chip (e.g. `3 matches found`).
* **Empty State:** `EmptyState.kt` with user-search illustration: *"No clients found matching '{query}'"*.

### 7. Database Schema & Query Optimization
```sql
-- Room Query in AppointmentDao.kt
@Query("""
    SELECT 
        phone,
        name,
        COUNT(*) as totalVisits,
        SUM(CASE WHEN status != 'cancel' THEN rate ELSE 0 END) as totalSpend,
        MAX(date) as lastVisitDate,
        svc as lastServices
    FROM appointments
    WHERE name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%'
    GROUP BY phone
    ORDER BY MAX(bookedAt) DESC
""")
fun searchClientProfiles(query: String): Flow<List<ClientProfileAggregate>>
```

### 8. API / Internal Architecture Contracts

```kotlin
// UseCase Contract (feature/clients/domain/usecase/SearchClientProfilesUseCase.kt)
class SearchClientProfilesUseCase @Inject constructor(
    private val clientProfileRepository: ClientProfileRepository
) {
    operator fun invoke(query: String): Flow<List<ClientProfile>>
}
```

### 9. Unit Tests
* **`SearchClientProfilesUseCaseTest.kt`:**
  * Tests case-insensitive substring matching.
  * Tests blank query emission (returns all profiles).
  * Tests whitespace trimming on input.

### 10. Integration Tests (Not Required)
* Covered by Room aggregate integration test in Slice #16 and ViewModel Turbine flow tests.

### 11. Internal Contract / Boundary Tests
* **`ClientsSearchFlowTest.kt`:** Verifies `150ms` debounce properly squashes intermediate keystrokes.

### 12. Files and Folder Mapping

```
[CREATED]
app/src/main/java/com/uniquemakeover/admin/feature/clients/domain/usecase/SearchClientProfilesUseCase.kt
app/src/test/java/com/uniquemakeover/admin/feature/clients/domain/usecase/SearchClientProfilesUseCaseTest.kt
[MODIFIED]
app/src/main/java/com/uniquemakeover/admin/feature/clients/presentation/list/ClientsViewModel.kt (Integrates search debounced flow)
app/src/main/java/com/uniquemakeover/admin/core/database/dao/AppointmentDao.kt (Adds searchClientProfiles query)
```

---

## Slice #18: Detailed Client History & Lifetime Metrics (US-CRM-03)

### 1. Business Goal
Provide a comprehensive single-client dashboard displaying total visits, lifetime spend, quick communication shortcuts (Call / WhatsApp), and a full reverse-chronological timeline of every past beauty treatment received at the parlour.

### 2. Actors
* **Salon Owner (Sunita):** Reviews a client's past beauty treatments, stylist history, and spending habits during an in-salon consultation.

### 3. User Story
> **As** the salon owner (Sunita),  
> **I want** to open a client's profile and see their complete chronological visit history,  
> **So that** I know their service preferences, spending habits, and visit frequency over time.

### 4. Acceptance Criteria
* **AC-CRM-03.1 (Header Lifetime Metrics):**
  * **Given** a client profile selected from the directory,
  * **When** opening `ClientDetailScreen`,
  * **Then** the header renders client name, phone, Lifetime Spend (`₹ 8,500`), Total Visits (`5`), and quick action buttons [📞 Call] and [💬 WhatsApp].
* **AC-CRM-03.2 (Reverse-Chronological Visit Timeline):**
  * **Given** a client with 5 visits over the past 6 months,
  * **When** viewing the history list,
  * **Then** all 5 visits render sorted newest first (`date DESC, time DESC`), displaying visit date, booked services snapshot, attending staff, rate, paid, and status chip.
* **AC-CRM-03.3 (Quick Phone Call Launch):**
  * **Given** the client detail header,
  * **When** tapping the [📞 Call] button,
  * **Then** the Android system dialer opens with client phone number pre-filled via `Intent.ACTION_DIAL`.

### 5. Edge Cases & Technical Resilience
* **EC-CRM-03.1 (Historical Services Deleted from Catalog):**
  * *Scenario:* Client received `"Hydra Facial v1"` 6 months ago, which was later deleted from the active catalog.
  * *System Behavior:* History item renders `"Hydra Facial v1"` normally from the immutable `AppointmentEntity.svc` snapshot without errors.
* **EC-CRM-03.2 (Tablet Without SIM Card / Dialer):**
  * *Scenario:* Owner taps [📞 Call] on an Android WiFi-only tablet.
  * *System Behavior:* Catches `ActivityNotFoundException`; shows a non-intrusive toast: *"No calling application available on this device"*.
* **EC-CRM-03.3 (Client with 100% Cancelled Appointments):**
  * *Scenario:* A client booked 3 times but cancelled all 3 appointments.
  * *System Behavior:* Timeline cards display gray cancelled status badges with cancellation dates; total spend displays `₹ 0`.

### 6. UI/UX Specification
* **Screen:** `ClientDetailScreen.kt` (Route: `Routes.ClientDetail/{phone}`).
* **Anatomy:**
  * **Header Card (`ClientMetricsHeader.kt`):**
    * Top row: Client Name, 10-digit Phone, [Quick Rebook] action button.
    * Middle row: Two metric cards: `[Total Spend: ₹ 8,500]` (Emerald green) and `[Visits: 5]` (Luxury gold).
    * Bottom row: Quick action buttons: `[📞 Call Client]` and `[💬 WhatsApp Message]`.
  * **Tabs / Section Split:**
    * Tab 1: **Visit History** (`HistoryList.kt`).
    * Tab 2: **Private Notes** (`NoteEditor.kt` — Slice #19).
* **Timeline Card Anatomy:** Clean card displaying formatted date (e.g. `14-Oct-2026 • 02:30 PM`), staff chip (`Attended by Pooja`), services list, billed rate, and status chip.

### 7. Database Schema & Query Optimization
Indexed Room query on `phone` column:
```sql
-- Room Query in AppointmentDao.kt
@Query("SELECT * FROM appointments WHERE phone = :phone ORDER BY date DESC, time DESC")
fun getAppointmentsByPhone(phone: String): Flow<List<AppointmentEntity>>
```

### 8. API / Internal Architecture Contracts

```kotlin
// Domain Model (feature/clients/domain/model/ClientHistoryItem.kt)
data class ClientHistoryItem(
    val appointmentId: String,
    val date: String,
    val time: String,
    val formattedServices: String,
    val staff: String,
    val rate: Int,
    val paid: Int,
    val balanceDue: Int,
    val status: AppointmentStatus
)

// UseCase Contract (feature/clients/domain/usecase/GetClientHistoryUseCase.kt)
class GetClientHistoryUseCase @Inject constructor(
    private val appointmentRepository: AppointmentRepository
) {
    operator fun invoke(phone: String): Flow<List<ClientHistoryItem>>
}
```

### 9. Unit Tests
* **`GetClientHistoryUseCaseTest.kt`:** Tests reverse-chronological sorting and entity-to-history-item transformation.

### 10. Integration Tests (Not Required)
* Dao query and Compose UI tests provide full coverage; dialer intent uses standard Android `ACTION_DIAL`.

### 11. Internal Contract / Boundary Tests
* **`ClientDetailScreenTest.kt` (Compose UI Test):** Verifies header displays client name and formatted spend correctly.

### 12. Files and Folder Mapping

```
[CREATED]
app/src/main/java/com/uniquemakeover/admin/feature/clients/domain/model/ClientHistoryItem.kt
app/src/main/java/com/uniquemakeover/admin/feature/clients/domain/usecase/GetClientHistoryUseCase.kt
app/src/main/java/com/uniquemakeover/admin/feature/clients/presentation/detail/ClientDetailScreen.kt
app/src/main/java/com/uniquemakeover/admin/feature/clients/presentation/detail/ClientDetailViewModel.kt
app/src/main/java/com/uniquemakeover/admin/feature/clients/presentation/detail/ClientDetailUiState.kt
app/src/main/java/com/uniquemakeover/admin/feature/clients/presentation/detail/components/ClientMetricsHeader.kt
app/src/main/java/com/uniquemakeover/admin/feature/clients/presentation/detail/components/HistoryList.kt
app/src/test/java/com/uniquemakeover/admin/feature/clients/domain/usecase/GetClientHistoryUseCaseTest.kt
app/src/test/java/com/uniquemakeover/admin/feature/clients/presentation/detail/ClientDetailViewModelTest.kt
app/src/androidTest/java/com/uniquemakeover/admin/feature/clients/presentation/ClientDetailScreenTest.kt
[MODIFIED]
app/src/main/java/com/uniquemakeover/admin/core/database/dao/AppointmentDao.kt (Adds getAppointmentsByPhone)
```

---

## Slice #19: Private Client Skin & Hair Notes with Debounced Autosave (US-CRM-04)

### 1. Business Goal
Provide a private, confidential notepad for each client profile to document critical beauty data (hair dye chemical allergies, skin sensitivities, scalp conditions, bleaching patch test results) that autosaves continuously without pressing a button, preventing allergic accidents in the salon.

### 2. Actors
* **Salon Owner (Sunita):** Records treatment notes and checks client allergies before applying chemicals or facials.

### 3. User Story
> **As** the salon owner (Sunita),  
> **I want** to record private medical/beauty notes for each client (e.g. hair dye allergies, skin sensitivities, preferred stylist) that autosave without pressing a button,  
> **So that** I can provide personalized care and avoid allergic reactions without worrying about losing typed notes.

### 4. Acceptance Criteria
* **AC-CRM-04.1 (Continuous Autosave with 500ms Debounce):**
  * **Given** the owner is typing in a client's private notes: *"Allergic to ammonia dye. Sensitive to glycolic acid."*,
  * **When** typing pauses for 500 ms,
  * **Then** the note is written to SQLite `client_notes` table, and the corner status indicator transitions from *"Saving..."* $\rightarrow$ *"Saved ✓"*.
* **AC-CRM-04.2 (Zero Data Loss on Screen Exit):**
  * **Given** the owner types notes and immediately taps the Back button before the 500ms debounce timer completes,
  * **Then** the pending text flushes to SQLite synchronously via `NonCancellable` coroutine context, guaranteeing zero data loss.
* **AC-CRM-04.3 (Persistence Across App Restarts):**
  * **Given** a note saved for client `9822001122`,
  * **When** the app process is terminated and reopened,
  * **Then** navigating to `ClientDetailScreen` for `9822001122` restores the note text exactly as typed.

### 5. Edge Cases & Technical Resilience
* **EC-CRM-04.1 (Large Consultation Notes):**
  * *Scenario:* Extensive 5-year chemical consultation history (e.g. 5,000 words).
  * *System Behavior:* SQLite `TEXT` column handles large UTF-8 text safely; Compose text field uses virtualization and vertical scrolling without memory stutter.
* **EC-CRM-04.2 (Navigating Away Mid-Debounce / App Sent to Background):**
  * *Scenario:* Incoming call or home button pressed while typing.
  * *System Behavior:* `ViewModel.onCleared()` / `DisposableEffect` commits uncommitted string buffer using `withContext(NonCancellable + ioDispatcher)`.
* **EC-CRM-04.3 (Shared Household Mobile Number):**
  * *Scenario:* Two sisters share a single mobile number.
  * *System Behavior:* Single note field allows timestamped paragraph entries (e.g. `[Pooja]: allergic to bleach / [Priya]: normal skin`).

### 6. UI/UX Specification
* **Component:** `NoteEditor.kt` inside `ClientDetailScreen.kt`.
* **Anatomy:**
  * Container: Luxury surface container with amber security shield icon: *"🔒 Private Salon Treatment & Allergy Notes"*.
  * Status Indicator: Subtle caption in top-right corner:
    * Idle: *"Saved ✓"* (Muted gray/green)
    * Typing: *"Saving..."* (Amber pulsating)
  * Text Area: Large multiline `BasicTextField` / `OutlinedTextField` with placeholder: *"Enter confidential skin sensitivities, hair dye allergies, preferred stylist..."*.
* **Ergonomics:** Direct typing with no explicit [Save] button required.

### 7. Database Schema Reference
Uses `client_notes` table from Slice #01:
```sql
-- Room Entity in core/database/entity/ClientNoteEntity.kt
@Entity(tableName = "client_notes")
data class ClientNoteEntity(
    @PrimaryKey val phone: String,       // 10-digit sanitized
    val note: String,
    val updatedAt: Long                  // Epoch millis
)

-- Room DAO in ClientNoteDao.kt
@Query("SELECT * FROM client_notes WHERE phone = :phone")
fun observeNoteByPhone(phone: String): Flow<ClientNoteEntity?>

@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun insertOrUpdateNote(note: ClientNoteEntity)
```

### 8. API / Internal Architecture Contracts

```kotlin
// Domain Model (core/domain/model/ClientNote.kt)
data class ClientNote(
    val phone: String,
    val note: String,
    val updatedAt: Long
)

// Repository Interface (core/domain/repository/ClientNoteRepository.kt)
interface ClientNoteRepository {
    fun getNote(phone: String): Flow<ClientNote?>
    suspend fun saveNote(phone: String, noteText: String): DataResult<Unit>
}

// UseCase Contracts
class GetClientNoteUseCase @Inject constructor(
    private val clientNoteRepository: ClientNoteRepository
) {
    operator fun invoke(phone: String): Flow<ClientNote?> = clientNoteRepository.getNote(phone)
}

class SaveClientNoteUseCase @Inject constructor(
    private val clientNoteRepository: ClientNoteRepository
) {
    suspend operator fun invoke(phone: String, noteText: String): DataResult<Unit> =
        clientNoteRepository.saveNote(phone, noteText)
}
```

### 9. Unit Tests
* **`SaveClientNoteUseCaseTest.kt`:**
  * Verifies saving delegates to repository with current timestamp.
* **`ClientDetailViewModelNoteTest.kt`:**
  * Uses Turbine to verify that rapid text input debounces and emits save request only after 500 ms of inactivity.

### 10. Integration Tests (Required)
* **`ClientNoteDaoIntegrationTest.kt` (In-Memory Room):**
  * Inserts note for phone `9822001122`.
  * Verifies note is retrievable via `observeNoteByPhone()`.
  * Updates note for same phone; asserts row count remains 1 and text is replaced atomically.
* **`ClientNoteDebounceIntegrationTest.kt` (Compose UI Test + Turbine):**
  * Types text in `NoteEditor`; advances virtual clock by 400 ms (no write); advances by 150 ms (asserts write committed to Room).

### 11. Internal Contract / Boundary Tests
* **`ClientNoteMapperTest.kt`:** Verifies mapping between `ClientNoteEntity` and domain `ClientNote`.

### 12. Files and Folder Mapping

```
[CREATED]
app/src/main/java/com/uniquemakeover/admin/core/domain/model/ClientNote.kt
app/src/main/java/com/uniquemakeover/admin/core/domain/repository/ClientNoteRepository.kt
app/src/main/java/com/uniquemakeover/admin/feature/clients/data/mapper/ClientNoteMapper.kt
app/src/main/java/com/uniquemakeover/admin/feature/clients/data/repository/ClientNoteRepositoryImpl.kt
app/src/main/java/com/uniquemakeover/admin/feature/clients/domain/usecase/GetClientNoteUseCase.kt
app/src/main/java/com/uniquemakeover/admin/feature/clients/domain/usecase/SaveClientNoteUseCase.kt
app/src/main/java/com/uniquemakeover/admin/feature/clients/presentation/detail/components/NoteEditor.kt
app/src/test/java/com/uniquemakeover/admin/feature/clients/domain/usecase/SaveClientNoteUseCaseTest.kt
app/src/test/java/com/uniquemakeover/admin/feature/clients/presentation/detail/ClientDetailViewModelNoteTest.kt
app/src/androidTest/java/com/uniquemakeover/admin/feature/clients/data/ClientNoteDaoIntegrationTest.kt
app/src/androidTest/java/com/uniquemakeover/admin/feature/clients/presentation/ClientNoteDebounceIntegrationTest.kt
[MODIFIED]
app/src/main/java/com/uniquemakeover/admin/core/database/DatabaseModule.kt (Binds ClientNoteRepository)
app/src/main/java/com/uniquemakeover/admin/feature/clients/presentation/detail/ClientDetailScreen.kt (Integrates NoteEditor)
```

---

## Slice #20: 1-Tap Quick Rebooking Pre-Fill (US-CRM-05)

### 1. Business Goal
Accelerate repeat client bookings by providing a prominent **[Quick Rebook]** action on the client's profile that opens the booking sheet pre-filled with client name, phone number, and previously availed beauty packages in under 10 seconds.

### 2. Actors
* **Salon Owner (Sunita):** Quickly rebooks recurring appointments for regular clients over the counter.

### 3. User Story
> **As** the salon owner (Sunita),  
> **I want** a "Rebook" button on the client's profile that opens the booking form pre-populated with their details and last services,  
> **So that** repeat bookings take less than 10 seconds to create.

### 4. Acceptance Criteria
* **AC-CRM-05.1 (Pre-Fill Accuracy):**
  * **Given** client "Kavita" whose last completed visit was for `"Keratin Spa"` and `"Threading"`,
  * **When** tapping the [Quick Rebook] button from Kavita's profile header,
  * **Then** the booking sheet opens with Name = `"Kavita"`, Phone = `Kavita's phone`, and `"Keratin Spa"` + `"Threading"` pre-selected, with live subtotal calculated.
* **AC-CRM-05.2 (Open Slot & Date Selection):**
  * **Given** the pre-filled rebooking sheet opens,
  * **Then** Date defaults to today, Time slot is left unselected (`null`), and Staff is left open for the owner to pick.
* **AC-CRM-05.3 (Client with No Prior Completed Visits):**
  * **Given** a client who only had a cancelled booking,
  * **When** tapping [Quick Rebook],
  * **Then** Name and Phone pre-fill cleanly, while services selection starts empty.

### 5. Edge Cases & Technical Resilience
* **EC-CRM-05.1 (Historical Services No Longer in Catalog):**
  * *Scenario:* Client's last appointment had a seasonal promotional package that was subsequently deleted from the catalog.
  * *System Behavior:* `BuildRebookDraftUseCase` queries active catalog IDs; orphaned/deleted service IDs are filtered out cleanly, active services remain selected, and an inline caption informs: *"Some previous promotional services are no longer in the catalog"*.
* **EC-CRM-05.2 (Service Pricing Updated Since Last Visit):**
  * *Scenario:* A service cost ₹800 on client's last visit, but active catalog price is now ₹1,000.
  * *System Behavior:* The rebooking draft loads the current active catalog rate (`₹ 1,000`), recalculating subtotal and final bill accurately.

### 6. UI/UX Specification
* **Component:** Prominent action button `[⚡ Quick Rebook]` in `ClientMetricsHeader.kt`.
* **Flow:** Tapping button launches `BookingSheet.kt` directly with pre-populated `BookingDraft`.
* **Ergonomics:** Salon owner only needs to tap a date chip, pick an open time slot chip, and tap [Save Appointment]—completing repeat bookings in 3 taps.

### 7. Database Schema Reference
Reads `serviceIds` JSON string array from `appointments` table:
```kotlin
// Example row in appointments table:
// serviceIds = "[\"s3\",\"s4\"]"
```

### 8. API / Internal Architecture Contracts

```kotlin
// UseCase Contract (feature/clients/domain/usecase/BuildRebookDraftUseCase.kt)
class BuildRebookDraftUseCase @Inject constructor(
    private val appointmentRepository: AppointmentRepository,
    private val serviceRepository: ServiceRepository
) {
    suspend operator fun invoke(phone: String): DataResult<BookingDraft>
}
```

### 9. Unit Tests
* **`BuildRebookDraftUseCaseTest.kt`:**
  * Tests parsing of `serviceIds` JSON array from latest appointment.
  * Tests filtering out deleted service IDs against active catalog.
  * Asserts current catalog prices are applied to pre-filled draft.

### 10. Integration Tests (Not Required)
* Covered by `BookingSheetTest.kt` and `ClientProfileRepositoryImplTest.kt`.

### 11. Internal Contract / Boundary Tests
* **Navigation Contract Test:** Verifies navigation arguments pass client phone to booking sheet and pre-loads the draft.

### 12. Files and Folder Mapping

```
[CREATED]
app/src/main/java/com/uniquemakeover/admin/feature/clients/domain/usecase/BuildRebookDraftUseCase.kt
app/src/test/java/com/uniquemakeover/admin/feature/clients/domain/usecase/BuildRebookDraftUseCaseTest.kt
[MODIFIED]
app/src/main/java/com/uniquemakeover/admin/feature/clients/presentation/detail/components/ClientMetricsHeader.kt (Adds Quick Rebook button)
app/src/main/java/com/uniquemakeover/admin/feature/clients/presentation/detail/ClientDetailScreen.kt (Wires rebook navigation event)
app/src/main/java/com/uniquemakeover/admin/feature/appointments/presentation/booking/BookingViewModel.kt (Adds loadRebookDraft)
```

---

## Phase 4 Test Execution Matrix & QA Acceptance Gate

To declare Phase 4 complete and authorize progression to **Phase 5 (Reminders & WhatsApp Bridge)**, QA must verify that all 14 test suites pass with 100% green status:

```bash
# 1. Run All JVM Unit Tests (< 25 seconds)
./gradlew testDebugUnitTest

# 2. Run All Phase 4 Android Runtime Integration Tests
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=\
com.uniquemakeover.admin.feature.clients.data.ClientProfileRepositoryImplTest,\
com.uniquemakeover.admin.feature.clients.presentation.ClientDetailScreenTest,\
com.uniquemakeover.admin.feature.clients.data.ClientNoteDaoIntegrationTest,\
com.uniquemakeover.admin.feature.clients.presentation.ClientNoteDebounceIntegrationTest

# 3. Verify Detekt Architecture Rules (Zero Room in Presentation, Zero android.* in Domain)
./gradlew detekt

# 4. Strict Offline Invariant Check
grep -rn "INTERNET" app/src/main/AndroidManifest.xml && exit 1
```

---
**End of Phase 4 Vertical Slices Specification — Unique Makeover Admin Console**
