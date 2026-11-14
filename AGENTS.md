# Unique Makeover — Agent Operating Guidelines & Autonomous Execution Manual
### Autonomous Coding Agent Protocols · Native Android · Fully Offline · Clean Architecture
**Document Version:** 1.0.0  
**Target Audience:** Autonomous AI Coding Agents, Pair-Programming Subagents, Lead Architects  
**Authoritative References:** [`docs/ARD.md`](file:///home/shunya/Projects/uniqmaleoveradmin/docs/ARD.md), [`docs/FRD.md`](file:///home/shunya/Projects/uniqmaleoveradmin/docs/FRD.md), [`desing.md`](file:///home/shunya/Projects/uniqmaleoveradmin/desing.md), [`docs/USER_STORIES.md`](file:///home/shunya/Projects/uniqmaleoveradmin/docs/USER_STORIES.md), [`vertical-slice-indexmap.md`](file:///home/shunya/Projects/uniqmaleoveradmin/vertical-slice-indexmap.md)

---

## 1. Prime Mandate & Core Philosophy

You are an expert autonomous Android coding agent implementing the **Unique Makeover Admin Console** for a boutique luxury parlour in Jalgaon Jamod. You work with surgical precision, adhering to strict Clean Architecture and zero-regression protocols.

### The Golden Rule of Execution:
> **BUILD AND TEST EXACTLY ONE VERTICAL SLICE AT A TIME.**  
> Never implement multiple slices in a single turn. Never scaffold hollow horizontal layers across future slices. Every slice must cut cleanly through Presentation, Domain, Data, and Database/Platform layers, ending with green automated tests and an updated slice specification document.

---

## 2. Inviolable Architectural & Technical Invariants

Before writing or modifying any line of code, enforce these five inviolable system invariants:

### Invariant 1: Strict Offline Guarantee (Zero Network)
* `app/src/main/AndroidManifest.xml` must **NEVER** contain `<uses-permission android:name="android.permission.INTERNET" />`.
* No external HTTP/REST network libraries (no Retrofit, OkHttp, Ktor, Volley, Firebase, or Google Play Services).
* All QR generation must use local **ZXing Core** (`com.google.zxing:core`) in memory (ADR-0007).
* All customer messaging must use standard implicit Android OS intents (`Intent.ACTION_VIEW` targeting `wa.me`).

### Invariant 2: Clean Architecture Package Boundaries (ARD §14)
Enforced by Detekt custom rules and strict code review:
* `domain/` packages may **NEVER** import `android.*`, `androidx.*`, `androidx.room.*`, or any data/presentation classes. Domain is 100% pure Kotlin.
* `presentation/` packages may **NEVER** import `androidx.room.*` or any `*.data.*` classes. UI only communicates with domain UseCases and models.
* `data/` packages implement repository interfaces defined in `domain/` and map entities to domain models.
* Cross-feature dependencies are strictly forbidden except via `core/domain` shared models and repository interfaces.

### Invariant 3: Single-Module Strict Feature Packages (ARD §4, §6)
All feature code lives in `com.uniquemakeover.admin.feature.<feature_name>` with strict internal layering:
```
feature/<feature_name>/
├── data/
│   ├── local/ (dao, entity)
│   ├── mapper/
│   └── repository/
├── domain/
│   ├── model/
│   ├── repository/
│   └── usecase/
└── presentation/
    ├── <Screen>Screen.kt
    ├── <Screen>ViewModel.kt
    ├── <Screen>UiState.kt
    └── components/
```

### Invariant 4: Image Paths on Disk, Never Blobs in SQLite (ADR-0004)
* Camera and Gallery photos must be scaled to **320 px longest edge @ 78% JPEG** via `ImageCompressor` and saved in `context.filesDir/images/{uuid}.jpg`.
* Room `ServiceEntity.imagePath` stores relative file paths only (`"images/{uuid}.jpg"`). Never write binary image byte arrays into SQLite tables.

### Invariant 5: Storage Access Framework (SAF) for Backups (ADR-0005)
* Database export/import must use Android's native Storage Access Framework (`CreateDocument` / `OpenDocument`).
* Backup restoration must execute inside a single atomic `RoomDatabase.withTransaction`. If any validation fails, rollback must occur with zero data loss.

---

## 3. Frontend & UI System Invariants (Governed by `desing.md`)

When designing and generating Compose UI screens, you must strictly follow [`desing.md`](file:///home/shunya/Projects/uniqmaleoveradmin/desing.md):

1. **Design Tokens & Palette:**
   * Primary Brand: `BrandPrimary` (`#8E294D` Deep Burgundy)
   * Luxury Accent: `LuxuryGold` (`#C89B54`)
   * Canvas: `WarmBackground` (`#FBF5F7`)
   * Translucent Cards: `SurfaceCard` (`rgba(255, 255, 255, 0.95)`)
   * WhatsApp Buttons: `WhatsAppGreen` (`#25D366`) with `WhatsAppDarkText` (`#053618`)
   * Status Badges: `SuccessOk` (`#1B873B`), `DangerCancel` (`#A1163A`), `WarningDue` (`#856404`)
2. **Typography Standards:**
   * Display Headers: `Playfair Display` or `Cinzel` (Serif).
   * UI & Data Readability: `Inter` or `Plus Jakarta Sans` (Sans-Serif).
3. **Universal Multi-Device Responsiveness (Mandatory):**
   * Support **Compact** (`< 600dp` phones), **Medium** (`600dp – 840dp` foldables), and **Expanded** (`>= 840dp` reception tablets).
   * On phones: Use single-column layouts, 3-column `SlotGrid`, and bottom navigation.
   * On tablets: Use Navigation Rail / Drawer, 6-column `SlotGrid`, 3-column KPI matrix, and **Master-Detail Dual Pane** for Clients and Appointments.
4. **Ergonomics & Touch Targets:**
   * Every clickable element must meet or exceed minimum touch target size: $\ge 48\text{dp}$.
   * Tactile haptic feedback on key actions (`HapticFeedbackType.TextHandleMove` for selections, `LongPress` for errors).

---

## 4. Step-by-Step Vertical Slice Execution Protocol

For every vertical slice you execute from [`vertical-slice-indexmap.md`](file:///home/shunya/Projects/uniqmaleoveradmin/vertical-slice-indexmap.md), you must follow this exact 7-step sequence:

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                     VERTICAL SLICE IMPLEMENTATION PIPELINE                       │
├─────────────┬─────────────┬─────────────┬─────────────┬────────────┬─────────────┤
│ STEP 1      │ STEP 2      │ STEP 3      │ STEP 4      │ STEP 5     │ STEP 6 & 7  │
│ Context &   │ Test        │ Domain      │ Data Layer  │ UI Compose │ Tests & Doc │
│ Specs Read  │ Harness     │ UseCases    │ DAO & Repo  │ & ViewModel│ Update Spec │
└─────────────┴─────────────┴─────────────┴─────────────┴────────────┴─────────────┘
```

### Step 1: Pre-Execution Context & Dependency Validation
* Read the target slice from `vertical-slice-indexmap.md` (Slice #01 through #39).
* Read the corresponding Phase specification document (`docs/PHASE_X_VERTICAL_SLICES.md`).
* Verify that all prerequisite dependency slices are completed and verified before starting.

### Step 2: Test Harness Scaffolding (TDD Approach)
* Create the test files first or alongside the domain contracts:
  * Unit test: `app/src/test/java/.../<UseCase>Test.kt`
  * Integration test (if required by slice): `app/src/androidTest/java/.../<DaoOrScreen>Test.kt`
* Define test cases covering: Happy path, validation failure, and the specific edge cases documented in `docs/USER_STORIES.md`.

### Step 3: Domain Layer Implementation
* Create pure Kotlin domain models (`model/*.kt`).
* Create repository interfaces (`repository/*Repository.kt`).
* Create UseCase classes (`usecase/*UseCase.kt`) with single-responsibility `operator fun invoke(...)`.
* **Check:** Ensure zero `android.*` imports in domain.

### Step 4: Data Layer Implementation
* Create Room entities (`entity/*Entity.kt`) and DAOs (`dao/*Dao.kt`) if database tables are involved.
* Create mappers (`mapper/*Mapper.kt`) converting between Room Entities and Domain Models.
* Implement repository interface (`repository/*RepositoryImpl.kt`) using `@IoDispatcher` and Room transactions where needed.
* Wire Hilt dependency injection bindings in `core/di` or feature-local DI module.

### Step 5: Presentation Layer Implementation
* Define immutable UI state (`presentation/*UiState.kt`).
* Implement `ViewModel` (`presentation/*ViewModel.kt`) managing state via `StateFlow` and handling user events.
* Implement Compose UI screens and components (`presentation/*Screen.kt`, `components/*.kt`) strictly adhering to [`desing.md`](file:///home/shunya/Projects/uniqmaleoveradmin/desing.md) design tokens and responsive Window Size Classes.

### Step 6: Test Execution & Verification Gate
Execute automated checks via terminal commands:
```bash
# 1. Run JVM Unit Tests for the slice
./gradlew testDebugUnitTest --tests "*<TargetTest>*"

# 2. Run Android Runtime Integration Tests (if slice requires integration test)
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=*<TargetTest>*

# 3. Check Detekt Architectural Boundaries
./gradlew detekt

# 4. Enforce Zero-Network Invariant
grep -rn "INTERNET" app/src/main/AndroidManifest.xml && exit 1
```
* **Rule:** If any test fails, diagnose and fix immediately. Do not move to the next slice with broken tests.

### Step 7: Post-Slice Documentation Update (Mandatory)
* Open the corresponding phase document: `docs/PHASE_X_VERTICAL_SLICES.md`.
* Locate the section for the completed vertical slice: `### 12. Files and Folder Mapping`.
* Update the file lists to accurately reflect all **Created** and **Modified** files.
* **MANDATORY REQUIREMENT:** For every single file created or modified, append an explicit architectural rationale stating **WHY** this code file was created, what responsibility it fulfills, and which layer it belongs to.

---

## 5. File Creation Justification Standard (Documentation Template)

When updating the `Files and Folder Mapping` section in `docs/PHASE_X_VERTICAL_SLICES.md` after completing a slice, you must format every entry using this standard:

```markdown
### 12. Files and Folder Mapping (Updated Post-Implementation)

#### Files Created:
1. `app/src/main/java/com/uniquemakeover/admin/feature/appointments/domain/model/Slot.kt`
   - **Architectural Role:** Domain Model (Core/Appointments)
   - **Why Created:** Encapsulates the immutable time slot entity (`time24`, `time12`, `isAvailable`, `reason`) representing operational parlour time intervals independent of any database or UI framework.
2. `app/src/main/java/com/uniquemakeover/admin/feature/appointments/domain/usecase/GenerateAvailableSlotsUseCase.kt`
   - **Architectural Role:** Domain UseCase
   - **Why Created:** Implements the core business logic of generating daily time slots from operating hours, querying occupied slots from SQLite, evaluating past-time invalidation using injected `TimeProvider`, and tagging clashes with "Full" badges.
3. `app/src/main/java/com/uniquemakeover/admin/feature/appointments/data/repository/SlotRepositoryImpl.kt`
   - **Architectural Role:** Data Repository Implementation
   - **Why Created:** Implements `SlotRepository` interface; bridges domain use case with Room `AppointmentDao.countClashingAppointments()` on the `@IoDispatcher`.
4. `app/src/main/java/com/uniquemakeover/admin/feature/appointments/presentation/booking/components/SlotGrid.kt`
   - **Architectural Role:** Presentation (Compose Component)
   - **Why Created:** Renders the responsive time chip grid adhering to `desing.md`; adapts dynamically from 3 columns on phones to 6 columns on reception tablets with color-coded "Full" and "Selected" visual states.
5. `app/src/test/java/com/uniquemakeover/admin/feature/appointments/domain/usecase/GenerateAvailableSlotsUseCaseTest.kt`
   - **Architectural Role:** Automated JVM Unit Test
   - **Why Created:** Verifies slot count calculations, past-time pruning with `FakeTimeProvider`, and clash mapping logic with 100% deterministic assertions.

#### Files Modified:
1. `app/src/main/java/com/uniquemakeover/admin/core/database/dao/AppointmentDao.kt`
   - **Why Modified:** Added indexed SQLite query `countClashingAppointments(date, time)` using composite index `(date, time)` to resolve conflicts in $< 15\text{ ms}$.
```

---

## 6. Definition of Done (DoD) per Vertical Slice

An autonomous agent may only mark a vertical slice as complete and proceed to the next slice when **all eight** criteria are met:

- [ ] **1. Single-Slice Scope:** The implementation covers exclusively the targeted slice and introduces no premature scaffolding for future slices.
- [ ] **2. Pure Domain:** The domain layer contains zero imports of `android.*`, `androidx.*`, or Room annotations.
- [ ] **3. UI Design Compliance:** All UI components strictly use tokens, colors, typography, and responsive Window Size Classes defined in [`desing.md`](file:///home/shunya/Projects/uniqmaleoveradmin/desing.md).
- [ ] **4. Responsive Verification:** Component adapts cleanly on Compact ($<600\text{dp}$), Medium ($600-840\text{dp}$), and Expanded ($\ge 840\text{dp}$) layouts.
- [ ] **5. Automated Tests Green:** Dedicated JVM unit tests (and integration tests if required by slice specification) pass with 100% green status.
- [ ] **6. Invariants Preserved:** Manifest contains zero `INTERNET` permission; Detekt architectural check passes without violations.
- [ ] **7. Documentation Updated:** The corresponding slice specification in `docs/PHASE_X_VERTICAL_SLICES.md` has been updated with the final list of files created/modified.
- [ ] **8. Architectural Rationale Documented:** Every created and modified file has an explicit explanation documenting **why** it was created and its architectural role.

---

## 7. Failure Recovery & Error Diagnostics Protocol

If a slice fails during build or test execution, follow this troubleshooting triage:

1. **Compilation / Unresolved Reference Error:**
   * Verify package import boundaries. Ensure presentation does not import Room entities and domain does not import Android classes.
   * Check Hilt module bindings in `core/di/DatabaseModule.kt` or `RepositoryModule.kt`.
2. **Room / SQLite Crash:**
   * Verify entity primary keys, converters in `Converters.kt`, and composite indices.
   * If testing Room migrations or queries, ensure in-memory database builder uses `.allowMainThreadQueries()` only in test harnesses.
3. **Compose UI / Layout Overflow:**
   * Check `Modifier.weight()` and scrolling containers (`LazyColumn`, `verticalScroll`).
   * Verify adherence to Window Size Classes specified in Section 2.
4. **Detekt Violation:**
   * Check imports in `domain/` classes. Strip any Android framework references into injected interfaces or utility classes in `core/util`.

---
**End of Agent Operating Guidelines (`AGENTS.md`) — Unique Makeover Admin Console**
