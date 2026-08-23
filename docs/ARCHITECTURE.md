# Architecture

MVVM + unidirectional data flow. Room is the single source of truth; the remote
layer only feeds it. UI observes `StateFlow`s from a single `EmailViewModel`
injected through a lightweight manual DI container.

```
┌────────────────────────── UI (Compose) ──────────────────────────┐
│ EmailApp.kt (shell, ~300 LOC)                                    │
│   ├─ navigation/Screen.kt        sealed Inbox/Compose/AddAccount │
│   ├─ screens/                    ComposeScreen · AddAccountScreen│
│   └─ components/                 DrawerContent · EmailTopAppBar  │
│                                  EmailListPane · EmailItem       │
│                                  EmailDetailPane · PrivacyCard   │
└───────────────▲──────────────────────────────┬───────────────────┘
                │ StateFlow (collectAsStateWithLifecycle)
                │ events (functions)           ▼
┌───────────────┴──────────── viewmodel/EmailViewModel.kt ─────────┐
│ accounts · selectedAccount/Folder · searchQuery                  │
│ emails = combine(account, folder, query).flatMapLatest(repo.*)   │
│ selectedEmail/Attachments · compose fields (private _flows)      │
│ syncStatus · aiSummary · downloadStates                          │
└───────────────┬──────────────────────────────────────────────────┘
                ▼
┌────────────────────────── data layer ────────────────────────────┐
│ EmailRepository                                                  │
│   ├── EmailDatabase (Room v2) ── AccountDao / EmailDao           │
│   │     entities: AccountEntity ◀─CASCADE─ EmailEntity           │
│   │                              ◀─CASCADE─ AttachmentEntity     │
│   └── MailRemoteDataSource (interface)                           │
│         └── FakeMailRemoteDataSource  ← swap for JakartaMail/    │
│                                          Gmail REST/MS Graph     │
├── local stores: CredentialStore (EncryptedSharedPreferences)     │
│                DraftStore · ThemePrefs · UiStateStore (DataStore)│
│                AttachmentCache (cacheDir)                        │
└──────────────────────────────────────────────────────────────────┘
```

## Composition root (manual DI)

`EmailApplication` creates an `AppContainer` (`data/AppContainer.kt`) that lazily
exposes every dependency; `MainActivity` builds an `EmailViewModelFactory`
(`viewmodel/EmailViewModelFactory.kt`) and passes it to `viewModel(factory=…)`.
All ViewModel collaborators default to container lookups but can be overridden in
tests — e.g. `EmailViewModelTest` injects an in-memory `EmailRepository`.

Why not Hilt: single-ViewModel app, KSP already loaded (Room/Moshi); manual DI
keeps the build lean and is swappable later.

## Key flows

### Email list & selection
1. `EmailViewModel.emails` combines `_selectedAccount`, `_selectedFolder`,
   `_searchQuery` and `flatMapLatest`s into one of:
   `repository.searchEmails` (query non-blank), `getStarredEmails` (folder =
   `Starred`), or `getEmailsByFolder`.
2. DAO flows re-emit on any Room write, so list/detail stay live without
   refresh calls.
3. `selectEmail(id)` also fires `updateReadStatus(id, true)`.

### Sync (Phase 2)
- Pull: `SyncWorker` (15 min periodic, `NetworkType.CONNECTED`) →
  `repository.syncFolder(accountId, "Inbox")` → remote fetch →
  `withTransaction { emailDao.insertEmails }`.
- Push: `sendEmail()` tries `repository.sendViaRemote`; on failure the message is
  saved to `Drafts` and surfaced via `ComposeStatus.Error`.
- Status surfaces as `SyncStatus` → top-bar spinner + snackbar in `EmailApp.kt`.

### Gemini AI (Phase 3)
- `ai/GeminiService.kt` defines four operations; `FakeGeminiService` implements
  them deterministically offline. Swap-in point for Firebase AI when
  `GeminiConfig.isConfigured` (reads `BuildConfig.GEMINI_API_KEY` from `.env`).
- Every prompt passes through `AiGuardrail.redactPii` (emails, phones, IBAN,
  long digit runs) and is gated by DataStore-backed `ai_enabled` /
  `redact_pii` toggles exposed in `PrivacyCard`.

### State persistence (Phase 4)
| What | Where |
|------|-------|
| Selected account id / folder / search | `local/UiStateStore.kt` (DataStore) |
| Compose draft | `local/DraftStore.kt` (DataStore) |
| Theme: dynamicColor / forceDark / forceRtl | `local/ThemePrefs.kt` |
| AI opt-in / redact | `ai/AiGuardrail.kt` |

## Database schema notes

- Version 2 added `ForeignKey(CASCADE)` + indices on `emails.accountId` and
  `attachments.emailId` (Phase 0 bug fix). `MIGRATION_1_2` exists as a
  non-destructive hook; `fallbackToDestructiveMigration()` remains as demo
  safety net — remove before storing real user mail.
- Seed data lives in `EmailRepository.prepopulateDatabaseIfEmpty()`, wrapped in
  `database.withTransaction`, guarded by an empty-accounts check.
- Search uses `LIKE :query ESCAPE '\'` with `% _ \` escaped in
  `EmailRepository.searchEmails` — see `EmailRepositoryTest.search_escapesLikeWildcards`.

## Threading

- All DB access is `suspend` (Room generates safe implementations); Flow
  collectors run in `viewModelScope` (Main.immediate).
- Tests swap `Dispatchers.Main` with `StandardTestDispatcher` and drive the
  scheduler explicitly (`EmailViewModelTest`).
- `SyncWorker` extends `CoroutineWorker`; WorkManager uses the default
  configuration provided by `EmailApplication : Configuration.Provider`.

## Extension points

| Goal | Change |
|------|--------|
| Real IMAP/SMTP | Implement `MailRemoteDataSource` with Jakarta Mail; register it in `AppContainer.remoteDataSource` |
| Real Gemini | Add `FirebaseAiGeminiService : GeminiService`, select it when `GeminiConfig.isConfigured` |
| Paging | DAO already indexed; convert `getEmailsByFolder` to `PagingSource` (paging deps present) |
| Navigation library | Replace `AnimatedContent` switch in `EmailApp.kt` with `NavHost` (`navigation-compose` dependency already declared) |
