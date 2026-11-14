# Unique Makeover — Admin Console
# Phase 3 Vertical Slices Specification
### Catalog & Visual Media · Native Android · Fully Offline · Clean Architecture
**Document Version:** 1.0  
**Target Roles:** Veteran System Design Android Architect & QA Engineering Lead  
**Source References:** `docs/FRD.md` (§6.1, §6.2, §11.2), `docs/ARD.md` (§6.4, ADR-0004), `docs/USER_STORIES.md`, `vertical-slice-indexmap.md`

---

## Phase 3 Executive Summary

Phase 3 builds the **Catalog & Visual Media** subsystem of *Unique Makeover, Jalgaon Jamod*. While Phase 0 seeded 7 default beauty packages, Phase 3 provides full autonomy for the salon owner to add custom beauty packages, update service rates, and delete deprecated services—all while maintaining **historical snapshot safety** (so that past appointments retain their original pricing and service descriptions).

Crucially, Phase 3 incorporates local hardware camera and gallery image acquisition with strict **on-device image compression** (longest edge scaled to 320 px, JPEG quality 78%), storing files in app-internal storage (`filesDir/images/`) and keeping the SQLite database lean as specified in **ADR-0004**.

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                                 PHASE 3 VERTICAL SLICES                                 │
├─────────────────────────────────────────────┬───────────────────────────────────────────┤
│ SLICE #14                                   │ SLICE #15                                 │
│ Service Catalog CRUD & Snapshot Safety      │ Reference Photo & Local 320px Compression │
│ US-SVC-02 (Catalog Management)              │ US-SVC-03 (Hardware Camera & Image Engine)│
└─────────────────────────────────────────────┴───────────────────────────────────────────┘
```

---

## Slice #14: Service Catalog CRUD with Appointment Snapshot Safety (US-SVC-02)

### 1. Business Goal
Empower the salon owner to create new treatments, adjust pricing in response to inflation or seasonal demand, and retire discontinued services, while guaranteeing that past financial records and upcoming booked appointments retain their original price snapshots without data drift.

### 2. Actors
* **Salon Owner (Sunita):** Adds new parlour treatments (e.g., "O3+ Bridal D-Tan Facial"), updates prices, or deletes discontinued packages.

### 3. User Story
> **As** the salon owner (Sunita),  
> **I want** to add, edit, and delete services in my catalog without altering or breaking existing appointments,  
> **So that** my historical appointment records reflect the exact price and name charged at the time of booking.

### 4. Acceptance Criteria
* **AC-SVC-02.1 (Add New Service):**
  * **Given** the owner is on the Services screen and taps the `+` FAB,
  * **When** entering Service Name = `"O3+ Diamond Facial"`, Rate = `₹ 1,200`, and tapping Save,
  * **Then** a new `ServiceEntity` is inserted into SQLite, the sheet dismisses, and the new card appears in the catalog list immediately.
* **AC-SVC-02.2 (Price Update Snapshot Safety):**
  * **Given** an existing appointment booked yesterday with `"Gold Facial"` at `₹ 800`,
  * **When** the owner updates `"Gold Facial"` in the Service Catalog to `₹ 1,000`,
  * **Then** the catalog displays `₹ 1,000`, but yesterday's appointment strictly retains its original `₹ 800` rate and billing calculations.
* **AC-SVC-02.3 (Delete Service Historical Retention):**
  * **Given** past and upcoming appointments reference `"Keratin Spa"`,
  * **When** the owner deletes `"Keratin Spa"` from the catalog,
  * **Then** `"Keratin Spa"` disappears from the active catalog and booking picker, but all existing appointments continue to display `"Keratin Spa"` in their summary cards.

### 5. Edge Cases & Technical Resilience
* **EC-SVC-02.1 (Duplicate Service Name Collision):**
  * *Scenario:* Owner attempts to create a service named `"Haircut"` when `"Haircut"` already exists.
  * *System Behavior:* `AddServiceUseCase` checks case-insensitive name uniqueness in SQLite. Rejects save with message: *"A service named 'Haircut' already exists. Use a distinct title (e.g. Haircut - Men)"*.
* **EC-SVC-02.2 (Deleting Service Assigned to Tomorrow's Appointments):**
  * *Scenario:* Owner deletes `"Bridal Spa"` which is scheduled for 2 clients tomorrow.
  * *System Behavior:* System checks `countUpcomingAppointmentsForService(serviceId)`. Displays warning dialog: *"This service is scheduled in 2 upcoming appointments. Existing bookings will retain this service, but it will be unavailable for new bookings. Proceed?"*.
* **EC-SVC-02.3 (Extreme or Zero Pricing Entry):**
  * *Scenario:* Owner enters ₹0 or ₹9,99,999 in the rate field.
  * *System Behavior:* `ValidateServiceDraftUseCase` enforces positive integer boundaries (`₹ 1` to `₹ 1,00,000`). If ₹0 is entered, it prompts: *"Set as complimentary service (₹0)?"*.

### 6. UI/UX Specification
* **Screen:** `ServicesScreen.kt` $\rightarrow$ `ServiceEditorSheet.kt` (Modal Bottom Sheet).
* **Form Anatomy:**
  * Header: Title (*"Add Service"* or *"Edit Service"*).
  * Reference Photo Thumbnail with edit overlay badge.
  * Service Name: Material 3 `OutlinedTextField` (`titleMedium`), auto-capitalized.
  * Service Rate: Number input with leading `₹` currency symbol (`headlineSmall`).
  * Action Buttons: [Cancel] and [Save Service] (`PrimaryButton.kt`).
* **Delete Action:** Located in top-right overflow or secondary button on Edit sheet; triggers `ConfirmDialog.kt` with red destructive container.

### 7. Database Schema & Snapshot Safety Verification
Uses `services` table from Slice #01:
```sql
-- Room Queries in ServiceDao.kt
@Insert(onConflict = OnConflictStrategy.ABORT)
suspend fun insertService(service: ServiceEntity)

@Update
suspend fun updateService(service: ServiceEntity)

@Delete
suspend fun deleteService(service: ServiceEntity)

@Query("SELECT COUNT(*) FROM services WHERE LOWER(name) = LOWER(:name) AND id != :excludeId")
suspend fun countDuplicateNames(name: String, excludeId: String): Int
```
* **Snapshot Safety Proof:** `AppointmentEntity.svc` is a denormalized String snapshot (`"Gold Facial + Threading"`). No foreign key `ON DELETE CASCADE` links `services.id` to `appointments`, guaranteeing zero historical data mutation when services are updated or deleted.

### 8. API / Internal Architecture Contracts

```kotlin
// Domain Model (feature/services/domain/model/ServiceDraft.kt)
data class ServiceDraft(
    val id: String? = null,
    val name: String,
    val rate: Int,
    val imagePath: String? = null
)

// UseCase Contracts
class AddServiceUseCase @Inject constructor(
    private val serviceRepository: ServiceRepository
) {
    suspend operator fun invoke(draft: ServiceDraft): DataResult<String>
}

class UpdateServiceUseCase @Inject constructor(
    private val serviceRepository: ServiceRepository
) {
    suspend operator fun invoke(service: Service): DataResult<Unit>
}

class DeleteServiceUseCase @Inject constructor(
    private val serviceRepository: ServiceRepository,
    private val localImageStore: LocalImageStore
) {
    suspend operator fun invoke(serviceId: String): DataResult<Unit>
}
```

### 9. Unit Tests
* **`AddServiceUseCaseTest.kt`:**
  * Tests duplicate name rejection.
  * Tests rate boundaries (rejects negative numbers, rejects numbers $> 100,000$).
* **`DeleteServiceUseCaseTest.kt`:**
  * Asserts service deletion from repository and verifies deletion of associated local photo file on disk.

### 10. Integration Tests (Not Required)
* Pure Kotlin unit tests and Room DAO tests provide 100% coverage; no external hardware is involved.

### 11. Internal Contract / Boundary Tests
* **`SnapshotSafetyIntegrationTest.kt` (In-Memory Room):**
  * Seeds "Gold Facial" at ₹800.
  * Inserts an appointment referencing "Gold Facial" with rate = 800.
  * Updates "Gold Facial" in `services` table to ₹1,200.
  * Asserts appointment entity in SQLite still has `rate == 800` and `svc == "Gold Facial"`.
  * Deletes "Gold Facial" from `services` table.
  * Asserts appointment entity still exists intact with all details.

### 12. Files and Folder Mapping

```
[CREATED]
app/src/main/java/com/uniquemakeover/admin/feature/services/domain/model/ServiceDraft.kt
app/src/main/java/com/uniquemakeover/admin/feature/services/domain/usecase/AddServiceUseCase.kt
app/src/main/java/com/uniquemakeover/admin/feature/services/domain/usecase/UpdateServiceUseCase.kt
app/src/main/java/com/uniquemakeover/admin/feature/services/domain/usecase/DeleteServiceUseCase.kt
app/src/main/java/com/uniquemakeover/admin/feature/services/presentation/ServiceEditorSheet.kt
app/src/test/java/com/uniquemakeover/admin/feature/services/domain/usecase/AddServiceUseCaseTest.kt
app/src/test/java/com/uniquemakeover/admin/feature/services/domain/usecase/DeleteServiceUseCaseTest.kt
app/src/androidTest/java/com/uniquemakeover/admin/feature/services/data/SnapshotSafetyIntegrationTest.kt
[MODIFIED]
app/src/main/java/com/uniquemakeover/admin/core/database/dao/ServiceDao.kt (Adds insert, update, delete, countDuplicateNames)
app/src/main/java/com/uniquemakeover/admin/feature/services/data/repository/ServiceRepositoryImpl.kt (Implements CRUD operations)
app/src/main/java/com/uniquemakeover/admin/feature/services/presentation/ServicesViewModel.kt (Integrates CRUD operations)
```

---

## Slice #15: Service Reference Photo Acquisition & Local 320px Compression (US-SVC-03)

### 1. Business Goal
Allow the salon owner to take photos of bridal makeup, haircuts, and spa treatments using the device camera or gallery, automatically downscaling and compressing them locally to keep app storage ultra-compact ($< 50\text{ KB}$ per image) without network dependencies or cloud storage costs.

### 2. Actors
* **Salon Owner (Sunita):** Snaps hairstyle photos or picks catalog reference images on the salon device.

### 3. User Story
> **As** the salon owner (Sunita),  
> **I want** to capture or select a hairstyle/makeup photo for each service, with automatic image compression,  
> **So that** the catalog looks visually appealing while keeping app storage lightweight and fast on low-memory devices.

### 4. Acceptance Criteria
* **AC-SVC-03.1 (Camera Image Acquisition & Compression):**
  * **Given** the owner opens the Service Editor and chooses "Take Photo",
  * **When** capturing an image via the system camera,
  * **Then** the image longest edge is downscaled to **320 px**, compressed to **JPEG @ 78% quality**, saved to `filesDir/images/{uuid}.jpg`, and rendered as the service thumbnail.
* **AC-SVC-03.2 (Disk Footprint Optimization):**
  * **Given** an original camera photo of size 5 MB to 12 MB,
  * **When** processed by `ImageCompressor`,
  * **Then** the final stored JPEG file on disk does not exceed **50 KB**.
* **AC-SVC-03.3 (Reset to Fallback Icon):**
  * **Given** a service with an existing custom photo,
  * **When** the owner taps "Reset to Default Photo",
  * **Then** the custom JPEG file is deleted from internal storage, `imagePath` updates to `null`, and the fallback vector icon (`ic_spa`) renders.

### 5. Edge Cases & Technical Resilience
* **EC-SVC-03.1 (Camera Runtime Permission Denied):**
  * *Scenario:* Owner denies camera permission.
  * *System Behavior:* App catches permission rejection without crashing, displays an inline snackbar: *"Camera permission is needed to take photos. You can still select photos from Gallery"*, and opens the Gallery picker.
* **EC-SVC-03.2 (Ultra-High Resolution Image / OOM Guard):**
  * *Scenario:* Owner selects a 108 MP photo from an external SD card.
  * *System Behavior:* `ImageCompressor` reads image bounds with `BitmapFactory.Options.inJustDecodeBounds = true`, computes `inSampleSize` power of 2, sub-samples during decoding, and scales down, preventing `OutOfMemoryError` (OOM).
* **EC-SVC-03.3 (Corrupted or Non-Image File Chosen):**
  * *Scenario:* User selects a non-image file renamed with `.jpg`.
  * *System Behavior:* Bitmap decoder returns `null`; handled gracefully with toast: *"Unable to load selected image"*, leaving existing photo unchanged.
* **EC-SVC-03.4 (Device Storage Full):**
  * *Scenario:* Device has $< 1\text{ MB}$ space remaining.
  * *System Behavior:* Catches `IOException` during file stream flush, rolls back temp file, and alerts: *"Storage full. Free up device memory to save photos"*.

### 6. UI/UX Specification
* **Component:** `PhotoPickerField.kt` inside `ServiceEditorSheet.kt`.
* **Anatomy:**
  * Square preview container (120x120 dp, rounded corners 16 dp).
  * Displays Coil `AsyncImage` with crossfade transition, or gold aesthetic vector icon `ic_spa`.
  * Floating camera badge button in corner.
* **Source Chooser Bottom Sheet:**
  * Option 1: `[📷 Take Photo]` (Camera Intent: `ActivityResultContracts.TakePicture()`).
  * Option 2: `[🖼️ Choose from Gallery]` (Picker Intent: `ActivityResultContracts.PickVisualMedia()`).
  * Option 3: `[🗑️ Remove Photo]` (Only visible if custom photo exists).
* **Feedback:** Smooth crossfade animation upon image loading.

### 7. Storage Architecture & ADR-0004 Adoption
* **File Location:** `context.filesDir/images/{uuid}.jpg` (App-internal storage; no external storage permission required on any Android version).
* **Database Representation:** `ServiceEntity.imagePath` stores relative file path only (e.g. `"images/7b4f...jpg"`), NOT binary BLOBs. This keeps Room SQLite database backups small, prevents database page bloat, and enables fast cursor queries.

### 8. API / Internal Architecture Contracts

```kotlin
// Image Compressor Utility (core/util/ImageCompressor.kt)
interface ImageCompressor {
    suspend fun compressServicePhoto(sourceUri: Uri): File
    suspend fun compressLogo(sourceUri: Uri): File     // 200px circular (Phase 7)
    suspend fun compressWallpaper(sourceUri: Uri): File // 900px @ 72% (Phase 7)
}

// Local Image Store (feature/services/data/image/LocalImageStore.kt)
class LocalImageStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageCompressor: ImageCompressor,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend fun saveServiceImage(sourceUri: Uri): String // Returns relative path
    suspend fun deleteImage(relativePath: String): Boolean
    fun getAbsoluteFile(relativePath: String): File
}

// UseCase Contract (feature/services/domain/usecase/CompressAndStoreImageUseCase.kt)
class CompressAndStoreImageUseCase @Inject constructor(
    private val localImageStore: LocalImageStore
) {
    suspend operator fun invoke(imageUri: Uri): DataResult<String>
}
```

### 9. Unit Tests
* **`ImageCompressorTest.kt` (Robolectric):**
  * Decodes a simulated 4000x3000 test bitmap.
  * Asserts output bitmap dimensions do not exceed 320 px on longest edge.
  * Asserts output file size is $< 50\text{ KB}$.
* **`CompressAndStoreImageUseCaseTest.kt`:**
  * Mocks `LocalImageStore`, verifies returning relative path on success and `DataResult.Error` on `IOException`.

### 10. Integration Tests (Required)
* **`LocalImageStoreIntegrationTest.kt` (Robolectric / AndroidTest):**
  * Passes test URI to `LocalImageStore.saveServiceImage()`.
  * Verifies JPEG file exists on disk in `filesDir/images/`.
  * Calls `LocalImageStore.deleteImage()`; verifies file is removed from disk.
* **`PhotoPickerFieldTest.kt` (Compose UI Test):**
  * Launches `PhotoPickerField`; taps camera badge; asserts source chooser dialog renders options.

### 11. Internal Contract / Boundary Tests
* **`ImageSourceLauncherContractTest.kt`:** Verifies system `PickVisualMedia` and `TakePicture` contract contracts configure correct MIME types (`image/*`).

### 12. Files and Folder Mapping

```
[CREATED]
app/src/main/java/com/uniquemakeover/admin/core/util/ImageCompressor.kt
app/src/main/java/com/uniquemakeover/admin/core/util/ImageCompressorImpl.kt
app/src/main/java/com/uniquemakeover/admin/feature/services/data/image/LocalImageStore.kt
app/src/main/java/com/uniquemakeover/admin/feature/services/domain/usecase/CompressAndStoreImageUseCase.kt
app/src/main/java/com/uniquemakeover/admin/feature/services/presentation/components/PhotoPickerField.kt
app/src/test/java/com/uniquemakeover/admin/core/util/ImageCompressorTest.kt
app/src/test/java/com/uniquemakeover/admin/feature/services/domain/usecase/CompressAndStoreImageUseCaseTest.kt
app/src/androidTest/java/com/uniquemakeover/admin/feature/services/data/LocalImageStoreIntegrationTest.kt
app/src/androidTest/java/com/uniquemakeover/admin/feature/services/presentation/PhotoPickerFieldTest.kt
[MODIFIED]
app/src/main/java/com/uniquemakeover/admin/core/di/AppModule.kt (Binds ImageCompressor implementation)
app/src/main/java/com/uniquemakeover/admin/feature/services/presentation/ServiceEditorSheet.kt (Integrates PhotoPickerField)
```

---

## Phase 3 Test Execution Matrix & QA Acceptance Gate

To declare Phase 3 complete and authorize progression to **Phase 4 (Client CRM & Treatment Notes)**, QA must verify that all 10 test suites pass with 100% green status:

```bash
# 1. Run All JVM Unit Tests (< 20 seconds)
./gradlew testDebugUnitTest

# 2. Run All Phase 3 Android Runtime Integration Tests
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=\
com.uniquemakeover.admin.feature.services.data.SnapshotSafetyIntegrationTest,\
com.uniquemakeover.admin.feature.services.data.LocalImageStoreIntegrationTest,\
com.uniquemakeover.admin.feature.services.presentation.PhotoPickerFieldTest

# 3. Verify Detekt Architecture Rules (No android.* in domain, proper package boundaries)
./gradlew detekt

# 4. Strict Offline Invariant Check
grep -rn "INTERNET" app/src/main/AndroidManifest.xml && exit 1
```

---
**End of Phase 3 Vertical Slices Specification — Unique Makeover Admin Console**
