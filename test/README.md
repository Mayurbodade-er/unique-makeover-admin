# Unique Makeover Admin Console — Maestro End-to-End (E2E) Test Suite
### Native Android · 100% Offline · Jalgaon Jamod Luxury Boutique Parlour

This directory contains the automated End-to-End UI test flows for the **Unique Makeover Admin Console**, designed to be executed via [Maestro CLI](https://maestro.mobile.dev/) directly against the connected physical device (**Vivo 1811**, device ID `6d176c27`).

---

## 1. Quick Execution Guide (Connected Mobile Device)

### Prerequisites:
1. Ensure your physical mobile device is attached via USB with USB Debugging enabled.
2. Confirm the device is detected by ADB:
   ```bash
   adb devices
   # Expected output:
   # 6d176c27   device
   ```
3. Ensure Maestro CLI is available in your shell environment:
   ```bash
   export PATH="$HOME/.maestro/bin:$PATH"
   maestro --version
   # Expected output: 2.12.1
   ```
4. Verify the app is installed on the device (or build & install if needed):
   ```bash
   ./gradlew installDebug
   ```

---

## 2. Running the Tests

### Option A: Run the Master End-to-End Suite (Full User Journey)
This runs the complete business flow covering authentication, catalog browsing, booking, dashboard KPIs, status transitions, payment, client CRM notes, reminders, and data backup:
```bash
~/.maestro/bin/maestro test test/master_e2e_suite.yaml
```

### Option B: Run Individual Flow Tests by Vertical Slice
You can execute targeted test flows corresponding to specific vertical slices:

```bash
# 1. Auth PIN Gate & Lockout (Slices #03, #32)
~/.maestro/bin/maestro test test/01_auth_pin_gate.yaml

# 2. Service Catalog CRUD & Seed Services (Slices #02, #14, #15)
~/.maestro/bin/maestro test test/02_services_catalog.yaml

# 3. Dynamic Slots & Multi-Service Booking (Slices #04, #05, #06)
~/.maestro/bin/maestro test test/03_booking_and_slots.yaml

# 4. Real-Time Dashboard KPIs & Live Schedule (Slices #08, #09, #31)
~/.maestro/bin/maestro test test/04_dashboard_kpis_and_schedule.yaml

# 5. Status Lifecycle Transitions & 5s Undo (Slice #10)
~/.maestro/bin/maestro test test/05_status_lifecycle_and_undo.yaml

# 6. In-line Payment Recording & Mode Selection (Slice #11)
~/.maestro/bin/maestro test test/06_payment_recording_and_overpayment.yaml

# 7. Appointments Search, Filter, Reschedule & Delete (Slices #07, #12, #13)
~/.maestro/bin/maestro test test/07_appointments_directory_search_reschedule_delete.yaml

# 8. Clients CRM, Autosaved Notes & Quick Rebooking (Slices #16, #17, #18, #19, #20)
~/.maestro/bin/maestro test test/08_clients_crm_notes_rebooking.yaml

# 9. WhatsApp Tomorrow's Reminders Queue (Slices #21, #22, #23, #24)
~/.maestro/bin/maestro test test/09_whatsapp_reminders_queue.yaml

# 10. Offline ZXing UPI QR Engine & Presets (Slices #25, #26, #27, #28, #29)
~/.maestro/bin/maestro test test/10_offline_upi_qr_engine.yaml

# 11. Salon Branding & Settings Management (Slices #30, #04, #21, #22, #33)
~/.maestro/bin/maestro test test/11_settings_and_branding.yaml

# 12. Full Database JSON Export via SAF (Slice #34)
~/.maestro/bin/maestro test test/12_data_backup_export.yaml
```

### Option C: Interactive UI Inspection via Maestro Studio
Launch the visual interactive inspector in your browser:
```bash
~/.maestro/bin/maestro studio
```

---

## 3. Vertical Slice & Flow Traceability Matrix

| Test File | Vertical Slice References | User Story IDs | Architectural Layer Scope | Key Flows Verified |
|---|---|---|---|---|
| `01_auth_pin_gate.yaml` | **Slice #03**, **Slice #32** | `US-AUTH-01`, `US-AUTH-02` | Presentation, Security Keystore | Cold-start PIN screen, keypad typing, backspace, wrong PIN rejection, master PIN `1234` unlocking |
| `02_services_catalog.yaml` | **Slice #02**, **Slice #14**, **Slice #15** | `US-SVC-01`, `US-SVC-02`, `US-SVC-03` | Room Database, Presentation | 7 seeded services browsing, Add Service FAB, input validation, rate in ₹, catalog card rendering |
| `03_booking_and_slots.yaml` | **Slice #04**, **Slice #05**, **Slice #06** | `US-SET-02`, `US-APT-02`, `US-APT-03` | Domain UseCases, Room `@Transaction` | Client name + 10-digit phone, multi-service select, date chips, dynamic SlotGrid, commit to SQLite |
| `04_dashboard_kpis_and_schedule.yaml` | **Slice #08**, **Slice #09**, **Slice #31** | `US-DASH-01`, `US-DASH-03`, `US-DASH-02` | Room Reactive Flows, Presentation | Appointments / Revenue KPIs, today's chronological schedule cards, actionable alert banners |
| `05_status_lifecycle_and_undo.yaml` | **Slice #10** | `US-APT-05` | State Machine, SnackbarHost | One-tap "Mark Done", transition animation, 5-second transient Undo snackbar, rollback to Booked |
| `06_payment_recording_and_overpayment.yaml` | **Slice #11**, **Slice #26** | `US-APT-07`, `US-UPI-01` | Room Payments, Domain UseCase | Billed / Paid / Due summary, Cash vs UPI mode toggle, inline amount recording, balance settlement |
| `07_appointments_directory_search_reschedule_delete.yaml` | **Slice #07**, **Slice #12**, **Slice #13** | `US-APT-01`, `US-APT-04`, `US-APT-06` | Indexed Room Queries, Dialog | Substring search by name/phone, filter tabs (All, Upcoming, Completed), edit mode, delete guard dialog |
| `08_clients_crm_notes_rebooking.yaml` | **Slice #16**, **Slice #17**, **Slice #18**, **Slice #19**, **Slice #20** | `US-CRM-01`, `US-CRM-02`, `US-CRM-03`, `US-CRM-04`, `US-CRM-05` | SQL Aggregates, 500ms Debounce | Client directory search, lifetime spend & visits, private sensitive notes autosave, 1-tap quick rebook pre-fill |
| `09_whatsapp_reminders_queue.yaml` | **Slice #21**, **Slice #22**, **Slice #23**, **Slice #24** | `US-SET-03`, `US-REM-03`, `US-REM-01`, `US-REM-02` | Android OS Intents (`wa.me`), UseCase | Tomorrow's queue query, reminder status badges, 1-tap WhatsApp dispatch intent, overflow mark not sent |
| `10_offline_upi_qr_engine.yaml` | **Slice #25**, **Slice #26**, **Slice #27**, **Slice #28**, **Slice #29** | `US-UPI-05`, `US-UPI-01`, `US-UPI-02`, `US-UPI-03`, `US-UPI-04` | ZXing Core, MediaStore, FileProvider | Missing UPI alert guard, dynamic 512px QR bitmap, Balance/Advance presets, WhatsApp link, Gallery export & Share |
| `11_settings_and_branding.yaml` | **Slice #30**, **Slice #04**, **Slice #21**, **Slice #22**, **Slice #33** | `US-SET-01`, `US-SET-02`, `US-SET-03`, `US-REM-03`, `US-AUTH-03` | Settings DataStore / Room | Salon branding (shop name, tagline, logo), operations hours & staff CSV, contact & UPI ID, PIN change |
| `12_data_backup_export.yaml` | **Slice #34**, **Slice #36** | `US-DAT-01`, `US-DAT-03` | Storage Access Framework (SAF) | Full database JSON export via `CreateDocument`, schema snapshot, overdue backup indicator |
| `master_e2e_suite.yaml` | **Slices #01 - #34** | All Master User Stories | Complete End-to-End System | Complete end-to-end salon owner workflow executing the full operational and customer lifecycle |

---

## 4. Test Invariants Enforced
1. **Zero Network Traffic:** All tests run without internet connectivity (Airplane Mode compatible).
2. **Deterministic Data:** Tests verify state changes against local Room SQLite database tables.
3. **Responsive Design:** Coordinates and selectors are resilient across phone and tablet breakpoints.
