package com.example.data.remote

import com.example.data.AttachmentEntity
import com.example.data.EmailEntity
import kotlinx.coroutines.delay
import java.util.UUID
import kotlin.random.Random

/**
 * Phase 2.2 — Fake remote that simulates IMAP/SMTP without network.
 * Replace with JakartaMailRemoteDataSource / GmailApiRemoteDataSource for production.
 */
class FakeMailRemoteDataSource : MailRemoteDataSource {

    private val simulatedInbox = mutableMapOf<Int, MutableList<EmailEntity>>()

    override suspend fun fetchMessages(
        accountId: Int,
        folder: String,
        sinceUid: Long
    ): Result<List<EmailEntity>> {
        delay(600) // simulate IMAP FETCH
        if (Random.nextFloat() < 0.03f) return Result.failure(RemoteError.Network)

        // Return empty list for now — real sync would diff UIDs.
        // For demo, occasionally inject a simulated new message.
        if (Random.nextFloat() < 0.15f && folder == "Inbox") {
            val simulated = EmailEntity(
                accountId = accountId,
                senderName = "خادم المزامنة — رسالة تجريبية",
                senderEmail = "sync-demo@smart-mail.aistudio",
                recipientEmail = "user-$accountId@example.com",
                subject = "📬 مزامنة IMAP ناجحة — ${UUID.randomUUID().toString().take(8)}",
                body = "<p>تمت مزامنة صندوق الوارد عبر الطبقة الجديدة <b>MailRemoteDataSource</b>.</p><p>هذه رسالة تجريبية تحاكي وصول بريد جديد من الخادم.</p>",
                timestamp = System.currentTimeMillis(),
                isRead = false,
                isStarred = false,
                folder = "Inbox",
                hasAttachments = false
            )
            return Result.success(listOf(simulated))
        }
        return Result.success(emptyList())
    }

    override suspend fun sendMessage(
        accountId: Int,
        email: EmailEntity,
        attachments: List<AttachmentEntity>
    ): Result<Unit> {
        delay(900) // simulate SMTP handshake + TLS
        if (Random.nextFloat() < 0.02f) return Result.failure(RemoteError.Network)
        if (email.recipientEmail.contains("fail")) return Result.failure(RemoteError.Auth)
        return Result.success(Unit)
    }

    override suspend fun moveMessage(
        accountId: Int,
        messageId: String,
        targetFolder: String
    ): Result<Unit> {
        delay(300)
        return Result.success(Unit)
    }

    override suspend fun fetchAttachments(
        accountId: Int,
        messageId: String
    ): Result<List<AttachmentEntity>> {
        delay(400)
        return Result.success(emptyList())
    }

    override suspend fun validateCredentials(
        host: String,
        port: Int,
        username: String,
        credential: String
    ): Result<Unit> {
        delay(800) // simulate TCP + TLS + AUTH
        if (username.isBlank() || credential.isBlank()) return Result.failure(RemoteError.Auth)
        if (host.contains("fail") || username.contains("invalid")) return Result.failure(RemoteError.Auth)
        return Result.success(Unit)
    }
}
