# البريد الإلكتروني — Smart Email Client

Arabic-first, multi-account email client for Android. Jetpack Compose + Room + MVVM,
with IMAP/SMTP sync scaffolding, encrypted credential storage, and opt-in Gemini AI
(summarize / smart compose / spam triage / semantic ranking).

> AI Studio app: <https://ai.studio/apps/06b2bb56-82f0-45f4-ab00-3a5fa98a2bfe>

| | |
|---|---|
| applicationId | `com.aistudio.emailclient.armlq` |
| version | `1.1.0` (code 2) — see [RELEASE_NOTES.md](RELEASE_NOTES.md) |
| minSdk / targetSdk | 24 / 36 |
| Toolchain | Gradle 8.13 · AGP 8.13.0 · Kotlin 2.2.10 · KSP 2.2.10-2.0.2 · Compose BOM 2025.04.00 · JDK 17 |
| Tests | 22 unit/UI tests (JUnit, Turbine, Room in-memory, Robolectric, Roborazzi) |

## Documentation

| Doc | Contents |
|-----|----------|
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Module map, data flow, DI, key design decisions |
| [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) | Environment setup, daily commands, testing, troubleshooting |
| [docs/SECURITY.md](docs/SECURITY.md) | Credential storage, backup/extraction rules, network config, privacy & AI guardrails |
| [ROADMAP.md](ROADMAP.md) | Phased plan (Phases 0–7, all complete) with per-task verification refs |

## Quick start

Prerequisites: JDK 17, Android SDK (platform 36), Android Studio Ladybug+ optional.

```bash
git clone <repo> && cd Email-Client-App

# 1) point Gradle at your SDK (gitignored)
echo "sdk.dir=$ANDROID_HOME" > local.properties   # e.g. /opt/android-sdk

# 2) restore the debug keystore used by signingConfig "debugConfig"
base64 -d debug.keystore.base64 > debug.keystore

# 3) (optional) Gemini key — injected into BuildConfig via secrets plugin
cp .env.example .env               # then set GEMINI_API_KEY=...

# 4) run
./gradlew :app:installDebug        # or open in Android Studio and press Run
```

First launch seeds two demo accounts (Gmail/Outlook) with sample mail so every
screen is populated offline; the remote layer is a deterministic fake until you
wire a real `MailRemoteDataSource`.

## Common tasks

```bash
./gradlew assembleDebug                 # debug APK → app/build/outputs/apk/debug/
./gradlew testDebugUnitTest             # all 22 tests (Robolectric runs on JVM)
./gradlew lintDebug                     # Android lint
./gradlew verifyRoborazziDebug          # screenshot diff (continue-on-error in CI)
./gradlew recordRoborazziDebug          # re-record screenshots after UI change
./gradlew bundleRelease                 # AAB for Play (needs release keystore env)
```

CI (`.github/workflows/ci.yml`) runs assemble → unit tests → lint → roborazzi on
every push/PR to `main`, caching Gradle and decoding `debug.keystore.base64`.

## Feature overview

- **Multi-account inbox** — Gmail/Outlook/Yahoo presets + manual IMAP/SMTP; account color badges, drawer switching (`ui/components/DrawerContent.kt`).
- **Offline-first** — Room is the source of truth (`data/EmailDatabase.kt`); remote sync is delta-based via `data/remote/MailRemoteDataSource.kt`; periodic `work/SyncWorker.kt` (15 min, network-constrained) plus pull-to-refresh.
- **Compose & drafts** — validation, mock attachments, DataStore-backed draft autosave (`data/local/DraftStore.kt`) restored on relaunch.
- **Gemini AI (opt-in)** — thread summarization dialog, smart compose fill-in, spam scoring with reasons, semantic result ranking; PII redaction and offline fallback (`ai/AiGuardrail.kt`, `ai/FakeGeminiService.kt`).
- **Arabic excellence** — full RTL, `values-ar` strings (62 keys), locale-aware direction with user override, Arabic-aware HTML-to-text (`ui/util/HtmlUtil.kt`).
- **Privacy & security** — EncryptedSharedPreferences credentials, DB excluded from cloud backup, cleartext traffic disabled, notifications permission requested only when needed (see [docs/SECURITY.md](docs/SECURITY.md)).

## Project layout

```
app/src/main/java/com/example/
├── ai/            GeminiService interface, Fake impl, guardrails, config
├── data/          Room database, DAOs, entities, repository, AppContainer (DI)
│   ├── local/     CredentialStore, DraftStore, ThemePrefs, UiStateStore, AttachmentCache
│   └── remote/    MailRemoteDataSource interface + Fake implementation
├── ui/            EmailApp shell, navigation/Screen, components/, screens/, theme/, util/
├── util/          NotificationUtil, Telemetry (opt-in)
├── viewmodel/     EmailViewModel + statuses + factory
└── work/          SyncWorker (WorkManager)
```

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for data-flow diagrams and the reasoning behind each layer.
