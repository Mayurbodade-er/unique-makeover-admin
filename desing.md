# Unique Makeover — Design System & UI/UX Specification
## Native Android Admin Console · Jetpack Compose (Material 3) · Fully Offline
**Document Version:** 2.0.0 (Production-Grade Responsive Design System)  
**Target Roles:** Veteran Product Designer & UI System Design Team  
**Architecture Alignment:** `docs/FRD.md` (v2.0), `docs/ARD.md` (v1.0), `docs/USER_STORIES.md` (v2.0), `vertical-slice-indexmap.md`  
**Target Persona:** Sunita (Sole Owner & Master Beautician at *Unique Makeover*, Jalgaon Jamod)

---

## 1. Design Philosophy & Visual Pillars

The visual language of **Unique Makeover Admin Console** balances boutique parlour luxury with high-throughput operational ergonomics. As an admin-only tool operated on salon countertops, hand-held phones, and docked tablets, the interface adheres to four core design pillars:

1. **Luxury Wine & Warm Gold Identity:** Deep Burgundy (`#8E294D`) anchored with Warm Gold accents (`#C89B54`) and delicate rose undertones (`#FCEEF2`), projecting elegance and cleanliness.
2. **High-Contrast Salon Legibility:** High-contrast typography and clear visual hierarchy designed for readability under harsh salon lighting and at arm's length on counter stands.
3. **Tactile & Single-Hand Ergonomics:** Prominent finger-friendly touch targets ($\ge 48\text{dp}$), physical haptic feedback on key transitions, and primary operational actions accessible with minimal thumb travel.
4. **Universal Multi-Device Responsiveness:** True fluid responsiveness adapting seamlessly across budget Android phones, foldables, and mounted reception tablets using Material 3 Window Size Classes (Compact, Medium, Expanded).

---

## 2. Multi-Device Responsive Architecture (Compact · Medium · Expanded)

The application is engineered to be **100% responsive** across every Android device size and orientation, transitioning from single-column phone ergonomics to multi-pane front-desk tablet layouts:

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                        RESPONSIVE WINDOW SIZE CLASS ADAPTATIONS                        │
├──────────────────────────┬──────────────────────────┬──────────────────────────────────┤
│ COMPACT (< 600dp)        │ MEDIUM (600dp – 840dp)   │ EXPANDED (>= 840dp)              │
│ Budget / Mid-range Phone │ Foldables / Small Tablet │ Front-Desk Reception Tablet      │
├──────────────────────────┼──────────────────────────┼──────────────────────────────────┤
│ • Bottom Navigation Bar  │ • Navigation Rail (Left) │ • Permanent Nav Rail / Drawer    │
│ • Single Column Layout   │ • 2-Column Dashboard KPI │ • Master-Detail Dual Pane        │
│ • Modal Bottom Sheets    │ • 3-Column Slot Grid     │ • Side-by-Side List & Inspector  │
│ • Fullscreen Dialogs     │ • Side-by-Side Modals    │ • Persistent Split View          │
└──────────────────────────┴──────────────────────────┴──────────────────────────────────┘
```

### 2.1 Responsive Layout Specifications

| UI Screen / Component | Compact Phone (`< 600dp`) | Medium Tablet/Foldable (`600–840dp`) | Expanded Large Tablet (`>= 840dp`) |
| :--- | :--- | :--- | :--- |
| **Primary Navigation** | Fixed Bottom Navigation Bar (5 items) | Vertical Navigation Rail (Pinned Left) | Permanent Navigation Drawer with Text Labels |
| **Dashboard (Screen #2)** | 2-column KPI grid stacked above vertical Live Schedule Stream | 3-column KPI grid stacked above 2-column Schedule Stream | Split-Screen: Left Pane = KPI Matrix & Alert Banners; Right Pane = Live Schedule Stream |
| **Appointments (Screen #3)**| Full-width list cards with filter tabs | 2-column appointment cards with sticky filter row | Split-Screen: Left Pane = Filtered List; Right Pane = Selected Appointment Inspector |
| **Booking Sheet (Screen #4)**| Modal Bottom Sheet with vertical scroll | Center-Anchored Dialog (Width: `560dp`) | Dual-Column Sheet: Left = Client & Services; Right = SlotGrid & Live Financial Summary |
| **Slot Matrix (`SlotGrid`)** | 3 columns of time chips | 4 columns of time chips | 6 columns of time chips (minimizes scrolling) |
| **Service Catalog (Screen #5)**| 2-column adaptive card grid | 3-column card grid | 4-column card grid with expanded photo previews |
| **Clients CRM (Screens #6 & #7)**| List navigates to separate Detail Screen | List navigates to separate Detail Screen | Master-Detail: Left Pane = Directory & Search; Right Pane = Selected Client Detail, Notes & Rebook |
| **Offline UPI QR (Screen #9)**| Fullscreen dialog, centered 260dp QR | Centered modal (Width: `520dp`), 320dp QR | Dual-Pane Counter Stand: Left = QR Canvas (360dp); Right = Preset Buttons & WhatsApp Trigger |
| **Settings (Screen #10)** | Single-column expandable sections | 2-column section grid | Two-Pane Settings: Left = Section List; Right = Active Section Form |

---

## 3. Color Palette & Design Tokens

### 3.1 Material 3 Core Palette

```kotlin
// Core Brand Color Tokens
val BrandPrimary       = Color(0xFF8E294D) // Deep Burgundy Anchor (Top bar, Primary buttons, Active tabs)
val BrandDark          = Color(0xFF6B1B38) // Button pressed state, gradient endpoints
val BrandDarker        = Color(0xFF4D1127) // Top app bar deep gradient endpoint
val RoseAccent         = Color(0xFFC85A7C) // Secondary highlights, subtle chip borders
val LuxuryGold         = Color(0xFFC89B54) // Luxury action triggers, VIP badges, QR code frame
val GoldSoft           = Color(0xFFFDF6E9) // Soft warm gold card background (Allergy/Notes box)
val GoldGradientStart  = Color(0xFFD4A85C) // Luxury button gradient start
val GoldGradientEnd    = Color(0xFFB58438) // Luxury button gradient end
val BlushSurface       = Color(0xFFFCEEF2) // Selected chip background, soft badge highlight
val WarmBackground     = Color(0xFFFBF5F7) // App-wide warm canvas (rose-tinted cream)
val SurfaceCard        = Color(0xF2FFFFFF) // Translucent card surface (rgba 255,255,255, 0.95)
val TextPrimary        = Color(0xFF2A151D) // High-contrast text, titles, numbers
val TextHeading        = Color(0xFF54152C) // Section headers, form field labels
val TextMuted          = Color(0xFF75535F) // Subtitles, timestamps, captions, inactive icons
val BorderLine         = Color(0xFFEED3DC) // Card borders, dividers, subtle separators
val InputBorder        = Color(0xFFE4C2CE) // Inactive text field and slot chip borders

// Semantic & Status Tokens
val SuccessOk          = Color(0xFF1B873B) // Completed appointments, paid badges
val SuccessSoft        = Color(0xFFDCF3E3) // Completed pill container background
val WhatsAppGreen      = Color(0xFF25D366) // Official WhatsApp action buttons
val WhatsAppDarkText   = Color(0xFF053618) // High-contrast text on WhatsApp buttons
val DangerCancel       = Color(0xFFA1163A) // Cancelled status, destructive delete dialogs
val DangerSoft         = Color(0xFFFDE2EA) // Cancelled pill container background
val WarningDue         = Color(0xFF856404) // Outstanding balance alerts, overdue backup
val WarningSoft        = Color(0xFFFFF3CD) // Warning alert banner background
```

### 3.2 Visual Gradients & Layered Scrims

```kotlin
object AppGradients {
    // Top App Bar Header Gradient (Slice #01, #30)
    val BrandHeader = Brush.linearGradient(
        colors = listOf(BrandPrimary, BrandDark, BrandDarker),
        start = Offset.Zero,
        end = Offset.Infinite
    )

    // Luxury Gold Action Gradient (Slice #06, #26)
    val LuxuryGoldAction = Brush.linearGradient(
        colors = listOf(GoldGradientStart, GoldGradientEnd),
        start = Offset.Zero,
        end = Offset.Infinite
    )

    // Wallpaper Contrast Scrim Overlay (Slice #30 - Preserves WCAG AA Readability)
    val WallpaperDarkScrim = Brush.verticalGradient(
        colors = listOf(Color(0xD9FBF5F7), Color(0xFAFBF5F7)) // 85% to 98% opacity veil
    )
}
```

---

## 4. Typography Scale & Hierarchy

The typography pairs an elegant, high-fashion serif font for parlour identity with a clean, high-x-height sans-serif for numbers, prices, and forms.

* **Display & Branding Font:** `Playfair Display` or `Cinzel` (Serif).
* **UI & Operational Data Font:** `Inter` or `Plus Jakarta Sans` (Sans-Serif).

| Typography Role | Size | Weight | Line Height | Tracking | Material 3 Token | Primary Usage |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Headline Large** | `26sp` | SemiBold (600) | `32sp` | `-0.2sp` | `typography.headlineLarge` | QR payment amounts (`₹ 850.00`), PIN lockout timer |
| **Headline Medium**| `22sp` | Bold (700) | `28sp` | `0.0sp` | `typography.headlineMedium`| Screen titles (Dashboard, Reminders, Settings) |
| **Title Large** | `18sp` | Bold (700) | `24sp` | `0.1sp` | `typography.titleLarge` | Top bar title, Client names, Modal sheet titles |
| **Title Medium** | `16sp` | SemiBold (600) | `22sp` | `0.1sp` | `typography.titleMedium` | Card headers, Service catalog names |
| **Body Large** | `15sp` | Regular (400) | `22sp` | `0.1sp` | `typography.bodyLarge` | Form text fields, Client notes multiline editor |
| **Body Medium** | `13.5sp`| Medium (500) | `18sp` | `0.0sp` | `typography.bodyMedium` | Timestamps, service snapshots, staff names |
| **Body Small** | `12sp` | Regular (400) | `16sp` | `0.0sp` | `typography.bodySmall` | Muted captions, slot hints, table headers |
| **Label Large** | `15sp` | Bold (700) | `20sp` | `0.2sp` | `typography.labelLarge` | Primary buttons ([Save Appointment], [Remind]) |
| **Label Medium** | `13sp` | SemiBold (600) | `18sp` | `0.1sp` | `typography.labelMedium` | Secondary actions, Dialog buttons, Filter chips |
| **Label Small** | `11.5sp`| Bold (700) | `14sp` | `0.4sp` | `typography.labelSmall` | Status chips (`Booked`, `Done`), "Full" slot badges |

---

## 5. Spacing, Elevation & Corner Radii

### 5.1 Spacing Scale (4dp Grid System)

```kotlin
object AppSpacing {
    val xxs = 2.dp
    val xs  = 4.dp
    val sm  = 8.dp
    val md  = 12.dp
    val lg  = 16.dp
    val xl  = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp

    // Min Touch Target Size (Accessibility Mandate)
    val minTouchTarget = 48.dp
}
```

### 5.2 Corner Radii

* **Small (`8dp` – `10dp`):** Private note boxes, input badges, inner QR code container.
* **Medium (`12dp` – `14dp`):** Outlined input fields, slot chips, service thumbnails, action buttons.
* **Large (`16dp` – `18dp`):** Content cards, bottom sheets, alert banners, KPI containers.
* **Full Pill (`24dp` – `32dp`):** Status chips, filter tabs, circular logo avatars, toast notifications.

### 5.3 Elevation & Shadows

* **Cards (Level 1):** `elevation = 2.dp` with warm shadow tint `rgba(142, 41, 77, 0.08)`.
* **Elevated Buttons (Level 2):** `elevation = 4.dp` with shadow tint `rgba(142, 41, 77, 0.24)`.
* **Top App Bar (Level 3):** `elevation = 4.dp` with bottom gradient border.
* **Modal Bottom Sheets (Level 4):** `elevation = 8.dp` with 65% dark scrim backdrop.

---

## 6. Screen-by-Screen UI Component Specifications

### 6.1 Screen #1: PIN Lock Screen (Slice #01, #03, #32)

* **Layout:** Centered vertical stack, full height.
* **Emblem:** Salon circular logo (80dp) with gold border above *"Unique Makeover"*.
* **PIN Dots Indicator (`PinDots.kt`):**
  * 4 circular indicators (16dp diameter, 16dp spacing).
  * State Empty: Transparent fill, `1.5dp` gold border.
  * State Filled: Solid `BrandPrimary` animated fill with spring scaling.
  * State Error: Flashes `DangerCancel` (`#A1163A`) accompanied by horizontal shake animation ($\pm 12\text{dp}$).
* **Numeric Keypad (`PinKeypad.kt`):**
  * 3x4 grid of circular buttons (72dp diameter on phone, 84dp on tablet).
  * High-contrast dark numbers (`22sp` Bold); zero system soft keyboard.
  * Haptic Feedback: `HapticFeedbackType.TextHandleMove` on key press; `HapticFeedbackType.LongPress` on error.
* **Lockout State (Slice #32):**
  * Keypad alpha drops to `0.38` (disabled).
  * Pulsing red banner: *"⚠️ Device Locked: Try again in XXs"*.

---

### 6.2 Screen #2: Operations Dashboard (Slice #08, #09, #31)

* **KPI Cards Matrix (`KpiRow.kt`, `KpiCard.kt`):**
  * Responsive Grid: 2 columns on phones, 3 columns on tablets.
  * 6 Metric Cards:
    1. *Booked (Pending):* Count with blue calendar badge.
    2. *Completed (Done):* Count with green check badge.
    3. *Cancelled:* Count with muted neutral badge.
    4. *Scheduled Volume:* Total ₹ sum of active bookings.
    5. *Collected Revenue:* Bold green ₹ sum of payments collected.
    6. *Balance Outstanding:* Bold amber ₹ due amount.
* **Actionable Alert Banners (`AlertBannerCard.kt`):**
  * Dismissible cards with colored left accent strip (Red: Overdue Backup, Amber: Missing UPI/WA, Blue: Pending Reminders).
  * 1-tap [Fix Now $\rightarrow$] deep link button.
* **Today's Live Schedule Stream (`ScheduleRowCard.kt`):**
  * Chronologically ordered appointment list.
  * Quick 1-tap action strip on each card:
    * ✅ **Mark Done:** Triggers balance prompt.
    * 💬 **WhatsApp:** Opens client chat.
    * ₹ **Record Pay:** Opens in-line payment dialog.
    * ✏️ **Edit:** Opens booking sheet in edit mode.
    * ❌ **Cancel:** Updates status with 5s Undo snackbar.
    * 🗑 **Delete:** Prompts confirmation dialog.

---

### 6.3 Screen #3: Appointments Directory (Slice #07, #10, #13)

* **Top Search & Filter Bar:**
  * Substring search field matching client name or phone with clear button.
  * Horizontal scrollable `FilterTabs.kt` (**All · Upcoming · Completed · Cancelled**) with count badges.
  * Date range selector chip (`[Today ▼]`, `[This Week]`, `[Custom Range]`).
* **Appointment Cards (`AppointmentCard.kt`):**
  * Left: Date & Time block with staff stylist pill.
  * Center: Client name, phone, frozen services snapshot string.
  * Right: Rate, Paid, Balance Due, and colored status tag (`Booked`, `Done`, `Cancelled`).
* **Master-Detail Adaptation (Expanded Devices):**
  * Left 40% width = Filtered list.
  * Right 60% width = Selected appointment details, full payment history, and action buttons.

---

### 6.4 Screen #4: Booking & Reschedule Sheet (Slice #05, #06, #12)

* **Container:** Modal Bottom Sheet on phones; Centered Dialog (560dp width) on tablets.
* **Form Sections:**
  1. **Client Info:** Name text field + Phone field (10 digits with fixed `+91` icon prefix).
  2. **Service Multi-Select (`ServiceMultiSelect.kt`):** Horizontal chip flow with service name and rate (`[✓ Gold Facial ₹800]`).
  3. **Date & Dynamic Slot Matrix (`SlotGrid.kt`):**
     * Date picker row.
     * Grid of time slot chips:
       * *Available:* White background, gold border; animates to solid gold on selection.
       * *Booked:* Grayed out with red **"Full"** badge; non-clickable.
       * *Past:* Disabled opacity (38%) for today's past hours.
  4. **Staff Dropdown (`StaffDropdown.kt`):** Populated from configured staff roster.
  5. **Financial Computation Card:**
     * Subtotal: Auto-calculated live.
     * Discount Input: Currency field.
     * Total Billed Rate: Bold headline.
     * Advance Paid Input: Currency field.
     * Outstanding Due: Amber calculated badge.
  6. **Sticky Action Button:** [Save Appointment] full-width luxury button.

---

### 6.5 Screen #5: Service Catalog (Slice #02, #14, #15)

* **Adaptive Catalog Grid:**
  * 2 columns on phones, 3 columns on small tablets, 4 columns on expanded tablets.
* **Service Card (`ServiceCard.kt`):**
  * Top: 320px local photo thumbnail (or gold fallback vector `ic_spa`).
  * Center: Service Name (`titleMedium`).
  * Bottom: Rate Pill (`₹ 800` in Luxury Gold) + Edit pencil action.
* **Service Editor Sheet (`ServiceEditorSheet.kt`):**
  * Square Photo Picker (120x120 dp) with camera overlay badge.
  * Camera vs Gallery source chooser bottom sheet.
  * Name and Price inputs.
  * [Delete Service] destructive text action with confirmation dialog.

---

### 6.6 Screens #6 & #7: Client CRM & Treatment Notes (Slice #16, #17, #18, #19, #20)

* **Directory List (`ClientsScreen.kt`):**
  * Search bar with 150ms debounce.
  * Client row: Avatar with initials, Name, Phone, Lifetime Spend badge (`₹ 8,500`), and Total Visits counter.
* **Client Detail (`ClientDetailScreen.kt`):**
  * **Header (`ClientMetricsHeader.kt`):** Name, Phone, [📞 Call Client], [💬 WhatsApp], and prominent **[⚡ Quick Rebook]** button.
  * **Tab 1: Visit History (`HistoryList.kt`):** Chronological timeline of past visits with attending stylists and billed rates.
  * **Tab 2: Private Notes (`NoteEditor.kt`):**
    * Amber security card: *"🔒 Private Salon Treatment & Allergy Notes"*.
    * Corner autosave indicator: *"Saving..."* $\rightarrow$ *"Saved ✓"*.
    * Multiline text area for chemical allergies, bleach sensitivity, and stylist preferences (500ms debounced autosave).
* **Master-Detail Adaptation (Expanded Devices):**
  * Directory list pinned on left; Client profile, notes editor, and visit history on right.

---

### 6.7 Screen #8: Tomorrow's Reminders Queue (Slice #22, #23, #24)

* **Header:** Tomorrow's calendar date, client count pill, and completion progress bar (`X of Y Reminded`).
* **Reminder Row (`ReminderRow.kt`):**
  * Appointment time, Client Name, Booked services, Balance due.
  * Primary Action: **[💬 Remind]** button in official WhatsApp Green.
  * Status Chip: `[Not Sent]` (Muted) $\rightarrow$ `[Reminded ✓]` (Emerald green).
* **Transient Undo Snackbar:** Displays for 5 seconds after tapping Remind with `[UNDO]` action.

---

### 6.8 Screen #9: Offline UPI QR Modal (Slice #25, #26, #27, #28, #29)

* **Window Behavior:** Automatic window brightness maximization to 100% on enter; restores prior brightness on exit.
* **Center QR Canvas (`QrCanvas.kt`):**
  * 512x512 px sharp ZXing QR code on clean white card with quiet zone border.
  * Payee Name: *"Unique Makeover"*.
  * Merchant UPI ID: `uniquemakeover@upi`.
  * Requested Amount: Extra-bold headline (`₹ 850.00`).
* **Billing Presets Bar (`PresetSelector.kt`):**
  * Segmented control chips: `[Balance Due]` · `[Advance (30%)]` · `[Full Payment]` · `[Custom ✏️]`.
* **Action CTAs:**
  * [💾 Save to Gallery] (PNG MediaStore export).
  * [🔗 Share QR] (Native Android share sheet via FileProvider).
  * [💬 Send Payment Link on WhatsApp] (Dispatches clickable `upi://` URI link).

---

### 6.9 Screen #10: Settings & Data Management (Slice #04, #21, #30, #34–#37)

* **Organized Categorical Sections:**
  1. **Salon Branding:** Salon Name, Tagline, 200px circular logo uploader, 900px wallpaper uploader.
  2. **Access Control:** [Change Master PIN] two-step modal workflow.
  3. **Business Operations:** Opening/Closing TimePickers, Slot Interval selector (15/30/45/60m), Staff Roster chip manager.
  4. **Communication Settings:** Official WhatsApp number and Merchant UPI ID fields.
  5. **Reminder Template:** Multiline template editor with 8 placeholder token pills and real-time incoming chat preview bubble.
  6. **Data Management & Recovery:**
     * Overdue Backup warning badge (`Last Backup: X days ago`).
     * [📥 Export Backup (JSON)] button (Storage Access Framework).
     * [📤 Restore Backup] button (Diff preview dialog + atomic rollback).
     * [🗑️ Factory Reset Salon Data] (Red destructive button protected by PIN challenge).

---

## 7. Motion & Interaction Design Standards

1. **Touch Feedback (Compose Ripple):**
   * All clickable cards and buttons utilize `rememberRipple(bounded = true, color = BrandDark)`.
2. **State Selection Morphing:**
   * Slot chips and service multi-select cards animate border and background colors smoothly using `animateColorAsState(tween(150))`.
3. **Modal Transition Speeds:**
   * Bottom sheets slide in from bottom via `tween(280, easing = FastOutSlowInEasing)`.
   * Dialogs scale in with subtle fade (`fadeIn() + scaleIn(initialScale = 0.92f)`).
4. **Haptic Feedback Mapping:**
   * Number Keypad Tap: `HapticFeedbackType.TextHandleMove` (Light click).
   * Form Save / Rebook / Done: `HapticFeedbackType.TextHandleMove` (Crisp confirmation).
   * PIN Error / Invalid Input: `HapticFeedbackType.LongPress` (Heavy double buzz).

---

## 8. Responsive Design Validation Matrix

Before releasing any UI screen, the Product Design and QA teams must verify rendering across these standard reference device configurations:

| Device Profile | Screen Resolution | Width DP | Navigation Pattern | Layout Verification Goal |
| :--- | :--- | :--- | :--- | :--- |
| **Budget Phone** | $720 \times 1600\text{ px}$ | $\sim 360\text{dp}$ | Bottom Navigation Bar | Verify zero text cutoffs, 3-column slot grid readability, single-hand reach. |
| **Standard Phone** | $1080 \times 2400\text{ px}$| $\sim 411\text{dp}$ | Bottom Navigation Bar | Optimal reference ergonomics; card margins `16dp`. |
| **Foldable (Unfolded)** | $1812 \times 2176\text{ px}$| $\sim 670\text{dp}$ | Left Navigation Rail | 2-column KPI grid, wide booking dialogs. |
| **10-Inch Salon Tablet**| $1200 \times 1920\text{ px}$| $\sim 800\text{dp}$ | Left Navigation Rail | 3-column service catalog, 4-column slot grid. |
| **12-Inch Counter Tablet**| $1600 \times 2560\text{ px}$| $\sim 900\text{dp}$ | Permanent Nav Drawer | Full Master-Detail split pane across Clients and Appointments; large counter QR display. |

---
**End of Design System & UI/UX Specification v2.0 — Unique Makeover Admin Console**
