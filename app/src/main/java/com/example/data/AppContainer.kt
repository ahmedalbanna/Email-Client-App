package com.example.data

import android.content.Context
import com.example.ai.AiGuardrail
import com.example.ai.FakeGeminiService
import com.example.ai.GeminiService
import com.example.data.local.AttachmentCache
import com.example.data.local.CredentialStore
import com.example.data.local.DraftStore
import com.example.data.local.ThemePrefs
import com.example.data.local.UiStateStore
import com.example.data.remote.FakeMailRemoteDataSource
import com.example.data.remote.MailRemoteDataSource
import com.example.util.Telemetry

class AppContainer(private val context: Context) {
    val database: EmailDatabase by lazy { EmailDatabase.getDatabase(context) }
    val remoteDataSource: MailRemoteDataSource by lazy { FakeMailRemoteDataSource() }
    val emailRepository: EmailRepository by lazy { EmailRepository(database, remoteDataSource) }
    val credentialStore: CredentialStore by lazy { CredentialStore(context) }
    val draftStore: DraftStore by lazy { DraftStore(context) }
    val attachmentCache: AttachmentCache by lazy { AttachmentCache(context) }
    val geminiService: GeminiService by lazy { FakeGeminiService() }
    val aiGuardrail: AiGuardrail by lazy { AiGuardrail(context) }
    val themePrefs: ThemePrefs by lazy { ThemePrefs(context) }
    val uiStateStore: UiStateStore by lazy { UiStateStore(context) }
    val telemetry: Telemetry by lazy { Telemetry(context) }
}
