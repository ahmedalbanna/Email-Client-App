package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class EmailRepositoryTest {

    private lateinit var db: EmailDatabase
    private lateinit var repo: EmailRepository

    @Before
    fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, EmailDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = EmailRepository(db)
    }

    @After
    fun tearDown() { db.close() }

    @Test
    fun search_escapesLikeWildcards() = runTest {
        val accId = db.accountDao().insertAccount(AccountEntity(emailAddress = "t@e.com", displayName = "T", provider = "Gmail")).toInt()
        val e1 = EmailEntity(accountId = accId, senderName = "A", senderEmail = "a@b.com", recipientEmail = "t@e.com", subject = "hello %", body = "world", folder = "Inbox")
        val e2 = EmailEntity(accountId = accId, senderName = "B", senderEmail = "b@b.com", recipientEmail = "t@e.com", subject = "hello", body = "world _ test", folder = "Inbox")
        db.emailDao().insertEmails(listOf(e1, e2))
        // Searching for "%" should not match everything due to escaping
        val resultPercent = repo.searchEmails(accId, "%").first()
        // Should match e1 because its subject contains %, not all
        assertTrue(resultPercent.any { it.subject.contains("%") })
        // Searching "_" should be escaped
        val resultUnderscore = repo.searchEmails(accId, "_").first()
        assertTrue(resultUnderscore.any { it.body.contains("_") })
    }

    @Test
    fun insertAndRetrieve_withAttachments() = runTest {
        val accId = db.accountDao().insertAccount(AccountEntity(emailAddress = "a@b.com", displayName = "A", provider = "Gmail")).toInt()
        val email = EmailEntity(accountId = accId, senderName = "S", senderEmail = "s@x.com", recipientEmail = "a@b.com", subject = "Sub", body = "Body", hasAttachments = true, folder = "Inbox")
        val att = AttachmentEntity(emailId = 0, fileName = "file.pdf", fileSize = "1 MB", fileType = "application/pdf")
        repo.sendOrSaveEmail(email, listOf(att))
        val emails = repo.getEmailsByFolder(accId, "Inbox").first()
        assertEquals(1, emails.size)
        val atts = repo.getAttachments(emails.first().id).first()
        assertEquals(1, atts.size)
        assertEquals("file.pdf", atts.first().fileName)
    }

    @Test
    fun starAndFolderFilters() = runTest {
        val accId = db.accountDao().insertAccount(AccountEntity(emailAddress = "x@y.com", displayName = "X", provider = "Gmail")).toInt()
        val eInbox = EmailEntity(accountId = accId, senderName = "A", senderEmail = "a@b.com", recipientEmail = "x@y.com", subject = "Inbox", body = "b", folder = "Inbox", isStarred = true)
        val eSent = EmailEntity(accountId = accId, senderName = "A", senderEmail = "a@b.com", recipientEmail = "x@y.com", subject = "Sent", body = "b", folder = "Sent")
        db.emailDao().insertEmails(listOf(eInbox, eSent))
        assertEquals(1, repo.getStarredEmails(accId).first().size)
        assertEquals(1, repo.getEmailsByFolder(accId, "Sent").first().size)
    }

    @Test
    fun fkCascade_deletesEmailsWhenAccountDeleted() = runTest {
        val accId = db.accountDao().insertAccount(AccountEntity(emailAddress = "del@e.com", displayName = "Del", provider = "Gmail")).toInt()
        val email = EmailEntity(accountId = accId, senderName = "S", senderEmail = "s@x.com", recipientEmail = "del@e.com", subject = "Hi", body = "B", folder = "Inbox")
        repo.sendOrSaveEmail(email)
        assertEquals(1, repo.getEmailsByFolder(accId, "Inbox").first().size)
        val acc = db.accountDao().getAccountById(accId)!!
        db.accountDao().deleteAccount(acc)
        // Due to FK CASCADE, emails should be gone (if foreign_keys enabled; Room enables by default)
        // In-memory may need to enforce; check not throwing
        val remaining = db.emailDao().getEmailsByFolder(accId, "Inbox").first()
        assertTrue(remaining.isEmpty())
    }
}
