package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class EmailViewModel(
    application: Application,
    private val repository: EmailRepository = EmailRepository(EmailDatabase.getDatabase(application))
) : AndroidViewModel(application) {

    // State of all accounts
    val accounts: StateFlow<List<AccountEntity>> = repository.allAccounts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

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
        // Prepopulate database if required and select first account on ready
        viewModelScope.launch {
            repository.prepopulateDatabaseIfEmpty()
            accounts.collect { accountList ->
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

    // Compose Window States — encapsulated
    private val _composeTo = MutableStateFlow("")
    val composeTo: StateFlow<String> = _composeTo.asStateFlow()
    private val _composeCc = MutableStateFlow("")
    val composeCc: StateFlow<String> = _composeCc.asStateFlow()
    private val _composeBcc = MutableStateFlow("")
    val composeBcc: StateFlow<String> = _composeBcc.asStateFlow()
    private val _composeSubject = MutableStateFlow("")
    val composeSubject: StateFlow<String> = _composeSubject.asStateFlow()
    private val _composeBody = MutableStateFlow("")
    val composeBody: StateFlow<String> = _composeBody.asStateFlow()
    private val _composeAttachments = MutableStateFlow<List<AttachmentEntity>>(emptyList())
    val composeAttachments: StateFlow<List<AttachmentEntity>> = _composeAttachments.asStateFlow()

    private val _composeStatus = MutableStateFlow<ComposeStatus>(ComposeStatus.Idle)
    val composeStatus: StateFlow<ComposeStatus> = _composeStatus.asStateFlow()

    fun onComposeToChange(v: String) { _composeTo.value = v }
    fun onComposeCcChange(v: String) { _composeCc.value = v }
    fun onComposeBccChange(v: String) { _composeBcc.value = v }
    fun onComposeSubjectChange(v: String) { _composeSubject.value = v }
    fun onComposeBodyChange(v: String) { _composeBody.value = v }

    fun selectAccount(account: AccountEntity) {
        _selectedAccount.value = account
        _selectedFolder.value = "Inbox"
        _selectedEmailId.value = null
    }

    fun selectFolder(folder: String) {
        _selectedFolder.value = folder
        _selectedEmailId.value = null
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectEmail(id: Int?) {
        _selectedEmailId.value = id
        if (id != null) {
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
                _selectedEmailId.value = null
            }
        }
    }

    fun composeNewEmail() {
        _composeTo.value = ""
        _composeCc.value = ""
        _composeBcc.value = ""
        _composeSubject.value = ""
        _composeBody.value = ""
        _composeAttachments.value = emptyList()
        _composeStatus.value = ComposeStatus.Idle
    }

    fun prepareReply(email: EmailEntity) {
        composeNewEmail()
        _composeTo.value = email.senderEmail
        _composeSubject.value = "رد: " + email.subject
        _composeBody.value = "\n\n----- الرسالة السابقة -----\n" + email.body.replace(Regex("<[^>]*>"), "")
    }

    fun addMockAttachment() {
        val count = _composeAttachments.value.size + 1
        val isPdf = count % 2 == 1
        val name = if (isPdf) "وثيقة_المشروع_ملف_$count.pdf" else "صورة_معاينة_التصميم_$count.png"
        val size = if (isPdf) "${(1.2 + count * 0.3).toString().take(3)} MB" else "${(400 + count * 150)} KB"
        val type = if (isPdf) "application/pdf" else "image/png"

        val mockAttachment = AttachmentEntity(
            emailId = 0,
            fileName = name,
            fileSize = size,
            fileType = type
        )
        _composeAttachments.value = _composeAttachments.value + mockAttachment
    }

    fun removeMockAttachment(index: Int) {
        val list = _composeAttachments.value.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            _composeAttachments.value = list
        }
    }

    fun sendEmail() {
        val toValue = _composeTo.value
        val subjectValue = _composeSubject.value
        val bodyValue = _composeBody.value
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
                kotlinx.coroutines.delay(1000)

                val sentEmail = EmailEntity(
                    accountId = currentAcc.id,
                    senderName = currentAcc.displayName,
                    senderEmail = currentAcc.emailAddress,
                    recipientEmail = toValue,
                    cc = _composeCc.value,
                    bcc = _composeBcc.value,
                    subject = subjectValue,
                    body = bodyValue,
                    timestamp = System.currentTimeMillis(),
                    isRead = true,
                    isStarred = false,
                    folder = "Sent",
                    hasAttachments = _composeAttachments.value.isNotEmpty()
                )

                repository.sendOrSaveEmail(sentEmail, _composeAttachments.value)
                _composeStatus.value = ComposeStatus.Success
            } catch (e: Exception) {
                _composeStatus.value = ComposeStatus.Error("حدث خطأ أثناء الاتصال بخادم SMTP: ${e.localizedMessage}")
            }
        }
    }

    // Account Creation Window State — encapsulated
    private val _accountAddress = MutableStateFlow("")
    val accountAddress: StateFlow<String> = _accountAddress.asStateFlow()
    private val _accountDisplayName = MutableStateFlow("")
    val accountDisplayName: StateFlow<String> = _accountDisplayName.asStateFlow()
    private val _accountProvider = MutableStateFlow("Gmail")
    val accountProvider: StateFlow<String> = _accountProvider.asStateFlow()
    private val _accountIncomingServer = MutableStateFlow("imap.gmail.com")
    val accountIncomingServer: StateFlow<String> = _accountIncomingServer.asStateFlow()
    private val _accountOutgoingServer = MutableStateFlow("smtp.gmail.com")
    val accountOutgoingServer: StateFlow<String> = _accountOutgoingServer.asStateFlow()
    private val _accountPort = MutableStateFlow("993")
    val accountPort: StateFlow<String> = _accountPort.asStateFlow()
    private val _accountColorHex = MutableStateFlow("#EA4335")
    val accountColorHex: StateFlow<String> = _accountColorHex.asStateFlow()

    private val _accountCreationStatus = MutableStateFlow<AccountCreationStatus>(AccountCreationStatus.Idle)
    val accountCreationStatus: StateFlow<AccountCreationStatus> = _accountCreationStatus.asStateFlow()

    fun onAccountAddressChange(v: String) { _accountAddress.value = v }
    fun onAccountDisplayNameChange(v: String) { _accountDisplayName.value = v }
    fun onAccountIncomingServerChange(v: String) { _accountIncomingServer.value = v }
    fun onAccountOutgoingServerChange(v: String) { _accountOutgoingServer.value = v }
    fun onAccountPortChange(v: String) { _accountPort.value = v }
    fun onAccountColorHexChange(v: String) { _accountColorHex.value = v }

    fun updateAccountProvider(provider: String) {
        _accountProvider.value = provider
        when (provider) {
            "Gmail" -> {
                _accountIncomingServer.value = "imap.gmail.com"
                _accountOutgoingServer.value = "smtp.gmail.com"
                _accountPort.value = "993"
                _accountColorHex.value = "#EA4335"
            }
            "Outlook" -> {
                _accountIncomingServer.value = "outlook.office365.com"
                _accountOutgoingServer.value = "smtp.office365.com"
                _accountPort.value = "993"
                _accountColorHex.value = "#0078D4"
            }
            "Yahoo" -> {
                _accountIncomingServer.value = "imap.mail.yahoo.com"
                _accountOutgoingServer.value = "smtp.mail.yahoo.com"
                _accountPort.value = "993"
                _accountColorHex.value = "#6001D2"
            }
            else -> {
                _accountIncomingServer.value = "imap.corporate.com"
                _accountOutgoingServer.value = "smtp.corporate.com"
                _accountPort.value = "993"
                _accountColorHex.value = "#4CAF50"
            }
        }
    }

    fun createAccount() {
        val address = _accountAddress.value
        val name = _accountDisplayName.value
        val provider = _accountProvider.value
        val incoming = _accountIncomingServer.value
        val outgoing = _accountOutgoingServer.value
        val portStr = _accountPort.value

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
                kotlinx.coroutines.delay(1200)

                val newAccount = AccountEntity(
                    emailAddress = address,
                    displayName = "$name ($provider)",
                    provider = provider,
                    incomingServer = incoming,
                    outgoingServer = outgoing,
                    port = port,
                    colorHex = _accountColorHex.value
                )

                val newId = repository.createAccount(newAccount)
                
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

                val createdAcc = repository.allAccounts.first().find { it.id == newId }
                if (createdAcc != null) {
                    _selectedAccount.value = createdAcc
                    _selectedFolder.value = "Inbox"
                }

                _accountCreationStatus.value = AccountCreationStatus.Success

                _accountAddress.value = ""
                _accountDisplayName.value = ""
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
