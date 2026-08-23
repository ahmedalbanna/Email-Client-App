# Security & Privacy Model

Threat model: on-device email data must survive device theft and cloud leaks;
credentials must never be readable by other apps or backups; AI features must
never transmit message content without explicit opt-in.

## 1. Credentials

- `data/local/CredentialStore.kt` stores tokens/app-passwords in
  **EncryptedSharedPreferences** (`MasterKey AES256_GCM`, pref keys
  `AES256_SIV`, values `AES256_GCM`), file `email_credentials_enc.xml`.
- A plain-preferences fallback (`email_credentials_fallback.xml`) exists only
  for JVM test environments without AndroidKeyStore (Robolectric); it is
  excluded from backup like the encrypted store.
- `AccountEntity` itself holds **no secrets** — only host, port, display name,
  provider, color.
- Written on account creation (`EmailViewModel.createAccount` →
  `credentialStore.saveAuthToken`); `clearCredentials(accountId)` available for
  sign-out.

## 2. Backup & data extraction

| File | Rule |
|------|------|
| `res/xml/backup_rules.xml` | API ≤31 full-backup: excludes `email_database{,-shm,-wal}`, both credential prefs files, `attachments/` |
| `res/xml/data_extraction_rules.xml` | Cloud-backup: same exclusions; only non-sensitive prefs (`ai_prefs`, `theme_prefs`, `ui_state`) included. Device-transfer (D2D): DB + encrypted credentials included so migration works |

Consequence: a stolen cloud backup yields no mail and no tokens; a guided
device-to-device move keeps the user logged in.

Attachments live under `cacheDir/attachments/`
(`data/local/AttachmentCache.kt`) — cache-only, excluded from backup, cleared
with system cache eviction or `clearCache()`.

## 3. Network

- `res/xml/network_security_config.xml`: `cleartextTrafficPermitted="false"`
  globally; template block for pinning `imap.gmail.com` / `smtp.gmail.com`
  (uncomment with real SPKI pins + expiration before enabling).
- Manifest: `android:networkSecurityConfig` wired; permissions are
  `INTERNET`, `ACCESS_NETWORK_STATE`, `POST_NOTIFICATIONS` — nothing else.
  No location, contacts, camera, or storage permissions are requested.

## 4. Notifications

- `util/NotificationUtil.hasPostNotificationsPermission` gates every notify
  call; channel is low-importance ("Email sync").
- `MainActivity` registers `ActivityResultContracts.RequestPermission` but
  defers the prompt (commented) so first-run UX stays clean — enable when FCM/
  new-mail notifications ship.
- Notification text contains counts and account display name only.

## 5. Gemini AI guardrails

Layered in `ai/AiGuardrail.kt`:

1. **Opt-in** — DataStore flag `ai_enabled` (default true for demo; flip in
   `PrivacyCard`). Every ViewModel AI entry point can check it
   (`EmailViewModel.rankWithAi` does; wire the same check into summarize/
   suggest when moving past the fake).
2. **PII redaction** — `redactPii()` masks emails `[بريد محجوب]`, Saudi phone
   numbers `[جوال محجوب]`, IBANs `[آيبان محجوب]`, and ≥16-digit numbers before
   any prompt is built.
3. **Disclaimer** — `GeminiConfig.DISCLAIMER` rendered in the summary dialog
   and drawer footer.
4. **Offline fallback** — `FakeGeminiService` returns deterministic local
   content when `GeminiConfig.isConfigured` is false (no key shipped, no
   network call).
5. **Key handling** — `.env` → BuildConfig via secrets plugin; key never
   committed. For production prefer a server-side proxy so the raw key never
   ships inside the APK.

## 6. Telemetry

- `util/Telemetry.kt` is **opt-in** (`telemetry_enabled`, default false),
  records sync latency / screen perf locally via DataStore + `Log.d`.
- Never logs subjects, bodies, addresses, or credentials. Swap `Log.d` for
  Crashlytics/Analytics with sampling when enabled.

## 7. Remaining hardening backlog (post-1.1)

- [ ] SQLCipher (or `SupportFactory`) at-rest encryption of `email_database`.
- [ ] Real certificate pinning set for IMAP/SMTP hosts.
- [ ] Biometric app lock (`BiometricPrompt`) gate on launch.
- [ ] Per-account notification settings + silent hours.
- [ ] Play Integrity API attestation before enabling remote sync of real accounts.

## 8. Verification checklist (pre-release)

```bash
./gradlew lintDebug                 # no security findings
grep -R "password\|secret\|token" app/src/main --include="*.kt" -l   # review hits
aapt2 dump xmltree app-debug.apk --file AndroidManifest.xml            # confirm permissions only INTERNET/NETWORK_STATE/POST_NOTIFICATIONS
unzip -p app-release.aab BUNDLE-METADATA/* 2>/dev/null | grep -i gemini || echo "no key leaked"
```

Manual QA: airplane-mode read (offline cache), backup via `adb backup` shows no
mail payload, D2D transfer restores session.
