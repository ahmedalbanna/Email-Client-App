# Development Guide

## 1. Environment

| Requirement | Version | Notes |
|-------------|---------|-------|
| JDK | 17 (Temurin) | `java -version` must report 17 |
| Android SDK | Platform 36 + Build-Tools 35 | `sdkmanager --list_installed` |
| Gradle | 8.13 via wrapper | do not use a system Gradle |

### One-time setup

```bash
# SDK path (local.properties is gitignored)
echo "sdk.dir=$ANDROID_HOME" > local.properties

# Debug keystore used by signingConfig "debugConfig" (app/build.gradle.kts)
base64 -d debug.keystore.base64 > debug.keystore

# Optional Gemini key → BuildConfig.GEMINI_API_KEY via secrets plugin
cp .env.example .env    # set GEMINI_API_KEY=<your key>
```

`.gitignore` already excludes `.env`, `debug.keystore`, `local.properties`,
`build/`, and `.gradle/`.

## 2. Daily commands

```bash
./gradlew assembleDebug          # fastest full check
./gradlew :app:installDebug      # deploy to connected device/emulator
./gradlew testDebugUnitTest      # 22 tests, JVM only (Robolectric)
./gradlew lintDebug              # Android lint
./gradlew verifyRoborazziDebug   # screenshot compare
```

Release build requires signing env vars:

```bash
export KEYSTORE_PATH=/secure/path/upload.jks
export STORE_PASSWORD=... KEY_PASSWORD=...
./gradlew bundleRelease
```

## 3. Testing strategy

| Layer | Tooling | Location |
|-------|---------|----------|
| ViewModel | Robolectric + Turbine + in-memory Room + `StandardTestDispatcher` | `app/src/test/java/com/example/viewmodel/EmailViewModelTest.kt` |
| Repository / DAO | Room in-memory, FK cascade & LIKE-escape coverage | `app/src/test/java/com/example/data/EmailRepositoryTest.kt` |
| HTML utils | Plain JUnit | `app/src/test/java/com/example/ui/DateFormatUtilTest.kt` |
| Resources | Robolectric, incl. `@Config(qualifiers = "ar")` | `ExampleRobolectricTest.kt` |
| Screenshots | Roborazzi (`GraphicsMode.NATIVE`, Pixel 8 qualifier) | `EmailUiScreenshotTest.kt` → `src/test/screenshots/*.png` |

Current total: **22 tests** across 7 suites — keep them green before pushing.

### Writing a new test

1. ViewModel tests: mirror the setup block in `EmailViewModelTest`
   (`Dispatchers.setMain`, in-memory DB, injected repo), then
   `runTest(testDispatcher)` + `scheduler.advanceUntilIdle()`.
2. Screenshot tests: follow `EmailUiScreenshotTest`; after an intentional UI
   change re-record with:

   ```bash
   ./gradlew recordRoborazziDebug
   git add app/src/test/screenshots/
   ```

3. Name tests with backticks describing behavior:
   `` fun `selectFolder resets selected email`() ``.

## 4. Conventions

- **Files < 300 LOC** — split composables into `ui/components/` or
  `ui/screens/` (see Phase 1 of ROADMAP).
- **State hoisting** — screens take plain values + lambdas; only `EmailApp`
  talks to the ViewModel. Never write `viewModel.someFlow.value = x` from UI;
  add an `onXChange` function instead.
- **Strings** — all user-visible text goes through `stringResource`;
  English in `res/values/strings.xml`, Arabic in `res/values-ar/strings.xml`.
- **Icons** — use `Icons.AutoMirrored.Filled.*` for directional glyphs
  (Send, Reply, ArrowBack) and semantic icons (AttachFile for attachments).
- **Dates** — use `rememberListDateFormat()` / `rememberDetailDateFormat()`
  from `ui/util/DateFormatUtil.kt`; never construct `SimpleDateFormat` inside
  a composable body without `remember`.
- **DB writes** — multi-statement seeds/migrations go through
  `database.withTransaction { … }`.

## 5. Debugging tips

- **Kotlin classes missing from APK** → confirm `libs.plugins.kotlin.android`
  is applied in both root and module `build.gradle.kts` (Phase 0 fix); check
  `unzip -l app/build/intermediates/runtime_app_classes_jar/debug/bundleDebugClassesToRuntimeJar/classes.jar`.
- **JVM target mismatch** → Java `compileOptions` and Kotlin `jvmTarget` must
  both be 17 (`app/build.gradle.kts:51-57`).
- **`validateSigningDebug` fails** → restore `debug.keystore` from base64.
- **Configuration cache weirdness after catalog edits** → run with
  `--no-configuration-cache` once or `./gradlew --stop && rm -rf .gradle`.
- **Robolectric locale** → default qualifier is en-US; annotate with
  `@Config(qualifiers = "ar", sdk = [36])` for Arabic assertions.

## 6. CI

`.github/workflows/ci.yml` runs on push/PR to `main`:

1. JDK 17 setup + Gradle cache (`~/.gradle/caches`, wrapper).
2. `base64 -d debug.keystore.base64 > debug.keystore`.
3. `assembleDebug` → `testDebugUnitTest` → `lintDebug` (non-blocking) →
   `verifyRoborazziDebug` (non-blocking).
4. Artifacts: `app-debug.apk`, test/lint reports.

Secrets to configure in the repo when wiring releases:
`KEYSTORE_PATH` (or a base64 secret decoded like the debug key),
`STORE_PASSWORD`, `KEY_PASSWORD`.

## 7. Release checklist

See [RELEASE_NOTES.md](../RELEASE_NOTES.md) for the current version and
[SECURITY.md](SECURITY.md) for the pre-upload security review:

- [ ] `versionCode` bumped, `versionName` updated (`app/build.gradle.kts:14`)
- [ ] All tests green locally and in CI
- [ ] Screenshots re-recorded if UI changed
- [ ] `store/` listings + `data_safety.csv` still accurate
- [ ] `RELEASE_NOTES.md` updated
