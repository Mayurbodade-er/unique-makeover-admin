# Unique Makeover — Admin Console
# Phase 8 Vertical Slices Specification
### Data Portability & Disaster Recovery · Native Android · Fully Offline · Clean Architecture
**Document Version:** 1.0  
**Target Roles:** Veteran System Design Android Architect & QA Engineering Lead  
**Source References:** `docs/FRD.md` (§10.5, §11.1–§11.5), `docs/ARD.md` (§6.8, ADR-0005), `docs/USER_STORIES.md`, `vertical-slice-indexmap.md`

---

## Phase 8 Executive Summary

Phase 8 builds the **Data Portability & Disaster Recovery** subsystem of *Unique Makeover, Jalgaon Jamod*. In a 100% offline application with zero cloud sync and zero remote servers, local data portability is the ultimate safeguard for the salon's entire operational livelihood.

Phase 8 delivers complete offline data freedom:
1. **Full Database JSON Export:** Serializing the entire database (Settings, Service Catalog, Appointments, Client Private Notes) into a standardized `UniqueMakeover_backup_YYYY-MM-DD.json` file via Android's native **Storage Access Framework (SAF)**.
2. **Atomic Backup Import with Diff Preview & Rollback (ADR-0005):** Providing a pre-import diff preview summary (counts of services, appointments, and client notes) before executing an atomic SQLite restore wrapped in `RoomDatabase.withTransaction`. If an invalid file or mid-write power cut occurs, the transaction rolls back completely, ensuring zero data loss.
3. **Overdue Backup Monitoring:** Tracking elapsed days since `settings.lastBackup` and raising amber warnings when backups are older than 3 days.
4. **Factory Reset with PIN Challenge:** Enabling a safe clean-slate purge protected by a two-stage confirmation dialog and master 4-digit PIN challenge.

```
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                     PHASE 8 VERTICAL SLICES                                      │
├──────────────────────────┬──────────────────────────┬──────────────────────────┬─────────────────┤
│ SLICE #34                │ SLICE #35                │ SLICE #36                │ SLICE #37       │
│ Full JSON SAF Export     │ Atomic Import & Rollback │ Overdue Backup Monitor   │ Factory Reset   │
│ US-DAT-01 (Export)       │ US-DAT-02 (Atomic Import)│ US-DAT-03 (Backup Audit) │ US-DAT-04 (Wipe)│
└──────────────────────────┴──────────────────────────┴──────────────────────────┴─────────────────┘
```

---

## Slice #34: Full Database JSON Export via Storage Access Framework (US-DAT-01)

### 1. Business Goal
Allow the salon owner to export the entire parlour database to an external SD card, USB OTG drive, Google Drive folder, or device Downloads folder in a standardized, human-readable JSON format, enabling disaster recovery or seamless device migration without internet.

### 2. Actors
* **Salon Owner (Sunita):** Initiates database backup export to protect parlour records.

### 3. User Story
> **As** the salon owner (Sunita),  
> **I want** to export my entire app database to a single JSON backup file on my phone or SD card,  
> **So that** I can safeguard my business records or transfer them to a new phone without internet.

### 4. Acceptance Criteria
* **AC-DAT-01.1 (Standardized File Creation via SAF):**
  * **Given** today is 2026-11-14,
  * **When** the owner taps [Export Backup],
  * **Then** the Android system file creator (`CreateDocument`) opens suggesting filename `UniqueMakeover_backup_2026-11-14.zip` (consolidated archive bundling `database.json` and all local `filesDir/images`).
* **AC-DAT-01.2 (Complete Schema v2 JSON & Visual Asset Capture):**
  * **Given** an active database containing 50 appointments, 10 services with reference photos, and 15 client notes,
  * **When** the backup archive is written,
  * **Then** the exported archive contains `database.json` with all database tables AND an `images/` directory bundling all compressed service photo JPEGs, ensuring 100% visual asset portability across device restores.
* **AC-DAT-01.3 (Last Backup Timestamp Update):**
  * **Given** a successful backup export,
  * **Then** `settings.lastBackup` updates to the current ISO-8601 date, and a confirmation snackbar displays *"Backup exported successfully (XX KB)"*.

### 5. Edge Cases & Technical Resilience
* **EC-DAT-01.1 (User Cancels System File Picker):** Owner taps [Export Backup] but presses Back/Cancel on the system picker. `ActivityResultCallback` receives `null` Uri; export aborts silently without toast spam or corrupting `lastBackup`.
* **EC-DAT-01.2 (Target Storage Media Disconnected Mid-Write):** USB OTG flash drive unplugged during serialization. Stream catches `IOException`, safely rolls back, and displays an alert: *"Backup failed: Storage device was disconnected"*.
* **EC-DAT-01.3 (Concurrent Database Writes During Export):** An appointment is booked while the export coroutine reads tables. Room runs on snapshot isolation, guaranteeing the JSON document represents an internally consistent database state.

### 6. UI/UX Specification
* **Screen:** `SettingsScreen.kt` $\rightarrow$ `DataManagementSection.kt`.
* **Component:** Backup Action Card.
* **Anatomy:**
  * Header: *"📦 Database Backup & Recovery"*.
  * Status Caption: *"Last Backup: 14-Nov-2026 (Today)"*.
  * Action Button: [📥 Export Backup (JSON)] (`PrimaryButton.kt`).
* **Progress:** Indeterminate circular progress indicator displayed during serialization.

### 7. JSON Backup Schema Specification (Version 2)
Formatted per FRD §11.3 via `kotlinx.serialization`:
```json
{
  "version": 2,
  "exportedAt": "2026-11-14T10:00:00Z",
  "settings": {
    "shop": "Unique Makeover",
    "tagline": "Jalgaon Jamod • Luxury Beauty Parlour",
    "wa": "9822001122",
    "upi": "uniquemakeover@upi",
    "staff": "Sunita, Pooja, Priya",
    "open": "10:00",
    "close": "20:00",
    "slot": 30,
    "msg": "Hello {name}..."
  },
  "services": [
    {
      "id": "s1",
      "name": "Gold Facial",
      "rate": 800,
      "imagePath": "images/s1.jpg",
      "sortOrder": 0
    }
  ],
  "appointments": [
    {
      "id": "apt-uuid-1",
      "name": "Pooja Patil",
      "phone": "9876543210",
      "svc": "Gold Facial",
      "serviceIds": "[\"s1\"]",
      "rate": 800,
      "paid": 300,
      "staff": "Pooja",
      "date": "2026-11-15",
      "time": "10:30",
      "note": "Sensitive skin",
      "status": "booked",
      "reminded": null,
      "bookedAt": 1731578400000
    }
  ],
  "clientNotes": {
    "9876543210": "Allergic to ammonia bleach. Prefers herbal treatments."
  }
}
```

### 8. API / Internal Architecture Contracts

```kotlin
// Backup Snapshot DTO (feature/settings/data/backup/BackupSnapshot.kt)
@Serializable
data class BackupSnapshot(
    val version: Int = 2,
    val exportedAt: String,
    val settings: SettingsBackupDto,
    val services: List<ServiceBackupDto>,
    val appointments: List<AppointmentBackupDto>,
    val clientNotes: Map<String, String> // Phone -> Note
)

// Platform Writer Contract (feature/settings/data/backup/BackupWriter.kt)
interface BackupWriter {
    suspend fun writeBackup(targetUri: Uri, snapshot: BackupSnapshot): DataResult<Int> // returns file size in bytes
}

// UseCase Contract (feature/settings/domain/usecase/ExportBackupUseCase.kt)
class ExportBackupUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val serviceRepository: ServiceRepository,
    private val appointmentRepository: AppointmentRepository,
    private val clientNoteRepository: ClientNoteRepository,
    private val backupWriter: BackupWriter,
    private val timeProvider: TimeProvider
) {
    suspend operator fun invoke(targetUri: Uri): DataResult<Unit>
}
```

### 9. Unit Tests
* **`BackupSerializerTest.kt`:**
  * Tests that `BackupSnapshot` serializes to golden schema v2 JSON.
  * Verifies `version == 2` and all required fields are present.
* **`ExportBackupUseCaseTest.kt`:**
  * Mocks repositories, invokes export, asserts `settings.updateLastBackup()` is called on success.

### 10. Integration Tests (Required)
* **`BackupExportIntegrationTest.kt` (Robolectric / ShadowContentResolver):**
  * Seeds database with 5 appointments, 3 services, 2 notes.
  * Invokes `ExportBackupUseCase` writing to a shadow document URI.
  * Reads back the output stream and validates JSON structure.

### 11. Internal Contract / Boundary Tests
* **`BackupFileNameTest.kt`:** Validates default backup naming convention: `UniqueMakeover_backup_YYYY-MM-DD.json`.

### 12. Files and Folder Mapping

```
[CREATED]
app/src/main/java/com/uniquemakeover/admin/feature/settings/data/backup/BackupSnapshot.kt
app/src/main/java/com/uniquemakeover/admin/feature/settings/data/backup/BackupDtoMappers.kt
app/src/main/java/com/uniquemakeover/admin/feature/settings/data/backup/BackupWriter.kt
app/src/main/java/com/uniquemakeover/admin/feature/settings/data/backup/BackupWriterImpl.kt
app/src/main/java/com/uniquemakeover/admin/feature/settings/data/backup/BackupFileName.kt
app/src/main/java/com/uniquemakeover/admin/feature/settings/domain/usecase/ExportBackupUseCase.kt
app/src/main/java/com/uniquemakeover/admin/feature/settings/presentation/sections/DataManagementSection.kt
app/src/test/java/com/uniquemakeover/admin/feature/settings/data/backup/BackupSerializerTest.kt
app/src/test/java/com/uniquemakeover/admin/feature/settings/domain/usecase/ExportBackupUseCaseTest.kt
app/src/androidTest/java/com/uniquemakeover/admin/feature/settings/data/BackupExportIntegrationTest.kt
[MODIFIED]
app/src/main/java/com/uniquemakeover/admin/core/di/AppModule.kt (Binds BackupWriter)
app/src/main/java/com/uniquemakeover/admin/feature/settings/presentation/SettingsScreen.kt (Integrates DataManagementSection)
```

---

## Slice #35: Atomic Backup Import with Schema Validation, Diff Preview & Rollback (US-DAT-02)

### 1. Business Goal
Provide an indestructible restore mechanism that inspects any backup file, verifies schema version compatibility, shows an explicit diff preview summary (counts of services, appointments, and client notes) before touching data, and executes an atomic replacement inside a single SQLite transaction with automatic rollback if any corruption occurs (ADR-0005).

### 2. Actors
* **Salon Owner (Sunita):** Restores parlour records onto a new or replacement tablet.

### 3. User Story
> **As** the salon owner (Sunita),  
> **I want** the app to validate any backup file and show a summary diff before overwriting data,  
> **So that** I never accidentally wipe my data with an invalid or corrupted file.

### 4. Acceptance Criteria
* **AC-DAT-02.1 (Schema Validation & Diff Preview Dialog):**
  * **Given** a valid backup file chosen via `OpenDocument`,
  * **When** parsed by the app,
  * **Then** a confirmation dialog appears displaying backup date and counts:
    *"Backup contains: 12 Services, 48 Appointments, 20 Client Notes. Replace current data?"* with [Cancel] and [Confirm & Overwrite].
* **AC-DAT-02.2 (Single-Transaction Atomic Overwrite - ADR-0005):**
  * **Given** the owner confirms the import dialog,
  * **When** the database transaction executes,
  * **Then** existing tables are cleared and repopulated inside `RoomDatabase.withTransaction`, all UI screens refresh reactively, and a toast confirms *"Data restored successfully"*.
* **AC-DAT-02.3 (Corrupted File Rollback & Safety):**
  * **Given** an invalid, truncated, or incompatible JSON file selected,
  * **When** import is attempted,
  * **Then** an error dialog *"Corrupted or invalid backup file"* appears, the transaction aborts, and all existing salon data remains 100% intact.

### 5. Edge Cases & Technical Resilience
* **EC-DAT-02.1 (Unsupported Future Schema Version):**
  * *Scenario:* Owner attempts to import a backup file with `"version": 99` from a newer release.
  * *System Behavior:* `BackupReader` checks the version header prior to database clearing; rejects with: *"Unsupported backup version (v99). Update the app to restore this file"*. Zero database tables are modified.
* **EC-DAT-02.2 (Selecting Non-JSON Arbitrary File):**
  * *Scenario:* User selects an MP3 or video file renamed to `.json`.
  * *System Behavior:* Parser catches `SerializationException`, safely surfaces error dialog, and preserves active database without modification.
* **EC-DAT-02.3 (Device Power Cut / Battery Death Mid-Import):**
  * *Scenario:* Phone dies halfway through writing imported tables.
  * *System Behavior:* SQLite WAL mode rolls back the uncommitted transaction upon next boot, leaving the pre-import database state completely intact.

### 6. UI/UX Specification
* **Component:** `BackupDiffDialog.kt` inside `DataManagementSection.kt`.
* **Anatomy:**
  * Title: *"Restore Database Backup"* (`titleLarge`).
  * Sub-header: *"Exported on: 12-Nov-2026 at 06:30 PM"*.
  * **Summary Diff Card:**
    * 💇 Services: `12 items`
    * 📅 Appointments: `48 records`
    * 📝 Client Private Notes: `20 profiles`
  * Warning Caption: *"⚠️ Warning: Restoring will overwrite all current data on this device."* (Amber text).
  * Action Buttons: [Cancel] and Red Tonal [Confirm & Overwrite].

### 7. Database Transaction Architecture (ADR-0005)
Executed inside `RoomDatabase.withTransaction`:
```kotlin
// Atomic Database Restoration
database.withTransaction {
    // 1. Clear current tables
    appointmentDao.deleteAll()
    serviceDao.deleteAll()
    clientNoteDao.deleteAll()
    paymentDao.deleteAll()

    // 2. Insert imported snapshot entities
    serviceDao.insertAll(snapshot.services.map { it.toEntity() })
    appointmentDao.insertAll(snapshot.appointments.map { it.toEntity() })
    clientNoteDao.insertAll(snapshot.clientNotes.map { (phone, note) -> 
        ClientNoteEntity(phone, note, System.currentTimeMillis()) 
    })
    settingsDao.updateFromBackup(snapshot.settings.toEntity())
}
```

### 8. API / Internal Architecture Contracts

```kotlin
// Backup Diff Summary (feature/settings/data/backup/BackupDiff.kt)
data class BackupDiff(
    val exportDate: String,
    val serviceCount: Int,
    val appointmentCount: Int,
    val clientNoteCount: Int
)

// Platform Reader Contract (feature/settings/data/backup/BackupReader.kt)
interface BackupReader {
    suspend fun inspectBackup(sourceUri: Uri): DataResult<Pair<BackupSnapshot, BackupDiff>>
}

// UseCase Contract (feature/settings/domain/usecase/ImportBackupUseCase.kt)
class ImportBackupUseCase @Inject constructor(
    private val database: UniqueMakeoverDatabase,
    private val backupReader: BackupReader,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend fun inspect(sourceUri: Uri): DataResult<BackupDiff>
    suspend fun executeImport(sourceUri: Uri): DataResult<Unit>
}
```

### 9. Unit Tests
* **`ImportBackupUseCaseTest.kt`:**
  * Tests schema version guard (rejects `version != 2`).
  * Tests diff calculation accuracy.

### 10. Integration Tests (Required)
* **`BackupRoundTripTest.kt` (In-Memory Room):**
  * Seeds database with known data.
  * Exports database to snapshot.
  * Mutates live database.
  * Imports snapshot; asserts database tables exactly match the pre-mutation state.
* **`BackupRollbackTest.kt` (In-Memory Room):**
  * Forces an insertion exception halfway through import.
  * Asserts transaction rolls back and original records remain intact.

### 11. Internal Contract / Boundary Tests
* **`BackupDiffDialogTest.kt` (Compose UI Test):** Verifies Cancel dismisses without action; Confirm triggers import execution.

### 12. Files and Folder Mapping

```
[CREATED]
app/src/main/java/com/uniquemakeover/admin/feature/settings/data/backup/BackupDiff.kt
app/src/main/java/com/uniquemakeover/admin/feature/settings/data/backup/BackupReader.kt
app/src/main/java/com/uniquemakeover/admin/feature/settings/data/backup/BackupReaderImpl.kt
app/src/main/java/com/uniquemakeover/admin/feature/settings/domain/usecase/ImportBackupUseCase.kt
app/src/main/java/com/uniquemakeover/admin/feature/settings/presentation/components/BackupDiffDialog.kt
app/src/test/java/com/uniquemakeover/admin/feature/settings/domain/usecase/ImportBackupUseCaseTest.kt
app/src/androidTest/java/com/uniquemakeover/admin/feature/settings/data/BackupRoundTripTest.kt
app/src/androidTest/java/com/uniquemakeover/admin/feature/settings/data/BackupRollbackTest.kt
[MODIFIED]
app/src/main/java/com/uniquemakeover/admin/core/di/AppModule.kt (Binds BackupReader)
app/src/main/java/com/uniquemakeover/admin/feature/settings/presentation/sections/DataManagementSection.kt (Integrates Import flow)
```

---

## Slice #36: Overdue Backup Detection & Warning (US-DAT-03)

### 1. Business Goal
Continuously audit the recency of database backups and surface clear amber warning indicators when the last backup is older than 3 days, prompting the salon owner to export a fresh backup.

### 2. Actors
* **Salon Owner (Sunita):** Reminded to safeguard parlour data regularly.

### 3. User Story
> **As** the salon owner (Sunita),  
> **I want** to see how long ago my last backup was taken and receive a reminder if it exceeds 3 days,  
> **So that** I remember to plug in a pen drive or export a backup regularly to prevent data loss.

### 4. Acceptance Criteria
* **AC-DAT-03.1 (Overdue Status Calculation):**
  * **Given** the last backup date in Settings was 5 days ago,
  * **When** viewing Data Management in Settings,
  * **Then** the backup status displays *"Last backup was 5 days ago (Overdue)"* highlighted in amber warning color.
* **AC-DAT-03.2 (Never Backed Up Fresh Install State):**
  * **Given** a fresh install where `settings.lastBackup == null`,
  * **Then** the label displays *"Last backup: Never (Overdue)"*, prompting immediate export.
* **AC-DAT-03.3 (Fresh Backup Up-to-Date State):**
  * **Given** a backup taken today or yesterday,
  * **Then** the label displays *"Last backup: Today ✓"* in calm green text.

### 5. Edge Cases & Technical Resilience
* **EC-DAT-03.1 (Device System Clock Shifted Backwards):** Owner sets device clock backwards to 2020. Day duration evaluates negative. Logic gracefully handles negative values and outputs *"Last backup: Up to date"*, preventing negative-day display glitches.
* **EC-DAT-03.2 (Corrupted Date String):** If `settings.lastBackup` contains an unparseable string, it defaults safely to overdue state.

### 6. UI/UX Specification
* **Component:** Backup Status Badge in `DataManagementSection.kt`.
* **Visual States:**
  * **Up to Date ($\le 3$ days):** Calm Green container with checkmark icon (`ic_check_circle`).
  * **Overdue ($> 3$ days):** Amber Warning container with alert icon (`ic_warning`).
  * **Critical ($> 7$ days):** Red Warning container with pulsing icon.

### 7. Database Schema Reference
Reads `lastBackup TEXT NULL` column from singleton `settings` table.

### 8. API / Internal Architecture Contracts

```kotlin
// Domain Model (feature/settings/domain/model/BackupInfo.kt)
data class BackupInfo(
    val lastBackupDate: String?, // "YYYY-MM-DD"
    val daysSinceLastBackup: Int?,
    val isOverdue: Boolean
)

// UseCase Contract (feature/settings/domain/usecase/GetLastBackupInfoUseCase.kt)
class GetLastBackupInfoUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val timeProvider: TimeProvider
) {
    operator fun invoke(): Flow<BackupInfo>
}
```

### 9. Unit Tests
* **`GetLastBackupInfoUseCaseTest.kt`:**
  * Tests `lastBackup = null` $\rightarrow$ `isOverdue = true`.
  * Tests `now - lastBackup = 2 days` $\rightarrow$ `isOverdue = false`.
  * Tests `now - lastBackup = 4 days` $\rightarrow$ `isOverdue = true`.

### 10. Integration Tests (Not Required)
* Pure Kotlin time difference evaluation; unit tests with `FakeTimeProvider` provide 100% coverage.

### 11. Internal Contract / Boundary Tests
* **`BackupStatusFormattingTest.kt`:** Verifies string formatting across 0, 1, and N days.

### 12. Files and Folder Mapping

```
[CREATED]
app/src/main/java/com/uniquemakeover/admin/feature/settings/domain/model/BackupInfo.kt
app/src/main/java/com/uniquemakeover/admin/feature/settings/domain/usecase/GetLastBackupInfoUseCase.kt
app/src/test/java/com/uniquemakeover/admin/feature/settings/domain/usecase/GetLastBackupInfoUseCaseTest.kt
[MODIFIED]
app/src/main/java/com/uniquemakeover/admin/feature/settings/presentation/sections/DataManagementSection.kt (Displays BackupInfo badge)
app/src/main/java/com/uniquemakeover/admin/feature/settings/presentation/SettingsViewModel.kt (Collects BackupInfo Flow)
```

---

## Slice #37: Factory Reset / Clear All Local Data with Guardrail (US-DAT-04)

### 1. Business Goal
Allow the salon owner to wipe all appointments, client notes, and custom services prior to official salon launch or upon transferring the device, protected by a two-stage warning barrier and mandatory 4-digit admin PIN challenge to prevent accidental catastrophic data erasure.

### 2. Actors
* **Salon Owner (Sunita):** Purges test records before opening day.

### 3. User Story
> **As** the salon owner (Sunita),  
> **I want** to factory-reset the application only after entering my admin PIN and confirming twice,  
> **So that** I can clear test data before official launch without risking accidental erasure during daily use.

### 4. Acceptance Criteria
* **AC-DAT-04.1 (Two-Stage Confirmation Barrier):**
  * **Given** the owner taps "Erase All Salon Data",
  * **When** Stage 1 warning dialog appears,
  * **Then** it explicitly warns: *"All appointments, services, notes, and revenue logs will be permanently wiped."* with [Cancel] and [Proceed to Verification].
* **AC-DAT-04.2 (Admin PIN Challenge):**
  * **Given** Stage 1 confirmed,
  * **When** prompted for admin PIN, entering an incorrect PIN aborts with *"Incorrect PIN. Data not cleared."*.
* **AC-DAT-04.3 (Complete Wipe & Seed Re-Initialization):**
  * **Given** correct PIN entered,
  * **Then** `clearAllTables()` executes in Room, custom image files in `filesDir/` are deleted, default seed services are re-inserted, and app navigates to `Routes.PinLock`.

### 5. Edge Cases & Technical Resilience
* **EC-DAT-04.1 (App Sent to Background During Stage 2 PIN Prompt):** Incoming call received while on PIN challenge. The destructive dialog is immediately dismissed; upon resume, the process must be re-initiated from Stage 1.
* **EC-DAT-04.2 (Purging Orphaned Image Files from Disk):** Custom service photos and logos stored in `filesDir/images/` and `filesDir/branding/` are recursively purged by `ClearAllDataUseCase` to free device storage completely.

### 6. UI/UX Specification
* **Component:** Destructive button at bottom of `DataManagementSection.kt`.
* **Button:** Red Outlined Button with trash icon: `[🗑️ Factory Reset Salon Data]`.
* **Dialogs:**
  * Stage 1: Destructive Material 3 `AlertDialog` with bold red confirm button.
  * Stage 2: PIN Challenge Sheet with numeric keypad.

### 7. Database & Disk Purge Architecture
```kotlin
// Database Wipe & Re-seed Execution
database.withTransaction {
    database.clearAllTables()
    // Re-seed default 7 services
    seedCallback.seedInitialServices(database)
}
// Disk Cleanup
context.filesDir.resolve("images").deleteRecursively()
context.filesDir.resolve("branding").deleteRecursively()
```

### 8. API / Internal Architecture Contracts

```kotlin
// UseCase Contract (feature/settings/domain/usecase/ClearAllDataUseCase.kt)
class ClearAllDataUseCase @Inject constructor(
    private val database: UniqueMakeoverDatabase,
    private val verifyPinUseCase: VerifyPinUseCase,
    private val seedCallback: SeedCallback,
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(adminPin: String): DataResult<Unit>
}
```

### 9. Unit Tests
* **`ClearAllDataUseCaseTest.kt`:**
  * Tests incorrect PIN rejection (tables not cleared).
  * Tests correct PIN execution (calls `clearAllTables()` and re-seeds).

### 10. Integration Tests (Required)
* **`FactoryResetIntegrationTest.kt` (In-Memory Room + FileSystem):**
  * Seeds custom appointments, services, notes, and dummy disk image file.
  * Executes `ClearAllDataUseCase("1234")`.
  * Asserts custom appointments table is empty.
  * Asserts exactly 7 seed services exist.
  * Asserts disk image file was deleted.

### 11. Internal Contract / Boundary Tests
* **Reset Navigation Test:** Verifies factory reset completes by resetting navigation backstack and directing to `Routes.PinLock`.

### 12. Files and Folder Mapping

```
[CREATED]
app/src/main/java/com/uniquemakeover/admin/feature/settings/domain/usecase/ClearAllDataUseCase.kt
app/src/main/java/com/uniquemakeover/admin/feature/settings/presentation/components/FactoryResetDialogs.kt
app/src/test/java/com/uniquemakeover/admin/feature/settings/domain/usecase/ClearAllDataUseCaseTest.kt
app/src/androidTest/java/com/uniquemakeover/admin/feature/settings/data/FactoryResetIntegrationTest.kt
[MODIFIED]
app/src/main/java/com/uniquemakeover/admin/feature/settings/presentation/sections/DataManagementSection.kt (Adds Factory Reset trigger)
app/src/main/java/com/uniquemakeover/admin/feature/settings/presentation/SettingsViewModel.kt (Wires clearAllData)
```

---

## Phase 8 Test Execution Matrix & QA Acceptance Gate

To declare Phase 8 complete and authorize progression to **Phase 9 (Background Automation & Invariants)**, QA must verify that all 14 test suites pass with 100% green status:

```bash
# 1. Run All JVM Unit Tests (< 20 seconds)
./gradlew testDebugUnitTest

# 2. Run All Phase 8 Android Runtime Integration Tests
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=\
com.uniquemakeover.admin.feature.settings.data.BackupExportIntegrationTest,\
com.uniquemakeover.admin.feature.settings.data.BackupRoundTripTest,\
com.uniquemakeover.admin.feature.settings.data.BackupRollbackTest,\
com.uniquemakeover.admin.feature.settings.data.FactoryResetIntegrationTest

# 3. Verify Detekt Architecture Rules (Zero Room in Presentation, Zero android.* in Domain)
./gradlew detekt

# 4. Strict Offline Invariant Check (Zero internet permission in manifest)
grep -rn "INTERNET" app/src/main/AndroidManifest.xml && exit 1
```

---
**End of Phase 8 Vertical Slices Specification — Unique Makeover Admin Console**
