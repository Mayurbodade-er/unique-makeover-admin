# Unique Makeover — Admin Console
# Phase 0 Vertical Slices Specification
### Core Spine & Walking Skeleton · Native Android · Fully Offline · Clean Architecture
**Document Version:** 1.0  
**Target Roles:** Veteran System Design Android Architect & QA Engineering Lead  
**Source References:** `docs/FRD.md`, `docs/ARD.md`, `docs/USER_STORIES.md`, `vertical-slice-indexmap.md`

---

## Phase 0 Executive Summary

Phase 0 establishes the **Walking Skeleton** and the **Core Persistence Spine** of the Unique Makeover Admin Console. In Clean Architecture, Phase 0 does not build detached horizontal scaffolding; instead, it delivers three fully functional, end-to-end vertical slices that bootstrap database persistence, pre-populate default catalog data, and secure app entry with hardware-backed encryption.

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                             PHASE 0 VERTICAL SLICES                              │
├──────────────────────────┬────────────────────────────┬──────────────────────────┤
│ SLICE #01                │ SLICE #02                  │ SLICE #03                │
│ Core Database & WAL      │ Seed Catalog Initialization│ Cold-Start PIN Gate      │
│ US-SYS-03 (System Spine) │ US-SVC-01 (Data Bootstrap) │ US-AUTH-01 (Entry Gate)  │
└──────────────────────────┴────────────────────────────┴──────────────────────────┘
```

---

## Slice #01: Room SQLite Database Engine, Indices & WAL Concurrency (US-SYS-03)

### 1. Business Goal
Provide an indestructible, zero-cloud local data engine that starts in under 2 seconds, guarantees zero data corruption during sudden device power loss, and allows seamless concurrent reads/writes while the salon owner navigates appointments.

### 2. Actors
* **System Runtime:** Android OS Local Application Process.
* **Salon Owner (Sunita):** Indirect beneficiary experiencing zero latency, instant app wake-up, and zero data loss.

### 3. User Story
> **As** the salon owner (Sunita),  
> **I want** the app to open in under 2 seconds and scroll through thousands of appointments at a smooth 60 frames per second,  
> **So that** the app never freezes or lags during busy salon hours in Jalgaon Jamod.

### 4. Acceptance Criteria
* **AC-SYS-03.1 (Sub-2s Cold-Start Time):**
  * **Given** a device running Android 8.0+ (API 26) with a pre-existing database of 5,000 records,
  * **When** the application process cold-starts from launcher,
  * **Then** the first interactive frame renders on screen in $\le 1.8$ seconds.
* **AC-SYS-03.2 (Indexed Query Speed under Load):**
  * **Given** 5,000 appointment entities populated in SQLite,
  * **When** executing clash query `SELECT COUNT(*) FROM appointments WHERE date = :d AND time = :t`,
  * **Then** the composite index `(date, time)` resolves the query in $< 15$ ms.
* **AC-SYS-03.3 (Write-Ahead Logging Active):**
  * **Given** an open database instance,
  * **When** inspecting SQLite PRAGMA journal mode,
  * **Then** it returns `wal`, enabling simultaneous background workers and foreground UI reads without thread lockups.

### 5. Edge Cases & Technical Resilience
* **EC-SYS-03.1 (Sudden Power Loss / Battery Depletion during Flush):** SQLite WAL mode ensures uncommitted transactions reside in the `-wal` file; upon boot recovery, SQLite rolls back partial pages automatically without corrupting the main `.db` file.
* **EC-SYS-03.2 (Disk Full on Flash Storage):** SQLite throws `SQLiteFullException`. The database layer wraps all disk writes in `DataResult` error handling to inform the UI gracefully.
* **EC-SYS-03.3 (Concurrent Read/Write Thread Collisions):** Room DAO operations run on an injected `@IoDispatcher` with an optimized connection pool, preventing `SQLiteDatabaseLockedException`.

### 6. UI/UX Specification
* As a foundational infrastructure slice, there is no direct UI screen.
* **UX Impact:** Smooth Android 12+ `SplashScreen` handoff directly into the entry gate with zero jank or white screen flash.

### 7. Database Schema & SQLite Configuration

```sql
-- SQLite Schema Definition (Room v1)

CREATE TABLE IF NOT EXISTS `settings` (
    `id` INTEGER PRIMARY KEY NOT NULL,
    `shop` TEXT NOT NULL,
    `tagline` TEXT NOT NULL,
    `adminPinHash` TEXT NOT NULL,
    `logoPath` TEXT,
    `bgPath` TEXT,
    `wa` TEXT NOT NULL,
    `upi` TEXT NOT NULL,
    `staff` TEXT NOT NULL,
    `open` TEXT NOT NULL,
    `close` TEXT NOT NULL,
    `slot` INTEGER NOT NULL,
    `msg` TEXT NOT NULL,
    `lastBackup` TEXT
);

CREATE TABLE IF NOT EXISTS `services` (
    `id` TEXT PRIMARY KEY NOT NULL,
    `name` TEXT NOT NULL,
    `rate` INTEGER NOT NULL,
    `imagePath` TEXT,
    `sortOrder` INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS `appointments` (
    `id` TEXT PRIMARY KEY NOT NULL,
    `name` TEXT NOT NULL,
    `phone` TEXT NOT NULL,
    `svc` TEXT NOT NULL,
    `serviceIds` TEXT NOT NULL,
    `rate` INTEGER NOT NULL,
    `paid` INTEGER NOT NULL,
    `staff` TEXT NOT NULL,
    `date` TEXT NOT NULL,
    `time` TEXT NOT NULL,
    `note` TEXT NOT NULL,
    `status` TEXT NOT NULL,
    `reminded` TEXT,
    `bookedAt` INTEGER NOT NULL
);

-- Crucial Performance Indices
CREATE INDEX IF NOT EXISTS `index_appointments_date` ON `appointments` (`date`);
CREATE INDEX IF NOT EXISTS `index_appointments_phone` ON `appointments` (`phone`);
CREATE INDEX IF NOT EXISTS `index_appointments_date_time` ON `appointments` (`date`, `time`);

CREATE TABLE IF NOT EXISTS `client_notes` (
    `phone` TEXT PRIMARY KEY NOT NULL,
    `note` TEXT NOT NULL,
    `updatedAt` INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS `payments` (
    `id` TEXT PRIMARY KEY NOT NULL,
    `appointmentId` TEXT NOT NULL,
    `amount` INTEGER NOT NULL,
    `mode` TEXT NOT NULL,
    `paidAt` INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS `index_payments_appointmentId` ON `payments` (`appointmentId`);
```

### 8. API / Internal Architecture Contracts

```kotlin
// DataResult Container (core/domain/result/DataResult.kt)
sealed interface DataResult<out T> {
    data class Success<out T>(val data: T) : DataResult<T>
    data class Error(val message: UiText, val cause: Throwable? = null) : DataResult<Nothing>
}

// Room Database Declaration (core/database/UniqueMakeoverDatabase.kt)
@Database(
    entities = [
        SettingsEntity::class,
        ServiceEntity::class,
        AppointmentEntity::class,
        ClientNoteEntity::class,
        PaymentEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class UniqueMakeoverDatabase : RoomDatabase() {
    abstract fun settingsDao(): SettingsDao
    abstract fun serviceDao(): ServiceDao
    abstract fun appointmentDao(): AppointmentDao
    abstract fun clientNoteDao(): ClientNoteDao
    abstract fun paymentDao(): PaymentDao
}
```

### 9. Unit Tests
* **`DateFormattersTest.kt`:** Verifies conversion between epoch millis, `LocalDate`, and `"YYYY-MM-DD"`.
* **`ConvertersTest.kt`:** Verifies JSON list serialization/deserialization for `serviceIds` converter.
* **`DispatcherModuleTest.kt`:** Verifies that `@IoDispatcher` and `@DefaultDispatcher` inject correct test coroutine dispatchers under test rules.

### 10. Integration Tests (Required)
* **`UniqueMakeoverDatabaseTest.kt` (Robolectric / AndroidTest):**
  * Verifies database build with `Room.inMemoryDatabaseBuilder`.
  * Verifies `PRAGMA journal_mode=WAL` is active.
  * Verifies composite index `index_appointments_date_time` exists in `sqlite_master`.
  * Executes concurrent stress read/write loops across 8 coroutines to assert zero deadlocks.

### 11. Internal Contract / Boundary Tests
* **DAO Boundary Tests:** Test that DAO methods return typed domain-ready entities and emit updates through reactive Kotlin `Flow`.

### 12. Files and Folder Mapping (Updated Post-Implementation)

#### Files Created:
1. `app/src/main/java/com/uniquemakeover/admin/core/common/UiText.kt`
   - **Architectural Role:** Core Presentation / UI Abstraction
   - **Why Created:** Encapsulates dynamic and string-resource messages without coupling domain/business logic to the Android `Context` framework.
2. `app/src/main/java/com/uniquemakeover/admin/core/domain/result/DataResult.kt`
   - **Architectural Role:** Pure Domain Result Model
   - **Why Created:** Implements a pure Kotlin algebraic data type (`Success`, `Error`) for cross-layer error and payload propagation without throwing uncaught exceptions to UI.
3. `app/src/main/java/com/uniquemakeover/admin/core/di/DispatcherModule.kt`
   - **Architectural Role:** Core DI Module
   - **Why Created:** Binds `@IoDispatcher`, `@DefaultDispatcher`, and `@MainDispatcher` qualifiers to allow deterministic test dispatcher overriding in JVM and instrumentation test suites.
4. `app/src/main/java/com/uniquemakeover/admin/core/di/AppModule.kt`
   - **Architectural Role:** Core DI Module
   - **Why Created:** Provides application-scoped singletons including pre-configured `kotlinx.serialization.json.Json` with leniency and unknown-key tolerance.
5. `app/src/main/java/com/uniquemakeover/admin/core/database/entity/SettingsEntity.kt`
   - **Architectural Role:** Data Layer (Room Entity)
   - **Why Created:** Represents the persistent SQLite schema for parlour settings, operating hours, WhatsApp number, UPI ID, and PIN hash.
6. `app/src/main/java/com/uniquemakeover/admin/core/database/entity/ServiceEntity.kt`
   - **Architectural Role:** Data Layer (Room Entity)
   - **Why Created:** Represents the catalog treatment records (`name`, `rate`, `imagePath`, `sortOrder`) stored in local SQLite.
7. `app/src/main/java/com/uniquemakeover/admin/core/database/entity/AppointmentEntity.kt`
   - **Architectural Role:** Data Layer (Room Entity)
   - **Why Created:** Encapsulates parlour appointment records with performance indices on `date`, `phone`, and composite `(date, time)` for fast conflict lookups.
8. `app/src/main/java/com/uniquemakeover/admin/core/database/entity/ClientNoteEntity.kt`
   - **Architectural Role:** Data Layer (Room Entity)
   - **Why Created:** Persists private treatment preferences and allergy notes mapped directly to client phone numbers.
9. `app/src/main/java/com/uniquemakeover/admin/core/database/entity/PaymentEntity.kt`
   - **Architectural Role:** Data Layer (Room Entity)
   - **Why Created:** Stores transaction records (`amount`, `mode`, `paidAt`) linked to parent appointments via an indexed foreign reference.
10. `app/src/main/java/com/uniquemakeover/admin/core/database/Converters.kt`
    - **Architectural Role:** Data Layer (Room TypeConverter)
    - **Why Created:** Marshals/unmarshals `List<String>` service IDs to and from JSON arrays in SQLite without third-party reflection.
11. `app/src/main/java/com/uniquemakeover/admin/core/database/dao/SettingsDao.kt`
    - **Architectural Role:** Data Layer (Room DAO)
    - **Why Created:** Provides reactive Flow and synchronous queries for salon metadata, operating hours, and security parameters.
12. `app/src/main/java/com/uniquemakeover/admin/core/database/dao/ServiceDao.kt`
    - **Architectural Role:** Data Layer (Room DAO)
    - **Why Created:** Exposes reactive Flow and CRUD access to salon catalog treatments ordered by `sortOrder ASC, name ASC`.
13. `app/src/main/java/com/uniquemakeover/admin/core/database/dao/AppointmentDao.kt`
    - **Architectural Role:** Data Layer (Room DAO)
    - **Why Created:** Executes appointment scheduling operations including the indexed sub-15ms clash detection query.
14. `app/src/main/java/com/uniquemakeover/admin/core/database/dao/ClientNoteDao.kt`
    - **Architectural Role:** Data Layer (Room DAO)
    - **Why Created:** Provides atomic upsert and lookup for customer profile notes.
15. `app/src/main/java/com/uniquemakeover/admin/core/database/dao/PaymentDao.kt`
    - **Architectural Role:** Data Layer (Room DAO)
    - **Why Created:** Provides payment recording and total revenue sum queries per appointment.
16. `app/src/main/java/com/uniquemakeover/admin/core/database/UniqueMakeoverDatabase.kt`
    - **Architectural Role:** Data Layer (Room Database)
    - **Why Created:** Root SQLite Room database coordinating entities, DAOs, type converters, and schema versioning.
17. `app/src/main/java/com/uniquemakeover/admin/core/database/DatabaseModule.kt`
    - **Architectural Role:** Data Layer (Hilt DI Module)
    - **Why Created:** Injects the SQLite database instance configured with `WRITE_AHEAD_LOGGING` and `PRAGMA foreign_keys = ON;`, and exposes all DAOs.
18. `app/src/test/java/com/uniquemakeover/admin/core/database/ConvertersTest.kt`
    - **Architectural Role:** Automated JVM Unit Test
    - **Why Created:** Verifies null, empty, malformed, and valid JSON array transformations for Room TypeConverters.
19. `app/src/test/java/com/uniquemakeover/admin/core/database/UniqueMakeoverDatabaseTest.kt`
    - **Architectural Role:** Automated JVM / Robolectric Integration Test
    - **Why Created:** Validates in-memory Room instantiation, index existence in SQLite master, DAO CRUD, clash logic, and multi-threaded concurrency.
20. `app/schemas/com.uniquemakeover.admin.core.database.UniqueMakeoverDatabase/1.json`
    - **Architectural Role:** Room Schema Artifact
    - **Why Created:** Version 1 SQLite schema artifact emitted by KSP for migration validation.

#### Files Modified:
1. `detekt.yml`
   - **Why Modified:** Configured `thresholdInInterfaces: 20` and `functionPattern` for Compose compatibility.

---

## Slice #02: Service Catalog Browsing & Seed Data Initialization (US-SVC-01)

### 1. Business Goal
Eliminate cold-start onboarding friction for the salon owner by pre-seeding 7 standard Jalgaon Jamod beauty treatments (Gold Facial, Bridal Package, Haircut, etc.) on first install, allowing immediate appointment scheduling.

### 2. Actors
* **Salon Owner (Sunita):** Browses and uses pre-configured salon services immediately after installing the app.

### 3. User Story
> **As** the salon owner (Sunita),  
> **I want** the app to come pre-loaded with standard salon services upon first install,  
> **So that** I can begin booking appointments immediately without having to type dozens of services from scratch.

### 4. Acceptance Criteria
* **AC-SVC-01.1 (First-Run Seed Insertion):**
  * **Given** a brand new app installation with an empty database,
  * **When** the database is created,
  * **Then** Room `SeedCallback` executes and inserts exactly 7 curated services (Haircut ₹150, Threading ₹50, Gold Facial ₹800, Keratin Spa ₹1,500, Manicure ₹400, Pedicure ₹500, Bridal Package ₹5,000) into `services` table.
* **AC-SVC-01.2 (Subsequent Cold Starts No-Op):**
  * **Given** the database was already seeded and the owner edited "Gold Facial" to ₹1,000,
  * **When** the app is terminated and reopened,
  * **Then** the seed callback does not re-insert or overwrite the owner's customized pricing.
* **AC-SVC-01.3 (Services Screen Catalog Render):**
  * **Given** seeded services in SQLite,
  * **When** opening the Services screen,
  * **Then** services are rendered in a responsive grid showing thumbnail/icon, title, and formatted INR currency (`₹ 800`).

### 5. Edge Cases & Technical Resilience
* **EC-SVC-01.1 (Owner Deletes All 7 Seed Services):** The owner intentionally deletes all 7 services. The seed callback must NOT re-seed on next reboot; it runs strictly on `RoomDatabase.Callback.onCreate()`, never on `onOpen()`.
* **EC-SVC-01.2 (Process Termination During Seeding):** Seeding runs inside an atomic SQLite transaction (`withTransaction`). If killed mid-seed, partial records are rolled back cleanly and retried on next cold launch.
* **EC-SVC-01.3 (Empty Catalog UI Handling):** If all services are deleted, `ServicesScreen` renders an aesthetic `EmptyState` component with a direct call to action: *"No services found. Tap '+' to create your first service"*.

### 6. UI/UX Specification
* **Screen:** `ServicesScreen` (Route: `Routes.Services`).
* **Layout:** Material 3 Adaptive Vertical Grid (2 columns on phones, 3-4 columns on salon tablets).
* **Card Anatomy (`ServiceCard.kt`):**
  * 320px aspect-ratio-locked image header (or gold vector fallback icon `ic_spa`).
  * Typography: Title Medium (`Poppins`/`Inter` Semi-Bold) for Service Name.
  * Price Chip: Primary container pill badge with bold Indian Rupee formatting (e.g., `₹ 800`).
* **Interactions:** Subtle scale animation on touch; pull-to-refresh disabled (local-only instant reactive flow).

### 7. Database Schema Reference
Uses `services` table from Slice #01:
* Primary Key: `id` (e.g. `"s1"`, `"s2"`, ... `"s7"`).
* Columns: `name TEXT`, `rate INTEGER`, `imagePath TEXT NULL`, `sortOrder INTEGER`.

### 8. API / Internal Architecture Contracts

```kotlin
// Domain Model (core/domain/model/Service.kt)
data class Service(
    val id: String,
    val name: String,
    val rate: Int,
    val imagePath: String?,
    val sortOrder: Int = 0
)

// Repository Interface (core/domain/repository/ServiceRepository.kt)
interface ServiceRepository {
    fun getServices(): Flow<List<Service>>
    suspend fun getServiceById(id: String): Service?
}

// UseCase Contract (feature/services/domain/usecase/GetServicesUseCase.kt)
class GetServicesUseCase @Inject constructor(
    private val serviceRepository: ServiceRepository
) {
    operator fun invoke(): Flow<List<Service>> = serviceRepository.getServices()
}

// UI State Contract (feature/services/presentation/ServicesUiState.kt)
data class ServicesUiState(
    val isLoading: Boolean = true,
    val services: List<Service> = emptyList(),
    val errorMessage: UiText? = null
)
```

### 9. Unit Tests
* **`GetServicesUseCaseTest.kt`:** Mocks `ServiceRepository`, asserts use case properly sorts and maps domain `Service` entities.
* **`ServicesViewModelTest.kt`:** Uses `StandardTestDispatcher` and `Turbine` to verify emission of `ServicesUiState(isLoading = false, services = listOf(...))`.

### 10. Integration Tests (Required)
* **`SeedCallbackTest.kt` (In-Memory Room):**
  * Instantiates database with `SeedCallback`.
  * Triggers database open; queries `serviceDao.getAllServices()`.
  * Asserts exactly 7 records seeded with correct default pricing.
* **`ServicesScreenTest.kt` (Compose UI Test):**
  * Verifies `ServiceCard` renders "Gold Facial" and "₹ 800" text elements.

### 11. Internal Contract / Boundary Tests
* **Mapper Contract Test (`ServiceMapperTest.kt`):** Verifies bidirectional mapping between `ServiceEntity` and domain `Service`.

### 12. Files and Folder Mapping

```
[CREATED]
app/src/main/java/com/uniquemakeover/admin/core/database/SeedCallback.kt
app/src/main/java/com/uniquemakeover/admin/core/domain/model/Service.kt
app/src/main/java/com/uniquemakeover/admin/core/domain/repository/ServiceRepository.kt
app/src/main/java/com/uniquemakeover/admin/feature/services/data/mapper/ServiceMapper.kt
app/src/main/java/com/uniquemakeover/admin/feature/services/data/repository/ServiceRepositoryImpl.kt
app/src/main/java/com/uniquemakeover/admin/feature/services/domain/usecase/GetServicesUseCase.kt
app/src/main/java/com/uniquemakeover/admin/feature/services/presentation/ServicesScreen.kt
app/src/main/java/com/uniquemakeover/admin/feature/services/presentation/ServicesViewModel.kt
app/src/main/java/com/uniquemakeover/admin/feature/services/presentation/ServicesUiState.kt
app/src/main/java/com/uniquemakeover/admin/feature/services/presentation/components/ServiceCard.kt
app/src/test/java/com/uniquemakeover/admin/feature/services/domain/usecase/GetServicesUseCaseTest.kt
app/src/test/java/com/uniquemakeover/admin/feature/services/presentation/ServicesViewModelTest.kt
app/src/androidTest/java/com/uniquemakeover/admin/feature/services/SeedCallbackTest.kt
app/src/androidTest/java/com/uniquemakeover/admin/feature/services/presentation/ServicesScreenTest.kt
[MODIFIED]
app/src/main/java/com/uniquemakeover/admin/core/database/DatabaseModule.kt (Binds SeedCallback and ServiceRepository)
```

---

## Slice #03: App Cold-Start PIN Verification Gate (US-AUTH-01)

### 1. Business Goal
Protect confidential client contact numbers, sensitive skin treatment histories, and daily parlour revenue metrics from unauthorized eyes (clients, assistants, visitors) on the shared salon tablet.

### 2. Actors
* **Salon Owner (Sunita):** Enters master 4-digit PIN to authenticate into the system.
* **Unauthorized Person (Client / Helper):** Blocked at app launch without valid credentials.

### 3. User Story
> **As** the salon owner (Sunita),  
> **I want** the application to prompt for a 4-digit PIN on every cold start before revealing any salon data,  
> **So that** clients, helpers, or unauthorized persons holding the device cannot view confidential client notes, contact numbers, or revenue metrics.

### 4. Acceptance Criteria
* **AC-AUTH-01.1 (Valid PIN Authentication):**
  * **Given** the app cold-launches into `PinLockScreen`,
  * **When** entering the 4 digits matching the configured PIN (default `1234` on fresh install),
  * **Then** the dots fill sequentially, navigation immediately transitions to `Routes.Dashboard`, and `PinLockScreen` is popped from the backstack.
* **AC-AUTH-01.2 (Incorrect PIN Feedback):**
  * **Given** `PinLockScreen` is displayed,
  * **When** an incorrect PIN (e.g. `9999`) is typed,
  * **Then** the 4 dots clear, a horizontal shake animation triggers with haptic error vibration, and *"Incorrect PIN. Try again."* displays.
* **AC-AUTH-01.3 (Hardware Back Button Invariant):**
  * **Given** the owner is on `PinLockScreen`,
  * **When** pressing the Android hardware/gesture back button,
  * **Then** the app exits to the Android launcher; under no circumstances is any internal app screen exposed.

### 5. Edge Cases & Technical Resilience
* **EC-AUTH-01.1 (Rapid Tap Flooding / Bouncing):** ViewModel debounces keypad inputs (75ms window) and clamps buffer strictly to 4 digits.
* **EC-AUTH-01.2 (App Sent to Background Mid-PIN Entry):** When app goes to background (`onStop`), the partially typed PIN buffer is wiped from memory. Upon resume, 4 blank dots are presented.
* **EC-AUTH-01.3 (Screen Rotation / Config Change):** Entered digits are preserved safely in `PinLockViewModel` state flow.
* **EC-AUTH-01.4 (Keystore Hardware Exception):** If `KeyPermanentlyInvalidatedException` occurs, `PinLockManager` falls back safely without unhandled fatal crashes.

### 6. UI/UX Specification
* **Screen:** `PinLockScreen` (Route: `Routes.PinLock`).
* **Visual Hierarchy:**
  * Top: Salon Gold Emblem/Logo + *"Unique Makeover"*.
  * Sub-header: *"Enter Admin PIN"*.
  * Center: 4 animated PIN Dot Indicators (16dp diameter, 16dp spacing; active state fills with primary gold brush; error state flashes red).
  * Bottom: High-contrast 3x4 numeric keypad (digits 1–9, empty space, 0, backspace `ic_backspace`).
* **Haptics:** Light haptic tick (`HapticFeedbackType.TextHandleMove`) on digit tap; heavy error buzz (`HapticFeedbackType.LongPress`) on invalid PIN.
* **Keyboard Invariant:** Zero soft keyboard popup (`FocusManager` disabled for system IME).

### 7. Database & Keystore Storage Specification
* **Keystore Key Alias:** `UniqueMakeoverMasterKey`.
* **Encrypted Storage:** `EncryptedSharedPreferences` stores master salt + hash:
  $$\text{Hash} = \text{PBKDF2WithHmacSHA256}(\text{PIN}, \text{Salt}, \text{iterations} = 100{,}000, \text{keyLength} = 256)$$
* **Database Sync:** `SettingsEntity.adminPinHash` stores validation marker.
* **Default Setup:** First-run callback initializes PIN hash for `1234`.

### 8. API / Internal Architecture Contracts

```kotlin
// Domain Model (feature/auth/domain/model/PinResult.kt)
sealed interface PinResult {
    object Success : PinResult
    object Invalid : PinResult
    data class Locked(val remainingSeconds: Int) : PinResult
}

// Repository Interface (feature/auth/domain/repository/PinRepository.kt)
interface PinRepository {
    suspend fun verifyPin(enteredPin: String): PinResult
    suspend fun isDefaultPin(): Boolean
}

// UseCase Contract (feature/auth/domain/usecase/VerifyPinUseCase.kt)
class VerifyPinUseCase @Inject constructor(
    private val pinRepository: PinRepository
) {
    suspend operator fun invoke(pin: String): PinResult {
        if (pin.length != 4 || !pin.all { it.isDigit() }) return PinResult.Invalid
        return pinRepository.verifyPin(pin)
    }
}

// Presentation UI State (feature/auth/presentation/PinLockUiState.kt)
data class PinLockUiState(
    val digitsEntered: Int = 0,
    val isError: Boolean = false,
    val errorMessage: UiText? = null,
    val shakeTrigger: Long = 0L
)
```

### 9. Unit Tests
* **`PinHasherTest.kt`:** Verifies PBKDF2 hashing with unique random salts; asserts same PIN with different salts yields distinct ciphertexts.
* **`VerifyPinUseCaseTest.kt`:** Tests 4-digit formatting validation, success return on matching hash, and rejection on mismatch.
* **`PinLockViewModelTest.kt`:** Verifies digit accumulation, backspace deletion, and auto-submit upon 4th digit.

### 10. Integration Tests (Required)
* **`PinLockManagerInstrumentedTest.kt` (Android Runtime / Keystore):**
  * Real Android Keystore encryption/decryption round-trip test.
  * Validates master key generation and persistence across simulated process recreation.
* **`PinLockScreenTest.kt` (Compose UI Test):**
  * Taps digits '1', '2', '3', '4' on Compose keypad; asserts navigation callback triggers.
* **`UniqueMakeoverNavHostTest.kt` (Navigation Architecture Test):**
  * Asserts start destination is strictly `Routes.PinLock`.
  * Verifies hardware back button on `PinLock` finishes the Activity.

### 11. Internal Contract / Boundary Tests
* **Auth Guard Navigation Contract Test:** Verifies that attempting to deep-link directly to `Routes.Dashboard` without passing `VerifyPinUseCase` redirects back to `Routes.PinLock`.

### 12. Files and Folder Mapping

```
[CREATED]
app/src/main/java/com/uniquemakeover/admin/core/security/PinHasher.kt
app/src/main/java/com/uniquemakeover/admin/core/security/PinLockManager.kt
app/src/main/java/com/uniquemakeover/admin/feature/auth/domain/model/PinResult.kt
app/src/main/java/com/uniquemakeover/admin/feature/auth/domain/repository/PinRepository.kt
app/src/main/java/com/uniquemakeover/admin/feature/auth/domain/usecase/VerifyPinUseCase.kt
app/src/main/java/com/uniquemakeover/admin/feature/auth/data/PinRepositoryImpl.kt
app/src/main/java/com/uniquemakeover/admin/feature/auth/presentation/PinLockScreen.kt
app/src/main/java/com/uniquemakeover/admin/feature/auth/presentation/PinLockViewModel.kt
app/src/main/java/com/uniquemakeover/admin/feature/auth/presentation/PinLockUiState.kt
app/src/main/java/com/uniquemakeover/admin/feature/auth/presentation/components/PinKeypad.kt
app/src/main/java/com/uniquemakeover/admin/feature/auth/presentation/components/PinDots.kt
app/src/main/java/com/uniquemakeover/admin/navigation/Routes.kt
app/src/main/java/com/uniquemakeover/admin/navigation/UniqueMakeoverNavHost.kt
app/src/test/java/com/uniquemakeover/admin/core/security/PinHasherTest.kt
app/src/test/java/com/uniquemakeover/admin/feature/auth/domain/usecase/VerifyPinUseCaseTest.kt
app/src/test/java/com/uniquemakeover/admin/feature/auth/presentation/PinLockViewModelTest.kt
app/src/androidTest/java/com/uniquemakeover/admin/core/security/PinLockManagerInstrumentedTest.kt
app/src/androidTest/java/com/uniquemakeover/admin/feature/auth/presentation/PinLockScreenTest.kt
app/src/androidTest/java/com/uniquemakeover/admin/navigation/UniqueMakeoverNavHostTest.kt
[MODIFIED]
app/src/main/java/com/uniquemakeover/admin/MainActivity.kt (Set Compose Content to UniqueMakeoverNavHost)
app/src/main/java/com/uniquemakeover/admin/UniqueMakeoverApp.kt (@HiltAndroidApp initialization)
```

---

## Phase 0 Test Execution Matrix & QA Acceptance Gate

To declare Phase 0 complete and authorize progression to **Phase 1 (Core Booking Heartbeat)**, QA must verify that all 12 test suites pass with 100% green status:

```bash
# 1. Run All JVM Unit Tests (< 15 seconds)
./gradlew testDebugUnitTest

# 2. Run All Phase 0 Android Runtime Integration Tests
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=\
com.uniquemakeover.admin.core.database.UniqueMakeoverDatabaseTest,\
com.uniquemakeover.admin.feature.services.SeedCallbackTest,\
com.uniquemakeover.admin.core.security.PinLockManagerInstrumentedTest,\
com.uniquemakeover.admin.feature.auth.presentation.PinLockScreenTest,\
com.uniquemakeover.admin.navigation.UniqueMakeoverNavHostTest

# 3. Architectural Boundary & Offline Invariant Enforcement
./gradlew detekt
grep -rn "INTERNET" app/src/main/AndroidManifest.xml && exit 1
```

---
**End of Phase 0 Vertical Slices Specification — Unique Makeover Admin Console**
