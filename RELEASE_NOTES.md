# Release Notes — v1.1.0 (2)

**Date:** 2026-08-23
**Track:** Internal → Closed → Production staged 5%

### Highlights
- **Build stability (Phase 0):** Restored Gradle wrapper 8.13, aligned AGP 8.13/Kotlin 2.2.10/KSP 2.2.10-2.0.2, fixed FK orphan & Success race, added INTERNET permission.
- **Architecture (Phase 1):** Decomposed 1611 LOC `EmailApp.kt` → 212 LOC + 9 modules, encapsulated StateFlow, `withTransaction` prepopulate, LIKE escaping, Migration 1→2, Hilt-lite AppContainer, removed Color.kt, HorizontalDivider, DateFormatUtil.
- **Real Email (Phase 2):** `MailRemoteDataSource` + `Fake` sync, `CredentialStore` (EncryptedSharedPrefs), `DraftStore` (DataStore), `SyncWorker` 15m, `EmailRepository.syncFolder`, `AttachmentCache`, `HtmlUtil` (HtmlCompat).
- **Gemini AI (Phase 3):** `GeminiService`/`FakeGeminiService`, `GeminiConfig` (BuildConfig), `AiGuardrail` (redact, opt-in), `EmailApp` summary dialog & `ComposeScreen` smart suggestion, spam triage, semantic ranking.
- **UX Polish (Phase 4):** Locale-aware RTL (ThemePrefs forceRtl), dynamic color toggle, DataStore `UiStateStore` for folder/search, `values`/`values-ar` 60 strings, `PullToRefreshBox`, `baseline-prof.txt`.
- **Security (Phase 5):** `backup_rules`/`data_extraction_rules` exclude DB/creds, `network_security_config` cleartext false, `POST_NOTIFICATIONS` + `NotificationUtil`, `PrivacyCard` with switches.
- **Quality (Phase 6):** 22 tests (ViewModel turbine, Repository in-memory, HtmlUtil, Robolectric, Roborazzi 4 screenshots), CI workflow (assemble, test, lint, roborazzi), `isCrunchPngs` & `contentType`.
- **Release (Phase 7):** Bump `versionCode 2` `versionName 1.1.0`, `rootProject` rename, `isMinifyEnabled`+`shrinkResources` true, `Telemetry` opt-in, store listings `store/`.

### Signing
- Play App Signing; `release` config uses `KEYSTORE_PATH/STORE_PASSWORD/KEY_PASSWORD` env; `debug.keystore` restored from `debug.keystore.base64` for local.

### How to release
```bash
./gradlew assembleRelease
# CI injects env, uploads to Play via fastlane/supply
./gradlew bundleRelease
```
- Data safety declared `store/data_safety.csv` (INTERNET + POST_NOTIFICATIONS optional, no content sharing without Gemini opt-in).
- Telemetry opt-in only, no PII.

### Known backlog (post-1.1)
- FCM push, undo send 5s, swipe multi-select, labels, biometric lock, Wear widget.

**Verified:** `./gradlew assembleDebug` + `testDebugUnitTest` 22/22 + lint + roborazzi on CI.

AI Studio: https://ai.studio/apps/06b2bb56-82f0-45f4-ab00-3a5fa98a2bfe
