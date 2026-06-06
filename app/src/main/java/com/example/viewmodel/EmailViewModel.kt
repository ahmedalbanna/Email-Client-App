package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class EmailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: EmailRepository

    // State of all accounts
    val accounts: StateFlow<List<AccountEntity>>

    // Selected account state
    private val _selectedAccount = MutableStateFlow<AccountEntity?>(null)
    val selectedAccount: StateFlow<AccountEntity?> = _selectedAccount.asStateFlow()

    // Current folder state ("Inbox", "Sent", "Drafts", "Spam", "Trash", "Starred")
    private val _selectedFolder = MutableStateFlow("Inbox")
    val selectedFolder: StateFlow<String> = _selectedFolder.asStateFlow()

    // Search text state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        val database = EmailDatabase.getDatabase(application)
        repository = EmailRepository(database)
        accounts = repository.allAccounts.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Prepopulate database if required and select first account on ready
        viewModelScope.launch {
            repository.prepopulateDatabaseIfEmpty()
            // Observe accounts and select first one if null
            repository.allAccounts.collect { accountList ->
                if (accountList.isNotEmpty() && _selectedAccount.value == null) {
                    _selectedAccount.value = accountList.first()
                }
            }
        }
    }

    // Dynamic Emails flow that automatically updates when selected account, folder, or search query changes!
    @OptIn(ExperimentalCoroutinesApi::class)
    val emails: StateFlow<List<EmailEntity>> = combine(
        _selectedAccount,
        _selectedFolder,
        _searchQuery
    ) { account, folder, query ->
        Triple(account, folder, query)
    }.flatMapLatest { (account, folder, query) ->
        if (account == null) {
            flowOf(emptyList())
        } else if (query.isNotBlank()) {
            repository.searchEmails(account.id, query)
        } else if (folder == "Starred") {
            repository.getStarredEmails(account.id)
        } else {
            repository.getEmailsByFolder(account.id, folder)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Selected email details state
    private val _selectedEmailId = MutableStateFlow<Int?>(null)
    val selectedEmailId: StateFlow<Int?> = _selectedEmailId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedEmail: StateFlow<EmailEntity?> = _selectedEmailId.flatMapLatest { id ->
        if (id == null) flowOf(null) else repository.getEmailById(id)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedEmailAttachments: StateFlow<List<AttachmentEntity>> = _selectedEmailId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.getAttachments(id)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Compose Window States
    var composeTo = MutableStateFlow("")
        private set
    var composeCc = MutableStateFlow("")
        private set
    var composeBcc = MutableStateFlow("")
        private set
    var composeSubject = MutableStateFlow("")
        private set
    var composeBody = MutableStateFlow("")
        private set
    var composeAttachments = MutableStateFlow<List<AttachmentEntity>>(emptyList())
        private set

    private val _composeStatus = MutableStateFlow<ComposeStatus>(ComposeStatus.Idle)
    val composeStatus: StateFlow<ComposeStatus> = _composeStatus.asStateFlow()

    // Screen navigation state (to avoid nested navigation graphs in complex setups if desired,
    // we can use standard compose navigation or a helper)
    fun selectAccount(account: AccountEntity) {
        _selectedAccount.value = account
        _selectedFolder.value = "Inbox" // Reset to inbox on account change
        _selectedEmailId.value = null // Close reading pane
    }

    fun selectFolder(folder: String) {
        _selectedFolder.value = folder
        _selectedEmailId.value = null // Close reading pane
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectEmail(id: Int?) {
        _selectedEmailId.value = id
        if (id != null) {
            // Automatically mark email as read
            viewModelScope.launch {
                repository.updateReadStatus(id, true)
            }
        }
    }

    fun toggleEmailStar(email: EmailEntity) {
        viewModelScope.launch {
            repository.updateStarredStatus(email.id, !email.isStarred)
        }
    }

    fun toggleEmailRead(email: EmailEntity) {
        viewModelScope.launch {
            repository.updateReadStatus(email.id, !email.isRead)
        }
    }

    fun moveEmailToFolder(emailId: Int, targetFolder: String) {
        viewModelScope.launch {
            repository.moveEmailToFolder(emailId, targetFolder)
            if (_selectedEmailId.value == emailId) {
                _selectedEmailId.value = null // Close if opened
            }
        }
    }

    fun composeNewEmail() {
        composeTo.value = ""
        composeCc.value = ""
        composeBcc.value = ""
        composeSubject.value = ""
        composeBody.value = ""
        composeAttachments.value = emptyList()
        _composeStatus.value = ComposeStatus.Idle
    }

    fun addMockAttachment() {
        val count = composeAttachments.value.size + 1
        val isPdf = count % 2 == 1
        val name = if (isPdf) "وثيقة_المشروع_ملف_$count.pdf" else "صورة_معاينة_التصميم_$count.png"
        val size = if (isPdf) "${(1.2 + count * 0.3).toString().take(3)} MB" else "${(400 + count * 150)} KB"
        val type = if (isPdf) "application/pdf" else "image/png"

        val mockAttachment = AttachmentEntity(
            emailId = 0, // Assigned later
            fileName = name,
            fileSize = size,
            fileType = type
        )
        composeAttachments.value = composeAttachments.value + mockAttachment
    }

    fun removeMockAttachment(index: Int) {
        val list = composeAttachments.value.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            composeAttachments.value = list
        }
    }

    fun sendEmail() {
        val toValue = composeTo.value
        val subjectValue = composeSubject.value
        val bodyValue = composeBody.value
        val currentAcc = _selectedAccount.value

        if (currentAcc == null) {
            _composeStatus.value = ComposeStatus.Error("لا يوجد حساب نشط للإرسال منه")
            return
        }

        if (toValue.isBlank() || !toValue.contains("@")) {
            _composeStatus.value = ComposeStatus.Error("الرجاء إدخال بريد إلكتروني صحيح للمستلم")
            return
        }

        if (subjectValue.isBlank()) {
            _composeStatus.value = ComposeStatus.Error("الرجاء إدخال موضوع الرسالة")
            return
        }

        _composeStatus.value = ComposeStatus.Loading

        viewModelScope.launch {
            try {
                // Simulate network latency (SMTP connection and security exchange)
                kotlinx.coroutines.delay(1000)

                val sentEmail = EmailEntity(
                    accountId = currentAcc.id,
                    senderName = currentAcc.displayName,
                    senderEmail = currentAcc.emailAddress,
                    recipientEmail = toValue,
                    cc = composeCc.value,
                    bcc = composeBcc.value,
                    subject = subjectValue,
                    body = bodyValue,
                    timestamp = System.currentTimeMillis(),
                    isRead = true, // Emails sent are already read
                    isStarred = false,
                    folder = "Sent",
                    hasAttachments = composeAttachments.value.isNotEmpty()
                )

                repository.sendOrSaveEmail(sentEmail, composeAttachments.value)
                _composeStatus.value = ComposeStatus.Success
            } catch (e: Exception) {
                _composeStatus.value = ComposeStatus.Error("حدث خطأ أثناء الاتصال بخادم SMTP: ${e.localizedMessage}")
            }
        }
    }

    // Account Creation Window State
    var accountAddress = MutableStateFlow("")
        private set
    var accountDisplayName = MutableStateFlow("")
        private set
    var accountProvider = MutableStateFlow("Gmail")
        private set
    var accountIncomingServer = MutableStateFlow("imap.gmail.com")
        private set
    var accountOutgoingServer = MutableStateFlow("smtp.gmail.com")
        private set
    var accountPort = MutableStateFlow("993")
        private set
    var accountColorHex = MutableStateFlow("#EA4335")
        private set

    private val _accountCreationStatus = MutableStateFlow<AccountCreationStatus>(AccountCreationStatus.Idle)
    val accountCreationStatus: StateFlow<AccountCreationStatus> = _accountCreationStatus.asStateFlow()

    fun updateAccountProvider(provider: String) {
        accountProvider.value = provider
        when (provider) {
            "Gmail" -> {
                accountIncomingServer.value = "imap.gmail.com"
                accountOutgoingServer.value = "smtp.gmail.com"
                accountPort.value = "993"
                accountColorHex.value = "#EA4335"
            }
            "Outlook" -> {
                accountIncomingServer.value = "outlook.office365.com"
                accountOutgoingServer.value = "smtp.office365.com"
                accountPort.value = "993"
                accountColorHex.value = "#0078D4"
            }
            "Yahoo" -> {
                accountIncomingServer.value = "imap.mail.yahoo.com"
                accountOutgoingServer.value = "smtp.mail.yahoo.com"
                accountPort.value = "993"
                accountColorHex.value = "#6001D2"
            }
            else -> {
                accountIncomingServer.value = "imap.corporate.com"
                accountOutgoingServer.value = "smtp.corporate.com"
                accountPort.value = "993"
                accountColorHex.value = "#4CAF50"
            }
        }
    }

    fun createAccount() {
        val address = accountAddress.value
        val name = accountDisplayName.value
        val provider = accountProvider.value
        val incoming = accountIncomingServer.value
        val outgoing = accountOutgoingServer.value
        val portStr = accountPort.value

        if (address.isBlank() || !address.contains("@")) {
            _accountCreationStatus.value = AccountCreationStatus.Error("الرجاء إدخال عنوان بريد إلكتروني صالح")
            return
        }

        if (name.isBlank()) {
            _accountCreationStatus.value = AccountCreationStatus.Error("الرجاء إدخال الاسم المكتوب")
            return
        }

        val port = portStr.toIntOrNull() ?: 993
        _accountCreationStatus.value = AccountCreationStatus.Loading

        viewModelScope.launch {
            try {
                // Simulate IMAP authentication exchange secure handshake
                kotlinx.coroutines.delay(1200)

                val newAccount = AccountEntity(
                    emailAddress = address,
                    displayName = "$name ($provider)",
                    provider = provider,
                    incomingServer = incoming,
                    outgoingServer = outgoing,
                    port = port,
                    colorHex = accountColorHex.value
                )

                val newId = repository.createAccount(newAccount)
                
                // Construct a warm welcome email for the newly registered account
                val welcomeEmail = EmailEntity(
                    accountId = newId,
                    senderName = "البريد الإلكتروني الذكي",
                    senderEmail = "welcome@smart-mail.aistudio",
                    recipientEmail = address,
                    subject = "🎉 مرحباً بك في تطبيق البريد الإلكتروني الذكي!",
                    body = """
                        <h3>عزيزي الفاضل $name،</h3>
                        <p>نحن سعداء جداً بتسجيلك لحسابك الجديد <b>$address</b> عبر منصتنا الذكية.</p>
                        <p>تطبيقنا مصمم بأعلى درجات الكفاءة البرمجية، متضمناً معايير حماية استئنائية، وتزامن بيانات مستقر في الخلفية وقاعدة بيانات <b>Room Offline Persistence</b> لتصفح سريع دوماً دون إنترنت.</p>
                        <p>تمنياتنا لك بتجربة اتصال آمنة وموفقة!</p>
                        <p>فريق تطوير التطوير بأندرويد 🚀</p>
                    """.trimIndent(),
                    timestamp = System.currentTimeMillis(),
                    isRead = false,
                    isStarred = true,
                    folder = "Inbox",
                    hasAttachments = false
                )
                repository.sendOrSaveEmail(welcomeEmail)

                // Retrieve and change to the newly created account
                val createdAcc = repository.allAccounts.first().find { it.id == newId }
                if (createdAcc != null) {
                    _selectedAccount.value = createdAcc
                    _selectedFolder.value = "Inbox"
                }

                _accountCreationStatus.value = AccountCreationStatus.Success

                // Reset creation fields
                accountAddress.value = ""
                accountDisplayName.value = ""
                _accountCreationStatus.value = AccountCreationStatus.Idle
            } catch (e: Exception) {
                _accountCreationStatus.value = AccountCreationStatus.Error("فشلت عملية التحقق بروتوكول IMAP: ${e.localizedMessage}")
            }
        }
    }

    fun resetAccountCreation() {
        _accountCreationStatus.value = AccountCreationStatus.Idle
    }
}

sealed class ComposeStatus {
    object Idle : ComposeStatus()
    object Loading : ComposeStatus()
    object Success : ComposeStatus()
    data class Error(val message: String) : ComposeStatus()
}

sealed class AccountCreationStatus {
    object Idle : AccountCreationStatus()
    object Loading : AccountCreationStatus()
    object Success : AccountCreationStatus()
    data class Error(val message: String) : AccountCreationStatus()
}
