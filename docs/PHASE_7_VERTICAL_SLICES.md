# Unique Makeover — Admin Console
# Phase 7 Vertical Slices Specification
### Salon Branding & Alert Intelligence · Native Android · Fully Offline · Clean Architecture
**Document Version:** 1.0  
**Target Roles:** Veteran System Design Android Architect & QA Engineering Lead  
**Source References:** `docs/FRD.md` (§3.1, §4.2, §10.1, §10.2), `docs/ARD.md` (§6.1, §6.2, §6.8, ADR-0003), `docs/USER_STORIES.md`, `vertical-slice-indexmap.md`

---

## Phase 7 Executive Summary

Phase 7 brings **Salon Branding & Alert Intelligence** to *Unique Makeover, Jalgaon Jamod*. With core booking, financial cycles, catalog media, CRM notes, WhatsApp reminders, and UPI QR payments fully operational, Phase 7 personalizes the salon's luxury brand presence, introduces intelligent proactive alert banners across the dashboard, and hardens security access control.

Phase 7 delivers:
1. **Salon Branding Customization:** Tailoring the parlour name, address tagline, circular 200px compressed logo, and optional 900px compressed background wallpaper with automatic WCAG AA contrast scrims.
2. **Actionable Dashboard Intelligence:** Activating all 5 proactive alert banners (WhatsApp unconfigured, UPI missing, backup overdue $> 3$ days, overdue appointments, tomorrow's reminders pending) with instant deep linking.
3. **Brute-Force Rate Limiting & Lockout:** Enforcing a 5-failed-attempt $\rightarrow$ 30-second lockout penalty backed by Android `SystemClock.elapsedRealtime()` and persistent `DataStore<Preferences>`, completely immune to system clock tampering.
4. **In-App PIN Modification:** Allowing the master 4-digit PIN to be changed securely from Settings after verifying the existing PIN.

```
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                     PHASE 7 VERTICAL SLICES                                      │
├──────────────────────────┬──────────────────────────┬──────────────────────────┬─────────────────┤
│ SLICE #30                │ SLICE #31                │ SLICE #32                │ SLICE #33       │
│ Salon Branding & Logo    │ Actionable Alert Banners │ Brute-Force Rate Lockout │ Master PIN Mod  │
│ US-SET-01 (Brand Assets) │ US-DASH-02 (Intelligence)│ US-AUTH-02 (Rate Limit)  │ US-AUTH-03 (Mod)│
└──────────────────────────┴──────────────────────────┴──────────────────────────┴─────────────────┘
```

---

## Slice #30: Salon Branding Setup (Logo Crop & Wallpaper) (US-SET-01)

### 1. Business Goal
Allow the salon owner to personalize the application with the salon's official business name, Jalgaon Jamod boutique address tagline, circular compressed brand logo, and optional atmospheric parlour wallpaper, giving the app a distinct luxury aesthetic.

### 2. Actors
* **Salon Owner (Sunita):** Customizes parlour name, tagline, logo, and background styling.

### 3. User Story
> **As** the salon owner (Sunita),  
> **I want** to configure my salon name, address tagline, circular logo, and background wallpaper,  
> **So that** the app reflects my luxury brand aesthetic (*Unique Makeover, Jalgaon Jamod*).

### 4. Acceptance Criteria
* **AC-SET-01.1 (Header Branding Customization):**
  * **Given** the owner enters Salon Name = `"Unique Makeover Studio"` and Tagline = `"Jalgaon Jamod • Luxury Bridal Salon"`,
  * **When** saving Settings,
  * **Then** the top app bar across all screens immediately updates to display the new salon title and subtitle.
* **AC-SET-01.2 (Logo Acquisition & 200px Circular Crop):**
  * **Given** the owner selects an arbitrary rectangular photo from gallery or camera for the logo,
  * **When** saved by the app,
  * **Then** the image is center-cropped, downscaled to **200x200 px**, compressed to **JPEG @ 80%**, saved to `filesDir/branding/logo.jpg`, and rendered as a circular avatar in the app header and PIN screen.
* **AC-SET-01.3 (Wallpaper Compression & Readability Scrim):**
  * **Given** an optional background wallpaper photo selected by the owner,
  * **When** saved by the app,
  * **Then** the image is scaled to **900 px** longest edge @ 72% JPEG, saved to `filesDir/branding/wallpaper.jpg`, and rendered as the app background with an automatic 40% dark contrast scrim ensuring WCAG AA text readability.

### 5. Edge Cases & Technical Resilience
* **EC-SET-01.1 (Ultra-Long Salon Name / Tagline):** Owner enters 80 characters. UI app bar utilizes `TextOverflow.Ellipsis` with adaptive font scaling, preventing navigation icon clipping.
* **EC-SET-01.2 (Corrupted or Zero-Byte Image File Selected):** Bitmap decoder validates image stream; if unparseable, aborts save, shows toast: *"Invalid image file. Please choose another"*, and preserves prior logo.
* **EC-SET-01.3 (Reset Logo / Wallpaper to Default):** Owner taps "Reset Logo". Stored JPEG is deleted from disk, `settings.logoPath` resets to `null`, and default gold vector emblem (`ic_unique_makeover`) restores.

### 6. UI/UX Specification
* **Screen:** `SettingsScreen.kt` $\rightarrow$ `BrandingSection.kt`.
* **Anatomy:**
  * Salon Name: `OutlinedTextField` (`titleLarge`).
  * Address Tagline: `OutlinedTextField` (`bodyMedium`).
  * **Logo Picker:** Circular preview (80x80 dp) with gold border and pencil edit badge.
  * **Wallpaper Picker:** 16:9 preview card showing sample dark scrim overlay with [Change Wallpaper] and [Remove] actions.
  * Action Button: [Save Branding Details] (`PrimaryButton.kt`).
* **Visual Theme Harmony:** High-contrast luxury palette (Charcoal `#121212`, Gold `#D4AF37`, Warm Cream `#F5F5DC`).

### 7. Database Schema Reference
Updates singleton row (`id = 1`) in `settings` table (from Slice #01):
* Columns modified: `shop TEXT`, `tagline TEXT`, `logoPath TEXT NULL`, `bgPath TEXT NULL`.
* Adopts **ADR-0004**: Database stores relative file paths (`"branding/logo.jpg"`), keeping SQLite lean.

### 8. API / Internal Architecture Contracts

```kotlin
// Domain Model (feature/settings/domain/model/BrandingConfig.kt)
data class BrandingConfig(
    val shopName: String,
    val tagline: String,
    val logoPath: String?,
    val wallpaperPath: String?
)

// Platform Image Stores (feature/settings/data/image/BrandingImageStores.kt)
class LogoImageStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageCompressor: ImageCompressor
) {
    suspend fun saveLogo(sourceUri: Uri): String // 200px circular crop
    fun deleteLogo()
}

class WallpaperImageStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageCompressor: ImageCompressor
) {
    suspend fun saveWallpaper(sourceUri: Uri): String // 900px @ 72%
    fun deleteWallpaper()
}

// UseCase Contract (feature/settings/domain/usecase/UpdateBrandingUseCase.kt)
class UpdateBrandingUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(config: BrandingConfig): DataResult<Unit>
}
```

### 9. Unit Tests
* **`UpdateBrandingUseCaseTest.kt`:** Tests validation (shop name cannot be blank) and repository delegation.
* **`LogoImageStoreTest.kt` (Robolectric):** Verifies 200x200 pixel circular crop dimensions.
* **`WallpaperImageStoreTest.kt` (Robolectric):** Verifies 900 px max dimension and JPEG compression.

### 10. Integration Tests (Required)
* **`BrandingSectionIntegrationTest.kt` (Compose UI + Room In-Memory):**
  * Launches `BrandingSection`.
  * Enters new shop name; clicks Save.
  * Asserts database `settings.shop` updates.
  * Asserts top app bar header immediately reflects the updated shop name.

### 11. Internal Contract / Boundary Tests
* **`BrandingImageFileCleanupTest.kt`:** Verifies calling `deleteLogo()` deletes file from disk.

### 12. Files and Folder Mapping (Updated Post-Implementation)

#### Files Created:
1. `app/src/main/java/com/uniquemakeover/admin/feature/settings/domain/model/BrandingConfig.kt`
   - **Architectural Role:** Domain Model (Settings / Branding)
   - **Why Created:** Encapsulates the immutable branding configuration (`shopName`, `tagline`, `logoPath`, `bgPath`) independent of database entities or Android UI frameworks.
2. `app/src/main/java/com/uniquemakeover/admin/feature/settings/domain/repository/BrandingStorageRepository.kt`
   - **Architectural Role:** Domain Repository Interface
   - **Why Created:** Defines pure Kotlin contracts for persisting and resetting cropped logo and wallpaper images on the local filesystem, preserving Clean Architecture package boundaries.
3. `app/src/main/java/com/uniquemakeover/admin/feature/settings/domain/usecase/GetBrandingConfigUseCase.kt`
   - **Architectural Role:** Domain UseCase
   - **Why Created:** Retrieves the current salon branding configuration from `SettingsRepository`.
4. `app/src/main/java/com/uniquemakeover/admin/feature/settings/domain/usecase/UpdateBrandingUseCase.kt`
   - **Architectural Role:** Domain UseCase
   - **Why Created:** Validates that salon shop name cannot be blank and persists the updated branding model through `SettingsRepository`.
5. `app/src/main/java/com/uniquemakeover/admin/feature/settings/domain/usecase/SettingsUseCases.kt`
   - **Architectural Role:** Domain UseCase Aggregate
   - **Why Created:** Consolidates settings domain use cases into a single injectable aggregate to avoid Detekt `LongParameterList` violation in `SettingsViewModel`.
6. `app/src/main/java/com/uniquemakeover/admin/feature/settings/data/image/LogoImageStore.kt`
   - **Architectural Role:** Data (File Storage / Image Compression)
   - **Why Created:** Saves center-cropped 200x200 px logo images to `context.filesDir/branding/logo.jpg` @ 85% JPEG and cleans up disk files on logo reset.
7. `app/src/main/java/com/uniquemakeover/admin/feature/settings/data/image/WallpaperImageStore.kt`
   - **Architectural Role:** Data (File Storage / Image Compression)
   - **Why Created:** Saves downscaled wallpaper images (max 900 px longest edge) to `context.filesDir/branding/wallpaper.jpg` @ 72% JPEG and handles removal.
8. `app/src/main/java/com/uniquemakeover/admin/feature/settings/data/image/BrandingStorageRepositoryImpl.kt`
   - **Architectural Role:** Data Repository Implementation
   - **Why Created:** Implements `BrandingStorageRepository` by delegating to `LogoImageStore` and `WallpaperImageStore` on `@IoDispatcher`.
9. `app/src/main/java/com/uniquemakeover/admin/feature/settings/presentation/sections/BrandingSection.kt`
   - **Architectural Role:** Presentation (Compose UI Component)
   - **Why Created:** Renders salon branding controls including Salon Name and Tagline text fields, circular avatar preview with edit/reset actions, and 16:9 wallpaper preview with 40% scrim.
10. `app/src/test/java/com/uniquemakeover/admin/feature/settings/domain/usecase/UpdateBrandingUseCaseTest.kt`
    - **Architectural Role:** Unit Test (Domain UseCase)
    - **Why Created:** Verifies blank salon name rejection and successful delegation to `SettingsRepository`.
11. `app/src/test/java/com/uniquemakeover/admin/feature/settings/data/image/LogoImageStoreTest.kt`
    - **Architectural Role:** Unit Test (Data / Image Store)
    - **Why Created:** Verifies 200x200 px square downscaling and file creation/deletion in Robolectric environment.
12. `app/src/test/java/com/uniquemakeover/admin/feature/settings/data/image/WallpaperImageStoreTest.kt`
    - **Architectural Role:** Unit Test (Data / Image Store)
    - **Why Created:** Verifies 900 px max dimension downscaling and file creation/deletion in Robolectric environment.
13. `app/src/test/java/com/uniquemakeover/admin/feature/settings/presentation/sections/BrandingSectionTest.kt`
    - **Architectural Role:** Compose UI Unit Test
    - **Why Created:** Tests `BrandingSection` rendering, text field inputs, and callback emission for branding updates.

#### Files Modified:
1. `app/src/main/java/com/uniquemakeover/admin/core/domain/repository/SettingsRepository.kt`
   - **Why Modified:** Added `getBrandingConfig()` and `updateBrandingConfig()` methods with default implementations to support branding lifecycle.
2. `app/src/main/java/com/uniquemakeover/admin/core/di/RepositoryModule.kt`
   - **Why Modified:** Added Hilt binding for `BrandingStorageRepository`.
3. `app/src/main/java/com/uniquemakeover/admin/core/util/ImageCompressorImpl.kt`
   - **Why Modified:** Implemented `cropToCenterSquare()` helper to guarantee exact 200x200 square crops for boutique logo.
4. `app/src/main/java/com/uniquemakeover/admin/feature/settings/data/mapper/SettingsMapper.kt`
   - **Why Modified:** Added `SettingsEntity.toBrandingConfig()` mapping.
5. `app/src/main/java/com/uniquemakeover/admin/feature/settings/data/repository/SettingsRepositoryImpl.kt`
   - **Why Modified:** Implemented `getBrandingConfig()` and `updateBrandingConfig()` on Room SQLite `settings` table.
6. `app/src/main/java/com/uniquemakeover/admin/feature/settings/presentation/SettingsActions.kt`
   - **Why Modified:** Added `BrandingDraft` data class and `onSaveBranding` action callback.
7. `app/src/main/java/com/uniquemakeover/admin/feature/settings/presentation/SettingsUiState.kt`
   - **Why Modified:** Added `brandingConfig` and `brandingErrorMessage` fields to settings state.
8. `app/src/main/java/com/uniquemakeover/admin/feature/settings/presentation/SettingsViewModel.kt`
   - **Why Modified:** Injected `SettingsUseCases` and implemented `saveBranding()` handling logo/wallpaper file persistence and DB update.
9. `app/src/main/java/com/uniquemakeover/admin/feature/settings/presentation/SettingsScreen.kt`
   - **Why Modified:** Integrated `BrandingSection` at the top of the settings screen.
10. `app/src/main/java/com/uniquemakeover/admin/feature/dashboard/presentation/DashboardUiState.kt`
    - **Why Modified:** Added `shopName`, `tagline`, and `logoPath` to `DashboardUiState`.
11. `app/src/main/java/com/uniquemakeover/admin/feature/dashboard/presentation/DashboardViewModel.kt`
    - **Why Modified:** Loaded dynamic branding configuration into `DashboardUiState`.
12. `app/src/main/java/com/uniquemakeover/admin/feature/dashboard/presentation/DashboardScreen.kt`
    - **Why Modified:** Updated `DashboardTopBar` to display dynamic salon name, tagline, and circular avatar logo.
13. `app/src/test/java/com/uniquemakeover/admin/feature/settings/presentation/SettingsViewModelTest.kt`
    - **Why Modified:** Added unit tests for branding state loading, validation failure, and successful save.
14. `app/src/test/java/com/uniquemakeover/admin/feature/settings/presentation/SettingsScreenTest.kt`
    - **Why Modified:** Updated tests to handle scrolling over `BrandingSection`.
15. `app/src/test/java/com/uniquemakeover/admin/feature/dashboard/presentation/DashboardViewModelTest.kt`
    - **Why Modified:** Updated view model instantiation with `FakeSettingsRepository` and verified branding propagation.
16. `app/src/test/java/com/uniquemakeover/admin/feature/dashboard/presentation/DashboardKpiIntegrationTest.kt`
    - **Why Modified:** Updated test setup to inject fake settings repository for dashboard branding assertions.

---

## Slice #31: Actionable System Alert Banners & Deep Linking (US-DASH-02)

### 1. Business Goal
Proactively surface operational risks and pending daily tasks directly on the dashboard home screen through high-visibility, dismissible alert banners with 1-tap deep links, ensuring the salon owner never forgets to take database backups or send client reminders.

### 2. Actors
* **Salon Owner (Sunita):** Receives actionable alerts on the home screen during daily parlour operations.

### 3. User Story
> **As** the salon owner (Sunita),  
> **I want** prominent alert banners on the dashboard for missing configurations, overdue backups, and pending actions,  
> **So that** I never forget to send client reminders or take database backups.

### 4. Acceptance Criteria
* **AC-DASH-02.1 (5 Distinct Trigger Conditions):**
  * **Trigger 1 (`WaNotConfigured`):** `settings.wa.isBlank()` $\rightarrow$ Banner: *"WhatsApp number not configured. Tap to setup."* $\rightarrow$ Deep-links to Settings (Comms).
  * **Trigger 2 (`UpiNotConfigured`):** `settings.upi.isBlank()` $\rightarrow$ Banner: *"Merchant UPI ID missing. Offline QR payments disabled."* $\rightarrow$ Deep-links to Settings (Comms).
  * **Trigger 3 (`BackupOverdue`):** `lastBackup == null` OR `lastBackup < (today - 3 days)` $\rightarrow$ Banner: *"Backup overdue! Protect client records."* $\rightarrow$ Deep-links to Settings (Data).
  * **Trigger 4 (`OverdueAppointments`):** Any appointment with `date < today` and `status == "booked"` $\rightarrow$ Banner: *"N overdue appointments pending settlement."* $\rightarrow$ Deep-links to Appointments (Overdue).
  * **Trigger 5 (`RemindersPending`):** Tomorrow appointments with `reminded != tomorrow` $\rightarrow$ Banner: *"N client reminders pending for tomorrow."* $\rightarrow$ Deep-links to Reminders screen.
* **AC-DASH-02.2 (Instant Auto-Dismissal on Resolution):**
  * **Given** the WhatsApp alert banner is active,
  * **When** the owner navigates to Settings and saves a valid WhatsApp number,
  * **Then** returning to the Dashboard immediately removes the WhatsApp banner.
* **AC-DASH-02.3 (Session-Scoped Dismissal):**
  * **Given** an alert banner,
  * **When** the owner taps the `(X)` dismiss button,
  * **Then** the banner hides for the remainder of the active app session.

### 5. Edge Cases & Technical Resilience
* **EC-DASH-02.1 (All 5 Banners Triggered Simultaneously on Fresh Install):**
  * *System Behavior:* Banners render in a weighted prioritized column (`BackupOverdue` > `RemindersPending` > `UpiNotConfigured` > `WaNotConfigured` > `OverdueAppointments`); dashboard KPIs below remain scrollable and accessible.
* **EC-DASH-02.2 (Deep Link Target Concurrently Deleted):**
  * *Scenario:* Owner taps "Overdue Appointments" banner, but an appointment was just deleted in the background.
  * *System Behavior:* Destination screen handles empty query gracefully without crashes, showing: *"No overdue appointments found"*.

### 6. UI/UX Specification
* **Component:** `AlertBannerCard.kt` stacked inside `DashboardScreen.kt`.
* **Anatomy:**
  * Container: Rounded Material 3 card with colored left accent strip (Amber for Warnings, Red for Critical Backup Overdue, Blue for Info).
  * Leading Icon: `ic_warning`, `ic_backup`, or `ic_notifications`.
  * Title & Body: `titleSmall` bold headline with `bodySmall` actionable description.
  * Trailing Controls: [Fix Now $\rightarrow$] button and `(X)` dismiss icon.
* **Animation:** Smooth expand/collapse slide transition when banners appear or resolve.

### 7. Database Schema Reference
Pure query evaluations over `settings` and `appointments` tables.

### 8. API / Internal Architecture Contracts

```kotlin
// Domain Model (feature/dashboard/domain/model/AlertBanner.kt)
sealed interface AlertBanner {
    object WaNotConfigured : AlertBanner
    object UpiNotConfigured : AlertBanner
    data class BackupOverdue(val daysSinceLast: Int?) : AlertBanner
    data class OverdueAppointments(val count: Int) : AlertBanner
    data class RemindersPending(val count: Int) : AlertBanner
}

// Navigation Deep Link Target Contract
sealed interface BannerDeepLinkTarget {
    object SettingsComms : BannerDeepLinkTarget
    object SettingsData : BannerDeepLinkTarget
    object AppointmentsOverdue : BannerDeepLinkTarget
    object RemindersTomorrow : BannerDeepLinkTarget
}

// UseCase Contract (feature/dashboard/domain/usecase/GetAlertBannersUseCase.kt)
class GetAlertBannersUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val appointmentRepository: AppointmentRepository,
    private val timeProvider: TimeProvider
) {
    operator fun invoke(): Flow<List<AlertBanner>>
}
```

### 9. Unit Tests
* **`GetAlertBannersUseCaseTest.kt`:**
  * Tests Trigger 1: Emits `WaNotConfigured` when `wa` is blank.
  * Tests Trigger 2: Emits `UpiNotConfigured` when `upi` is blank.
  * Tests Trigger 3: Emits `BackupOverdue` when `lastBackup` $> 3$ days ago.
  * Tests Trigger 4: Emits `OverdueAppointments` when past appointments are "booked".
  * Tests Trigger 5: Emits `RemindersPending` when tomorrow appointments have `reminded == null`.

### 10. Integration Tests (Not Required)
* Pure Kotlin Flow evaluation; covered by ViewModel unit tests and Compose UI tests.

### 11. Internal Contract / Boundary Tests
* **`AlertBannerDeepLinkTest.kt` (Navigation Test):** Verifies tapping each banner navigates to the exact intended `Routes` destination.

### 12. Files and Folder Mapping (Updated Post-Implementation)

#### Files Created:
1. `app/src/main/java/com/uniquemakeover/admin/feature/dashboard/domain/model/AlertBanner.kt`
   - **Architectural Role:** Domain Model (Dashboard / Alerts)
   - **Why Created:** Defines the immutable sealed hierarchy representing the 5 distinct operational alert banners (`BackupOverdueMissing`, `BackupOverdue`, `RemindersPending`, `UpiNotConfigured`, `WaNotConfigured`, `OverdueAppointments`), priorities, and `BannerDeepLinkTarget` contracts independent of any database or UI framework.
2. `app/src/main/java/com/uniquemakeover/admin/feature/dashboard/domain/usecase/GetAlertBannersUseCase.kt`
   - **Architectural Role:** Domain UseCase
   - **Why Created:** Evaluates operational alert conditions by combining reactive `SettingsRepository.getSettings()` and `AppointmentRepository.getAllAppointments()`, checking for missing communications configs, overdue backups (>3 days), overdue booked appointments, and pending tomorrow reminders, sorted in priority order.
3. `app/src/main/java/com/uniquemakeover/admin/feature/dashboard/presentation/components/AlertBannerCard.kt`
   - **Architectural Role:** Presentation (Compose UI Component)
   - **Why Created:** Renders high-visibility alert banner cards adhering to `desing.md` with left accent colored strips (Red for backup overdue, Gold for reminders, Amber for missing comms/overdue appointments), leading type icon, title, description, 1-tap action button, and session dismiss `(X)` button.
4. `app/src/main/java/com/uniquemakeover/admin/feature/dashboard/presentation/DashboardActions.kt`
   - **Architectural Role:** Presentation (Action Contracts)
   - **Why Created:** Consolidates UI callbacks and action delegates for alerts, navigation, and dashboard overview interactions, ensuring ViewModel and Compose screens adhere strictly to Detekt parameter limits.
5. `app/src/test/java/com/uniquemakeover/admin/feature/dashboard/domain/usecase/GetAlertBannersUseCaseTest.kt`
   - **Architectural Role:** Automated JVM Unit Test
   - **Why Created:** Verifies trigger conditions for all 5 alert types, backup threshold edge cases (>3 days vs <=3 days), empty vs non-empty configurations, and priority sorting.
6. `app/src/test/java/com/uniquemakeover/admin/feature/dashboard/presentation/AlertBannerDeepLinkTest.kt`
   - **Architectural Role:** Automated JVM Unit Test
   - **Why Created:** Verifies boundary mapping from alert banners to `BannerDeepLinkTarget` and resolution to `Routes` navigation paths.
7. `app/src/test/java/com/uniquemakeover/admin/feature/dashboard/presentation/components/AlertBannerCardTest.kt`
   - **Architectural Role:** Compose UI Unit Test
   - **Why Created:** Tests `AlertBannerCard` rendering, visual elements, and action/dismiss callbacks in Robolectric environment.
8. `app/src/test/java/com/uniquemakeover/admin/feature/dashboard/presentation/DashboardScreenAlertsTest.kt`
   - **Architectural Role:** Compose UI Unit Test
   - **Why Created:** Verifies that alert banners are displayed prominently above the operational overview section on the dashboard and handle action and dismissal clicks.

#### Files Modified:
1. `app/src/main/java/com/uniquemakeover/admin/feature/dashboard/presentation/DashboardUiState.kt`
   - **Why Modified:** Added `alertBanners: List<AlertBanner>` to state representation.
2. `app/src/main/java/com/uniquemakeover/admin/feature/dashboard/presentation/DashboardViewModel.kt`
   - **Why Modified:** Injected `GetAlertBannersUseCase`, maintained session-scoped `_dismissedBannerIds`, combined visible banners into `uiState`, and provided `dismissBanner(id)` action.
3. `app/src/main/java/com/uniquemakeover/admin/feature/dashboard/presentation/DashboardScreen.kt`
   - **Why Modified:** Added `DashboardAlertActions`, integrated `DashboardAlertBannersSection` into scrollable overview, and wired deep link navigation and dismiss callbacks.
4. `app/src/main/java/com/uniquemakeover/admin/navigation/UniqueMakeoverNavHost.kt`
   - **Why Modified:** Connected dashboard `onNavigateToDeepLink` callback to `Routes.Settings`, `Routes.Appointments`, and `Routes.Reminders`.
5. `app/src/test/java/com/uniquemakeover/admin/feature/dashboard/presentation/DashboardViewModelTest.kt`
   - **Why Modified:** Injected `GetAlertBannersUseCase` in test harness and verified alert banner emission and session-scoped dismissal in `DashboardUiState`.
6. `app/src/test/java/com/uniquemakeover/admin/feature/dashboard/presentation/DashboardKpiIntegrationTest.kt`
   - **Why Modified:** Updated `DashboardViewModel` instantiation with `GetAlertBannersUseCase`.

---

## Slice #32: Brute-Force Protection & Lockout Rate Limiting (US-AUTH-02)

### 1. Business Goal
Protect sensitive client beauty consultation notes, contact records, and parlour financial earnings against unauthorized brute-force PIN guessing by imposing a strict 30-second lockout penalty after 5 consecutive failed PIN attempts.

### 2. Actors
* **Unauthorized Person (Client / Helper / Visitor):** Enters repeated incorrect PIN guesses.
* **Salon Owner (Sunita):** Protected by the rate-limiting security gate.

### 3. User Story
> **As** the salon owner (Sunita),  
> **I want** the PIN input to lock temporarily after repeated failed attempts,  
> **So that** someone cannot brute-force guess my 4-digit PIN by typing combinations consecutively.

### 4. Acceptance Criteria
* **AC-AUTH-02.1 (Soft Lockout Activation after 5 Attempts):**
  * **Given** 4 consecutive failed PIN attempts have occurred,
  * **When** entering the 5th incorrect PIN,
  * **Then** the keypad disables immediately, a 30-second lockout timer starts, and a countdown displays tick-by-tick: *"Too many failed attempts. Try again in 30s"*.
* **AC-AUTH-02.2 (Keypad Restoration on Expiry):**
  * **Given** an active 30-second lockout countdown,
  * **When** the timer reaches 0 seconds,
  * **Then** the keypad re-enables, the failed counter resets to 0, and the owner can enter PIN digits again.
* **AC-AUTH-02.3 (Lockout Persistence Across App Restart):**
  * **Given** the app is locked with 20 seconds remaining,
  * **When** the app process is terminated and reopened after 5 seconds,
  * **Then** the PIN screen remains locked with approximately 15 seconds remaining.
* **AC-AUTH-02.4 (Offline Master Security Recovery Gate):**
  * **Given** the owner has forgotten her custom PIN and is locked out,
  * **When** tapping the secondary action [Forgot PIN / Master Reset],
  * **Then** an offline security challenge prompts for the owner's pre-configured security answer (salted SHA-256 in Keystore); answering correctly resets the failed count and restores access with default PIN (`1234`), eliminating risk of permanent lockout.

### 5. Edge Cases & Technical Resilience
* **EC-AUTH-02.1 (System Wall-Clock Tampering):**
  * *Scenario:* Unauthorized user advances Android system date/time by 1 day in device settings to bypass lockout.
  * *System Behavior:* Lockout policy relies strictly on `SystemClock.elapsedRealtime()` (monotonic uptime since boot) combined with secure boot-cycle counter in `DataStore<Preferences>`. Wall-clock changes have zero effect on the lockout duration.
* **EC-AUTH-02.2 (Device Reboot During Active Lockout):**
  * *Scenario:* Device is rebooted with 20 seconds remaining.
  * *System Behavior:* `PinLockManager` detects boot cycle change via boot timestamp. It enforces the full 30-second penalty upon reboot as a security failsafe.
* **EC-AUTH-02.3 (Touch Event Flooding While Disabled):**
  * *Scenario:* User spams keypad buttons while locked.
  * *System Behavior:* Compose `Modifier.clickable(enabled = false)` drops gestures at the input layer; no queued clicks fire when the timer expires.

### 6. UI/UX Specification
* **Screen:** `PinLockScreen.kt` (Lockout State).
* **Visual Representation:**
  * Numeric Keypad alpha dims to 0.38 (disabled).
  * Error Caption: Pulsating red badge: *"⚠️ Device Locked: 29s remaining"*.
  * Haptic Feedback: Heavy error buzz on trigger.

### 7. Security Storage Architecture & ADR-0003
* **Storage:** `DataStore<Preferences>` stores:
  * `failed_pin_attempts: Int`
  * `lockout_until_elapsed_realtime: Long`
  * `last_boot_timestamp: Long`
* **Pure Policy Function:**
  $$\text{isLockedOut}(\text{failCount}, \text{elapsedRealtime}) \implies \text{failCount} \ge 5 \land \text{elapsedRealtime} < \text{lockoutUntil}$$

### 8. API / Internal Architecture Contracts

```kotlin
// Domain Model (feature/auth/domain/model/LockoutState.kt)
sealed interface LockoutState {
    object Unlocked : LockoutState
    data class Locked(val remainingSeconds: Int) : LockoutState
}

// Pure Policy (core/security/LockoutPolicy.kt)
class LockoutPolicy {
    fun evaluate(
        failCount: Int,
        lastFailElapsedRealtime: Long,
        currentElapsedRealtime: Long
    ): LockoutState {
        if (failCount < 5) return LockoutState.Unlocked
        val elapsedSinceLock = currentElapsedRealtime - lastFailElapsedRealtime
        val remainingMillis = 30_000L - elapsedSinceLock
        return if (remainingMillis > 0) {
            LockoutState.Locked((remainingMillis / 1000).toInt() + 1)
        } else {
            LockoutState.Unlocked
        }
    }
}

// UseCase Contract (feature/auth/domain/usecase/GetLockoutStateUseCase.kt)
class GetLockoutStateUseCase @Inject constructor(
    private val pinRepository: PinRepository
) {
    operator fun invoke(): Flow<LockoutState> = pinRepository.observeLockoutState()
}
```

### 9. Unit Tests
* **`LockoutPolicyTest.kt`:**
  * Tests that 1 to 4 failed attempts return `LockoutState.Unlocked`.
  * Tests that 5 failed attempts trigger `LockoutState.Locked(30)`.
  * Tests countdown progression: 15s elapsed returns `Locked(15)`.
  * Tests expiry: 31s elapsed returns `Unlocked`.

### 10. Integration Tests (Required)
* **`PinLockoutIntegrationTest.kt` (Android Runtime / Compose):**
  * Inputs 5 incorrect PINs sequentially.
  * Asserts UI disables keypad and displays countdown text.
  * Simulates app process recreation; asserts lockout state persists.

### 11. Internal Contract / Boundary Tests
* **`DataStoreLockoutPersistenceTest.kt`:** Verifies atomic write and read of attempt counters in `DataStore`.

### 12. Files and Folder Mapping (Updated Post-Implementation)

#### Files Created:
1. `app/src/main/java/com/uniquemakeover/admin/feature/auth/domain/model/LockoutState.kt`
   - **Architectural Role:** Domain Model (Auth / Access Control)
   - **Why Created:** Defines the immutable domain representation of lockout states (`Unlocked` vs `Locked(remainingSeconds: Int)`), completely decoupled from Android framework and persistence mechanisms.
2. `app/src/main/java/com/uniquemakeover/admin/feature/auth/domain/usecase/GetLockoutStateUseCase.kt`
   - **Architectural Role:** Domain UseCase
   - **Why Created:** Exposes a single-responsibility observable `Flow<LockoutState>` from `PinRepository` to the presentation layer without leaking data or security implementation details.
3. `app/src/main/java/com/uniquemakeover/admin/core/security/LockoutPolicy.kt`
   - **Architectural Role:** Core Security Policy
   - **Why Created:** Implements pure algorithmic lockout rate limiting rules: evaluates failed attempts threshold (>= 5), computes 30-second countdown penalty using monotonic uptime, and provides automatic reboot recovery.
4. `app/src/main/java/com/uniquemakeover/admin/core/di/DataStoreModule.kt`
   - **Architectural Role:** Core Dependency Injection Module
   - **Why Created:** Provides the singleton `DataStore<Preferences>` instance (`auth_lockout_prefs`) via Hilt for persistent, non-volatile storage of brute-force attempt counts and lockout timestamps.
5. `app/src/test/java/com/uniquemakeover/admin/core/security/LockoutPolicyTest.kt`
   - **Architectural Role:** Automated JVM Unit Test
   - **Why Created:** Verifies lockout policy calculation: 0 to 4 failed attempts allow access, 5th attempt triggers 30s lockout, tick progression, expiry at 30s, and monotonic reboot reset penalty.
6. `app/src/test/java/com/uniquemakeover/admin/feature/auth/domain/usecase/GetLockoutStateUseCaseTest.kt`
   - **Architectural Role:** Automated JVM Unit Test
   - **Why Created:** Verifies delegation of `GetLockoutStateUseCase` to `PinRepository.observeLockoutState()` using Turbine flow assertion.
7. `app/src/test/java/com/uniquemakeover/admin/feature/auth/presentation/PinLockoutIntegrationTest.kt`
   - **Architectural Role:** Compose UI Integration Test (Robolectric)
   - **Why Created:** End-to-end integration test verifying that submitting 5 consecutive incorrect PINs transitions the screen into lockout, renders the tick countdown, disables the numeric keypad, and ignores input clicks.

#### Files Modified:
1. `app/src/main/java/com/uniquemakeover/admin/core/util/TimeProvider.kt`
   - **Why Modified:** Added `elapsedRealtime(): Long` to `TimeProvider` contract and implemented using `android.os.SystemClock.elapsedRealtime()` in `DefaultTimeProvider` for monotonic clock measurement immune to wall-clock tampering.
2. `app/src/test/java/com/uniquemakeover/admin/testutil/FakeTimeProvider.kt`
   - **Why Modified:** Added `currentElapsedRealtime: Long` field with default value for deterministic time testing in unit and integration test suites.
3. `app/src/main/java/com/uniquemakeover/admin/feature/auth/domain/repository/PinRepository.kt`
   - **Why Modified:** Added `fun observeLockoutState(): Flow<LockoutState>` to the domain repository interface.
4. `app/src/main/java/com/uniquemakeover/admin/feature/auth/data/PinRepositoryImpl.kt`
   - **Why Modified:** Implemented `observeLockoutState()` delegating to `PinLockManager` and wired `verifyPin` returning `PinResult`.
5. `app/src/main/java/com/uniquemakeover/admin/core/security/PinLockManager.kt`
   - **Why Modified:** Integrated `LockoutPolicy`, `TimeProvider`, and `DataStore<Preferences>` to persist failure attempts, enforce 30s lockout upon 5 failures, return `PinResult.Locked`, and emit live ticker updates in `observeLockoutState()`.
6. `app/src/main/java/com/uniquemakeover/admin/feature/auth/presentation/PinLockUiState.kt`
   - **Why Modified:** Added `lockoutState: LockoutState = LockoutState.Unlocked` and helper property `isLocked: Boolean`.
7. `app/src/main/java/com/uniquemakeover/admin/feature/auth/presentation/PinLockViewModel.kt`
   - **Why Modified:** Injected `GetLockoutStateUseCase`, collected lockout state into `PinLockUiState`, handled `PinResult.Locked` on submission, and dropped keypad/backspace inputs while locked.
8. `app/src/main/java/com/uniquemakeover/admin/feature/auth/presentation/components/PinKeypad.kt`
   - **Why Modified:** Added `enabled: Boolean = true` parameter, applied `.alpha(0.38f)` when disabled, disabled clicks on all digit and action buttons, and extracted `KeypadBottomRow` for clean function decomposition.
9. `app/src/main/java/com/uniquemakeover/admin/feature/auth/presentation/PinLockScreen.kt`
   - **Why Modified:** Passed `enabled = !uiState.isLocked` to `PinKeypad` and triggered `HapticFeedbackType.LongPress` when lockout triggers.
10. `app/src/test/java/com/uniquemakeover/admin/core/security/PinLockManagerTest.kt`
    - **Why Modified:** Added test cases for five consecutive failures triggering lockout, rejection while locked, counter reset on correct PIN, and initial unlocked state.
11. `app/src/test/java/com/uniquemakeover/admin/feature/auth/presentation/PinLockViewModelTest.kt`
    - **Why Modified:** Added unit tests verifying locked state emission on 5th failure, input dropping during lockout, and UI restoration when lockout transitions to unlocked.
12. `app/src/test/java/com/uniquemakeover/admin/feature/auth/presentation/PinLockScreenTest.kt`
    - **Why Modified:** Added Compose test case asserting countdown text is displayed and keypad is disabled when `uiState.isLocked` is true.

---

## Slice #33: Admin PIN Modification with Existing Validation (US-AUTH-03)

### 1. Business Goal
Allow the salon owner to update their master 4-digit PIN from Settings by verifying their existing PIN first, ensuring operational security is maintained and the factory default PIN (`1234`) can be personalized.

### 2. Actors
* **Salon Owner (Sunita):** Updates parlour master PIN.

### 3. User Story
> **As** the salon owner (Sunita),  
> **I want** to change my 4-digit PIN from the Settings menu after verifying my existing PIN,  
> **So that** I can maintain operational security or update the factory default PIN.

### 4. Acceptance Criteria
* **AC-AUTH-03.1 (Successful PIN Update):**
  * **Given** the owner enters the correct current PIN,
  * **When** entering a new 4-digit PIN (e.g. `5678`) and matching confirmation PIN,
  * **Then** the database and Keystore update with the new salted hash, a toast confirms *"PIN changed successfully"*, and subsequent logins require `5678`.
* **AC-AUTH-03.2 (Mismatched Confirmation PIN):**
  * **Given** the owner enters valid current PIN and new PIN `5678`,
  * **When** entering `5679` in the confirmation step,
  * **Then** an inline error displays *"PINs do not match"*, and the PIN is not updated.
* **AC-AUTH-03.3 (Incorrect Current PIN):**
  * **Given** an incorrect current PIN entered in step 1,
  * **Then** an error displays *"Incorrect current PIN"* and the workflow does not proceed.

### 5. Edge Cases & Technical Resilience
* **EC-AUTH-03.1 (New PIN Identical to Current PIN):** Owner enters current PIN as new PIN. UI shows notice: *"New PIN must be different from current PIN"*; Save button remains disabled.
* **EC-AUTH-03.2 (Weak Trivial PIN Warning):** Owner sets `0000`, `1111`, or `1234`. System warns with dialog: *"Weak PIN detected. Are you sure you want to use this PIN?"* with options [Choose Stronger] or [Keep Anyway].
* **EC-AUTH-03.3 (Process Kill Mid-Workflow):** App killed while entering confirmation PIN. Because writes commit only on full verification, the existing PIN remains active in Keystore without corruption.

### 6. UI/UX Specification
* **Screen:** `SettingsScreen.kt` $\rightarrow$ `AccessControlSection.kt`.
* **Two-Step Modal Workflow:**
  * Step 1: *"Enter Current 4-digit PIN"*.
  * Step 2: *"Enter New 4-digit PIN"* followed by *"Confirm New PIN"*.
* **Feedback:** Haptic success vibration upon completion; toast confirmation.

### 7. Keystore & Cryptographic Storage Reference
Updates master salt and hash in `EncryptedSharedPreferences` backed by Android Keystore (Slice #01 & Slice #03):
$$\text{NewHash} = \text{PBKDF2WithHmacSHA256}(\text{NewPIN}, \text{NewSalt}, \text{iter} = 100{,}000)$$
Synchronizes salt marker with `SettingsEntity.adminPinHash`.

### 8. API / Internal Architecture Contracts

```kotlin
// Domain Model (feature/auth/domain/model/ChangePinResult.kt)
sealed interface ChangePinResult {
    object Success : ChangePinResult
    object IncorrectCurrentPin : ChangePinResult
    object PinsDoNotMatch : ChangePinResult
    object SameAsCurrentPin : ChangePinResult
    data class Error(val message: UiText) : ChangePinResult
}

// UseCase Contract (feature/auth/domain/usecase/ChangePinUseCase.kt)
class ChangePinUseCase @Inject constructor(
    private val pinRepository: PinRepository
) {
    suspend operator fun invoke(
        currentPin: String,
        newPin: String,
        confirmPin: String
    ): ChangePinResult
}
```

### 9. Unit Tests
* **`ChangePinUseCaseTest.kt`:**
  * Tests incorrect current PIN rejection.
  * Tests mismatched confirmation rejection (`5678` vs `5679`).
  * Tests identical PIN rejection.
  * Tests success: updates repository and returns `ChangePinResult.Success`.

### 10. Integration Tests (Required)
* **`ChangePinIntegrationTest.kt` (Android Runtime / Keystore):**
  * Changes PIN from default `1234` to `8899`.
  * Simulates app cold restart.
  * Verifies `verifyPin("1234")` fails; verifies `verifyPin("8899")` succeeds.

### 11. Internal Contract / Boundary Tests
* **Keystore Key Re-Encryption Test:** Verifies that a newly generated salt produces a completely distinct PBKDF2 hash.

### 12. Files and Folder Mapping (Updated Post-Implementation)

#### Files Created:
1. `app/src/main/java/com/uniquemakeover/admin/feature/auth/domain/model/ChangePinResult.kt`
   - **Architectural Role:** Domain Model (Auth / Access Control)
   - **Why Created:** Defines sealed domain outcomes of PIN modification attempts (`Success`, `IncorrectCurrentPin`, `PinsDoNotMatch`, `SameAsCurrentPin`, `Error(message)`), keeping domain callers completely decoupled from UI or database logic.
2. `app/src/main/java/com/uniquemakeover/admin/feature/auth/domain/usecase/ChangePinUseCase.kt`
   - **Architectural Role:** Domain UseCase
   - **Why Created:** Implements master security business rules for changing PIN: validates 4-digit numeric constraints, guards against identical new PINs, ensures confirmation matches, verifies current credentials via `PinRepository.verifyPin()`, and detects trivial/weak PIN sequences.
3. `app/src/main/java/com/uniquemakeover/admin/feature/settings/presentation/sections/AccessControlSection.kt`
   - **Architectural Role:** Presentation (Compose UI Component)
   - **Why Created:** Renders the Security & Access Control card inside Salon Settings with status indicator, "Change PIN" action button, two-step credential dialog with inline validation, and weak PIN confirmation alerts adhering to `desing.md`.
4. `app/src/test/java/com/uniquemakeover/admin/feature/auth/domain/usecase/ChangePinUseCaseTest.kt`
   - **Architectural Role:** Automated JVM Unit Test
   - **Why Created:** Verifies domain business logic of `ChangePinUseCase`: format checks, identical PIN rejection, mismatched confirmation, incorrect current PIN rejection, and weak PIN detection.
5. `app/src/test/java/com/uniquemakeover/admin/feature/auth/data/ChangePinIntegrationTest.kt`
   - **Architectural Role:** Robolectric Integration Test
   - **Why Created:** Tests full cryptographic cycle: verifies default PIN `1234`, executes `ChangePinUseCase("1234", "8899", "8899")`, asserts Keystore updates, verifies `1234` fails, verifies `8899` succeeds, and confirms Room `SettingsEntity.adminPinHash` is synchronized.
6. `app/src/test/java/com/uniquemakeover/admin/feature/settings/presentation/sections/AccessControlSectionTest.kt`
   - **Architectural Role:** Compose UI Unit Test
   - **Why Created:** Tests `AccessControlSection` UI interactions: card rendering, opening Change PIN dialog, input validation, and submission callback.

#### Files Modified:
1. `app/src/main/java/com/uniquemakeover/admin/core/security/PinLockManager.kt`
   - **Why Modified:** Updated `setPin(newPin)` to return the computed PBKDF2 hash for Room SQLite synchronization.
2. `app/src/main/java/com/uniquemakeover/admin/feature/auth/data/PinRepositoryImpl.kt`
   - **Why Modified:** Injected `SettingsDao` and updated `changePin(newPin)` to update Keystore and synchronize `adminPinHash` in Room database.
3. `app/src/main/java/com/uniquemakeover/admin/feature/settings/domain/usecase/SettingsUseCases.kt`
   - **Why Modified:** Injected `ChangePinUseCase` into `SettingsUseCases` bundle to expose PIN modification capabilities to `SettingsViewModel`.
4. `app/src/main/java/com/uniquemakeover/admin/feature/settings/presentation/SettingsUiState.kt`
   - **Why Modified:** Added `pinErrorMessage: UiText?` and `isChangePinSuccess: Boolean` to state representation.
5. `app/src/main/java/com/uniquemakeover/admin/feature/settings/presentation/SettingsActions.kt`
   - **Why Modified:** Added `onChangePin: (current: String, new: String, confirm: String) -> Unit` action callback.
6. `app/src/main/java/com/uniquemakeover/admin/feature/settings/presentation/SettingsViewModel.kt`
   - **Why Modified:** Added `changePin(current, new, confirm)` and `handlePinResult(result)` to process PIN modifications and post success or error messages.
7. `app/src/main/java/com/uniquemakeover/admin/feature/settings/presentation/SettingsScreen.kt`
   - **Why Modified:** Integrated `AccessControlSection` into settings content scroll stream and wired `onChangePin` callback to ViewModel.
8. `app/src/test/java/com/uniquemakeover/admin/feature/settings/presentation/SettingsViewModelTest.kt`
   - **Why Modified:** Updated test harness with `ChangePinUseCase` and added unit test cases for successful PIN change and error handling.
9. `app/src/test/java/com/uniquemakeover/admin/feature/auth/presentation/PinLockoutIntegrationTest.kt`
   - **Why Modified:** Updated `PinRepositoryImpl` instantiation with `fakeSettingsDao`.

---

## Phase 7 Test Execution Matrix & QA Acceptance Gate

To declare Phase 7 complete and authorize progression to **Phase 8 (Data Portability & Disaster Recovery)**, QA must verify that all 14 test suites pass with 100% green status:

```bash
# 1. Run All JVM Unit Tests (< 20 seconds)
./gradlew testDebugUnitTest

# 2. Run All Phase 7 Android Runtime Integration Tests
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=\
com.uniquemakeover.admin.feature.settings.presentation.BrandingSectionIntegrationTest,\
com.uniquemakeover.admin.feature.auth.presentation.PinLockoutIntegrationTest,\
com.uniquemakeover.admin.feature.auth.data.ChangePinIntegrationTest

# 3. Verify Detekt Architecture Rules (Zero Room in Presentation, Zero android.* in Domain)
./gradlew detekt

# 4. Strict Offline Invariant Check (Zero internet permission in manifest)
grep -rn "INTERNET" app/src/main/AndroidManifest.xml && exit 1
```

---
**End of Phase 7 Vertical Slices Specification — Unique Makeover Admin Console**
