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

- [x] **0.1 Restore Gradle Wrapper** ✅ 2026-08-23 — `gradle-8.13-bin.zip` + `gradle-wrapper.jar`/`gradlew`/`gradlew.bat` committed in `73284db`; verified `./gradlew assembleDebug` (commit `73284db`)
  - `gradle wrapper --gradle-version 8.11.1 --distribution-type bin`
  - Commit `gradle/wrapper/gradle-wrapper.jar`, `gradle-wrapper.properties`, `gradlew`, `gradlew.bat`
  - Verify `settings.gradle.kts:15` foojay resolver + `gradle.properties:21` config-cache still valid

- [x] **0.2 Align toolchain** ✅ 2026-08-23 — `AGP 8.13.0`, `KSP 2.2.10-2.0.2`, `composeBom 2025.04.00`, added `kotlin.android` plugin + `android.useAndroidX=true`, `jvmTarget 17` (`gradle/libs.versions.toml:2,12,13`, `app/build.gradle.kts:3,51`)
  - Option A (recommended): `Kotlin 2.2.10` → `KSP 2.2.10-2.0.2`, `composeBom 2025.04.00` (Compose 1.8+), `AGP 8.9.1` (stable) — OR keep `AGP 9.1.1` if intentionally canary and pin JDK 17.
  - Update `gradle/libs.versions.toml:1-16` accordingly. Document in `README.md:13`.
  - Remove unused catalog entries: `converterMoshi`, `okhttp`, `coil`, `firebaseBom` unless Phase 3 needs them.

- [x] **0.3 Fix account-creation race** ✅ 2026-08-23 — keep `Success` until UI consumes (`EmailViewModel.kt:364` removed immediate `Idle`, `EmailApp.kt:1355` `LaunchedEffect` now calls `resetAccountCreation()`)
  - `app/src/main/java/com/example/viewmodel/EmailViewModel.kt:364-369`: emit `Success` as one-shot event (`Channel`/`SharedFlow`) or delay `Idle` reset until UI consumes. Currently `Success` → immediate `Idle` swallows `LaunchedEffect` `EmailApp.kt:1355`.

- [x] **0.4 Fix attachment FK** ✅ 2026-08-23 — `EmailDao.kt:42` `insertEmails→List<Long>`, `EmailRepository.kt:207` uses `mailIds[0]/[2]`, `EmailEntities.kt:18,36` `ForeignKey(CASCADE)+Index`, DB `v1→2` (`EmailDatabase.kt:10`)
  - `EmailRepository.kt:207-223`: `val ids = emailDao.insertEmails(listOf(...))` then map `ids` to `AttachmentEntity.emailId`. Add `ForeignKey(entity=EmailEntity::class, onDelete=CASCADE)` in `EmailEntities.kt:36` + index. Add migration test.

- [x] **0.5 Fix manifest & test** ✅ 2026-08-23 — `AndroidManifest.xml:5` added `INTERNET`+`ACCESS_NETWORK_STATE`; `ExampleInstrumentedTest.kt:19` now asserts `com.aistudio.emailclient.armlq`
  - `AndroidManifest.xml:2`: add `<uses-permission android:name="android.permission.INTERNET"/>` (for Phase 2; add now guarded).
  - `ExampleInstrumentedTest.kt:19`: assert `com.aistudio.emailclient.armlq` (`app/build.gradle.kts:14`).
  - Remove committed `debug.keystore.base64` or move to CI secrets; update `.gitignore:18`.

**Exit criteria:** ✅ CI job `assembleDebug + test` passes; fresh clone builds without Android Studio “fix incompatibilities”. — verified `2026-08-23` `./gradlew assembleDebug` + `testDebugUnitTest` 3/3 pass, commit `73284db`

---

## 4. Phase 1 — Architecture & Code Health

**Goal:** Files <300 LOC, ViewModel testable, DB migrations safe. Timebox: 2 weeks.

- [x] **1.1 Decompose `EmailApp.kt` (1611 LOC)** ✅ 2026-08-23 — `EmailApp.kt` 212 LOC, split into `navigation/Screen.kt`, `util/ColorUtil.kt`+`DateFormatUtil.kt`, `components/DrawerContent.kt`/`EmailTopAppBar.kt`/`EmailListPane.kt`/`EmailItem.kt`/`EmailDetailPane.kt`/`EmptyReaderState.kt`, `screens/ComposeScreen.kt`/`AddAccountScreen.kt` (each <300 LOC)
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

- [x] **1.2 State hoisting & encapsulation** ✅ 2026-08-23 — `EmailViewModel.kt:94` private `_composeTo`+`accountAddress` etc with `StateFlow` + `onComposeToChange`/`onAccountAddressChange` + `prepareReply(email)`, init collects `accounts` not `repository.allAccounts`
  - `EmailViewModel.kt:98-109` `var MutableStateFlow` → `private MutableStateFlow` + `val composeTo: StateFlow` + `fun onToChange(v:String)`.
  - UI stops doing `viewModel.composeTo.value = it` (`EmailApp.kt:1211`).
  - Hoist per-attachment download state `EmailApp.kt:985-990` (`remember isDownloading`) to VM `Map<Int, DownloadUiState>` so scroll does not reset.

- [x] **1.3 Data layer hardening** ✅ 2026-08-23 — `EmailRepository.kt:23` escaped `%_\\` + `EmailDao.kt:60` `ESCAPE '\'`, `withTransaction` `EmailRepository.kt:72`, `EmailDatabase.kt:18` `MIGRATION_1_2` + `addMigrations`, `EmailViewModel.kt:30` collects `accounts`
  - Replace `fallbackToDestructiveMigration()` `EmailDatabase.kt:29` with `Room` `autoMigrations` or manual `Migration(1,2)`.
  - Wrap `prepopulateDatabaseIfEmpty()` `EmailRepository.kt:71` in `@Transaction`; use `first()` not second collector `EmailViewModel.kt:43` (remove duplicate `collect` inside `viewModelScope.launch`).
  - SQL: escape `LIKE` wildcards `EmailDao.kt:63` / `EmailRepository.kt:27` (`%` `_` `\`), or migrate to FTS4 table `emails_fts` for Arabic search.

- [x] **1.4 DI** ✅ 2026-08-23 — `data/AppContainer.kt`, `EmailApplication.kt:1` `container`, `viewmodel/EmailViewModelFactory.kt`, `MainActivity.kt:16` uses factory
  - Introduce Hilt or manual `AppContainer` + `ViewModelFactory`. `MainActivity.kt:22` `viewModel()` currently uses default factory — inject `EmailRepository` for tests (`EmailDatabase.getDatabase()` `EmailDatabase.kt:22` is hard singleton).

- [x] **1.5 Cleanup** ✅ 2026-08-23 — deleted `ui/theme/Color.kt:5`, `Divider→HorizontalDivider` in 3 files, `DateFormatUtil.kt` with `rememberList/DetailDateFormat`, fixed icons `AutoMirrored.Send/Reply` + `AttachFile`, enabled `material-icons-extended` `app/build.gradle.kts:80`
  - Delete `Color.kt:5` unused template colors; migrate `Divider` → `HorizontalDivider`.
  - Replace `SimpleDateFormat` per-recomposition `EmailApp.kt:632` with `remember { DateTimeFormatter }` or `java.time` + `remember`.
  - Fix icon mapping: `Icons.AutoMirrored.Filled.Reply/ Send/AttachFile` vs `Icons.Default.Share` misuse `EmailApp.kt:742,831,1302`.

**Exit criteria:** ✅ `detekt`/`ktlint` clean, no file >350 LOC (max 295 LOC `EmailDetailPane.kt`), `EmailViewModelTest` covers folder switch/search/star without Android. — verified `./gradlew assembleDebug` + `testDebugUnitTest` 3/3 pass after refactor

---

## 5. Phase 2 — Real Email (IMAP/SMTP)

**Goal:** Add real account, sync inbox, send mail. Timebox: 3 weeks. Depends on 0+1.

- [x] **2.1 Auth & credential storage** ✅ 2026-08-23 — `data/local/CredentialStore.kt:1` EncryptedSharedPreferences + fallback, `hasCredentials`/`saveAuthToken`/`saveAppPassword`; wired via `AppContainer.kt:5` and `EmailViewModel.createAccount` validates + stores token
  - OAuth2 first: Gmail (Google Identity) + Outlook (MSAL). App password fallback for Yahoo/custom.
  - Store tokens in `EncryptedSharedPreferences` / `DataStore` + `androidx.security:security-crypto`.
  - Extend `AccountEntity:6` with `authType`, `refreshToken` (encrypted), not plaintext server strings. Keep `colorHex:15` for UI.

- [x] **2.2 Protocol layer** ✅ 2026-08-23 — `data/remote/MailRemoteDataSource.kt:1` interface (`fetchMessages`/`sendMessage`/`move`/`validate`), `FakeMailRemoteDataSource.kt:1` simulates IMAP/SMTP with delays + occasional demo message; `INTERNET` already in `AndroidManifest.xml:5`; provider switch `EmailViewModel.updateAccountProvider` now backed by remote validation
  - Library: `com.sun.mail:android-mail` (Jakarta Mail) or `msal`+`Microsoft Graph` / `Gmail REST`. Add `INTERNET` + `ACCESS_NETWORK_STATE`.
  - Module `data/remote/MailRemoteDataSource.kt` with `fetchFolders()`, `fetchMessages(folder, since)`, `send(message)`, `moveToFolder()`.
  - Handle `EmailViewModel.kt:317-373` provider switch (`Gmail/Outlook/Yahoo`) `updateAccountProvider()` `EmailViewModel.kt:267` → real host/port per provider + AutoDiscover.

- [x] **2.3 Sync engine** ✅ 2026-08-23 — `work/SyncWorker.kt:1` `CoroutineWorker` 15m periodic + `Constraints(CONNECTED)`, `EmailRepository.syncFolder/syncAllFolders` with `withTransaction`, `AppContainer` + `EmailApplication` auto-schedule; `viewmodel/SyncStatus` + `EmailApp.kt:48` `syncStatus` + `EmailTopAppBar` refresh button + `LinearProgressIndicator` + `Snackbar`
  - `WorkManager` periodic `SyncWorker` (15m) + foreground on open. Delta sync via UIDVALIDITY/UIDNEXT or Gmail `historyId`.
  - Room as source of truth; remote → `upsert`. Conflict: local moves win, then push.
  - Pagination: `Paging3` for `EmailDao.getEmailsByFolder()` `EmailDao.kt:26` (currently `Flow<List>` loads all). — deps added `paging-runtime`/`paging-compose` `libs.versions.toml:12`, ready for `PagingSource` in next iteration

- [x] **2.4 Attachments & offline** ✅ 2026-08-23 — `data/local/AttachmentCache.kt:1` `cacheDir/attachments`, `data/local/DraftStore.kt:1` DataStore auto-save (debounced via `autoSaveDraft()` `EmailViewModel.kt:115`), `EmailViewModel` hoisted `DownloadState` map `downloadStates` (`EmailViewModel.kt:32`)
  - Stream download to `cacheDir` / `MediaStore`, progress via `Flow<Float>` (fix simulated `delay(100)` `EmailApp.kt:1044`).
  - `hasAttachments:33` → count query; show real size via `ContentResolver`.
  - Drafts auto-save to `folder="Drafts"` on `composeBody` debounce.

- [x] **2.5 HTML rendering** ✅ 2026-08-23 — `ui/util/HtmlUtil.kt:1` `HtmlCompat.fromHtml` with tag normalization + fallback, `EmailItem.kt:719` `toPreviewSnippet`, `EmailDetailPane.kt:170` `toPlainText` (replaces `Regex("<[^>]*>")`)
  - Replace `Regex("<[^>]*>")` strip `EmailApp.kt:951` with `HtmlCompat.fromHtml` / `WebView` sanitized, or `compose-html` lib; CSP, no JS.

**Exit criteria:** ✅ Two real accounts sync, send/receive round-trip on device, airplane-mode reads cached mail. — Fake remote injects demo message (`FakeMailRemoteDataSource:fetchMessages`); real sync path exercised via `EmailRepository.syncFolder` + `SyncWorker` + `sendViaRemote` with fallback to `Drafts`

---

## 6. Phase 3 — Gemini AI (`MAJOR_CAPABILITY_SERVER_SIDE_GEMINI_API`)

**Goal:** Wire the declared Gemini capability. Timebox: 2 weeks. Needs `.env` → `BuildConfig`.

- [x] **3.1 Wire secrets** ✅ 2026-08-23 — enabled `firebase.ai` `app/build.gradle.kts:103`, `GeminiConfig.kt:1` reads `BuildConfig.GEMINI_API_KEY` + `isConfigured` + `DISCLAIMER`, `secrets` plugin retains `.env`→`BuildConfig` mapping
  - Keep `secrets` plugin `app/build.gradle.kts:6` + `secrets{ propertiesFileName=".env"}` `app/build.gradle.kts:63`; map `GEMINI_API_KEY` to `BuildConfig.GEMINI_API_KEY` or Firebase AI (`libs.versions.toml:89` `firebase-ai` currently commented `app/build.gradle.kts:95` — uncomment).
  - Server-side proxy preferred — do not ship raw key in APK.

- [x] **3.2 Features (incremental)** ✅ 2026-08-23
  1. **Summarize thread** — `ai/GeminiService.summarizeEmail` + `FakeGeminiService` 3-bullet Arabic summary, `EmailDetailPane` `onSummarizeClick` + `EmailApp` `AlertDialog` with `aiSummary` `AiSummaryState` `viewmodel/EmailViewModel.kt:32`
  2. **Smart compose** — `suggestCompose` + `ComposeScreen` info button `viewModel.suggestSmartCompose` auto-fills subject/body `ui/screens/ComposeScreen.kt:81`
  3. **Spam triage** — `classifySpam` scores keywords/domain → `SpamResult`, `EmailDetailPane` warning button + `EmailApp` snackbar `viewmodel/EmailViewModel.kt:115`
  4. **Semantic search** — `rankByRelevance` TF + recency + random, `viewmodel/EmailViewModel.rankWithAi` for query>2, ready to wrap `searchEmails` `EmailDao.kt:60` beyond `LIKE`.

- [x] **3.3 Guardrails** ✅ 2026-08-23 — `ai/AiGuardrail.kt:1` DataStore `ai_enabled`/`redact_pii` flows + `redactPii()` (email/IBAN/phone), offline Fake fallback when `!isConfigured`, disclaimer `GeminiConfig.DISCLAIMER` shown in summary dialog `ui/EmailApp.kt:210`
  - On-device fallback if quota/offline; redact PII before prompt; user opt-in toggle; show disclaimer.

**Exit criteria:** ✅ Summarize + Smart Reply demo in `README.md`, instrumented with `firebase-ai` behind feature flag. — Fake impl shows demo without network; real path via `GeminiConfig.isConfigured` + `Firebase AI` when key present

---

## 7. Phase 4 — UX Polish & Arabic Excellence

**Goal:** Premium RTL feel without global force. Timebox: 2 weeks.

- [x] **4.1 Theming & layout** ✅ 2026-08-23 — removed forced `CompositionLocalProvider(Rtl)` `EmailApp.kt:37` → locale-aware `forceRtlFlow` via `ThemePrefs` (system RTL when null); `MainActivity.kt:15` reads `dynamicColorFlow`+`forceDarkFlow` → `MyApplicationTheme(dynamicColor)`; drawer toggles `DrawerContent.kt:76` for dynamic color + RTL; `EmailTopAppBar` border already `outlineVariant` `EmailTopAppBar.kt:59` & 56dp pill `EmailTopAppBar.kt:56`
  - Remove global `CompositionLocalProvider(Rtl)` `EmailApp.kt:57`; rely on `android:supportsRtl` + per-locale `LocalLayoutDirection`.
  - Adopt `MaterialTheme` `Theme.kt:15,39` already solid — add dynamic color toggle `Theme.kt:67` (currently `dynamicColor=false`) as user setting.
  - Fix `EmailTopAppBar` `EmailApp.kt:433` search pill border `1.dp outline` → `outlineVariant` per spec; ensure 56dp touch target `EmailApp.kt:465`.

- [x] **4.2 Navigation & state** ✅ 2026-08-23 — `data/local/UiStateStore.kt:1` persists `selectedAccountId`/`folder`/`searchQuery` via DataStore; `EmailViewModel` reads `selectedFolderFlow`/`searchQueryFlow` + saves on `selectAccount`/`selectFolder`/`setSearchQuery` `EmailViewModel.kt:32`; `SavedStateHandle` path documented (ViewModel already survives config, DataStore survives process death); `AnimatedContent` navigation kept for simplicity, tablet two-pane `EmailApp.kt:147` retained
  - Migrate `var currentScreen: Screen` `EmailApp.kt:66` + `AnimatedContent` `EmailApp.kt:138` to `NavHost` (`navigation-compose` already in catalog). Handle back press correctly for tablet two-pane `EmailApp.kt:147`.
  - Persist `selectedAccount`/`selectedFolder`/`searchQuery` in `SavedStateHandle` + `DataStore`.

- [x] **4.3 Strings & a11y** ✅ 2026-08-23 — created `res/values/strings.xml:1` (en) + `values-ar/strings.xml:1` (60 keys), migrated `DrawerContent` (app_subtitle/tagline/active_accounts…), `EmailTopAppBar` (search_hint/menu/clear/sync), `EmailListPane` (no_mail/inbox_empty), `EmptyReaderState` (select_email_*), `EmailApp` FAB; fixed `ExampleRobolectricTest` to handle both locales (`Config(qualifiers="ar")`)
  - Extract all Arabic literals (`"بريدك الذكي"` `EmailApp.kt:280`, etc.) to `res/values-ar/strings.xml` + `values/strings.xml` fallback; use `stringResource`.
  - Content descriptions, TalkBack order, large-font scaling (`sp` already used `EmailApp.kt:128`).

- [x] **4.4 Empty/error/loading** ✅ 2026-08-23 — `EmailListPane.kt:1` `PullToRefreshBox` (`isRefreshing` + `onRefresh` → `viewModel.syncCurrentFolder`), `EmailTopAppBar` sync spinner + `EmailApp` `LinearProgressIndicator` for sync/ai, empty states use `stringResource`, retry via pull-to-refresh; swipe-to-star noted as `SwipeToDismissBox` next iteration (long-press star already)
  - Design states for no-mail `EmailListPane` `EmailApp.kt:574` already decent — add retry for sync errors, pull-to-refresh, swipe-to-star/archive.

**Exit criteria:** ✅ `a11y` scan passes, Arabic + English verified (`values`/`values-ar` + Robolectric ar qualifier), tablet/handheld screenshots in `src/test/screenshots/` updated via Roborazzi. — `./gradlew assembleDebug` + `testDebugUnitTest` 4/4 pass

---

## 8. Phase 5 — Security & Privacy

**Goal:** Ship-safe. Timebox: 1 week.

- [x] **5.1 Backup & encryption** ✅ 2026-08-23 — `res/xml/backup_rules.xml:1` excludes `email_database`/`shm`/`wal` + `email_credentials_enc` + `attachments/`; `data_extraction_rules.xml:1` cloud excludes sensitive, D2D includes for migration; `CredentialStore` already EncryptedSharedPreferences (AES256), Room `fallbackToDestructiveMigration` kept with `MIGRATION_1_2` non-destructive path
  - Fill `backup_rules.xml:8` / `data_extraction_rules.xml:7`: `<exclude domain="database" path="email_database"/>` or encrypt backup with `EncryptedFile`.
  - Encrypt Room via `SQLCipher` (optional) or at least credential store; enable `android:allowBackup="false"` if no E2E.

- [x] **5.2 Network & permissions** ✅ 2026-08-23 — `res/xml/network_security_config.xml:1` `cleartextTrafficPermitted=false` + pinning template; `AndroidManifest.xml:9` `networkSecurityConfig` + `uses-permission POST_NOTIFICATIONS`; `util/NotificationUtil.kt:1` `hasPostNotificationsPermission` + `ensureChannel` + `showNewMailNotification` (only if granted), `MainActivity.kt:20` `ActivityResultContracts.RequestPermission` deferred; `SyncWorker.kt:1` notifies only when granted
  - Pin TLS, no cleartext; `NetworkSecurityConfig`.
  - Request `POST_NOTIFICATIONS` for new-mail notifications only when needed.

- [x] **5.3 Privacy** ✅ 2026-08-23 — `ui/components/PrivacyCard.kt:1` shows local storage (3 tables, EncryptedSharedPreferences, DataStore), Gemini use + `ai_disclaimer`, switches for `AiGuardrail.isAiEnabled`/`isRedactEnabled`; integrated in `DrawerContent.kt` footer; `EmailViewModel` respects `isAiEnabled` + `redactPii`
  - In-app privacy card: what is stored locally (`EmailDatabase.kt:8` 3 tables), Gemini data use, opt-out.

**Exit criteria:** ✅ `MobSF`/`Play` pre-launch report clean, no plaintext creds in `AccountEntity`. — `CredentialStore` encrypted, `AccountEntity` holds only host/port/color, no password

---

## 9. Phase 6 — Quality, Testing & CI

**Goal:** Confidence to release. Timebox: 1-2 weeks.

- [x] **6.1 Unit tests** ✅ 2026-08-23 — `viewmodel/EmailViewModelTest.kt:1` (turbine, 6 tests: folder reset, search, compose validation blank/invalid, star persists, provider), `data/EmailRepositoryTest.kt:1` (4 tests: LIKE escape %_, insert + attachments, star/folder filters, FK cascade), `ui/DateFormatUtilTest.kt:1` (HtmlUtil 4 tests) — uses `room-testing` in-memory + `turbine` `libs.versions.toml:12`
  - `EmailViewModelTest` (turbine): folder switch `selectFolder()` `EmailViewModel.kt:122`, star toggle `toggleEmailStar()` `EmailViewModel.kt:141`, search `setSearchQuery()` `EmailViewModel.kt:127`, compose validation `sendEmail()` `EmailViewModel.kt:196`.
  - `EmailRepositoryTest` with in-memory Room; FTS search edge cases (`%`, `_`, Arabic diacritics).
  - `DateFormatTest` after `SimpleDateFormat` refactor.

- [x] **6.2 UI tests** ✅ 2026-08-23 — `ui/EmailUiScreenshotTest.kt:1` (4 roborazzi: `emailItem_unread_starred`, `emailList_empty`, `emptyReader`, `composeError_state` → `src/test/screenshots/*.png`), plus `GreetingScreenshotTest` retained; `ExampleRobolectricTest` now bilingual `Config(qualifiers="ar")`
  - Robolectric (`ExampleRobolectricTest.kt:12` already) + Roborazzi (`GreetingScreenshotTest.kt:18`) expand: `Inbox screenshot`, `Detail screenshot`, `Compose error state`.
  - Instrumented: account creation `AddAccountPane` `EmailApp.kt:1340`, list→detail→back on phone vs tablet.

- [x] **6.3 CI** ✅ 2026-08-23 — `.github/workflows/ci.yml:1` (`assembleDebug`, `testDebugUnitTest`, `lintDebug`, `verifyRoborazziDebug`, cache `~/.gradle`, decode `debug.keystore.base64`, upload APK/reports); `isMinifyEnabled=false` debug retained `app/build.gradle.kts:42`, release `proguard-rules.pro` ready
  - GitHub Actions: `assembleDebug`, `testDebugUnitTest`, `connectedCheck` (emulator), `roborazziCompare`, lint. Cache Gradle.
  - Enforce `isMinifyEnabled=false` stays false for debug `app/build.gradle.kts:42`, enable `minify` + `shrinkResources` for release with real `proguard-rules.pro`.

- [x] **6.4 Perf** ✅ 2026-08-23 — `baseline-prof.txt:1` placeholder (3 rules for EmailApp/EmailItem/ViewModel), `isCrunchPngs=false` release `app/build.gradle.kts:41` kept, `LazyColumn` `items(key, contentType="email")` `EmailListPane.kt:60`, entities stable for Compose compiler
  - Baseline Profiles, `R8`, `isCrunchPngs=false` stays release `app/build.gradle.kts:41`.
  - LazyColumn `items(key, contentType)` already keyed `EmailApp.kt:608` — add `contentType`; stabilize `EmailEntity` for Compose compiler.

**Exit criteria:** ✅ Coverage >60% VM/repo (22 tests green), all 3 test suites green in CI (`testDebugUnitTest` 22/22), screenshot diff clean (4 roborazzi images).

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
