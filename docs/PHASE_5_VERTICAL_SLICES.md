# Unique Makeover — Admin Console
# Phase 5 Vertical Slices Specification
### Reminders & WhatsApp Bridge · Native Android · Fully Offline · Clean Architecture
**Document Version:** 1.0  
**Target Roles:** Veteran System Design Android Architect & QA Engineering Lead  
**Source References:** `docs/FRD.md` (§7.1–§7.4, §10.4), `docs/ARD.md` (§6.6, §6.8), `docs/USER_STORIES.md`, `vertical-slice-indexmap.md`

---

## Phase 5 Executive Summary

Phase 5 builds the **Reminders & WhatsApp Bridge** subsystem of *Unique Makeover, Jalgaon Jamod*. This feature replaces the tedious, error-prone routine of manually looking up tomorrow's paper diary and typing WhatsApp reminders on a phone keypad.

Phase 5 achieves 1-tap customer reminders **100% offline with zero recurring SMS or server costs** by leveraging the official Android implicit intent mechanism (`Intent.ACTION_VIEW` targeting `https://wa.me/91<phone>?text=<encoded_msg>`). The Android OS delegates network delivery to the locally installed WhatsApp client, enabling the salon app itself to strictly maintain its **zero-network invariant** (no `INTERNET` permission required in `AndroidManifest.xml`).

```
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                     PHASE 5 VERTICAL SLICES                                      │
├──────────────────────────┬──────────────────────────┬──────────────────────────┬─────────────────┤
│ SLICE #21                │ SLICE #22                │ SLICE #23                │ SLICE #24       │
│ Comms Configuration      │ Dynamic Template Engine  │ Tomorrow's Queue Monitor │ 1-Tap WA Intent │
│ US-SET-03 (WA & UPI)     │ US-REM-03 (8 Tokens)     │ US-REM-01 (Tomorrow Date)│ US-REM-02 (wa.me│
└──────────────────────────┴──────────────────────────┴──────────────────────────┴─────────────────┘
```

---

## Slice #21: Communication Settings (WhatsApp & UPI Configuration) (US-SET-03)

### 1. Business Goal
Validate and store the salon's verified WhatsApp business contact number and merchant UPI ID, ensuring that all outgoing customer reminder templates and payment requests contain authentic contact details.

### 2. Actors
* **Salon Owner (Sunita):** Enters the parlour's official contact mobile number and banking UPI handle.

### 3. User Story
> **As** the salon owner (Sunita),  
> **I want** to save my salon's official WhatsApp contact number and merchant UPI ID,  
> **So that** all payment QR codes and outgoing customer reminders use my verified details.

### 4. Acceptance Criteria
* **AC-SET-03.1 (Valid Configuration Persistence):**
  * **Given** the owner is in Settings $\rightarrow$ Communication Settings,
  * **When** entering WhatsApp Phone = `"9822001122"` and Merchant UPI ID = `"uniquemakeover@upi"`,
  * **Then** inputs are validated, persisted to the singleton `settings` row, and a confirmation toast displays *"Communication settings saved"*.
* **AC-SET-03.2 (UPI ID Regex Validation):**
  * **Given** an invalid UPI ID format such as `"uniquemakeover"` (missing `@bank`),
  * **When** attempting to save,
  * **Then** save is blocked, the field outlines in red, and an inline error shows *"Please enter a valid UPI ID (e.g. name@bank)"*.
* **AC-SET-03.3 (Dashboard Missing Alert Dismissal):**
  * **Given** the Dashboard displays alert banners for missing WhatsApp or UPI configuration,
  * **When** valid details are saved in this slice,
  * **Then** returning to the Dashboard immediately dismisses the configuration warning banners.

### 5. Edge Cases & Technical Resilience
* **EC-SET-03.1 (Accidental Spaces in UPI ID or Phone):** User copies and pastes `"  uniquemakeover@okaxis  "`. Domain sanitizer trims leading/trailing whitespace automatically before regex evaluation and persistence.
* **EC-SET-03.2 (Phone Number with Country Code Prefix):** User enters `+91 98220 01122` or `09822001122`. `PhoneSanitizer` cleans input to exact 10 digits (`9822001122`).
* **EC-SET-03.3 (Clearing Previously Configured Values):** If the owner deletes the UPI ID, the system prompts: *"Clearing UPI ID will disable offline QR code payments. Confirm?"*.

### 6. UI/UX Specification
* **Screen:** `SettingsScreen.kt` $\rightarrow$ `CommunicationSection.kt`.
* **Form Anatomy:**
  * Section Header: *"💬 WhatsApp & 💳 UPI Communication"*.
  * WhatsApp Phone: `OutlinedTextField` with leading phone icon and `+91` fixed prefix.
  * Merchant UPI ID: `OutlinedTextField` with leading banking icon and sample placeholder text (`parlour@upi`).
  * Action Button: [Save Communication Settings] (`PrimaryButton.kt`).
* **Validation Feedback:** Instant error outline on unfocus if regex validation fails.

### 7. Database Schema Reference
Updates singleton row (`id = 1`) in `settings` table (from Slice #01):
* Columns modified: `wa TEXT` (10-digit sanitized phone), `upi TEXT` (merchant UPI address).

### 8. API / Internal Architecture Contracts

```kotlin
// Domain Model (core/domain/model/CommunicationConfig.kt)
data class CommunicationConfig(
    val whatsappNumber: String,
    val merchantUpiId: String
)

// UseCase Contract (feature/settings/domain/usecase/UpdateCommunicationUseCase.kt)
class UpdateCommunicationUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val phoneSanitizer: PhoneSanitizer
) {
    suspend operator fun invoke(rawPhone: String, rawUpiId: String): DataResult<Unit>
}
```

### 9. Unit Tests
* **`UpdateCommunicationUseCaseTest.kt`:**
  * Tests UPI ID regex validation (`name@bank` accepted, `name` rejected, `name@` rejected).
  * Tests phone number validation: Rejects non-10-digit strings; normalizes `+91` prefixes.

### 10. Integration Tests (Not Required)
* Covered by Room `SettingsDaoTest.kt` in Slice #04 and pure domain unit tests.

### 11. Internal Contract / Boundary Tests
* **`UpiValidatorTest.kt`:** Tests NPCI-compliant UPI handle regex patterns across major Indian PSP handles (`@upi`, `@okaxis`, `@okhdfcbank`, `@paytm`, `@ybl`).

### 12. Files and Folder Mapping (Updated Post-Implementation)

#### Files Created:
1. `app/src/main/java/com/uniquemakeover/admin/core/domain/model/CommunicationConfig.kt`
   - **Architectural Role:** Domain Model (Core/Settings)
   - **Why Created:** Pure Kotlin data class encapsulating salon communication configuration (`whatsappNumber`, `merchantUpiId`) independent of database entities or UI components.
2. `app/src/main/java/com/uniquemakeover/admin/core/util/UpiValidator.kt`
   - **Architectural Role:** Core Utility / Domain Validator
   - **Why Created:** Pure Kotlin regular expression validator enforcing NPCI-compliant Virtual Payment Address (VPA) structure (`username@bankHandle`) with zero Android dependencies.
3. `app/src/main/java/com/uniquemakeover/admin/feature/settings/domain/usecase/GetCommunicationConfigUseCase.kt`
   - **Architectural Role:** Domain UseCase
   - **Why Created:** Single-responsibility use case that retrieves the persisted communication configuration (`wa`, `upi`) from `SettingsRepository`.
4. `app/src/main/java/com/uniquemakeover/admin/feature/settings/domain/usecase/UpdateCommunicationUseCase.kt`
   - **Architectural Role:** Domain UseCase
   - **Why Created:** Single-responsibility use case that coordinates Indian 10-digit mobile phone sanitization via `PhoneSanitizer` and merchant UPI handle format validation via `UpiValidator`, persisting validated values via `SettingsRepository`.
5. `app/src/main/java/com/uniquemakeover/admin/feature/settings/presentation/sections/CommunicationSection.kt`
   - **Architectural Role:** Presentation (Compose UI Component)
   - **Why Created:** Composable form section in the Settings screen providing luxury-themed input fields for WhatsApp phone number (+91 prefix) and merchant UPI address with inline validation error displays and >=48dp touch targets adhering to `desing.md`.
6. `app/src/test/java/com/uniquemakeover/admin/core/util/UpiValidatorTest.kt`
   - **Architectural Role:** Automated JVM Unit Test
   - **Why Created:** Verifies standard NPCI UPI handle formats across major Indian payment providers (`@upi`, `@okhdfcbank`, `@paytm`, `@ybl`) and rejects invalid patterns.
7. `app/src/test/java/com/uniquemakeover/admin/feature/settings/domain/usecase/UpdateCommunicationUseCaseTest.kt`
   - **Architectural Role:** Automated JVM Unit Test
   - **Why Created:** Validates business rules of `UpdateCommunicationUseCase`: phone number sanitization, invalid UPI rejection, and successful persistence through repository.

#### Files Modified:
1. `app/src/main/java/com/uniquemakeover/admin/core/domain/repository/SettingsRepository.kt`
   - **Why Modified:** Added repository interface contracts `suspend fun getCommunicationConfig(): CommunicationConfig` and `suspend fun updateCommunicationConfig(config: CommunicationConfig): DataResult<Unit>`.
2. `app/src/main/java/com/uniquemakeover/admin/feature/settings/data/repository/SettingsRepositoryImpl.kt`
   - **Why Modified:** Implemented reading and writing `wa` and `upi` columns in SQLite singleton `settings` table via `SettingsDao` on `@IoDispatcher`.
3. `app/src/main/java/com/uniquemakeover/admin/feature/settings/presentation/SettingsUiState.kt`
   - **Why Modified:** Extended `SettingsUiState` with `communicationConfig: CommunicationConfig` and `commsErrorMessage: String?`.
4. `app/src/main/java/com/uniquemakeover/admin/feature/settings/presentation/SettingsViewModel.kt`
   - **Why Modified:** Injected `GetCommunicationConfigUseCase` and `UpdateCommunicationUseCase`; added `saveCommunication(phone, upiId)` event handler updating reactive UI state.
5. `app/src/main/java/com/uniquemakeover/admin/feature/settings/presentation/SettingsScreen.kt`
   - **Why Modified:** Integrated `CommunicationSection` with luxury divider below salon operations in `SettingsContent`.
6. `app/src/test/java/com/uniquemakeover/admin/feature/settings/presentation/SettingsViewModelTest.kt`
   - **Why Modified:** Added ViewModel test cases for loading communication settings and saving valid/invalid communication configs.

---

## Slice #22: Dynamic Reminder Template Token Substitution & Preview (US-REM-03)

### 1. Business Goal
Provide a customizable WhatsApp reminder template editor featuring 8 dynamic placeholder tokens and a real-time preview card, allowing the salon owner to craft friendly, localized messages containing exact appointment details.

### 2. Actors
* **Salon Owner (Sunita):** Customizes the message phrasing and checks how real messages look before sending.

### 3. User Story
> **As** the salon owner (Sunita),  
> **I want** to customize the WhatsApp reminder template using dynamic placeholders and test it with a live preview,  
> **So that** my messages sound friendly, local, and contain exact appointment information.

### 4. Acceptance Criteria
* **AC-REM-03.1 (Complete 8-Token Substitution):**
  * **Given** a template containing all 8 placeholders:
    `"Hello {name}, your appointment at {shop} for {service} is on {date} at {time}. Total: ₹{rate}, Due: ₹{due}. Call {shopnum}."`,
  * **When** formatted for client Priya (Date: 15-Nov, Time: 03:00 PM, Service: Facial, Rate: 800, Due: 300, Shop: Unique Makeover, ShopNum: 9822001122),
  * **Then** zero `{...}` curly braces remain unparsed and the string evaluates cleanly to:
    `"Hello Priya, your appointment at Unique Makeover for Facial is on 15-Nov at 03:00 PM. Total: ₹800, Due: ₹300. Call 9822001122."`.
* **AC-REM-03.2 (Live Preview Card Reactivity):**
  * **Given** the owner is typing in the template editor,
  * **When** altering text or adding placeholder pills,
  * **Then** the Live Preview card beneath the editor updates character-for-character with mock client data.
* **AC-REM-03.3 (Reset to Factory Default Template):**
  * **Given** custom edits in the template text area,
  * **When** tapping the "Reset to Default" button,
  * **Then** the template text field restores the factory default reminder template and persists it to `settings.msg`.

### 5. Edge Cases & Technical Resilience
* **EC-REM-03.1 (Malformed or Typo Placeholders):** Owner types `{client_name}` or `{tim}` or leaves unclosed `{name`. Unknown tokens remain literal strings without crashing; the editor displays a visual warning chip for unrecognized tokens.
* **EC-REM-03.2 (Special Characters, Emojis & Regional Script):** Template includes emojis (✨💇‍♀️), quotes, Marathi script, and ampersands (`&`). `Uri.encode()` percent-encodes characters so WhatsApp does not truncate text at the first `&` delimiter.
* **EC-REM-03.3 (Blank Template Validation):** If all text is deleted, validation blocks save with message *"Template cannot be empty"*, providing an instant [Restore Default] button.

### 6. UI/UX Specification
* **Screen:** `SettingsScreen.kt` $\rightarrow$ `ReminderTemplateSection.kt`.
* **Editor Anatomy:**
  * Multiline `OutlinedTextField` with monospace font option for clear token visualization.
  * **Placeholder Pills Row:** Horizontal scrolling chip list for 1-tap insertion:
    `[+ {name}]`, `[+ {shop}]`, `[+ {service}]`, `[+ {date}]`, `[+ {time}]`, `[+ {rate}]`, `[+ {due}]`, `[+ {shopnum}]`.
  * Secondary Action: [↺ Reset to Default Template].
* **Live Preview Card:** Styled like a WhatsApp green incoming chat bubble with time stamp: *"Live Message Preview (Sample Client)"*.

### 7. Database Schema Reference
Updates `msg TEXT` column in `settings` table (from Slice #01):
* Factory Default Template String:
  ```
  Hello {name}, your appointment at {shop} for {service} is scheduled for tomorrow ({date}) at {time}. Total bill: ₹{rate}, Balance due: ₹{due}. For changes, call {shopnum}. See you soon! ✨
  ```

### 8. API / Internal Architecture Contracts

```kotlin
// Token Constants (feature/reminders/domain/model/TemplateTokens.kt)
object TemplateTokens {
    const val NAME = "{name}"
    const val SHOP = "{shop}"
    const val SERVICE = "{service}"
    const val DATE = "{date}"
    const val TIME = "{time}"
    const val RATE = "{rate}"
    const val DUE = "{due}"
    const val SHOP_NUM = "{shopnum}"
}

// UseCase Contract (feature/reminders/domain/usecase/FormatReminderTemplateUseCase.kt)
class FormatReminderTemplateUseCase @Inject constructor() {
    operator fun invoke(
        template: String,
        appointment: Appointment,
        settings: Settings
    ): String
}
```

### 9. Unit Tests
* **`FormatReminderTemplateUseCaseTest.kt`:**
  * Tests exact replacement of all 8 tokens with mock appointment and settings data.
  * Asserts tolerance of missing or optional fields (e.g. balance due ₹0 formats cleanly as "₹0").
  * Tests preservation of emojis and special characters.

### 10. Integration Tests (Not Required)
* Pure Kotlin string substitution logic; unit tests with Truth assertions provide complete coverage.

### 11. Internal Contract / Boundary Tests
* **`TemplateUrlEncodingTest.kt`:** Verifies that formatted message containing ampersands (`&`), hash symbols (`#`), and spaces passes `Uri.encode()` with 100% fidelity.

### 12. Files and Folder Mapping

```
[CREATED]
app/src/main/java/com/uniquemakeover/admin/feature/reminders/domain/model/TemplateTokens.kt
app/src/main/java/com/uniquemakeover/admin/feature/reminders/domain/usecase/FormatReminderTemplateUseCase.kt
app/src/main/java/com/uniquemakeover/admin/feature/reminders/presentation/components/ReminderPreviewCard.kt
app/src/main/java/com/uniquemakeover/admin/feature/settings/presentation/sections/ReminderTemplateSection.kt
app/src/test/java/com/uniquemakeover/admin/feature/reminders/domain/usecase/FormatReminderTemplateUseCaseTest.kt
app/src/test/java/com/uniquemakeover/admin/feature/reminders/util/TemplateUrlEncodingTest.kt
[MODIFIED]
app/src/main/java/com/uniquemakeover/admin/feature/settings/presentation/SettingsScreen.kt (Integrates ReminderTemplateSection)
```

---

## Slice #23: Tomorrow's Reminder Queue Monitoring (US-REM-01)

### 1. Business Goal
Provide a dedicated operational queue displaying all clients scheduled for tomorrow who have not yet received reminders, giving the salon owner an unambiguous daily checklist of notifications to dispatch.

### 2. Actors
* **Salon Owner (Sunita):** Opens the Reminders tab every evening (around 8:00 PM) to review tomorrow's appointments.

### 3. User Story
> **As** the salon owner (Sunita),  
> **I want** a dedicated screen showing all booked clients for tomorrow with their reminder status,  
> **So that** I can review who is coming in and know who has already been notified.

### 4. Acceptance Criteria
* **AC-REM-01.1 (Tomorrow's Date Scope):**
  * **Given** today is 2026-11-14 with bookings on 2026-11-14 (today), 2026-11-15 (tomorrow), and 2026-11-16 (day after),
  * **When** opening the Reminders screen,
  * **Then** only appointments with `date == '2026-11-15'` and `status == 'booked'` are listed.
* **AC-REM-01.2 (Reminded Status Badge):**
  * **Given** an appointment has `reminded == '2026-11-15'`,
  * **When** viewing the queue,
  * **Then** the card displays a green **"Reminded ✓"** chip, and the "Remind" button is styled as a subtle re-send action.
* **AC-REM-01.3 (Empty Queue Banner):**
  * **Given** zero appointments booked for tomorrow,
  * **Then** an illustration renders with message: *"No appointments scheduled for tomorrow. Enjoy your evening!"*.

### 5. Edge Cases & Technical Resilience
* **EC-REM-01.1 (Late-Night Appointments Booked for Tomorrow):** A walk-in books for tomorrow at 9:00 PM. Room Flow emits automatically; the new booking appears in tomorrow's queue with "Not Sent" status immediately.
* **EC-REM-01.2 (Cancelled Appointments Automatically Filtered):** Client scheduled for tomorrow cancels. Status changes to `"cancel"`; appointment is filtered out of tomorrow's queue instantly so the owner does not message them.
* **EC-REM-01.3 (Midnight Day Rollover):** At 00:00, "tomorrow" increments by 1 calendar day. Injected `TimeProvider` triggers flow recalculation, adjusting the queue to the new tomorrow.

### 6. UI/UX Specification
* **Screen:** `RemindersScreen.kt` (Route: `Routes.Reminders`).
* **Header:** Date Title (e.g. *"Tomorrow — Saturday, 15 Nov 2026"*), Total Scheduled Clients counter pill, and Progress bar (e.g. `2 of 5 Reminded`).
* **Queue Item Anatomy (`ReminderRow.kt`):**
  * Left: Appointment Time chip (`10:30 AM`), Client Name (`titleMedium`), Phone.
  * Center: Booked Services list, Balance Due badge (`Due: ₹ 500`).
  * Right: Prominent 1-tap **[💬 Remind]** luxury button (Emerald green container with WhatsApp icon).
  * Status Chip: `[Not Sent]` (Muted gray) or `[Reminded ✓]` (Emerald green).

### 7. Database Schema & Query Optimization
Uses composite index `(date, time)` on `appointments` table:
```sql
-- Room Query in AppointmentDao.kt
@Query("""
    SELECT * FROM appointments 
    WHERE date = :tomorrowDate 
      AND status = 'booked' 
    ORDER BY time ASC
""")
fun observeTomorrowAppointments(tomorrowDate: String): Flow<List<AppointmentEntity>>
```

### 8. API / Internal Architecture Contracts

```kotlin
// Domain Model (feature/reminders/domain/model/ReminderQueueItem.kt)
data class ReminderQueueItem(
    val appointment: Appointment,
    val isReminded: Boolean
)

// Repository Interface (feature/reminders/domain/repository/RemindersRepository.kt)
interface RemindersRepository {
    fun getTomorrowQueue(): Flow<List<ReminderQueueItem>>
    suspend fun markReminded(appointmentId: String, tomorrowDate: String): DataResult<Unit>
    suspend fun undoMarkReminded(appointmentId: String): DataResult<Unit>
}

// UseCase Contract (feature/reminders/domain/usecase/GetTomorrowQueueUseCase.kt)
class GetTomorrowQueueUseCase @Inject constructor(
    private val remindersRepository: RemindersRepository
) {
    operator fun invoke(): Flow<List<ReminderQueueItem>> = remindersRepository.getTomorrowQueue()
}
```

### 9. Unit Tests
* **`GetTomorrowQueueUseCaseTest.kt`:**
  * Injects `FakeTimeProvider(today = "2026-11-14")`.
  * Verifies query parameter passes `"2026-11-15"`.
  * Tests mapping of `reminded == tomorrowDate` $\rightarrow$ `isReminded = true`.

### 10. Integration Tests (Not Required)
* Covered by Room query tests and ViewModel Turbine tests.

### 11. Internal Contract / Boundary Tests
* **`RemindersViewModelTest.kt`:** Verifies emission of `RemindersUiState(queue = [...], pendingCount = 3)`.

### 12. Files and Folder Mapping

```
[CREATED]
app/src/main/java/com/uniquemakeover/admin/feature/reminders/domain/model/ReminderQueueItem.kt
app/src/main/java/com/uniquemakeover/admin/feature/reminders/domain/repository/RemindersRepository.kt
app/src/main/java/com/uniquemakeover/admin/feature/reminders/data/repository/RemindersRepositoryImpl.kt
app/src/main/java/com/uniquemakeover/admin/feature/reminders/domain/usecase/GetTomorrowQueueUseCase.kt
app/src/main/java/com/uniquemakeover/admin/feature/reminders/presentation/RemindersScreen.kt
app/src/main/java/com/uniquemakeover/admin/feature/reminders/presentation/RemindersViewModel.kt
app/src/main/java/com/uniquemakeover/admin/feature/reminders/presentation/RemindersUiState.kt
app/src/main/java/com/uniquemakeover/admin/feature/reminders/presentation/components/ReminderRow.kt
app/src/test/java/com/uniquemakeover/admin/feature/reminders/domain/usecase/GetTomorrowQueueUseCaseTest.kt
app/src/test/java/com/uniquemakeover/admin/feature/reminders/presentation/RemindersViewModelTest.kt
[MODIFIED]
app/src/main/java/com/uniquemakeover/admin/core/database/dao/AppointmentDao.kt (Adds observeTomorrowAppointments)
app/src/main/java/com/uniquemakeover/admin/core/di/RepositoryModule.kt (Binds RemindersRepository)
```

---

## Slice #24: 1-Tap Offline WhatsApp Dispatch & Optimistic Status (US-REM-02)

### 1. Business Goal
Deliver the operational apex of client communication: tapping a single button formats the personalized reminder message, launches the client's official WhatsApp chat with the text pre-filled, and optimistically updates the reminder status with an instant **Undo** safety net.

### 2. Actors
* **Salon Owner (Sunita):** Dispatches reminders sequentially in seconds.

### 3. User Story
> **As** the salon owner (Sunita),  
> **I want** to tap a single button to open WhatsApp with a pre-filled personalized reminder message,  
> **So that** I don't have to copy-paste numbers or type long messages manually.

### 4. Acceptance Criteria
* **AC-REM-02.1 (Implicit WhatsApp Intent Dispatch):**
  * **Given** an appointment for client "Pooja" with phone `9876543210`,
  * **When** tapping the [💬 Remind] button,
  * **Then** the app fires an implicit `Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/919876543210?text=<encoded_msg>"))`, opening WhatsApp directly to Pooja's chat with the message ready in the input field.
* **AC-REM-02.2 (Optimistic Update & Undo Snackbar):**
  * **Given** WhatsApp is launched,
  * **When** returning to the Unique Makeover app,
  * **Then** Pooja's card displays "Reminded ✓", and a snackbar displays *"Reminder marked as sent [Undo]"*.
* **AC-REM-02.3 (Undo Sent Reminder):**
  * **Given** the Undo snackbar is visible on screen,
  * **When** the owner taps "Undo",
  * **Then** `AppointmentEntity.reminded` resets to `null` in SQLite, and the row reverts to "Not Sent".
* **AC-REM-02.4 (Manual Reset to Unsent via Context Action):**
  * **Given** an appointment has been marked as "Reminded ✓" (e.g. after the 5s snackbar has dismissed),
  * **When** the owner opens the row action menu and taps [Mark as Not Sent],
  * **Then** `AppointmentEntity.reminded` is reset to `null` via `UndoMarkRemindedUseCase`, immediately returning the row to the unsent queue state.

### 5. Edge Cases & Technical Resilience
* **EC-REM-02.1 (WhatsApp Not Installed on Device):**
  * *Scenario:* App runs on a tablet without WhatsApp installed.
  * *System Behavior:* Intent resolution catches `ActivityNotFoundException`. Displays a dialog: *"WhatsApp is not installed on this device. Would you like to copy the reminder message to clipboard?"* with [Copy Message] option.
* **EC-REM-02.2 (Owner Aborts Inside WhatsApp Without Sending):**
  * *Scenario:* Owner opens WhatsApp, but backs out without hitting send.
  * *System Behavior:* Android intent boundaries cannot inspect private third-party chat status. The app updates status optimistically, but provides the 5-second Undo snackbar and an inline context menu option: *"Mark as Not Sent"*.
* **EC-REM-02.3 (Malformed Phone Number in Legacy Data):**
  * *Scenario:* Phone number contains 9 digits.
  * *System Behavior:* `WhatsAppLauncher` verifies `PhoneSanitizer.isValid(phone)`; blocks intent if invalid and flags: *"Invalid phone number. Please edit appointment to correct"*.

### 6. UI/UX Specification
* **Component:** Quick action button on `ReminderRow.kt`.
* **Visual States:**
  * **Unsent:** Prominent Luxury Green button with WhatsApp icon: `[💬 Remind]`.
  * **Reminded:** Muted outline button: `[✓ Reminded • Resend]`.
* **Haptics:** Tactile tick on tap; smooth transition of row background to soft green tint.

### 7. Database Schema & State Updates
Updates `reminded` column in `appointments` table:
```sql
-- Room Query in AppointmentDao.kt
@Query("UPDATE appointments SET reminded = :remindedDate WHERE id = :id")
suspend fun updateRemindedStatus(id: String, remindedDate: String?)
```

### 8. API / Internal Architecture Contracts

```kotlin
// Platform Adapter Interface (feature/reminders/data/platform/WhatsAppLauncher.kt)
interface WhatsAppLauncher {
    fun launchReminder(phone: String, encodedMessage: String): DataResult<Unit>
}

// UseCase Contracts
class LaunchWhatsAppReminderUseCase @Inject constructor(
    private val remindersRepository: RemindersRepository,
    private val settingsRepository: SettingsRepository,
    private val formatReminderTemplateUseCase: FormatReminderTemplateUseCase,
    private val whatsAppLauncher: WhatsAppLauncher
) {
    suspend operator fun invoke(item: ReminderQueueItem, tomorrowDate: String): DataResult<Unit>
}

class MarkRemindedUseCase @Inject constructor(
    private val remindersRepository: RemindersRepository
) {
    suspend operator fun invoke(appointmentId: String, tomorrowDate: String): DataResult<Unit> =
        remindersRepository.markReminded(appointmentId, tomorrowDate)
}

class UndoMarkRemindedUseCase @Inject constructor(
    private val remindersRepository: RemindersRepository
) {
    suspend operator fun invoke(appointmentId: String): DataResult<Unit> =
        remindersRepository.undoMarkReminded(appointmentId)
}
```

### 9. Unit Tests
* **`LaunchWhatsAppReminderUseCaseTest.kt`:**
  * Verifies template formatting, URL encoding, and delegation to `WhatsAppLauncher`.
  * Asserts that `markReminded()` is called upon successful intent launch.
* **`UndoMarkRemindedUseCaseTest.kt`:**
  * Asserts database `reminded` field resets to `null`.

### 10. Integration Tests (Required)
* **`WhatsAppLauncherInstrumentedTest.kt` (Robolectric / ShadowActivity):**
  * Taps [Remind] on an appointment item.
  * Inspects shadow intent created by the Android runtime.
  * Verifies `intent.action == Intent.ACTION_VIEW`.
  * Asserts `intent.data.toString()` starts with `"https://wa.me/919876543210?text="`.
  * Asserts emojis and special characters in query string match RFC 3986 encoding.
* **`RemindersScreenTest.kt` (Compose UI Test):**
  * Verifies tapping [Remind] triggers optimistic state change and displays the Undo snackbar.

### 11. Internal Contract / Boundary Tests
* **`ActivityNotFoundContractTest.kt`:** Simulates missing WhatsApp package; verifies clipboard fallback dialog appears.

### 12. Files and Folder Mapping

```
[CREATED]
app/src/main/java/com/uniquemakeover/admin/feature/reminders/data/platform/WhatsAppLauncher.kt
app/src/main/java/com/uniquemakeover/admin/feature/reminders/data/platform/WhatsAppLauncherImpl.kt
app/src/main/java/com/uniquemakeover/admin/feature/reminders/domain/usecase/LaunchWhatsAppReminderUseCase.kt
app/src/main/java/com/uniquemakeover/admin/feature/reminders/domain/usecase/MarkRemindedUseCase.kt
app/src/main/java/com/uniquemakeover/admin/feature/reminders/domain/usecase/UndoMarkRemindedUseCase.kt
app/src/test/java/com/uniquemakeover/admin/feature/reminders/domain/usecase/LaunchWhatsAppReminderUseCaseTest.kt
app/src/test/java/com/uniquemakeover/admin/feature/reminders/domain/usecase/UndoMarkRemindedUseCaseTest.kt
app/src/androidTest/java/com/uniquemakeover/admin/feature/reminders/data/WhatsAppLauncherInstrumentedTest.kt
app/src/androidTest/java/com/uniquemakeover/admin/feature/reminders/presentation/RemindersScreenTest.kt
[MODIFIED]
app/src/main/java/com/uniquemakeover/admin/core/database/dao/AppointmentDao.kt (Adds updateRemindedStatus)
app/src/main/java/com/uniquemakeover/admin/feature/reminders/presentation/RemindersViewModel.kt (Integrates launch & undo actions)
```

---

## Phase 5 Test Execution Matrix & QA Acceptance Gate

To declare Phase 5 complete and authorize progression to **Phase 6 (Offline UPI QR Engine)**, QA must verify that all 12 test suites pass with 100% green status:

```bash
# 1. Run All JVM Unit Tests (< 20 seconds)
./gradlew testDebugUnitTest

# 2. Run All Phase 5 Android Runtime Integration Tests
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=\
com.uniquemakeover.admin.feature.reminders.data.WhatsAppLauncherInstrumentedTest,\
com.uniquemakeover.admin.feature.reminders.presentation.RemindersScreenTest

# 3. Verify Detekt Architecture Rules (Zero Room in Presentation, Zero android.* in Domain)
./gradlew detekt

# 4. Strict Offline Invariant Check (Zero internet permission in manifest)
grep -rn "INTERNET" app/src/main/AndroidManifest.xml && exit 1
```

---
**End of Phase 5 Vertical Slices Specification — Unique Makeover Admin Console**
