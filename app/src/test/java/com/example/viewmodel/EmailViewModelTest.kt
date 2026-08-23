package com.example.viewmodel

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.example.data.AccountEntity
import com.example.data.EmailDatabase
import com.example.data.EmailEntity
import com.example.data.EmailRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class EmailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var db: EmailDatabase
    private lateinit var repo: EmailRepository
    private lateinit var vm: EmailViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, EmailDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = EmailRepository(db)
        // ViewModel with injected repo + empty credential/draft stores
        vm = EmailViewModel(ctx as android.app.Application, repo)
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun selectFolder_resetsSelectedEmail() = runTest(testDispatcher) {
        val acc = AccountEntity(emailAddress = "a@b.com", displayName = "A", provider = "Gmail")
        val id = repo.createAccount(acc)
        val fetched = repo.allAccounts.first().first { it.id == id }
        vm.selectAccount(fetched)
        testDispatcher.scheduler.advanceUntilIdle()

        // Insert email and select it
        val email = EmailEntity(accountId = id, senderName = "S", senderEmail = "s@x.com", recipientEmail = "a@b.com", subject = "Hi", body = "Body", folder = "Inbox")
        repo.sendOrSaveEmail(email)
        testDispatcher.scheduler.advanceUntilIdle()
        val inserted = db.emailDao().getEmailsByFolder(id, "Inbox").first().first()
        vm.selectEmail(inserted.id)
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(vm.selectedEmailId.value)

        vm.selectFolder("Sent")
        testDispatcher.scheduler.advanceUntilIdle()
        assertNull(vm.selectedEmailId.value)
        assertEquals("Sent", vm.selectedFolder.value)
    }

    @Test
    fun searchQuery_updatesFlow() = runTest(testDispatcher) {
        vm.setSearchQuery("hello")
        assertEquals("hello", vm.searchQuery.value)
        vm.setSearchQuery("")
        assertEquals("", vm.searchQuery.value)
    }

    @Test
    fun composeValidation_failsWhenSubjectBlank() = runTest(testDispatcher) {
        // Wait for prepopulate to create default accounts
        testDispatcher.scheduler.advanceUntilIdle()
        val acc = repo.allAccounts.first().firstOrNull()
        if (acc != null) vm.selectAccount(acc)
        testDispatcher.scheduler.advanceUntilIdle()
        vm.composeNewEmail()
        vm.onComposeToChange("to@x.com")
        vm.onComposeSubjectChange("") // blank -> should fail
        vm.onComposeBodyChange("Body")
        vm.sendEmail()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.composeStatus.value is ComposeStatus.Error)
    }

    @Test
    fun composeValidation_failsWhenRecipientInvalid() = runTest(testDispatcher) {
        testDispatcher.scheduler.advanceUntilIdle()
        val acc = repo.allAccounts.first().firstOrNull()
        if (acc != null) vm.selectAccount(acc)
        testDispatcher.scheduler.advanceUntilIdle()
        vm.composeNewEmail()
        vm.onComposeToChange("invalid-email") // no @
        vm.onComposeSubjectChange("Subject")
        vm.onComposeBodyChange("Body")
        vm.sendEmail()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.composeStatus.value is ComposeStatus.Error)
    }

    @Test
    fun toggleStar_persists() = runTest(testDispatcher) {
        val acc = AccountEntity(emailAddress = "b@c.com", displayName = "B", provider = "Gmail")
        val id = repo.createAccount(acc)
        val email = EmailEntity(accountId = id, senderName = "S", senderEmail = "s@x.com", recipientEmail = "b@c.com", subject = "Star", body = "B", isStarred = false, folder = "Inbox")
        repo.sendOrSaveEmail(email)
        testDispatcher.scheduler.advanceUntilIdle()
        val inserted = db.emailDao().getEmailsByFolder(id, "Inbox").first().first()
        assertFalse(inserted.isStarred)
        vm.toggleEmailStar(inserted)
        testDispatcher.scheduler.advanceUntilIdle()
        val updated = db.emailDao().getEmailById(inserted.id)!!
        assertTrue(updated.isStarred)
    }

    @Test
    fun accountProvider_updatesServers() = runTest(testDispatcher) {
        vm.updateAccountProvider("Outlook")
        assertEquals("outlook.office365.com", vm.accountIncomingServer.value)
        assertEquals("smtp.office365.com", vm.accountOutgoingServer.value)
        vm.updateAccountProvider("Gmail")
        assertEquals("imap.gmail.com", vm.accountIncomingServer.value)
    }
}
