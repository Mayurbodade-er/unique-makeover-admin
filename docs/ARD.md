
---

## 1. Architectural Goals & Constraints

| Goal | Derivation from FRD | Architectural Consequence |
|---|---|---|
| 100% offline | §15 NFR | No `INTERNET` permission in manifest; no Retrofit/OkHttp anywhere; all I/O via Room + SAF |
| Admin-only, single actor | §2 Roles | No auth tokens, no sessions; PIN gate only; no multi-tenant scoping |
| < 2 s cold start | §15 | Hilt with lazy bindings, Room WAL, no eager WorkManager boot work |
| 5,000+ appointments performant | §15 | Paging-capable queries + indices (`date`, `phone`, `date+time`) as specified in §13.3 |
| Local-only privacy | §15 | No crash SDKs; ZXing Core (no Play Services) for QR |
| Fast iteration by solo dev | §1 | **Single Gradle module**, strict package boundaries — split to multi-module only when team grows |
| Testability | — | Pure Kotlin domain layer, interfaces at repository boundary, dispatcher injection |

**Deliberate non-goals (v1 ARD):** Multi-module Gradle, feature flagging, Remote Config, Dagger (use Hilt), KMP, Room KMP.

---

## 2. Technology Stack (Locked)

| Concern | Choice | Rationale |
|---|---|---|
| Language | Kotlin 2.x | Coroutines/Flow/Compose native |
| UI | **Jetpack Compose** + Material 3 | FRD allows Compose/XML; Compose wins for Sheet-heavy booking flows |
| Navigation | Navigation-Compose (type-safe, 2.8+) | Single-activity |
| DI | **Hilt** | Compile-time safety, WorkManager/ViewModel integration |
| Persistence | **Room 2.6+** (KSP), WAL enabled | Per §11.1 |
| Serialization | **kotlinx.serialization** | Backup JSON, no reflection, R8 friendly |
| Async | Coroutines + Flow + `StateFlow` | UI streams |
| Background | **WorkManager** (Hilt-Worker) | Daily 8 PM reminder + backup check |
| QR | **ZXing Core** (not `journeyapps`) | §9.3, no Play Services dependency |
| Image loading | Coil-Compose | Photo references, header logo |
| Crypto | AndroidX **Security-Crypto** + Keystore | PIN hash + salt storage |
| Testing | JUnit5 + MockK + Turbine + Truth; Robolectric; Compose UI Test; Hilt Testing | Per §12 test strategy |
| Build | Gradle KTS + version catalog (`libs.versions.toml`) | Reproducibility |

---

## 3. High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│ presentation (per feature)                                  │
│   Screen ──> ViewModel ──> UiState (StateFlow)              │
│      ▲            │                                         │
│      └── events   └── invokes UseCase                       │
├─────────────────────────────────────────────────────────────┤
│ domain (per feature)                                        │
│   UseCase (pure Kotlin, no android.*)                       │
│   Model (domain data classes)                               │
│   Repository interface (feature-local or core-shared)       │
├─────────────────────────────────────────────────────────────┤
│ data (per feature)                                          │
│   RepositoryImpl ──> Dao ──> Room Entity                    │
│   Mapper (Entity ⇄ Domain)                                  │
│   Platform adapter (WhatsApp, ZXing, SAF, ImageStore)       │
├─────────────────────────────────────────────────────────────┤
│ core (cross-cutting)                                        │
│   database · di · ui-theme · common · util · security · nav │
└─────────────────────────────────────────────────────────────┘
```

**Rules enforced by review / Detekt custom rules:**
1. `domain/*` may import only `kotlin.*`, `javax.inject.*`, `kotlinx.coroutines.*`, `kotlinx.serialization.*` — **never `android.*`**.
2. `presentation/*` may not import `androidx.room.*` or any `*.data.*`.
3. Cross-feature imports are forbidden except via `core.domain` shared interfaces (e.g. `SettingsRepository`, `AppointmentRepository` — declared in `core` because they are read by multiple features; see §6).
4. No `data/*` class may be referenced by more than its owning feature.

---

## 4. Gradle & Source Tree (Single-Module Feature-Based)

```
UniqueMakeover/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle/libs.versions.toml
├── detekt.yml
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml              # NO <uses-permission INTERNET>
        │   ├── res/…
        │   └── java/com/uniquemakeover/admin/
        │       ├── UniqueMakeoverApp.kt              (@HiltAndroidApp)
        │       ├── MainActivity.kt                   (@AndroidEntryPoint)
        │       ├── core/                            ← §5
        │       ├── navigation/                      ← §7
        │       └── feature/                         ← §6 (one package per FRD §14 screen)
        ├── test/                                    ← §11 unit tests (JVM)
        └── androidTest/                             ← §12 instrumented tests
```

---

## 5. `core/` Package Map

```
core/
├── database/
│   ├── UniqueMakeoverDatabase.kt               @Database(entities = [Settings, Service, Appointment, ClientNote, Payment])
│   ├── Converters.kt                            (String ↔ List<String> JSON, etc.)
│   ├── DatabaseModule.kt                        @Provides Room, DAOs — with WAL config
│   └── SeedCallback.kt                          RoomDatabase.Callback — seeds services (§6.3) & default PIN (§3.1)
├── di/
│   ├── AppModule.kt                             app-scoped bindings (Clock, Dispatchers, Json)
│   ├── DispatcherModule.kt                      @IoDispatcher, @DefaultDispatcher, @MainDispatcher
│   └── RepositoryModule.kt                      @Binds shared repos (Settings, Appointment, Service)
├── domain/                                      ← shared interfaces used by ≥2 features
│   ├── model/
│   │   ├── Appointment.kt
│   │   ├── Service.kt
│   │   ├── Settings.kt
│   │   ├── ClientNote.kt
│   │   ├── Payment.kt
│   │   ├── AppointmentStatus.kt                 enum { BOOKED, DONE, CANCEL }
│   │   ├── PaymentMode.kt                       enum { CASH, UPI }
│   │   └── Slot.kt
│   ├── repository/
│   │   ├── SettingsRepository.kt                (interface)
│   │   ├── AppointmentRepository.kt             (interface)
│   │   ├── ServiceRepository.kt                 (interface)
│   │   ├── ClientNoteRepository.kt              (interface)
│   │   └── PaymentRepository.kt                 (interface, optional)
│   └── result/
│       └── DataResult.kt                        sealed Success | Error | Failure
├── security/
│   ├── PinHasher.kt                             PBKDF2/HMAC-SHA256 + per-install salt
│   ├── PinLockManager.kt                        @Singleton — hash store, lockout counter, elapsed realtime
│   ├── LockoutPolicy.kt                         pure: failCount + lastFailAt → lockUntilMillis
│   └── BiometricGate.kt                         optional, wraps BiometricPrompt
├── util/
│   ├── DateFormatters.kt                        "YYYY-MM-DD" ↔ LocalDate, 12-hr formatter
│   ├── PhoneSanitizer.kt                        10-digit extraction & validation
│   ├── CurrencyFormatter.kt                     Indian grouping ₹
│   ├── SlotTimeCalculator.kt                    "HH:MM" arithmetic (open+step*i)
│   ├── TimeProvider.kt                          interface + System impl (test seam)
│   └── ImageCompressor.kt                       Bitmap.compress scaling (320/200/900 px rules)
├── common/
│   ├── Constants.kt
│   ├── UiText.kt                                String | StringRes indirection
│   └── ext/FlowExt.kt                           debounce, throttleFirst, combine helpers
└── ui/
    ├── theme/                                   Color, Type, Shape, Theme.kt
    ├── components/
    │   ├── AppScaffold.kt
    │   ├── PrimaryButton.kt
    │   ├── ConfirmDialog.kt
    │   ├── AmountInput.kt
    │   ├── DatePickerField.kt
    │   ├── TimePickerField.kt
    │   ├── EmptyState.kt
    │   └── ErrorBanner.kt
    └── qr/QrImage.kt                            Shared bitmap renderer (used by UPI + future)
```

---

## 6. Feature Packages (per FRD §14)

> **Canonical feature layout** — every feature package mirrors this: `data / domain / presentation`.

### 6.1 `feature/auth` — PIN Lock (§3.1, Screen #1)

```
feature/auth/
├── data/
│   ├── PinRepositoryImpl.kt                     uses PinLockManager + SettingsRepository
│   └── local/                                   (none — no Room access here)
├── domain/
│   ├── model/
│   │   └── PinResult.kt                         sealed: Success | Invalid | Locked(untilMillis)
│   ├── repository/PinRepository.kt
│   └── usecase/
│       ├── VerifyPinUseCase.kt
│       ├── ChangePinUseCase.kt
│       └── GetLockoutStateUseCase.kt
└── presentation/
    ├── PinLockScreen.kt
    ├── PinLockViewModel.kt
    ├── PinLockUiState.kt                        (digitsEntered, error, lockedUntil, shakeTrigger)
    └── components/
        ├── PinKeypad.kt
        └── PinDots.kt
```
**Tests:** `test/feature/auth/domain/usecase/{VerifyPinUseCaseTest, ChangePinUseCaseTest, GetLockoutStateUseCaseTest}.kt`, `test/feature/auth/presentation/PinLockViewModelTest.kt`, `androidTest/feature/auth/PinLockScreenTest.kt`.

---

### 6.2 `feature/dashboard` — KPIs & Live Schedule (§4, Screen #2)

```
feature/dashboard/
├── domain/
│   ├── model/
│   │   ├── DashboardKpis.kt                     (booked, done, cancelled, volume, collected, outstanding)
│   │   ├── AlertBanner.kt                       sealed: WaNotConfigured | UpiNotConfigured | BackupOverdue | OverdueAppointments | RemindersPending
│   │   └── ScheduleItem.kt
│   └── usecase/
│       ├── GetDashboardKpisUseCase.kt
│       ├── GetAlertBannersUseCase.kt
│       └── GetTodayScheduleUseCase.kt
└── presentation/
    ├── DashboardScreen.kt
    ├── DashboardViewModel.kt
    ├── DashboardUiState.kt
    └── components/
        ├── KpiCard.kt
        ├── KpiRow.kt
        ├── AlertBannerCard.kt
        └── ScheduleRowCard.kt                   (row actions delegate to feature/appointments via nav)
```
**Tests:** `GetDashboardKpisUseCaseTest` (pure math), `GetAlertBannersUseCaseTest` (5 branch triggers), `DashboardViewModelTest` (flow combinations), `DashboardScreenTest` (Compose).

---

### 6.3 `feature/appointments` — CRUD, Lifecycle, Payments (§5, §9.5, Screens #3 + #4)

```
feature/appointments/
├── data/
│   ├── local/
│   │   ├── dao/
│   │   │   ├── AppointmentDao.kt                indices as per §13.3; clash queries
│   │   │   └── PaymentDao.kt                    optional
│   │   ├── entity/
│   │   │   ├── AppointmentEntity.kt             mirrors §13.3
│   │   │   └── PaymentEntity.kt                 mirrors §13.5
│   │   └── mapper/
│   │       └── AppointmentMapper.kt
│   └── repository/
│       ├── AppointmentRepositoryImpl.kt         @Transaction on book/edit/markDone
│       └── PaymentRepositoryImpl.kt
├── domain/
│   ├── model/
│   │   ├── BookingDraft.kt                      form state model
│   │   ├── SlotAvailability.kt                  (time, available, reason)
│   │   └── AppointmentFilter.kt                 (tab, dateRange, query)
│   ├── repository/
│   │   ├── AppointmentRepository.kt             (interface, bound in Hilt)
│   │   └── SlotRepository.kt
│   └── usecase/
│       ├── BookAppointmentUseCase.kt            validate + clash re-check + insert
│       ├── EditAppointmentUseCase.kt            re-check against *other* ids
│       ├── MarkAppointmentDoneUseCase.kt        prompts balance handling
│       ├── CancelAppointmentUseCase.kt
│       ├── UndoAppointmentStatusUseCase.kt
│       ├── DeleteAppointmentUseCase.kt          hard delete
│       ├── RecordPaymentUseCase.kt              cash/upi, paid += amount, overpay dialog flag
│       ├── GenerateAvailableSlotsUseCase.kt     uses SettingsRepository + now()
│       ├── ValidateBookingDraftUseCase.kt       pure validation (10-digit phone, etc.)
│       └── GetAppointmentsUseCase.kt            filter/query
└── presentation/
    ├── list/
    │   ├── AppointmentsScreen.kt
    │   ├── AppointmentsViewModel.kt
    │   ├── AppointmentsUiState.kt
    │   └── components/
    │       ├── FilterTabs.kt
    │       ├── AppointmentCard.kt
    │       └── AppointmentActionsMenu.kt
    ├── booking/
    │   ├── BookingSheet.kt
    │   ├── BookingViewModel.kt
    │   ├── BookingUiState.kt
    │   ├── BookingFormEvent.kt
    │   └── components/
    │       ├── ServiceMultiSelect.kt
    │       ├── SlotGrid.kt                   ("Full" badge on unavailable)
    │       ├── StaffDropdown.kt
    │       └── PaymentInputRow.kt
    └── payment/
        ├── RecordPaymentDialog.kt
        └── RecordPaymentViewModel.kt
```
**Tests:**
- `test/feature/appointments/domain/usecase/BookAppointmentUseCaseTest` (clash, past date, validation)
- `EditAppointmentUseCaseTest` (self-exclusion from clash)
- `GenerateAvailableSlotsUseCaseTest` (open/close/step, past-slot pruning for today, cancel slots freed)
- `RecordPaymentUseCaseTest` (overpay flag)
- `ValidateBookingDraftUseCaseTest` (phone regex, required fields)
- `androidTest/feature/appointments/data/AppointmentDaoTest` (in-memory Room, clash SQL)
- `AppointmentRepositoryImplTest` (Robolectric, transaction integrity)
- `androidTest/feature/appointments/presentation/{AppointmentsScreenTest, BookingSheetTest}`

---

### 6.4 `feature/services` — Catalog & Photos (§6, Screen #5)

```
feature/services/
├── data/
│   ├── local/
│   │   ├── dao/ServiceDao.kt
│   │   ├── entity/ServiceEntity.kt              mirrors §13.2
│   │   └── mapper/ServiceMapper.kt
│   ├── repository/ServiceRepositoryImpl.kt
│   └── image/
│       ├── LocalImageStore.kt                   filesDir/images/*.jpg — §11.2 recommendation
│       ├── ImageSourceLauncher.kt               Intent wrapper: Camera / ACTION_PICK
│       └── CompressorDefaults.kt                320 px / 78% per §6.2
├── domain/
│   ├── repository/ServiceRepository.kt
│   └── usecase/
│       ├── GetServicesUseCase.kt
│       ├── AddServiceUseCase.kt
│       ├── UpdateServiceUseCase.kt
│       ├── DeleteServiceUseCase.kt              snapshot-safe per §6.1
│       ├── ResetServicePhotoUseCase.kt
│       └── CompressAndStoreImageUseCase.kt      seams to ImageCompressor
└── presentation/
    ├── ServicesScreen.kt
    ├── ServicesViewModel.kt
    ├── ServicesUiState.kt
    └── components/
        ├── ServiceCard.kt
        ├── ServiceEditorSheet.kt
        └── PhotoPickerField.kt
```
**Tests:** `AddServiceUseCaseTest`, `DeleteServiceUseCaseTest` (existing appointments retain `svc` string), `CompressAndStoreImageUseCaseTest` (uses Robolectric bitmap), `ServiceRepositoryImplTest`, `ServicesScreenTest`.

---

### 6.5 `feature/clients` — CRM & Notes (§8, Screens #6 + #7)

```
feature/clients/
├── data/
│   ├── local/
│   │   ├── dao/
│   │   │   └── ClientNoteDao.kt                 mirrors §13.4
│   │   ├── entity/ClientNoteEntity.kt
│   │   └── mapper/ClientNoteMapper.kt
│   └── repository/
│       ├── ClientNoteRepositoryImpl.kt
│       └── ClientProfileRepositoryImpl.kt       derives profiles by grouping appointments (SQL aggregate)
├── domain/
│   ├── model/
│   │   ├── ClientProfile.kt                     (name, phone, visits, spend, lastVisit, lastService)
│   │   └── ClientHistoryItem.kt
│   ├── repository/
│   │   ├── ClientProfileRepository.kt
│   │   └── ClientNoteRepository.kt
│   └── usecase/
│       ├── GetClientProfilesUseCase.kt          search combines name+phone
│       ├── GetClientHistoryUseCase.kt
│       ├── GetClientNoteUseCase.kt
│       ├── SaveClientNoteUseCase.kt             debounced from VM
│       └── BuildRebookDraftUseCase.kt           pre-fills §8.4
└── presentation/
    ├── list/
    │   ├── ClientsScreen.kt
    │   ├── ClientsViewModel.kt
    │   ├── ClientsUiState.kt
    │   └── components/ClientRow.kt
    └── detail/
        ├── ClientDetailScreen.kt
        ├── ClientDetailViewModel.kt
        ├── ClientDetailUiState.kt
        └── components/
            ├── ClientMetricsHeader.kt
            ├── HistoryList.kt
            └── NoteEditor.kt                  (debounce 500 ms → autosave)
```
**Tests:** `GetClientProfilesUseCaseTest` (grouping/sanitization), `BuildRebookDraftUseCaseTest` (uses last services), `SaveClientNoteUseCaseTest`, `ClientProfileRepositoryImplTest` (Robolectric Room), `ClientDetailScreenTest` (note autosave via Turbine on fake repo).

---

### 6.6 `feature/reminders` — Tomorrow Queue & WhatsApp (§7, Screen #8)

```
feature/reminders/
├── data/
│   ├── repository/RemindersRepositoryImpl.kt    delegates to AppointmentRepository + SettingsRepository
│   └── platform/
│       └── WhatsAppLauncher.kt                  Intent.ACTION_VIEW wa.me — no INTERNET perm needed
├── domain/
│   ├── model/
│   │   ├── ReminderQueueItem.kt                 (appointment + reminded: Boolean)
│   │   └── ReminderMessage.kt
│   ├── repository/RemindersRepository.kt
│   └── usecase/
│       ├── GetTomorrowQueueUseCase.kt           date = tomorrow, status = booked
│       ├── FormatReminderTemplateUseCase.kt     placeholders per §7.3
│       ├── LaunchWhatsAppReminderUseCase.kt
│       ├── MarkRemindedUseCase.kt               sets reminded = tomorrowDate
│       └── UndoMarkRemindedUseCase.kt
└── presentation/
    ├── RemindersScreen.kt
    ├── RemindersViewModel.kt
    ├── RemindersUiState.kt
    └── components/ReminderRow.kt
```
**Tests:** `FormatReminderTemplateUseCaseTest` (all 8 placeholders, missing values, HTML/URL-encoding), `GetTomorrowQueueUseCaseTest` (uses injected `TimeProvider`), `LaunchWhatsAppReminderUseCaseTest` (verify Intent shape via Shadow), `RemindersViewModelTest` (optimistic reminded + Undo).

---

### 6.7 `feature/upi` — Offline QR & Payment Requests (§9, Screen #9)

```
feature/upi/
├── data/
│   └── platform/
│       ├── ZxingQrEncoder.kt                    com.google.zxing:core, no network
│       ├── UpiUriBuilderImpl.kt                 pure string building (also unit-tested in domain)
│       ├── QrPngExporter.kt                     MediaStore / SAF save
│       └── QrShareIntentBuilder.kt              FileProvider share sheet
├── domain/
│   ├── model/
│   │   ├── UpiRequest.kt                        (upiId, payeeName, amount, note)
│   │   └── PaymentPreset.kt                     sealed: Balance | Advance30 | Full | Custom
│   ├── repository/
│   │   └── QrRepository.kt                      interface: encode(uri, sizePx): Bitmap
│   └── usecase/
│       ├── BuildUpiUriUseCase.kt                §9.1 format
│       ├── ComputePresetAmountUseCase.kt        30% round rule
│       ├── GenerateQrBitmapUseCase.kt
│       ├── SaveQrPngUseCase.kt
│       ├── ShareQrUseCase.kt
│       └── BuildUpiWhatsAppMessageUseCase.kt    §9.4
└── presentation/
    ├── UpiQrScreen.kt
    ├── UpiQrViewModel.kt
    ├── UpiQrUiState.kt
    └── components/
        ├── PresetSelector.kt
        ├── UpiBlockingBanner.kt                 settings.upi empty → deep link
        └── QrCanvas.kt                          screen-brightness boost
```
**Tests:** `BuildUpiUriUseCaseTest` (exact string, 2-decimal), `ComputePresetAmountUseCaseTest` (rounding, edge ₹0, ₹9999), `GenerateQrBitmapUseCaseTest` (Robolectric, non-null 512×512), `SaveQrPngUseCaseTest` (ShadowContentResolver), `UpiQrScreenTest` (blocking banner rendering).

---

### 6.8 `feature/settings` — Branding, Hours, Backup (§10–§11, Screen #10)

```
feature/settings/
├── data/
│   ├── local/
│   │   ├── dao/SettingsDao.kt
│   │   ├── entity/SettingsEntity.kt             mirrors §13.1
│   │   └── mapper/SettingsMapper.kt
│   ├── repository/SettingsRepositoryImpl.kt
│   ├── backup/
│   │   ├── BackupSnapshot.kt                    @Serializable mirror of §11.3
│   │   ├── BackupSerializer.kt                  kotlinx.serialization (version=2)
│   │   ├── BackupWriter.kt                      SAF ACTION_CREATE_DOCUMENT
│   │   ├── BackupReader.kt                      SAF ACTION_OPEN_DOCUMENT + schema validate
│   │   ├── BackupDiff.kt                        counts summary before overwrite (§11.4)
│   │   └── BackupFileName.kt                    UniqueMakeover_backup_YYYY-MM-DD.json
│   └── image/
│       ├── LogoImageStore.kt                    200 px, circular crop (§10.1)
│       └── WallpaperImageStore.kt               900 px @ 72% (§10.1)
├── domain/
│   ├── repository/SettingsRepository.kt         (interface, shared in core/domain)
│   └── usecase/
│       ├── GetSettingsUseCase.kt
│       ├── UpdateBrandingUseCase.kt
│       ├── UpdateOperationsUseCase.kt           open/close/slot/staff parsing
│       ├── UpdateCommunicationUseCase.kt
│       ├── UpdateLogoUseCase.kt
│       ├── ExportBackupUseCase.kt               builds snapshot → writer
│       ├── ImportBackupUseCase.kt               validate → diff → overwrite in @Transaction
│       ├── GetLastBackupInfoUseCase.kt
│       └── ClearAllDataUseCase.kt               optional, double confirm
└── presentation/
    ├── SettingsScreen.kt
    ├── SettingsViewModel.kt
    ├── SettingsUiState.kt
    └── sections/
        ├── BrandingSection.kt
        ├── AccessControlSection.kt               (delegates change PIN → feature/auth)
        ├── OperationsSection.kt
        ├── CommunicationSection.kt
        └── DataManagementSection.kt
```
**Tests:** `UpdateOperationsUseCaseTest` (staff CSV parsing, invalid HH:MM rejection), `ExportBackupUseCaseTest` (schema v2 JSON shape, kotlinx snapshot), `ImportBackupUseCaseTest` (valid / invalid version / missing keys → no writes), `SettingsRepositoryImplTest`, `SettingsScreenTest`.

---

### 6.9 `feature/notifications` — Local WorkManager (§12, optional)

```
feature/notifications/
├── data/
│   └── worker/
│       ├── DailyReminderWorker.kt               @HiltWorker — 8 PM, N tomorrow
│       └── BackupOverdueWorker.kt
├── domain/
│   └── usecase/
│       ├── ScheduleDailyReminderUseCase.kt
│       └── ScheduleBackupCheckUseCase.kt
└── presentation/                                 (none — background only)
```
**Tests:** `androidTest/feature/notifications/DailyReminderWorkerTest` (WorkerFactory + test driver), `ScheduleDailyReminderUseCaseTest` (uses injected `WorkManager` mock).

---

## 7. Navigation

```
navigation/
├── UniqueMakeoverNavHost.kt                    single NavHost, guarded by PIN route
├── Routes.kt                                   @Serializable route objects (type-safe nav)
└── DeepLinks.kt                                banner → target mapping (dashboard → appointments/settings/reminders)
```

Route graph (top-level, per FRD §14):

```
pin ──(verified)──► dashboard ──┬── appointments ──► bookingSheet (modal)
                                │                └── recordPayment (dialog)
                                ├── services ──► serviceEditor (modal)
                                ├── clients ──► clientDetail ──► rebook → bookingSheet
                                ├── reminders
                                ├── upiQr (args: appointmentId?, preset?)
                                └── settings ──► changePin (auth feature)
```

Deep links from dashboard alert banners are **internal** `NavDestination` args, not external intents (offline, admin-only).

---

## 8. Dependency Injection Graph

```
AppModule
├── @Provides Json (ignoreUnknownKeys = true)
├── @Provides Clock / TimeProvider (System impl; test replaces)
└── @Provides ImageCompressor

DatabaseModule
├── @Singleton UniqueMakeoverDatabase (WAL enabled, SeedCallback)
├── SettingsDao, ServiceDao, AppointmentDao, ClientNoteDao, PaymentDao
└── @Binds SettingsRepository, AppointmentRepository,
          ServiceRepository, ClientNoteRepository, PaymentRepository

RepositoryModule
└── @Binds feature-local repos → their Impls

Per-feature @Module (presentation)
└── (mostly constructor-injected ViewModels; no modules needed)

WorkerModule (Hilt)
└── DailyReminderWorker, BackupOverdueWorker
```

**Rule:** Only `core/domain/repository` interfaces are bound in `core/di`. Feature-local repos (e.g. `ClientProfileRepository`, `QrRepository`) are bound inside their feature's `data/di/FeatureModule.kt`.

---

## 9. Data Flow Examples (Traceability)

### 9.1 Book Appointment (§5.2)
```
BookingViewModel.submit()
  → ValidateBookingDraftUseCase(draft)          // pure
  → BookAppointmentUseCase(draft)
       ├─ SlotRepository.isClashFree(date,time, excludeId=null)   // Room clash query
       ├─ AppointmentRepository.insert(entity)  // @Transaction
       └─ returns Result.success
  → emits BookingUiState.Saved → nav pops sheet
  → DashboardViewModel collects Room Flow → KPIs update (reactive)
```

### 9.2 Send Reminder (§7.2)
```
RemindersViewModel.remind(item)
  → FormatReminderTemplateUseCase(item, settings)       // pure, 8 placeholders
  → LaunchWhatsAppReminderUseCase(phone, message)        // Intent; no INTERNET perm
  → MarkRemindedUseCase(item.id, tomorrowDate)           // Room write
  → UiState shows snackbar + Undo → UndoMarkRemindedUseCase on tap
```

### 9.3 UPI QR (§9.1–9.3)
```
UpiQrViewModel.selectPreset(Balance)
  → ComputePresetAmountUseCase(rate, paid)      // pure
  → BuildUpiUriUseCase(upiId, shopName, amount, note)   // pure string
  → GenerateQrBitmapUseCase(uri) → ZxingQrEncoder.encode(uri, 512)
  → UiState.qrBitmap rendered by QrCanvas (no network at any point)
```

---

## 10. Cross-Cutting Concerns

| Concern | Implementation |
|---|---|
| **PIN security** | `PinLockManager` stores `PBKDF2WithHmacSHA256(hash, salt, iter=100k)` in `EncryptedSharedPreferences` (Keystore-backed). FRD §13.1 places hash in `Settings` — ARD keeps the *schema field* but the value is the salt+hash pair split into `adminPinHash` and a companion encrypted pref; document deviation in `docs/adr/0003-pin-storage.md`. |
| **Lockout** | `LockoutPolicy` = pure function over `(failCount, lastFailElapsedRealtime)`; state persists in `DataStore<Preferences>`. |
| **Backup atomicity** | `ImportBackupUseCase` runs in a single `RoomDatabase.withTransaction { deleteAll(); insertAll() }` after full schema validation. Failure → rollback, no data lost. |
| **Image storage** | Files in `filesDir/images/{uuid}.jpg`; DB stores relative path only (§11.2 recommended). Backup JSON stores base64 only when file missing (fallback) — documented. |
| **Offline invariant** | Manifest has **no** `INTERNET`. CI step: `grep -r "INTERNET" AndroidManifest.xml → fail`. |
| **Clock** | All `now()` calls go through `TimeProvider` (injectable) — required for deterministic slot & reminder tests. |
| **Dispatcher injection** | `@IoDispatcher`, `@DefaultDispatcher` qualifiers; all `withContext` uses injected dispatcher; ViewModels use `viewModelScope` + `TestDispatcher` in tests. |
| **Error surfacing** | `DataResult<T>` sealed → ViewModel maps to `UiText` (never raw exceptions to UI). |
| **Schema migration** | Room `exportSchema = true`, `schemas/` committed, `Migration` per version bump; ARD requires a `MigrationTest`. |
| **R8** | kotlinx.serialization + Room already R8-safe; add keep rules for `@Serializable` backup DTOs only. |
| **Detekt** | Custom rules: no `android.*` in `domain/`, no `androidx.room.*` in `presentation/`, max function length 40, no `!!`. |

---

## 11. Test Strategy — File Mapping

> **Naming:** `<ClassUnderTest>Test.kt` for JVM unit; `<Screen>Test.kt` for Compose; `<Dao>Test.kt` for Room.

### 11.1 JVM Unit Tests (`app/src/test/java/…`)

```
test/java/com/uniquemakeover/admin/
├── core/
│   ├── util/
│   │   ├── PhoneSanitizerTest.kt
│   │   ├── SlotTimeCalculatorTest.kt
│   │   ├── CurrencyFormatterTest.kt
│   │   ├── DateFormattersTest.kt
│   │   └── ImageCompressorTest.kt                     (Robolectric bitmap)
│   ├── security/
│   │   ├── PinHasherTest.kt                           (salt differs; verify true/false)
│   │   └── LockoutPolicyTest.kt                       (5 fails → 30 s; boundary elapsed)
│   └── common/ext/FlowExtTest.kt
│
├── feature/auth/
│   ├── domain/usecase/
│   │   ├── VerifyPinUseCaseTest.kt
│   │   ├── ChangePinUseCaseTest.kt
│   │   └── GetLockoutStateUseCaseTest.kt
│   └── presentation/PinLockViewModelTest.kt
│
├── feature/dashboard/
│   ├── domain/usecase/
│   │   ├── GetDashboardKpisUseCaseTest.kt
│   │   ├── GetAlertBannersUseCaseTest.kt
│   │   └── GetTodayScheduleUseCaseTest.kt
│   └── presentation/DashboardViewModelTest.kt
│
├── feature/appointments/
│   ├── domain/usecase/
│   │   ├── ValidateBookingDraftUseCaseTest.kt
│   │   ├── GenerateAvailableSlotsUseCaseTest.kt
│   │   ├── BookAppointmentUseCaseTest.kt
│   │   ├── EditAppointmentUseCaseTest.kt
│   │   ├── MarkAppointmentDoneUseCaseTest.kt
│   │   ├── CancelAppointmentUseCaseTest.kt
│   │   ├── UndoAppointmentStatusUseCaseTest.kt
│   │   ├── DeleteAppointmentUseCaseTest.kt
│   │   ├── RecordPaymentUseCaseTest.kt
│   │   └── GetAppointmentsUseCaseTest.kt
│   └── presentation/
│       ├── list/AppointmentsViewModelTest.kt
│       ├── booking/BookingViewModelTest.kt
│       └── payment/RecordPaymentViewModelTest.kt
│
├── feature/services/
│   ├── domain/usecase/
│   │   ├── AddServiceUseCaseTest.kt
│   │   ├── UpdateServiceUseCaseTest.kt
│   │   ├── DeleteServiceUseCaseTest.kt
│   │   ├── ResetServicePhotoUseCaseTest.kt
│   │   └── CompressAndStoreImageUseCaseTest.kt
│   └── presentation/ServicesViewModelTest.kt
│
├── feature/clients/
│   ├── domain/usecase/
│   │   ├── GetClientProfilesUseCaseTest.kt
│   │   ├── GetClientHistoryUseCaseTest.kt
│   │   ├── SaveClientNoteUseCaseTest.kt
│   │   └── BuildRebookDraftUseCaseTest.kt
│   └── presentation/
│       ├── list/ClientsViewModelTest.kt
│       └── detail/ClientDetailViewModelTest.kt
│
├── feature/reminders/
│   ├── domain/usecase/
│   │   ├── GetTomorrowQueueUseCaseTest.kt
│   │   ├── FormatReminderTemplateUseCaseTest.kt
│   │   ├── LaunchWhatsAppReminderUseCaseTest.kt
│   │   ├── MarkRemindedUseCaseTest.kt
│   │   └── UndoMarkRemindedUseCaseTest.kt
│   └── presentation/RemindersViewModelTest.kt
│
├── feature/upi/
│   ├── domain/usecase/
│   │   ├── BuildUpiUriUseCaseTest.kt
│   │   ├── ComputePresetAmountUseCaseTest.kt
│   │   ├── GenerateQrBitmapUseCaseTest.kt
│   │   ├── SaveQrPngUseCaseTest.kt
│   │   ├── ShareQrUseCaseTest.kt
│   │   └── BuildUpiWhatsAppMessageUseCaseTest.kt
│   └── presentation/UpiQrViewModelTest.kt
│
├── feature/settings/
│   ├── domain/usecase/
│   │   ├── GetSettingsUseCaseTest.kt
│   │   ├── UpdateBrandingUseCaseTest.kt
│   │   ├── UpdateOperationsUseCaseTest.kt
│   │   ├── UpdateCommunicationUseCaseTest.kt
│   │   ├── ExportBackupUseCaseTest.kt
│   │   ├── ImportBackupUseCaseTest.kt
│   │   ├── GetLastBackupInfoUseCaseTest.kt
│   │   └── ClearAllDataUseCaseTest.kt
│   └── presentation/SettingsViewModelTest.kt
│
└── feature/notifications/
    └── domain/usecase/ScheduleDailyReminderUseCaseTest.kt
```

**Shared test infrastructure:**

```
test/java/com/uniquemakeover/admin/testutil/
├── MainDispatcherRule.kt                       (StandardTestDispatcher)
├── FakeTimeProvider.kt
├── TestData.kt                                 (factory for appointments/services/settings)
├── FakeSettingsRepository.kt
├── FakeAppointmentRepository.kt
├── FakeServiceRepository.kt
├── FakeClientNoteRepository.kt
├── FakeQrRepository.kt
├── TurbineExt.kt
└── CoroutineTestRule.kt
```

### 11.2 Instrumented / Robolectric Tests (`app/src/androidTest/java/…`)

```
androidTest/java/com/uniquemakeover/admin/
├── core/
│   ├── database/
│   │   ├── UniqueMakeoverDatabaseTest.kt        (open, WAL flag, SeedCallback)
│   │   ├── MigrationTest.kt                     (schema→schema)
│   │   └── ConverterTest.kt
│   └── security/PinLockManagerInstrumentedTest.kt (EncryptedSharedPreferences real)
│
├── feature/appointments/
│   ├── data/
│   │   ├── AppointmentDaoTest.kt                (clash query, indices exist, date range)
│   │   └── PaymentDaoTest.kt
│   ├── repository/AppointmentRepositoryImplTest.kt (@Transaction integrity, concurrent writes)
│   └── presentation/
│       ├── AppointmentsScreenTest.kt
│       └── BookingSheetTest.kt                  (slot grid disables "Full", date validation)
│
├── feature/services/
│   ├── data/ServiceDaoTest.kt
│   ├── repository/ServiceRepositoryImplTest.kt
│   └── presentation/ServicesScreenTest.kt
│
├── feature/clients/
│   ├── data/ClientNoteDaoTest.kt
│   ├── repository/ClientProfileRepositoryImplTest.kt (SQL GROUP BY aggregates)
│   └── presentation/ClientDetailScreenTest.kt        (note autosave debounce)
│
├── feature/reminders/
│   ├── data/WhatsAppLauncherInstrumentedTest.kt      (Intent captured, wa.me shape)
│   └── presentation/RemindersScreenTest.kt
│
├── feature/upi/
│   ├── data/ZxingQrEncoderTest.kt                    (bitmap dimensions, deterministic seed)
│   ├── data/QrPngExporterInstrumentedTest.kt
│   └── presentation/UpiQrScreenTest.kt               (blocking banner when upi empty)
│
├── feature/settings/
│   ├── data/
│   │   ├── SettingsDaoTest.kt                        (singleton enforcement)
│   │   ├── BackupSerializerTest.kt                   (schema v2 golden JSON)
│   │   └── BackupRoundTripTest.kt                    (export→import→equality)
│   └── presentation/SettingsScreenTest.kt
│
├── feature/auth/presentation/PinLockScreenTest.kt    (keypad, shake, lockout message)
├── feature/dashboard/presentation/DashboardScreenTest.kt
│
├── navigation/NavHostTest.kt                         (route wiring, PIN guard)
├── e2e/BookingFlowE2ETest.kt                         (PIN → book → dashboard KPI reflect)
├── e2e/OfflineInvariantTest.kt                       (assert no INTERNET permission via PackageManager)
└── testutil/
    ├── HiltTestRunner.kt
    ├── InMemoryDatabaseRule.kt
    └── ComposeRuleExt.kt
```

### 11.3 Test matrix vs FRD sections

| FRD § | Coverage source |
|---|---|
| §3.1 PIN lockout | `LockoutPolicyTest`, `PinLockViewModelTest`, `PinLockScreenTest` |
| §4.1 KPIs | `GetDashboardKpisUseCaseTest` |
| §4.2 Banners (5) | `GetAlertBannersUseCaseTest` (one `@Test` per trigger) |
| §5.3 Slot & clash | `GenerateAvailableSlotsUseCaseTest`, `BookAppointmentUseCaseTest`, `AppointmentDaoTest` |
| §5.4 Lifecycle | `MarkAppointmentDoneUseCaseTest`, `CancelAppointmentUseCaseTest`, `UndoAppointmentStatusUseCaseTest` |
| §5.5 Edit self-exclusion | `EditAppointmentUseCaseTest` |
| §6.2 Image compression | `CompressAndStoreImageUseCaseTest` |
| §7.3 Placeholders (8) | `FormatReminderTemplateUseCaseTest` (8 assertions) |
| §8.1 Grouping | `GetClientProfilesUseCaseTest` |
| §8.4 Rebook | `BuildRebookDraftUseCaseTest` |
| §9.1 UPI URI | `BuildUpiUriUseCaseTest` (exact string match) |
| §9.2 Presets | `ComputePresetAmountUseCaseTest` |
| §9.5 Payment cap | `RecordPaymentUseCaseTest` |
| §10.3 Staff CSV | `UpdateOperationsUseCaseTest` |
| §11.3–11.4 Backup | `BackupSerializerTest`, `ImportBackupUseCaseTest`, `BackupRoundTripTest` |
| §15 Offline | `OfflineInvariantTest` + CI grep |

---

## 12. Build & CI Additions

```kotlin
// app/build.gradle.kts (excerpt)
android {
    defaultConfig {
        minSdk = 26; targetSdk = libs.versions.targetSdk.get().toInt()
        testInstrumentationRunner = "com.uniquemakeover.admin.testutil.HiltTestRunner"
    }
    buildFeatures { compose = true }
    testOptions { unitTests.isIncludeAndroidResources = true }  // Robolectric
}
dependencies {
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.hilt.android); ksp(libs.hilt.compiler)
    implementation(libs.zxing.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.security.crypto)
    testImplementation(libs.junit5.api)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.truth)
    testImplementation(libs.robolectric)
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.hilt.android.testing)
}
```

**CI gates (GitHub Actions):**
1. `./gradlew detekt` (custom rules: no `INTERNET`, no `android.*` in domain, no Room in presentation)
2. `grep -rn "INTERNET" app/src/main/AndroidManifest.xml && exit 1`
3. `./gradlew testDebugUnitTest`
4. `./gradlew connectedDebugAndroidTest` (or Firebase Test Lab / Gradle Managed Devices)
5. `./gradlew lintDebug` + `assembleRelease`

---

## 13. Architecture Decision Records (to be authored alongside code)

| ADR | Topic |
|---|---|
| `0001-single-module-feature-packages.md` | Why single Gradle module + strict package rules |
| `0002-compose-over-xml.md` | Compose for sheet-heavy booking |
| `0003-pin-storage.md` | Deviation from FRD §13.1: hash+salt in EncryptedSharedPreferences; `adminPinHash` field stores salt marker only |
| `0004-image-paths-not-blobs.md` | Files on disk, paths in DB (§11.2 recommendation, adopted) |
| `0005-backup-atomicity.md` | Single-transaction import, diff preview, schema version guard |
| `0006-time-provider.md` | Injectable `TimeProvider` required for slot/reminder determinism |
| `0007-zxing-core-over-play-services.md` | No Play Services dependency |

---

## 14. Package-Level Import Rules (Detekt config summary)

| Package | Allowed `android.*` | Allowed `androidx.room.*` | Allowed cross-feature |
|---|---|---|---|
| `core.domain` | ❌ | ❌ | ❌ |
| `core.data` | ✅ | ✅ | ❌ |
| `core.database` | ✅ | ✅ | ❌ |
| `core.ui` | ✅ | ❌ | ❌ |
| `feature.*.domain` | ❌ | ❌ | only `core.domain` |
| `feature.*.data` | ✅ | ✅ | only `core.*` |
| `feature.*.presentation` | ✅ | ❌ | only own domain + `core.domain`, `core.ui` |

Violations fail CI. This is what enforces the feature boundary without Gradle module overhead — the same trade the solo-dev team of the `Now in Android` sample makes at the package level before splitting.

---

**End of ARD v1.0 — Single-Module Feature-Based Clean Architecture, Fully Offline**

Would you like me to next produce: (a) the **Hilt module files** ready to paste, (b) the **Room DAO interfaces** with clash/aggregate queries, or (c) the **Detekt custom rules YAML** that enforce the import boundaries in §14?
