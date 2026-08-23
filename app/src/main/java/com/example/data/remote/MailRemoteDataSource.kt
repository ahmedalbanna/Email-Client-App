package com.example.data.remote

import com.example.data.AttachmentEntity
import com.example.data.EmailEntity

/**
 * Phase 2.2 — Protocol abstraction.
 * Real implementation would use Jakarta Mail (IMAP/SMTP) or Gmail/MS Graph REST.
 * Fake implementation simulates network latency and returns deterministic data.
 */
interface MailRemoteDataSource {

    /** Fetch message headers + bodies for a folder since [sinceUid] (delta sync). */
    suspend fun fetchMessages(
        accountId: Int,
        folder: String,
        sinceUid: Long = 0L
    ): Result<List<EmailEntity>>

    /** Send a message via SMTP / Graph. */
    suspend fun sendMessage(
        accountId: Int,
        email: EmailEntity,
        attachments: List<AttachmentEntity>
    ): Result<Unit>

    /** Move a message to another folder on the server. */
    suspend fun moveMessage(
        accountId: Int,
        messageId: String,
        targetFolder: String
    ): Result<Unit>

    /** Fetch attachments for a message. */
    suspend fun fetchAttachments(
        accountId: Int,
        messageId: String
    ): Result<List<AttachmentEntity>>

    /** Validate credentials (IMAP login / OAuth token ). */
    suspend fun validateCredentials(
        host: String,
        port: Int,
        username: String,
        credential: String
    ): Result<Unit>
}

sealed class RemoteError : Exception() {
    object Network : RemoteError()
    object Auth : RemoteError()
    object NotFound : RemoteError()
}
