# Unique Makeover — Admin Console
# Phase 6 Vertical Slices Specification
### Offline UPI QR Payment Engine · Native Android · Fully Offline · Clean Architecture
**Document Version:** 1.0  
**Target Roles:** Veteran System Design Android Architect & QA Engineering Lead  
**Source References:** `docs/FRD.md` (§9.1–§9.4), `docs/ARD.md` (§6.7, ADR-0007), `docs/USER_STORIES.md`, `vertical-slice-indexmap.md`

---

## Phase 6 Executive Summary

Phase 6 builds the **Offline UPI QR Payment Engine** of *Unique Makeover, Jalgaon Jamod*. This feature frees the parlour owner from paying monthly rental fees on commercial POS soundboxes or waiting for payment aggregator servers during rural cellular outages.

By embedding **ZXing Core (`com.google.zxing:core`)** directly into the binary (as codified in **ADR-0007**), the app generates crisp, NPCI-standard 512x512 pixel UPI QR codes entirely in memory within $< 100\text{ ms}$ on the device screen—even in **Airplane Mode**. Phase 6 incorporates automatic screen brightness maximization for seamless client camera scanning, 1-tap billing presets (Balance Due, 30% Advance, Full, and Custom), remote payment request dispatches via WhatsApp, MediaStore PNG exports for printing, and native Android share sheet integration via secure `FileProvider`.

```
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                     PHASE 6 VERTICAL SLICES                                      │
├──────────────────────────┬──────────────────────────┬──────────────────────────┬─────────────────┤
│ SLICE #25                │ SLICE #26                │ SLICE #27                │ SLICE #28       │
│ Missing UPI ID Guard     │ Dynamic Offline QR Engine│ 30% Advance & Presets    │ WhatsApp Link   │
│ US-UPI-05 (Safety Guard) │ US-UPI-01 (ZXing Core)   │ US-UPI-02 (Preset Math)  │ US-UPI-03 (URI) │
├──────────────────────────┴──────────────────────────┴──────────────────────────┴─────────────────┤
│ SLICE #29                                                                                        │
│ QR PNG Export & Native Android Share Sheet                                                       │
│ US-UPI-04 (MediaStore & FileProvider)                                                            │
└──────────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## Slice #25: Missing UPI Configuration Blocking Alert (US-UPI-05)

### 1. Business Goal
Protect the parlour's cash flow by preventing the generation or display of corrupt or non-functional QR codes if the owner has not yet configured their merchant UPI ID in Settings, providing a direct 1-tap deep link to configuration.

### 2. Actors
* **Salon Owner (Sunita):** Tries to collect payment before finishing setup.

### 3. User Story
> **As** the salon owner (Sunita),  
> **I want** the app to block QR generation and warn me if my UPI ID is not configured,  
> **So that** I never display an invalid or broken QR code to a paying client.

### 4. Acceptance Criteria
* **AC-UPI-05.1 (Blocking Screen on Empty UPI ID):**
  * **Given** `settings.upi` is blank or unconfigured,
  * **When** opening the UPI QR payment screen,
  * **Then** the QR canvas is suppressed, a prominent blocking banner displays *"Merchant UPI ID Not Configured"*, and a primary action button [Configure UPI in Settings] is shown.
* **AC-UPI-05.2 (Deep Link Navigation):**
  * **Given** the blocking banner is visible,
  * **When** tapping [Configure UPI in Settings],
  * **Then** navigation transitions directly to `Routes.Settings(section = "communication")`.
* **AC-UPI-05.3 (Automatic Resolution):**
  * **Given** the owner saves a valid UPI ID in Settings,
  * **When** navigating back to the UPI QR screen,
  * **Then** the blocking banner disappears and the QR code generates immediately.

### 5. Edge Cases & Technical Resilience
* **EC-UPI-05.1 (UPI Cleared in Background While QR Screen is Open):** Owner switches tabs, clears UPI ID in Settings, and returns. `UpiQrViewModel` observes reactive `SettingsRepository.getSettings()` Flow; UI state switches dynamically from `Content` to `MissingConfiguration`.
* **EC-UPI-05.2 (Malformed or Whitespace-Only UPI ID):** If `settings.upi` contains spaces only (`"   "`), domain validator treats it as blank, safely maintaining the blocking guard.

### 6. UI/UX Specification
* **Component:** `UpiBlockingBanner.kt` inside `UpiQrScreen.kt`.
* **Anatomy:**
  * Centered alert container with Amber Warning Shield icon (`ic_warning`).
  * Headline: *"Merchant UPI ID Required"* (`titleLarge`).
  * Body: *"To generate offline scan-and-pay QR codes for Google Pay, PhonePe, and Paytm, please configure your UPI address."* (`bodyMedium`).
  * Button: [⚙️ Configure UPI ID in Settings] (`PrimaryButton.kt`).

### 7. Database Schema Reference
Reads `upi TEXT` column from singleton `settings` table (from Slice #01).

### 8. API / Internal Architecture Contracts

```kotlin
// UI State Contract (feature/upi/presentation/UpiQrUiState.kt)
sealed interface UpiQrUiState {
    object Loading : UpiQrUiState
    object MissingConfiguration : UpiQrUiState
    data class Content(
        val qrBitmap: Bitmap,
        val upiUri: String,
        val amount: Int,
        val note: String,
        val selectedPreset: PaymentPreset,
        val shopName: String,
        val upiId: String
    ) : UpiQrUiState
    data class Error(val message: UiText) : UpiQrUiState
}
```

### 9. Unit Tests
* **`UpiQrViewModelTest.kt`:** Tests that an empty `settings.upi` string emits `UpiQrUiState.MissingConfiguration`.

### 10. Integration Tests (Not Required)
* Pure Compose UI state and ViewModel test; covered by `UpiQrScreenTest.kt`.

### 11. Internal Contract / Boundary Tests
* **`UpiBlockingBannerTest.kt` (Compose UI Test):** Verifies button click triggers deep link navigation callback.

### 12. Files and Folder Mapping (Updated Post-Implementation)

#### Files Created:
1. `app/src/main/java/com/uniquemakeover/admin/feature/upi/domain/model/PaymentPreset.kt`
   - **Architectural Role:** Domain Model (Core/UPI)
   - **Why Created:** Defines the immutable domain enum `PaymentPreset` (`BALANCE_DUE`, `ADVANCE_30`, `FULL`, `CUSTOM`) representing payment calculation presets independent of any presentation or database frameworks.
2. `app/src/main/java/com/uniquemakeover/admin/feature/upi/presentation/UpiQrUiState.kt`
   - **Architectural Role:** Presentation (UI State Contract)
   - **Why Created:** Models the exhaustive sealed interface states of the offline UPI screen (`Loading`, `MissingConfiguration`, `Content`, `Error`) ensuring compiler-enforced state handling and blocking corrupt QR rendering when UPI ID is absent.
3. `app/src/main/java/com/uniquemakeover/admin/feature/upi/presentation/components/UpiBlockingBanner.kt`
   - **Architectural Role:** Presentation (Compose UI Component)
   - **Why Created:** Renders the luxury-themed amber warning alert banner informing the salon owner when merchant UPI is unconfigured, featuring an ergonomic $\ge 48\text{dp}$ button to deep link to Settings.
4. `app/src/main/java/com/uniquemakeover/admin/feature/upi/presentation/UpiQrScreen.kt`
   - **Architectural Role:** Presentation (Compose Screen & Route)
   - **Why Created:** Implements `UpiQrRoute` and `UpiQrScreen` composables adhering to `desing.md` WarmBackground and BrandPrimary styling, rendering `UpiBlockingBanner` when in `MissingConfiguration` state.
5. `app/src/main/java/com/uniquemakeover/admin/feature/upi/presentation/UpiQrViewModel.kt`
   - **Architectural Role:** Presentation (Architecture Component ViewModel)
   - **Why Created:** Observes reactive `SettingsRepository.getSettings()` Flow, validates merchant UPI ID via `UpiValidator.isValid()`, prunes whitespace, and emits `MissingConfiguration` or `Content` state.
6. `app/src/test/java/com/uniquemakeover/admin/feature/upi/presentation/UpiQrViewModelConfigTest.kt`
   - **Architectural Role:** Automated JVM Unit Test
   - **Why Created:** Validates ViewModel state transitions for null, empty, whitespace-only, invalid handle, and valid UPI configurations, as well as reactive state emission.
7. `app/src/test/java/com/uniquemakeover/admin/feature/upi/presentation/components/UpiBlockingBannerTest.kt`
   - **Architectural Role:** Robolectric Compose UI Component Test
   - **Why Created:** Verifies the blocking banner headline, explanatory description, and deep link button click interaction.

#### Files Modified:
1. `app/src/main/java/com/uniquemakeover/admin/navigation/Routes.kt`
   - **Why Modified:** Added `Routes.UpiQr` destination constant and parameterized route builder `Routes.upiQr(amount, clientName)`.
2. `app/src/main/java/com/uniquemakeover/admin/navigation/UniqueMakeoverNavHost.kt`
   - **Why Modified:** Wired `Routes.UpiQr` destination to `UpiQrRoute` with back navigation and `onConfigureUpiClick` callback routing directly to `Routes.Settings`, decomposing navigation destinations into modular builder extensions to satisfy Detekt function length limit ($\le 60$ lines).

---

## Slice #26: Dynamic Offline UPI QR Generation using ZXing Core (US-UPI-01)

### 1. Business Goal
Render an instant, high-contrast, standardized UPI QR code directly on the salon tablet/phone screen in $< 100\text{ ms}$ with zero network connectivity, enabling clients to scan and pay via PhonePe, Google Pay, or Paytm even during local power or internet outages in Jalgaon Jamod.

### 2. Actors
* **Salon Owner (Sunita):** Presents the device screen to a walk-in client for payment.
* **Client (Pooja):** Scans the on-screen QR code with any Indian banking UPI app.

### 3. User Story
> **As** the salon owner (Sunita),  
> **I want** to generate a standard UPI QR code on my screen without an internet connection,  
> **So that** clients can scan and pay me directly via PhonePe, Google Pay, or Paytm even during internet outages.

### 4. Acceptance Criteria
* **AC-UPI-01.1 (Full Airplane Mode Functionality):**
  * **Given** the Android device has Airplane Mode enabled (no cellular, no WiFi),
  * **When** opening the UPI QR screen for an appointment with billed amount `₹ 850`,
  * **Then** a sharp 512x512 pixel QR code renders on screen in $< 100\text{ ms}$.
* **AC-UPI-01.2 (NPCI UPI URI Format Compliance):**
  * **Given** UPI ID `uniquemakeover@upi`, Shop Name `Unique Makeover`, Amount `₹ 850.00`, and Note `Pooja - Balance Payment`,
  * **When** scanned by a banking camera,
  * **Then** the decoded string matches standard NPCI format:
    `upi://pay?pa=uniquemakeover@upi&pn=Unique%20Makeover&am=850.00&cu=INR&tn=Pooja%20-%20Balance%20Payment`.
* **AC-UPI-01.3 (Automatic Window Brightness Maximization):**
  * **Given** the device screen brightness is at 30%,
  * **When** the UPI QR screen opens,
  * **Then** window brightness temporarily boosts to 100% for scanning ease, and restores to 30% upon exiting the screen.
* **AC-UPI-01.4 (In-Line Counter Payment QR Bridge):**
  * **Given** the owner is in `RecordPaymentDialog` collecting balance due at the counter,
  * **When** selecting `PaymentMode.UPI`,
  * **Then** a prominent `[📱 Show Payment QR]` button appears; tapping it launches the QR display modal pre-filled with the exact balance due amount and appointment reference without requiring the owner to leave the payment flow.

### 5. Edge Cases & Technical Resilience
* **EC-UPI-01.1 (Special Characters in Shop Name or Note):** Salon name contains ampersands or quotes (e.g. `Unique & Chic "Makeover"`). All query parameters pass through strict RFC 3986 percent-encoding (`Uri.encode()`), preventing banking apps from rejecting the URI.
* **EC-UPI-01.2 (Screen Rotation & Brightness Restoration):** If rotated or sent to background during a phone call, window brightness attribute cleanup triggers in `DisposableEffect`, preventing the device from being permanently stuck at 100% brightness.
* **EC-UPI-01.3 (Zero or Negative Amount Block):** If an amount of ₹0 is requested, QR generation is suppressed with prompt: *"Specify payment amount greater than ₹0"*.

### 6. UI/UX Specification
* **Screen:** `UpiQrScreen.kt` (Route: `Routes.UpiQr?appointmentId={id}&amount={amt}`).
* **Anatomy:**
  * **Top Bar:** Back button, Screen title *"UPI Scan & Pay"*.
  * **Center Stage (`QrCanvas.kt`):**
    * High-contrast white card containing the 512px black-on-white QR bitmap with generous quiet zone margin (4 modules).
    * Sub-label: Payee Name, Merchant UPI ID (`uniquemakeover@upi`), and Transaction Note.
    * Requested Amount: Large headline in luxury gold (`₹ 850.00`).
  * **Bottom Controls:** Presets Selector (Slice #27) and Action Buttons ([Share QR], [Send on WhatsApp]).
* **Feedback:** Haptic confirmation tick when QR renders.

### 7. Storage & In-Memory Rendering Architecture
* **Library:** `com.google.zxing:core` (`QRCodeWriter`).
* **Zero Play Services Dependency (ADR-0007):** No Google Play Services barcode SDKs; standalone pure-Kotlin/Java bytecode bundled inside the APK.
* **Color Depth:** `Bitmap.Config.ARGB_8888`, 512x512 pixels with black `#FF000000` data bits and white `#FFFFFFFF` background bits.

### 8. API / Internal Architecture Contracts

```kotlin
// Domain Model (feature/upi/domain/model/UpiRequest.kt)
data class UpiRequest(
    val upiId: String,
    val payeeName: String,
    val amount: Int,
    val transactionNote: String
)

// Platform QR Repository Interface (feature/upi/domain/repository/QrRepository.kt)
interface QrRepository {
    suspend fun generateQrBitmap(payload: String, sizePx: Int = 512): Bitmap
}

// UseCase Contracts
class BuildUpiUriUseCase @Inject constructor() {
    operator fun invoke(request: UpiRequest): String {
        val encodedPayee = Uri.encode(request.payeeName)
        val encodedNote = Uri.encode(request.transactionNote)
        val formattedAmount = String.format(Locale.US, "%.2f", request.amount.toDouble())
        return "upi://pay?pa=${request.upiId}&pn=$encodedPayee&am=$formattedAmount&cu=INR&tn=$encodedNote"
    }
}

class GenerateQrBitmapUseCase @Inject constructor(
    private val qrRepository: QrRepository
) {
    suspend operator fun invoke(payload: String): Bitmap =
        qrRepository.generateQrBitmap(payload, 512)
}
```

### 9. Unit Tests
* **`BuildUpiUriUseCaseTest.kt`:**
  * Tests standard URI formatting.
  * Asserts 2 decimal places (`850` $\rightarrow$ `"850.00"`).
  * Asserts special character and whitespace percent-encoding.
* **`ZxingQrEncoderTest.kt` (Robolectric):**
  * Verifies non-null 512x512 bitmap generated from test string.
  * Verifies center and corner pixel contrast.

### 10. Integration Tests (Required)
* **`UpiQrScreenTest.kt` (Compose UI Test in Airplane Mode):**
  * Injects mock `SettingsRepository` with valid UPI ID.
  * Launches `UpiQrScreen`.
  * Verifies `QrCanvas` node renders image with non-null bitmap.
  * Asserts window brightness attribute equals `1.0f`.

### 11. Internal Contract / Boundary Tests
* **URI Protocol Round-Trip Test:** Verifies that decoding the generated ZXing bitmap with ZXing's `MultiFormatReader` yields the exact original `upi://pay` URI string.

### 12. Files and Folder Mapping (Updated Post-Implementation)

#### Files Created:
1. `app/src/main/java/com/uniquemakeover/admin/feature/upi/domain/model/UpiRequest.kt`
   - **Architectural Role:** Domain Model (Core/UPI)
   - **Why Created:** Pure Kotlin immutable model (`upiId`, `payeeName`, `amount`, `transactionNote`) representing standard payment requests independent of Android or database frameworks.
2. `app/src/main/java/com/uniquemakeover/admin/feature/upi/domain/model/QrCodeMatrix.kt`
   - **Architectural Role:** Domain Model (Core/UPI)
   - **Why Created:** Encapsulates the 2D boolean module matrix of a generated QR code in pure Kotlin, ensuring domain boundaries stay 100% free of `android.graphics.Bitmap`.
3. `app/src/main/java/com/uniquemakeover/admin/feature/upi/domain/repository/QrRepository.kt`
   - **Architectural Role:** Domain Repository Interface
   - **Why Created:** Defines platform QR encoding contract `generateQrMatrix(payload, sizePx)` allowing pure Kotlin domain logic to invoke QR generation.
4. `app/src/main/java/com/uniquemakeover/admin/feature/upi/domain/usecase/BuildUpiUriUseCase.kt`
   - **Architectural Role:** Domain UseCase
   - **Why Created:** Encapsulates NPCI UPI URI generation with strict RFC 3986 percent-encoding and 2-decimal currency formatting (`850` -> `"850.00"`).
5. `app/src/main/java/com/uniquemakeover/admin/feature/upi/domain/usecase/GenerateQrMatrixUseCase.kt`
   - **Architectural Role:** Domain UseCase
   - **Why Created:** Single-responsibility use case delegating QR matrix calculation to `QrRepository` with zero Android framework imports.
6. `app/src/main/java/com/uniquemakeover/admin/feature/upi/data/platform/ZxingQrEncoder.kt`
   - **Architectural Role:** Platform Data Repository Implementation
   - **Why Created:** Implements `QrRepository` using embedded standalone ZXing Core (`QRCodeWriter`), generating 512x512 matrices and ARGB_8888 Bitmaps with error correction in $< 20\text{ ms}$ offline.
7. `app/src/main/java/com/uniquemakeover/admin/feature/upi/presentation/mapper/QrBitmapMapper.kt`
   - **Architectural Role:** Presentation Mapper
   - **Why Created:** Maps pure Kotlin `QrCodeMatrix` to Android `Bitmap.Config.ARGB_8888` for display on the Compose UI canvas.
8. `app/src/main/java/com/uniquemakeover/admin/feature/upi/presentation/components/QrCanvas.kt`
   - **Architectural Role:** Presentation (Compose UI Component)
   - **Why Created:** Renders high-contrast white QR card, luxury gold amount typography, merchant name and UPI handle, and transaction note adhering to `desing.md`.
9. `app/src/test/java/com/uniquemakeover/admin/feature/upi/domain/usecase/BuildUpiUriUseCaseTest.kt`
   - **Architectural Role:** Automated JVM Unit Test
   - **Why Created:** Validates standard NPCI URI format, 2-decimal formatting, special character percent-encoding, and zero-amount omission.
10. `app/src/test/java/com/uniquemakeover/admin/feature/upi/data/ZxingQrEncoderTest.kt`
    - **Architectural Role:** Platform Integration & Boundary Test
    - **Why Created:** Validates 512x512 bitmap creation, module contrast, and round-trip decoding via ZXing `MultiFormatReader`.
11. `app/src/test/java/com/uniquemakeover/admin/feature/upi/presentation/UpiQrScreenTest.kt`
    - **Architectural Role:** Robolectric Compose UI Test
    - **Why Created:** Verifies QR screen rendering, `QrCanvas` elements, and window brightness maximization effect.

#### Files Modified:
1. `app/src/main/java/com/uniquemakeover/admin/core/di/RepositoryModule.kt`
   - **Why Modified:** Bound `QrRepository` to `ZxingQrEncoder` in Hilt DI.
2. `app/src/main/java/com/uniquemakeover/admin/feature/upi/presentation/UpiQrViewModel.kt`
   - **Why Modified:** Injected `BuildUpiUriUseCase` and `GenerateQrMatrixUseCase` to generate and emit offline QR bitmap into `UpiQrUiState.Content`.
3. `app/src/main/java/com/uniquemakeover/admin/feature/upi/presentation/UpiQrScreen.kt`
   - **Why Modified:** Integrated `QrCanvas`, temporary window brightness boost to 100% via `DisposableEffect`, and haptic feedback tick upon QR rendering.
4. `app/src/main/java/com/uniquemakeover/admin/feature/appointments/presentation/payment/RecordPaymentDialog.kt`
   - **Why Modified:** Added `[📱 Show Payment QR]` button when `PaymentMode.UPI` is selected to enable counter-side 1-tap QR launching (AC-UPI-01.4).
5. `app/src/main/java/com/uniquemakeover/admin/feature/dashboard/presentation/DashboardScreen.kt`
   - **Why Modified:** Connected `RecordPaymentDialog.onShowPaymentQr` to `onNavigateToUpiQr` deep link.
6. `app/src/main/java/com/uniquemakeover/admin/navigation/UniqueMakeoverNavHost.kt`
   - **Why Modified:** Passed `onNavigateToUpiQr` callback from `DashboardRoute` to navigation host.
7. `app/src/test/java/com/uniquemakeover/admin/feature/upi/presentation/UpiQrViewModelConfigTest.kt`
   - **Why Modified:** Updated view model test harness with `BuildUpiUriUseCase` and `FakeQrRepository`.

---

## Slice #27: Payment Amount Preset Calculations (US-UPI-02)

### 1. Business Goal
Accelerate counter settlements by offering 1-tap calculation presets for **Balance Due**, **30% Booking Advance** (with clean whole-rupee rounding), **Full Payment**, and **Custom Amount**, dynamically regenerating the QR code instantly on tap.

### 2. Actors
* **Salon Owner (Sunita):** Requests advance deposits or final settlements without manual math.

### 3. User Story
> **As** the salon owner (Sunita),  
> **I want** 1-tap presets for Balance Due, 30% Advance, Full Payment, and Custom Amount on the QR screen,  
> **So that** I don't have to calculate percentages or manually enter amounts for standard payments.

### 4. Acceptance Criteria
* **AC-UPI-02.1 (Preset Amount Calculations):**
  * **Given** an appointment with Total Rate = `₹ 1,500` and Paid = `₹ 0`:
    * Selecting **"Advance (30%)"** sets amount to `₹ 450` and regenerates QR.
    * Selecting **"Full Payment"** sets amount to `₹ 1,500` and regenerates QR.
    * Selecting **"Balance Due"** sets amount to `₹ 1,500` and regenerates QR.
* **AC-UPI-02.2 (Integer Rupee Rounding on Fractional Advances):**
  * **Given** an appointment with Total Rate = `₹ 355`:
    * Selecting **"Advance (30%)"** calculates `355 * 0.30 = 106.50`, rounds cleanly to whole rupee `₹ 107`, and generates QR for `107.00`.
* **AC-UPI-02.3 (Custom Amount Entry):**
  * **Given** the owner selects the "Custom" preset,
  * **When** typing `₹ 700`,
  * **Then** the QR updates immediately to request `₹ 700.00`.

### 5. Edge Cases & Technical Resilience
* **EC-UPI-02.1 (Advance Already Covered):** Client already paid ₹600 on a ₹1,000 service. 30% advance is ₹300. The "Advance (30%)" chip shows as disabled or displays a note: *"Advance already paid"*, keeping "Balance Due" (₹400) selected.
* **EC-UPI-02.2 (Excessive Custom Amount / NPCI Limit):** Owner accidentally types ₹5,00,000. Field validation clamps amount to the maximum standard NPCI peer-to-merchant transaction ceiling (`₹ 1,00,000`), flagging an inline warning.
* **EC-UPI-02.3 (Total Rate is Zero):** For complimentary touch-ups (Rate = 0), all presets disable and QR remains blank.

### 6. UI/UX Specification
* **Component:** `PresetSelector.kt` inside `UpiQrScreen.kt`.
* **Anatomy:**
  * Segmented control row with 4 chips:
    * `[Balance Due]` (Pre-selected default)
    * `[Advance (30%)]`
    * `[Full Payment]`
    * `[Custom ✏️]`
  * If "Custom" is active: An inline animated number field appears with clear button and instant focus.
* **Interactions:** Tapping any preset produces a light haptic tick; the QR canvas crossfades to the newly generated bitmap in $< 50\text{ ms}$.

### 7. Database Schema Reference
Pure computational domain logic over `AppointmentEntity.rate` and `AppointmentEntity.paid`.

### 8. API / Internal Architecture Contracts

```kotlin
// Domain Model (feature/upi/domain/model/PaymentPreset.kt)
sealed interface PaymentPreset {
    object BalanceDue : PaymentPreset
    object Advance30 : PaymentPreset
    object FullPayment : PaymentPreset
    data class Custom(val customAmount: Int) : PaymentPreset
}

// UseCase Contract (feature/upi/domain/usecase/ComputePresetAmountUseCase.kt)
class ComputePresetAmountUseCase @Inject constructor() {
    operator fun invoke(rate: Int, paid: Int, preset: PaymentPreset): Int {
        val balanceDue = (rate - paid).coerceAtLeast(0)
        return when (preset) {
            is PaymentPreset.BalanceDue -> balanceDue
            is PaymentPreset.FullPayment -> rate
            is PaymentPreset.Advance30 -> kotlin.math.round(rate * 0.30).toInt()
            is PaymentPreset.Custom -> preset.customAmount.coerceIn(1, 100_000)
        }
    }
}
```

### 9. Unit Tests
* **`ComputePresetAmountUseCaseTest.kt`:**
  * Tests 30% advance rounding: `355 * 0.30` $\rightarrow$ `107`, `1000 * 0.30` $\rightarrow$ `300`.
  * Tests balance due deduction: Rate 1500, Paid 500 $\rightarrow$ 1000.
  * Tests custom amount boundary clamping (1 to 100,000).

### 10. Integration Tests (Not Required)
* Pure Kotlin mathematical logic; unit tests with Truth assertions provide 100% test coverage.

### 11. Internal Contract / Boundary Tests
* **`PresetSelectorTest.kt` (Compose UI Test):** Verifies tapping "Advance (30%)" triggers ViewModel callback with calculated figure.

### 12. Files and Folder Mapping (Updated Post-Implementation)

#### Files Created:
1. `app/src/main/java/com/uniquemakeover/admin/feature/upi/domain/usecase/ComputePresetAmountUseCase.kt`
   - **Architectural Role:** Domain UseCase
   - **Why Created:** Implements pure Kotlin business rules for calculating preset settlement amounts: 30% advance with whole-rupee rounding (`(rate * 0.30).roundToInt()`), balance due deduction (`(rate - paid).coerceAtLeast(0)`), full rate, and custom amount boundary clamping (`1` to `100,000`).
2. `app/src/main/java/com/uniquemakeover/admin/feature/upi/presentation/components/PresetSelector.kt`
   - **Architectural Role:** Presentation (Compose UI Component)
   - **Why Created:** Renders ergonomic $\ge 48\text{dp}$ touch chip selector for "Balance Due", "Advance (30%)", "Full Payment", and "Custom ✏️", dynamically displaying animated custom numeric input field with clear action button when custom mode is chosen.
3. `app/src/test/java/com/uniquemakeover/admin/feature/upi/domain/usecase/ComputePresetAmountUseCaseTest.kt`
   - **Architectural Role:** Automated JVM Unit Test
   - **Why Created:** Asserts whole-rupee rounding on fractional advances (`355 * 0.30` -> `107`), balance due deductions with overpayment protection, and NPCI boundary clamping.
4. `app/src/test/java/com/uniquemakeover/admin/feature/upi/presentation/components/PresetSelectorTest.kt`
   - **Architectural Role:** Robolectric Compose UI Component Test
   - **Why Created:** Tests rendering of all preset chips, chip selection callback triggers, disabled advance state when advance is already covered, and custom text input interactions.

#### Files Modified:
1. `app/src/main/java/com/uniquemakeover/admin/feature/upi/domain/model/PaymentPreset.kt`
   - **Why Modified:** Converted from simple enum to sealed interface hierarchy (`BalanceDue`, `Advance30`, `FullPayment`, `Custom(customAmount)`) to model parametric custom amounts cleanly in domain.
2. `app/src/main/java/com/uniquemakeover/admin/feature/upi/presentation/UpiQrUiState.kt`
   - **Why Modified:** Added `customAmountInput` and `isAdvanceDisabled` fields to `UpiQrUiState.Content` to drive reactive UI states.
3. `app/src/main/java/com/uniquemakeover/admin/feature/upi/presentation/UpiQrViewModel.kt`
   - **Why Modified:** Injected `ComputePresetAmountUseCase`, evaluated advance covered condition (`paid >= 30% rate`), handled `onPresetSelected` and `onCustomAmountChanged`, and dynamically regenerated the UPI URI and QR bitmap.
4. `app/src/main/java/com/uniquemakeover/admin/feature/upi/presentation/UpiQrScreen.kt`
   - **Why Modified:** Integrated `PresetSelector` into content scroll container, wiring preset and custom amount events to ViewModel.
5. `app/src/main/java/com/uniquemakeover/admin/navigation/Routes.kt`
   - **Why Modified:** Updated `Routes.UpiQr` route pattern and `upiQr(amount, rate, paid, clientName)` helper to forward rate and paid figures for accurate preset math.
6. `app/src/main/java/com/uniquemakeover/admin/navigation/UniqueMakeoverNavHost.kt`
   - **Why Modified:** Registered `rate` and `paid` navigation arguments in `upiNavDestination` and forwarded them from `DashboardRoute`.
7. `app/src/main/java/com/uniquemakeover/admin/feature/appointments/presentation/payment/RecordPaymentDialog.kt`
   - **Why Modified:** Forwarded `appointment.rate` and `appointment.paid` into `onShowPaymentQr` callback.
8. `app/src/main/java/com/uniquemakeover/admin/feature/dashboard/presentation/DashboardScreen.kt`
   - **Why Modified:** Passed `(amount, rate, paid, name)` from payment modal to `onNavigateToUpiQr`.
9. `app/src/test/java/com/uniquemakeover/admin/feature/upi/presentation/UpiQrViewModelConfigTest.kt`
   - **Why Modified:** Added test cases verifying preset selection amount updates, custom amount typing, and advance covered state.

---

## Slice #28: WhatsApp Direct UPI Payment Link Dispatch (US-UPI-03)

### 1. Business Goal
Allow the salon owner to send a personalized payment request with a clickable `upi://pay` link directly to a client's WhatsApp chat, enabling clients booking from home to pay their advance deposit easily on their mobile phone.

### 2. Actors
* **Salon Owner (Sunita):** Dispatches payment requests to clients booking remotely.

### 3. User Story
> **As** the salon owner (Sunita),  
> **I want** to send a clickable UPI payment request link to a client via WhatsApp,  
> **So that** clients booking from home can pay their advance deposit easily on their phone.

### 4. Acceptance Criteria
* **AC-UPI-03.1 (Clickable Link Composition):**
  * **Given** client "Neha" requesting ₹300 advance for Bridal Booking,
  * **When** tapping the [Send Payment Link on WhatsApp] button,
  * **Then** WhatsApp opens to Neha's chat with formatted message containing client greeting, requested amount, merchant UPI ID, and the raw clickable `upi://pay` URI string.
* **AC-UPI-03.2 (Offline Intent Handling):**
  * **Given** the device has no internet connection,
  * **When** dispatching the payment link,
  * **Then** the intent is handed off to the WhatsApp application locally without requiring network permissions in the parlour app.

### 5. Edge Cases & Technical Resilience
* **EC-UPI-03.1 (WhatsApp Clients that Do Not Hyperlink Raw URIs):** Some older WhatsApp builds do not auto-hyperlink `upi://pay`. The message format includes both the deep link and plain-text copyable instructions: *"UPI ID: uniquemakeover@upi | Amount: ₹300"*, ensuring client can pay even if deep linking is suppressed.
* **EC-UPI-03.2 (Standalone QR Without Client Record):** If QR is opened directly from dashboard without an appointment ID, tapping "Send on WhatsApp" opens Android system contact picker to select recipient.

### 6. UI/UX Specification
* **Component:** Action button on `UpiQrScreen.kt`.
* **Anatomy:** Secondary full-width button styled in Luxury Green with WhatsApp icon: `[💬 Send Payment Link on WhatsApp]`.

### 7. Database Schema Reference
No database schema mutations; uses appointment and settings models.

### 8. API / Internal Architecture Contracts

```kotlin
// UseCase Contract (feature/upi/domain/usecase/BuildUpiWhatsAppMessageUseCase.kt)
class BuildUpiWhatsAppMessageUseCase @Inject constructor(
    private val buildUpiUriUseCase: BuildUpiUriUseCase
) {
    operator fun invoke(
        clientName: String,
        amount: Int,
        services: String,
        request: UpiRequest
    ): String {
        val upiUri = buildUpiUriUseCase(request)
        return """
            Hello $clientName, please pay advance deposit of ₹$amount for your $services booking at Unique Makeover using this link:
            $upiUri
            
            Or transfer to UPI ID: ${request.upiId}
            Thank you! ✨
        """.trimIndent()
    }
}
```

### 9. Unit Tests
* **`BuildUpiWhatsAppMessageUseCaseTest.kt`:**
  * Asserts inclusion of client name, amount, service, and valid `upi://pay` URI.

### 10. Integration Tests (Not Required)
* Covered by `WhatsAppLauncherInstrumentedTest.kt` in Phase 5.

### 11. Internal Contract / Boundary Tests
* **`UpiMessageUrlEncodingTest.kt`:** Verifies multiline message with raw `upi://` URI passes `Uri.encode()` without truncation.

### 12. Files and Folder Mapping

```
[CREATED]
app/src/main/java/com/uniquemakeover/admin/feature/upi/domain/usecase/BuildUpiWhatsAppMessageUseCase.kt
app/src/test/java/com/uniquemakeover/admin/feature/upi/domain/usecase/BuildUpiWhatsAppMessageUseCaseTest.kt
[MODIFIED]
app/src/main/java/com/uniquemakeover/admin/feature/upi/presentation/UpiQrScreen.kt (Adds Send WhatsApp Link button)
app/src/main/java/com/uniquemakeover/admin/feature/upi/presentation/UpiQrViewModel.kt (Wires launchWhatsAppPaymentRequest)
```

---

## Slice #29: QR Code Image File Export & Native Share Sheet (US-UPI-04)

### 1. Business Goal
Allow the salon owner to save the generated QR code as a PNG image in the device gallery (for printing a front-desk acrylic display stand) or share it via the Android system share sheet to local bluetooth printers or nearby devices.

### 2. Actors
* **Salon Owner (Sunita):** Exports or shares the parlour QR code image.

### 3. User Story
> **As** the salon owner (Sunita),  
> **I want** to save the generated QR code as a PNG image or share it via the Android share sheet,  
> **So that** I can print it for the salon front desk or send it via other local sharing apps.

### 4. Acceptance Criteria
* **AC-UPI-04.1 (Save PNG to MediaStore):**
  * **Given** a generated QR code on screen,
  * **When** the owner taps [Save to Gallery],
  * **Then** a high-resolution PNG is saved to `Pictures/UniqueMakeover/UPI_QR_UniqueMakeover.png` without requiring legacy storage permissions, and a toast confirms *"QR saved to Gallery"*.
* **AC-UPI-04.2 (Native Android Share Sheet):**
  * **Given** the owner taps [Share QR],
  * **Then** the Android system share sheet opens displaying image preview, powered by a secure `FileProvider` `content://` URI.

### 5. Edge Cases & Technical Resilience
* **EC-UPI-04.1 (Scoped Storage Compatibility Android 10+ vs 8.0/9.0):**
  * *Android 10+ (API 29+):* Uses `MediaStore.Images.Media.getContentUri(VOLUME_EXTERNAL_PRIMARY)` with `RELATIVE_PATH = "Pictures/UniqueMakeover"`. Zero permissions required.
  * *Android 8.0–9.0 (API 26–28):* Uses app cache directory with `FileProvider` to share safely without requiring `WRITE_EXTERNAL_STORAGE`.
* **EC-UPI-04.2 (Cache Directory Pruned by OS):** Shared PNG cached in `cache/qr_share/qr_code.png`. If pruned, system regenerates bitmap on-the-fly prior to launching `ACTION_SEND`.

### 6. UI/UX Specification
* **Component:** Action row in `UpiQrScreen.kt`.
* **Buttons:**
  * `[💾 Save to Gallery]` (Filled tonal button).
  * `[🔗 Share QR]` (Outlined icon button).
* **Feedback:** Success toast on save; native Android share sheet bottom dialog on share.

### 7. File System & FileProvider Architecture
* **FileProvider Authority:** `com.uniquemakeover.admin.fileprovider`.
* **FileProvider Path:** `res/xml/file_paths.xml`:
  ```xml
  <paths>
      <cache-path name="shared_qr" path="qr_share/" />
  </paths>
  ```

### 8. API / Internal Architecture Contracts

```kotlin
// Platform Exporter Interfaces (feature/upi/data/platform/QrExporters.kt)
interface QrPngExporter {
    suspend fun saveToGallery(bitmap: Bitmap, filename: String): DataResult<Uri>
}

interface QrShareIntentBuilder {
    fun createShareIntent(bitmap: Bitmap): Intent
}

// UseCase Contracts
class SaveQrPngUseCase @Inject constructor(
    private val qrPngExporter: QrPngExporter
) {
    suspend operator fun invoke(bitmap: Bitmap, filename: String): DataResult<Uri> =
        qrPngExporter.saveToGallery(bitmap, filename)
}

class ShareQrUseCase @Inject constructor(
    private val qrShareIntentBuilder: QrShareIntentBuilder
) {
    operator fun invoke(bitmap: Bitmap): Intent =
        qrShareIntentBuilder.createShareIntent(bitmap)
}
```

### 9. Unit Tests
* **`SaveQrPngUseCaseTest.kt`:** Mocks `QrPngExporter`, verifies returning success result on URI creation.

### 10. Integration Tests (Required)
* **`QrPngExporterInstrumentedTest.kt` (Robolectric / ShadowContentResolver):**
  * Passes test 512x512 bitmap to `QrPngExporter.saveToGallery()`.
  * Verifies record created in `MediaStore.Images.Media`.
  * Verifies output stream contains valid PNG magic header bytes (`0x89 0x50 0x4E 0x47`).
* **`QrShareFileProviderTest.kt` (Android Runtime):**
  * Verifies `FileProvider.getUriForFile()` resolves cached PNG without `IllegalArgumentException`.

### 11. Internal Contract / Boundary Tests
* **Share Intent Contract Test:** Verifies `intent.action == Intent.ACTION_SEND` and `intent.flags` contains `FLAG_GRANT_READ_URI_PERMISSION`.

### 12. Files and Folder Mapping

```
[CREATED]
app/src/main/res/xml/file_paths.xml
app/src/main/java/com/uniquemakeover/admin/feature/upi/data/platform/QrPngExporter.kt
app/src/main/java/com/uniquemakeover/admin/feature/upi/data/platform/QrPngExporterImpl.kt
app/src/main/java/com/uniquemakeover/admin/feature/upi/data/platform/QrShareIntentBuilder.kt
app/src/main/java/com/uniquemakeover/admin/feature/upi/data/platform/QrShareIntentBuilderImpl.kt
app/src/main/java/com/uniquemakeover/admin/feature/upi/domain/usecase/SaveQrPngUseCase.kt
app/src/main/java/com/uniquemakeover/admin/feature/upi/domain/usecase/ShareQrUseCase.kt
app/src/test/java/com/uniquemakeover/admin/feature/upi/domain/usecase/SaveQrPngUseCaseTest.kt
app/src/androidTest/java/com/uniquemakeover/admin/feature/upi/data/QrPngExporterInstrumentedTest.kt
app/src/androidTest/java/com/uniquemakeover/admin/feature/upi/data/QrShareFileProviderTest.kt
[MODIFIED]
app/src/main/AndroidManifest.xml (Declares FileProvider for qr_share)
app/src/main/java/com/uniquemakeover/admin/core/di/AppModule.kt (Binds QrPngExporter and QrShareIntentBuilder)
app/src/main/java/com/uniquemakeover/admin/feature/upi/presentation/UpiQrScreen.kt (Integrates Save and Share buttons)
```

---

## Phase 6 Test Execution Matrix & QA Acceptance Gate

To declare Phase 6 complete and authorize progression to **Phase 7 (Salon Branding & Alert Intelligence)**, QA must verify that all 14 test suites pass with 100% green status:

```bash
# 1. Run All JVM Unit Tests (< 20 seconds)
./gradlew testDebugUnitTest

# 2. Run All Phase 6 Android Runtime Integration Tests
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=\
com.uniquemakeover.admin.feature.upi.presentation.UpiQrScreenTest,\
com.uniquemakeover.admin.feature.upi.data.QrPngExporterInstrumentedTest,\
com.uniquemakeover.admin.feature.upi.data.QrShareFileProviderTest

# 3. Verify Detekt Architecture Rules (Zero Room in Presentation, Zero android.* in Domain)
./gradlew detekt

# 4. Strict Offline Invariant Check (Zero internet permission in manifest)
grep -rn "INTERNET" app/src/main/AndroidManifest.xml && exit 1
```

---
**End of Phase 6 Vertical Slices Specification — Unique Makeover Admin Console**
