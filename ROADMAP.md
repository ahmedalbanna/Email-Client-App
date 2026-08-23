# ROADMAP — البريد الإلكتروني (Email Client App)

> Multi-account smart email client — Arabic-first, offline Room persistence, Material 3, Jetpack Compose.
> Source review baseline: `app/build.gradle.kts:11` compileSdk 36, `gradle/libs.versions.toml:11` Kotlin 2.2.10, `app/src/main/java/com/example/ui/EmailApp.kt` 1611 lines monolith.
> Metadata: `metadata.json:2-5` — `MAJOR_CAPABILITY_SERVER_SIDE_GEMINI_API` declared but not wired.

---

## 0. Vision & Non-Goals

**Vision:** Production-grade Android email client rivaling Gmail/Outlook on Arabic UX, with real IMAP/SMTP, encrypted offline store, and Gemini-assisted triage (summarize, draft, spam detect).

**Non-goals (v1.x):** Calendar/Contacts sync, enterprise MDM, iOS/desktop.

**Design principles:**
1. Offline-first — Room is source of truth (`app/src/main/java/com/example/data/EmailDatabase.kt:13`).
2. Arabic excellence — RTL correctness without forcing `LayoutDirection.Rtl` globally (`app/src/main/java/com/example/ui/EmailApp.kt:57`).
3. Minimal APK — remove dead deps (`app/build.gradle.kts:72-96` retrofit/moshi/coil/firebase unused).

---

## 1. Current State — Gap Analysis

| Area | Status | Evidence |
|------|--------|----------|
| Build | **Broken** — no `gradle/wrapper/`, Kotlin/KSP mismatch | `gradle/` has only `libs.versions.toml`; `libs.versions.toml:11` Kotlin 2.2.10 vs `13` KSP 2.3.5; `composeBom 2024.09.00` stale |
| Data | Mock-only, FK bug, destructive migrations | `EmailRepository.kt:207-222` hardcodes `emailId=1,3`; `EmailDatabase.kt:29` `fallbackToDestructiveMigration()` |
| Domain | No real network, no creds storage | `EmailViewModel.kt:321` `delay(1200)` handshake; `AndroidManifest.xml` no `INTERNET` |
| UI | Monolith, hardcoded strings, icon misuse | `EmailApp.kt` 1611 lines, 9 composables; strings inlined not `res/values/strings.xml` |
| AI | Declared but never called | `.env.example:5` `GEMINI_API_KEY` + `secrets` plugin `app/build.gradle.kts:63` unused |
| Tests | Placeholders, one failing | `ExampleInstrumentedTest.kt:19` wrong `applicationId`; `GreetingScreenshotTest.kt:24` tests dummy `Text` |

---

## 2. Roadmap Overview

```
Phase 0 (Week 0-1)  Critical Fixes & Build Stability — make it build/run/test green
Phase 1 (Week 1-3)  Architecture & Code Health — de-monolith, DI, clean data layer
Phase 2 (Week 3-6)  Real Email — IMAP/SMTP, sync engine, attachments
Phase 3 (Week 6-8)  Gemini AI — summarize, smart reply, spam, search
Phase 4 (Week 8-10) UX Polish — Arabic typography, adaptive, a11y, nav
Phase 5 (Week 10-11) Security & Privacy — encryption, backup rules, compliance
Phase 6 (Week 11-12) Quality — testing pyramid, CI/CD, perf
Phase  7 (Week 12+)  Release & Iterate — beta, telemetry, backlog
```

Each phase has **Exit Criteria** — do not advance until green.

---

## 3. Phase 0 — Critical Fixes & Build Stability

**Goal:** `./gradlew assembleDebug` + `testDebugUnitTest` green on clean clone. Timebox: 3-5 days.

- [ ] **0.1 Restore Gradle Wrapper**
  - `gradle wrapper --gradle-version 8.11.1 --distribution-type bin`
  - Commit `gradle/wrapper/gradle-wrapper.jar`, `gradle-wrapper.properties`, `gradlew`, `gradlew.bat`
  - Verify `settings.gradle.kts:15` foojay resolver + `gradle.properties:21` config-cache still valid

- [ ] **0.2 Align toolchain**
  - Option A (recommended): `Kotlin 2.2.10` → `KSP 2.2.10-2.0.2`, `composeBom 2025.04.00` (Compose 1.8+), `AGP 8.9.1` (stable) — OR keep `AGP 9.1.1` if intentionally canary and pin JDK 17.
  - Update `gradle/libs.versions.toml:1-16` accordingly. Document in `README.md:13`.
  - Remove unused catalog entries: `converterMoshi`, `okhttp`, `coil`, `firebaseBom` unless Phase 3 needs them.

- [ ] **0.3 Fix account-creation race**
  - `app/src/main/java/com/example/viewmodel/EmailViewModel.kt:364-369`: emit `Success` as one-shot event (`Channel`/`SharedFlow`) or delay `Idle` reset until UI consumes. Currently `Success` → immediate `Idle` swallows `LaunchedEffect` `EmailApp.kt:1355`.

- [ ] **0.4 Fix attachment FK**
  - `EmailRepository.kt:207-223`: `val ids = emailDao.insertEmails(listOf(...))` then map `ids` to `AttachmentEntity.emailId`. Add `ForeignKey(entity=EmailEntity::class, onDelete=CASCADE)` in `EmailEntities.kt:36` + index. Add migration test.

- [ ] **0.5 Fix manifest & test**
  - `AndroidManifest.xml:2`: add `<uses-permission android:name="android.permission.INTERNET"/>` (for Phase 2; add now guarded).
  - `ExampleInstrumentedTest.kt:19`: assert `com.aistudio.emailclient.armlq` (`app/build.gradle.kts:14`).
  - Remove committed `debug.keystore.base64` or move to CI secrets; update `.gitignore:18`.

**Exit criteria:** CI job `assembleDebug + test` passes; fresh clone builds without Android Studio “fix incompatibilities”.

---

## 4. Phase 1 — Architecture & Code Health

**Goal:** Files <300 LOC, ViewModel testable, DB migrations safe. Timebox: 2 weeks.

- [ ] **1.1 Decompose `EmailApp.kt` (1611 LOC)**
  ```
  app/src/main/java/com/example/ui/
    EmailApp.kt              // only Scaffold + nav host ( <150 LOC )
    screens/InboxScreen.kt
    screens/DetailScreen.kt
    screens/ComposeScreen.kt
    screens/AddAccountScreen.kt
    components/DrawerContent.kt   // from EmailApp.kt:245
    components/EmailItem.kt       // from 623
    components/TopAppBarSearch.kt // from 433
    components/AttachmentRow.kt
    theme/ + nav/
  ```
  - Extract `Screen` sealed class `EmailApp.kt:44` to `ui/navigation/Screen.kt` or adopt `androidx.navigation.compose` already in `libs.versions.toml:58`.

- [ ] **1.2 State hoisting & encapsulation**
  - `EmailViewModel.kt:98-109` `var MutableStateFlow` → `private MutableStateFlow` + `val composeTo: StateFlow` + `fun onToChange(v:String)`.
  - UI stops doing `viewModel.composeTo.value = it` (`EmailApp.kt:1211`).
  - Hoist per-attachment download state `EmailApp.kt:985-990` (`remember isDownloading`) to VM `Map<Int, DownloadUiState>` so scroll does not reset.

- [ ] **1.3 Data layer hardening**
  - Replace `fallbackToDestructiveMigration()` `EmailDatabase.kt:29` with `Room` `autoMigrations` or manual `Migration(1,2)`.
  - Wrap `prepopulateDatabaseIfEmpty()` `EmailRepository.kt:71` in `@Transaction`; use `first()` not second collector `EmailViewModel.kt:43` (remove duplicate `collect` inside `viewModelScope.launch`).
  - SQL: escape `LIKE` wildcards `EmailDao.kt:63` / `EmailRepository.kt:27` (`%` `_` `\`), or migrate to FTS4 table `emails_fts` for Arabic search.

- [ ] **1.4 DI**
  - Introduce Hilt or manual `AppContainer` + `ViewModelFactory`. `MainActivity.kt:22` `viewModel()` currently uses default factory — inject `EmailRepository` for tests (`EmailDatabase.getDatabase()` `EmailDatabase.kt:22` is hard singleton).

- [ ] **1.5 Cleanup**
  - Delete `Color.kt:5` unused template colors; migrate `Divider` → `HorizontalDivider`.
  - Replace `SimpleDateFormat` per-recomposition `EmailApp.kt:632` with `remember { DateTimeFormatter }` or `java.time` + `remember`.
  - Fix icon mapping: `Icons.AutoMirrored.Filled.Reply/ Send/AttachFile` vs `Icons.Default.Share` misuse `EmailApp.kt:742,831,1302`.

**Exit criteria:** `detekt`/`ktlint` clean, no file >350 LOC, `EmailViewModelTest` covers folder switch/search/star without Android.

---

## 5. Phase 2 — Real Email (IMAP/SMTP)

**Goal:** Add real account, sync inbox, send mail. Timebox: 3 weeks. Depends on 0+1.

- [ ] **2.1 Auth & credential storage**
  - OAuth2 first: Gmail (Google Identity) + Outlook (MSAL). App password fallback for Yahoo/custom.
  - Store tokens in `EncryptedSharedPreferences` / `DataStore` + `androidx.security:security-crypto`.
  - Extend `AccountEntity:6` with `authType`, `refreshToken` (encrypted), not plaintext server strings. Keep `colorHex:15` for UI.

- [ ] **2.2 Protocol layer**
  - Library: `com.sun.mail:android-mail` (Jakarta Mail) or `msal`+`Microsoft Graph` / `Gmail REST`. Add `INTERNET` + `ACCESS_NETWORK_STATE`.
  - Module `data/remote/MailRemoteDataSource.kt` with `fetchFolders()`, `fetchMessages(folder, since)`, `send(message)`, `moveToFolder()`.
  - Handle `EmailViewModel.kt:317-373` provider switch (`Gmail/Outlook/Yahoo`) `updateAccountProvider()` `EmailViewModel.kt:267` → real host/port per provider + AutoDiscover.

- [ ] **2.3 Sync engine**
  - `WorkManager` periodic `SyncWorker` (15m) + foreground on open. Delta sync via UIDVALIDITY/UIDNEXT or Gmail `historyId`.
  - Room as source of truth; remote → `upsert`. Conflict: local moves win, then push.
  - Pagination: `Paging3` for `EmailDao.getEmailsByFolder()` `EmailDao.kt:26` (currently `Flow<List>` loads all).

- [ ] **2.4 Attachments & offline**
  - Stream download to `cacheDir` / `MediaStore`, progress via `Flow<Float>` (fix simulated `delay(100)` `EmailApp.kt:1044`).
  - `hasAttachments:33` → count query; show real size via `ContentResolver`.
  - Drafts auto-save to `folder="Drafts"` on `composeBody` debounce.

- [ ] **2.5 HTML rendering**
  - Replace `Regex("<[^>]*>")` strip `EmailApp.kt:951` with `HtmlCompat.fromHtml` / `WebView` sanitized, or `compose-html` lib; CSP, no JS.

**Exit criteria:** Two real accounts sync, send/receive round-trip on device, airplane-mode reads cached mail.

---

## 6. Phase 3 — Gemini AI (`MAJOR_CAPABILITY_SERVER_SIDE_GEMINI_API`)

**Goal:** Wire the declared Gemini capability. Timebox: 2 weeks. Needs `.env` → `BuildConfig`.

- [ ] **3.1 Wire secrets**
  - Keep `secrets` plugin `app/build.gradle.kts:6` + `secrets{ propertiesFileName=".env"}` `app/build.gradle.kts:63`; map `GEMINI_API_KEY` to `BuildConfig.GEMINI_API_KEY` or Firebase AI (`libs.versions.toml:89` `firebase-ai` currently commented `app/build.gradle.kts:95` — uncomment).
  - Server-side proxy preferred — do not ship raw key in APK.

- [ ] **3.2 Features (incremental)**
  1. **Summarize thread** — button in `DetailScreen` (`EmailDetailPane` `EmailApp.kt:797`) → prompt `Summarize Arabic email in 3 bullets`.
  2. **Smart compose** — in `ComposeEmailPane` `EmailApp.kt:1110` suggest subject/body from context; streaming response.
  3. **Spam triage** — classify `EmailEntity.folder=Spam` `EmailEntities.kt:32` beyond dummy `spammer99@lottery-win-fast.xyz` `EmailRepository.kt:174`.
  4. **Semantic search** — embed `subject+body` locally, rank `searchEmails` `EmailDao.kt:60` beyond `LIKE`.

- [ ] **3.3 Guardrails**
  - On-device fallback if quota/offline; redact PII before prompt; user opt-in toggle; show disclaimer.

**Exit criteria:** Summarize + Smart Reply demo in `README.md`, instrumented with `firebase-ai` behind feature flag.

---

## 7. Phase 4 — UX Polish & Arabic Excellence

**Goal:** Premium RTL feel without global force. Timebox: 2 weeks.

- [ ] **4.1 Theming & layout**
  - Remove global `CompositionLocalProvider(Rtl)` `EmailApp.kt:57`; rely on `android:supportsRtl` + per-locale `LocalLayoutDirection`.
  - Adopt `MaterialTheme` `Theme.kt:15,39` already solid — add dynamic color toggle `Theme.kt:67` (currently `dynamicColor=false`) as user setting.
  - Fix `EmailTopAppBar` `EmailApp.kt:433` search pill border `1.dp outline` → `outlineVariant` per spec; ensure 56dp touch target `EmailApp.kt:465`.

- [ ] **4.2 Navigation & state**
  - Migrate `var currentScreen: Screen` `EmailApp.kt:66` + `AnimatedContent` `EmailApp.kt:138` to `NavHost` (`navigation-compose` already in catalog). Handle back press correctly for tablet two-pane `EmailApp.kt:147`.
  - Persist `selectedAccount`/`selectedFolder`/`searchQuery` in `SavedStateHandle` + `DataStore`.

- [ ] **4.3 Strings & a11y**
  - Extract all Arabic literals (`"بريدك الذكي"` `EmailApp.kt:280`, etc.) to `res/values-ar/strings.xml` + `values/strings.xml` fallback; use `stringResource`.
  - Content descriptions, TalkBack order, large-font scaling (`sp` already used `EmailApp.kt:128`).

- [ ] **4.4 Empty/error/loading**
  - Design states for no-mail `EmailListPane` `EmailApp.kt:574` already decent — add retry for sync errors, pull-to-refresh, swipe-to-star/archive.

**Exit criteria:** `a11y` scan passes, Arabic + English verified, tablet/handheld screenshots in `src/test/screenshots/` updated via Roborazzi.

---

## 8. Phase 5 — Security & Privacy

**Goal:** Ship-safe. Timebox: 1 week.

- [ ] **5.1 Backup & encryption**
  - Fill `backup_rules.xml:8` / `data_extraction_rules.xml:7`: `<exclude domain="database" path="email_database"/>` or encrypt backup with `EncryptedFile`.
  - Encrypt Room via `SQLCipher` (optional) or at least credential store; enable `android:allowBackup="false"` if no E2E.

- [ ] **5.2 Network & permissions**
  - Pin TLS, no cleartext; `NetworkSecurityConfig`.
  - Request `POST_NOTIFICATIONS` for new-mail notifications only when needed.

- [ ] **5.3 Privacy**
  - In-app privacy card: what is stored locally (`EmailDatabase.kt:8` 3 tables), Gemini data use, opt-out.

**Exit criteria:** `MobSF`/`Play` pre-launch report clean, no plaintext creds in `AccountEntity`.

---

## 9. Phase 6 — Quality, Testing & CI

**Goal:** Confidence to release. Timebox: 1-2 weeks.

- [ ] **6.1 Unit tests**
  - `EmailViewModelTest` (turbine): folder switch `selectFolder()` `EmailViewModel.kt:122`, star toggle `toggleEmailStar()` `EmailViewModel.kt:141`, search `setSearchQuery()` `EmailViewModel.kt:127`, compose validation `sendEmail()` `EmailViewModel.kt:196`.
  - `EmailRepositoryTest` with in-memory Room; FTS search edge cases (`%`, `_`, Arabic diacritics).
  - `DateFormatTest` after `SimpleDateFormat` refactor.

- [ ] **6.2 UI tests**
  - Robolectric (`ExampleRobolectricTest.kt:12` already) + Roborazzi (`GreetingScreenshotTest.kt:18`) expand: `Inbox screenshot`, `Detail screenshot`, `Compose error state`.
  - Instrumented: account creation `AddAccountPane` `EmailApp.kt:1340`, list→detail→back on phone vs tablet.

- [ ] **6.3 CI**
  - GitHub Actions: `assembleDebug`, `testDebugUnitTest`, `connectedCheck` (emulator), `roborazziCompare`, lint. Cache Gradle.
  - Enforce `isMinifyEnabled=false` stays false for debug `app/build.gradle.kts:42`, enable `minify` + `shrinkResources` for release with real `proguard-rules.pro`.

- [ ] **6.4 Perf**
  - Baseline Profiles, `R8`, `isCrunchPngs=false` stays release `app/build.gradle.kts:41`.
  - LazyColumn `items(key, contentType)` already keyed `EmailApp.kt:608` — add `contentType`; stabilize `EmailEntity` for Compose compiler.

**Exit criteria:** Coverage >60% VM/repo, all 3 test suites green in CI, screenshot diff clean.

---

## 10. Phase 7 — Release & Iteration

**Goal:** Play internal track → production.

- [ ] **7.1 Release prep**
  - Update `versionCode/versionName` `app/build.gradle.kts:17`, `rootProject.name` `settings.gradle.kts:25` (“My Application” → “البريد الإلكتروني”).
  - Signing: use Play App Signing; remove `debugConfig` from `buildTypes.debug` `app/build.gradle.kts:46` per `README.md:20` before upload; CI injects `KEYSTORE_PATH/STORE_PASSWORD`.
  - Complete `store` listing (Arabic), data safety form (declare `INTERNET`, media access).

- [ ] **7.2 Telemetry (opt-in)**
  - Non-PII: sync latency, crash (Crashlytics), screen perf. No email content logging.

- [ ] **7.3 Backlog (post-1.0)**
  - Push via FCM, rich notifications with actions (Archive/Reply).
  - Undo send (5s), swipe gestures, multi-select, labels, filters.
  - Biometric app lock, per-account notification settings.
  - Widget (unread count), Wear OS.

**Exit criteria:** Internal track build available via `https://ai.studio/apps/06b2bb56-82f0-45f4-ab00-3a5fa98a2bfe` → Play Console, staged rollout 5%.

---

## 11. Milestones & Timeline (12 weeks)

| Week | Milestone | Demo |
|------|-----------|------|
| 1 | **M0 — Green build** | Clone → `./gradlew test` passes |
| 3 | **M1 — Clean code** | `EmailApp.kt` <200 LOC, VM coverage 50% |
| 6 | **M2 — Real mail** | Gmail + Outlook sync + send (2 accounts) |
| 8 | **M3 — Gemini** | Summarize + Smart Reply toggle |
| 10 | **M4 — Polish** | Arabic/English, tablet, a11y pass |
| 11 | **M5 — Hardened** | Backup/enc review, permissions audit |
| 12 | **M6 — Release candidate** | CI green, Play internal track |

---

## 12. Risk Register

| Risk | Impact | Mitigation |
|------|--------|------------|
| OAuth review (Google/MS) rejection | Blocks Phase 2 | Start verification early, scope `gmail.readonly` first |
| KSP/Compose version churn | Build break | Pin versions in `libs.versions.toml`, use version catalog renovate bot |
| Gemini quota/cost | Feature cut | Server-side proxy + caching + local fallback |
| Destructive migration data loss | User mail loss | Never ship `fallbackToDestructiveMigration` beyond v1; add `Migration` tests now |

---

## 13. How to Contribute

1. Pick next unchecked task in current phase — do not jump phases.
2. Branch `phase/N-short-name`, reference file:line (e.g., `EmailRepository.kt:211`).
3. Add/adjust unit + screenshot test; run `./gradlew testDebugUnitTest roborazziRecord` if UI touched.
4. PR must pass CI before merge; update this ROADMAP checkbox.

---

*Last reviewed: 2026-08-23. Baseline commit `be2a431 build: initialize Android project structure`.*
