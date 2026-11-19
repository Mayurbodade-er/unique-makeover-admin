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

### 12. Files and Folder Mapping

```
[CREATED]
app/src/main/java/com/uniquemakeover/admin/feature/settings/domain/model/BrandingConfig.kt
app/src/main/java/com/uniquemakeover/admin/feature/settings/data/image/LogoImageStore.kt
app/src/main/java/com/uniquemakeover/admin/feature/settings/data/image/WallpaperImageStore.kt
app/src/main/java/com/uniquemakeover/admin/feature/settings/domain/usecase/UpdateBrandingUseCase.kt
app/src/main/java/com/uniquemakeover/admin/feature/settings/presentation/sections/BrandingSection.kt
app/src/test/java/com/uniquemakeover/admin/feature/settings/domain/usecase/UpdateBrandingUseCaseTest.kt
app/src/test/java/com/uniquemakeover/admin/feature/settings/data/image/LogoImageStoreTest.kt
app/src/androidTest/java/com/uniquemakeover/admin/feature/settings/presentation/BrandingSectionIntegrationTest.kt
[MODIFIED]
app/src/main/java/com/uniquemakeover/admin/feature/settings/presentation/SettingsScreen.kt (Integrates BrandingSection)
app/src/main/java/com/uniquemakeover/admin/core/ui/components/AppScaffold.kt (Binds dynamic shopName & logo)
```

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

### 12. Files and Folder Mapping

```
[CREATED]
app/src/main/java/com/uniquemakeover/admin/feature/dashboard/domain/model/AlertBanner.kt
app/src/main/java/com/uniquemakeover/admin/feature/dashboard/domain/usecase/GetAlertBannersUseCase.kt
app/src/main/java/com/uniquemakeover/admin/feature/dashboard/presentation/components/AlertBannerCard.kt
app/src/test/java/com/uniquemakeover/admin/feature/dashboard/domain/usecase/GetAlertBannersUseCaseTest.kt
app/src/test/java/com/uniquemakeover/admin/feature/dashboard/presentation/AlertBannerDeepLinkTest.kt
[MODIFIED]
app/src/main/java/com/uniquemakeover/admin/feature/dashboard/presentation/DashboardScreen.kt (Renders AlertBanner list)
app/src/main/java/com/uniquemakeover/admin/feature/dashboard/presentation/DashboardViewModel.kt (Combines AlertBanners Flow)
```

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

### 12. Files and Folder Mapping

```
[CREATED]
app/src/main/java/com/uniquemakeover/admin/core/security/LockoutPolicy.kt
app/src/main/java/com/uniquemakeover/admin/feature/auth/domain/model/LockoutState.kt
app/src/main/java/com/uniquemakeover/admin/feature/auth/domain/usecase/GetLockoutStateUseCase.kt
app/src/test/java/com/uniquemakeover/admin/core/security/LockoutPolicyTest.kt
app/src/test/java/com/uniquemakeover/admin/feature/auth/domain/usecase/GetLockoutStateUseCaseTest.kt
app/src/androidTest/java/com/uniquemakeover/admin/feature/auth/presentation/PinLockoutIntegrationTest.kt
[MODIFIED]
app/src/main/java/com/uniquemakeover/admin/core/security/PinLockManager.kt (Integrates LockoutPolicy & DataStore)
app/src/main/java/com/uniquemakeover/admin/feature/auth/presentation/PinLockViewModel.kt (Collects LockoutState)
app/src/main/java/com/uniquemakeover/admin/feature/auth/presentation/PinLockScreen.kt (Renders disabled keypad & timer)
```

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

### 12. Files and Folder Mapping

```
[CREATED]
app/src/main/java/com/uniquemakeover/admin/feature/auth/domain/model/ChangePinResult.kt
app/src/main/java/com/uniquemakeover/admin/feature/auth/domain/usecase/ChangePinUseCase.kt
app/src/main/java/com/uniquemakeover/admin/feature/settings/presentation/sections/AccessControlSection.kt
app/src/test/java/com/uniquemakeover/admin/feature/auth/domain/usecase/ChangePinUseCaseTest.kt
app/src/androidTest/java/com/uniquemakeover/admin/feature/auth/data/ChangePinIntegrationTest.kt
[MODIFIED]
app/src/main/java/com/uniquemakeover/admin/feature/auth/domain/repository/PinRepository.kt (Adds changePin)
app/src/main/java/com/uniquemakeover/admin/feature/auth/data/PinRepositoryImpl.kt (Implements changePin)
app/src/main/java/com/uniquemakeover/admin/feature/settings/presentation/SettingsScreen.kt (Integrates AccessControlSection)
```

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
