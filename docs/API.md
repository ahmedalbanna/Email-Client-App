# API / Integration Reference

Public seams of the app — the contracts you implement or call when extending it.
All classes live under `app/src/main/java/com/example/`.

## 1. `data/remote/MailRemoteDataSource.kt`

Contract for any real mail backend. The app currently binds
`FakeMailRemoteDataSource` in `AppContainer.remoteDataSource`.

```kotlin
interface MailRemoteDataSource {
    suspend fun fetchMessages(accountId: Int, folder: String, sinceUid: Long = 0L): Result<List<EmailEntity>>
    suspend fun sendMessage(accountId: Int, email: EmailEntity, attachments: List<AttachmentEntity>): Result<Unit>
    suspend fun moveMessage(accountId: Int, messageId: String, targetFolder: String): Result<Unit>
    suspend fun fetchAttachments(accountId: Int, messageId: String): Result<List<AttachmentEntity>>
    suspend fun validateCredentials(host: String, port: Int, username: String, credential: String): Result<Unit>
}
```

Implementation notes:
- Return `Result.failure(RemoteError.Network | .Auth | .NotFound)`; the
  ViewModel maps failures to Arabic user messages and draft fallback.
- Must be safe to call from `Dispatchers.IO` (make blocking calls inside
  `withContext`); `SyncWorker` calls `fetchMessages` for every account.
- Suggested production adapters: Jakarta Mail (`com.sun.mail:android-mail`)
  for generic IMAP/SMTP, Gmail REST + MS Graph for OAuth providers.

## 2. `ai/GeminiService.kt`

```kotlin
interface GeminiService {
    suspend fun summarizeEmail(email: EmailEntity): Result<String>          // 3-bullet Arabic summary
    suspend fun suggestCompose(context: String, tone: String = "رسمي"): Result<ComposeSuggestion>
    suspend fun classifySpam(email: EmailEntity): Result<SpamResult>        // score 0..1 + label + reasons
    suspend fun rankByRelevance(query: String, emails: List<EmailEntity>): Result<List<EmailEntity>>

    data class ComposeSuggestion(val subject: String, val body: String)
    data class SpamResult(val score: Float, val label: String, val reasons: List<String>)
}
```

- Current binding: `FakeGeminiService` (deterministic, no network).
- To ship real AI: create `FirebaseAiGeminiService`, gate selection on
  `GeminiConfig.isConfigured`, and route all inputs through
  `AiGuardrail.redactPii` (the ViewModel already does this).

## 3. `viewmodel/EmailViewModel.kt`

Single source of UI truth. Observed state (all `StateFlow`):

| Flow | Type | Purpose |
|------|------|---------|
| `accounts` | `List<AccountEntity>` | drawer + top-bar switcher |
| `selectedAccount` / `selectedFolder` / `searchQuery` | `AccountEntity?` / `String` / `String` | current context (persisted via `UiStateStore`) |
| `emails` | `List<EmailEntity>` | derived: search ▸ starred ▸ folder |
| `selectedEmailId` / `selectedEmail` / `selectedEmailAttachments` | `Int?` / `EmailEntity?` / `List<AttachmentEntity>` | reader pane |
| `composeTo/Cc/Bcc/Subject/Body/Attachments` | `String` / `List<AttachmentEntity>` | compose screen (read-only) |
| `composeStatus` | `ComposeStatus` | Idle · Loading · Success · Error(msg) |
| `accountCreationStatus` | `AccountCreationStatus` | same shape, add-account flow |
| `syncStatus` | `SyncStatus` | Idle · Loading · Success(n) · Error(msg) |
| `aiSummary` | `AiSummaryState` | Idle · Loading · Success(text) · Error(msg) |
| `spamState` | `SpamResult?` | one-shot snackbar payload |
| `downloadStates` | `Map<Int, DownloadState>` | attachment download progress (survives scroll) |

Commands (UI → VM): `selectAccount`, `selectFolder`, `setSearchQuery`,
`selectEmail(id?)`, `toggleEmailStar`, `toggleEmailRead`,
`moveEmailToFolder`, `composeNewEmail`, `prepareReply(email)`,
`onComposeToChange` (+ Cc/Bcc/Subject/Body), `addMockAttachment`,
`removeMockAttachment(index)`, `sendEmail`, `syncCurrentFolder`,
`syncAllForCurrentAccount`, `summarizeEmail(email)`, `clearSummary`,
`suggestSmartCompose(context)`, `classifySpam(email)`, `clearSpam`,
`updateAccountProvider(provider)`, `createAccount`, `resetAccountCreation`.

## 4. Room schema (`data/EmailEntities.kt`, DB v2)

```
accounts(id PK, emailAddress, displayName, provider,
         incomingServer, outgoingServer, port, colorHex)

emails(id PK, accountId FK→accounts.id ON DELETE CASCADE [idx],
       senderName, senderEmail, recipientEmail, cc, bcc,
       subject, body, timestamp, isRead, isStarred,
       folder /*Inbox|Sent|Drafts|Spam|Trash*/, hasAttachments)

attachments(id PK, emailId FK→emails.id ON DELETE CASCADE [idx],
            fileName, fileSize, fileType)
```

DAO surface (`data/EmailDao.kt`): folder/starred/search (`LIKE … ESCAPE '\'`),
by-id (Flow + one-shot), insert single/list returning ids, read/star/folder
updates, attachments per email.

## 5. Local stores

| Store | File | Keys / API |
|-------|------|-----------|
| Credentials | `local/CredentialStore.kt` | `saveAuthToken/getAuthToken`, `saveRefreshToken`, `saveAppPassword`, `clearCredentials`, `hasCredentials` |
| Draft | `local/DraftStore.kt` | `draftFlow: Flow<Draft>`, `saveDraft(Draft)`, `clearDraft()` |
| Theme | `local/ThemePrefs.kt` | `dynamicColorFlow`, `forceDarkFlow`, `forceRtlFlow` + setters |
| UI state | `local/UiStateStore.kt` | `selectedAccountIdFlow`, `selectedFolderFlow`, `searchQueryFlow` + setters |
| AI prefs | `ai/AiGuardrail.kt` | `isAiEnabled`, `isRedactEnabled`, `redactPii(text)` |
| Telemetry | `util/Telemetry.kt` | opt-in `isEnabled`, `recordSyncLatency(ms)`, `recordScreenPerf(screen, ms)` |

## 6. WorkManager

```kotlin
SyncWorker.schedulePeriodic(context)   // unique "email_periodic_sync", 15 min, CONNECTED
SyncWorker.cancel(context)
```
Runs `repository.syncFolder(acc.id, "Inbox")` for every account, posts a
notification through `NotificationUtil` **only if** `POST_NOTIFICATIONS` is
granted and new mail arrived. App auto-schedules on start
(`EmailApplication.onCreate`).
