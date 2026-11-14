# Unique Makeover — Admin Console (Android)
### Native Android · Fully Offline · Clean Architecture · Single Location (Jalgaon Jamod)

**Unique Makeover Admin Console** is a fully offline, native Android operational management application engineered for the owner/operator of a single boutique luxury parlour in Jalgaon Jamod.

---

## 🏛️ Inviolable Architectural Principles
* **100% Offline (Zero Network):** No `<uses-permission android:name="android.permission.INTERNET" />` in `AndroidManifest.xml`.
* **Clean Architecture:** Strict layering: `presentation` $\rightarrow$ `domain` $\rightarrow$ `data`. Domain contains 0 Android framework imports.
* **Single-Module Feature Packages:** Strict internal feature encapsulation (`com.uniquemakeover.admin.feature.*`).
* **On-Device Storage:** Camera/Gallery photos compressed to 320 px @ 78% JPEG on disk (`filesDir/images/`); Room database stores relative path strings only (ADR-0004).
* **Local UPI QR Codes:** Generated via embedded ZXing Core in memory; zero cloud payment gateway dependency (ADR-0007).
* **Atomic SAF Backups:** Full database JSON export/import via Storage Access Framework with atomic rollback on corruption (ADR-0005).

---

## 🛠️ Tech Stack
* **Language:** Kotlin 2.0.21
* **UI:** Jetpack Compose + Material 3 (Strictly adhering to [`desing.md`](file:///home/shunya/Projects/uniqmaleoveradmin/desing.md))
* **Architecture:** MVI/MVVM with Coroutines & StateFlow
* **DI:** Dagger Hilt
* **Database:** Room SQLite (Write-Ahead Logging enabled)
* **Testing:** JUnit 5, MockK, Turbine, Truth, Robolectric, Compose UI Test

---

## 📖 Specifications & Documentation
* [Architecture Requirements Document (ARD)](file:///home/shunya/Projects/uniqmaleoveradmin/docs/ARD.md)
* [Functional Requirements Document (FRD)](file:///home/shunya/Projects/uniqmaleoveradmin/docs/FRD.md)
* [User Stories Specification](file:///home/shunya/Projects/uniqmaleoveradmin/docs/USER_STORIES.md)
* [Vertical Slice Index Map](file:///home/shunya/Projects/uniqmaleoveradmin/vertical-slice-indexmap.md)
* [Design System Specification](file:///home/shunya/Projects/uniqmaleoveradmin/desing.md)
* [Agent Execution Manual (AGENTS.md)](file:///home/shunya/Projects/uniqmaleoveradmin/AGENTS.md)

---

## 🚀 Build & Verification Commands
```bash
# Run JVM Unit Tests
./gradlew testDebugUnitTest

# Run Detekt Architectural Boundary Checks
./gradlew detekt

# Verify Zero-Network Invariant
grep -rn "INTERNET" app/src/main/AndroidManifest.xml && exit 1
```
