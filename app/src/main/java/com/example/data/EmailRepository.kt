package com.example.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.Calendar

class EmailRepository(private val database: EmailDatabase) {

    private val accountDao = database.accountDao()
    private val emailDao = database.emailDao()

    val allAccounts: Flow<List<AccountEntity>> = accountDao.getAllAccounts()

    fun getEmailsByFolder(accountId: Int, folder: String): Flow<List<EmailEntity>> {
        return emailDao.getEmailsByFolder(accountId, folder)
    }

    fun getStarredEmails(accountId: Int): Flow<List<EmailEntity>> {
        return emailDao.getStarredEmails(accountId)
    }

    fun searchEmails(accountId: Int, query: String): Flow<List<EmailEntity>> {
        if (query.isBlank()) {
            return emailDao.getEmailsByFolder(accountId, "Inbox")
        }
        // Escape LIKE wildcards % _ and \ — prevents injection and mis-search
        val escaped = query
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
        return emailDao.searchEmails(accountId, "%$escaped%")
    }

    fun getEmailById(id: Int): Flow<EmailEntity?> {
        return emailDao.getEmailByIdFlow(id)
    }

    suspend fun getEmailByIdOneShot(id: Int): EmailEntity? {
        return emailDao.getEmailById(id)
    }

    fun getAttachments(emailId: Int): Flow<List<AttachmentEntity>> {
        return emailDao.getAttachmentsForEmail(emailId)
    }

    suspend fun sendOrSaveEmail(email: EmailEntity, attachments: List<AttachmentEntity> = emptyList()) {
        val insertedEmailId = emailDao.insertEmail(email)
        if (attachments.isNotEmpty()) {
            val updatedAttachments = attachments.map { it.copy(emailId = insertedEmailId.toInt()) }
            emailDao.insertAttachments(updatedAttachments)
        }
    }

    suspend fun updateReadStatus(emailId: Int, isRead: Boolean) {
        emailDao.updateReadStatus(emailId, isRead)
    }

    suspend fun updateStarredStatus(emailId: Int, isStarred: Boolean) {
        emailDao.updateStarredStatus(emailId, isStarred)
    }

    suspend fun moveEmailToFolder(emailId: Int, folder: String) {
        emailDao.moveEmailToFolder(emailId, folder)
    }

    suspend fun deleteEmailPermanently(email: EmailEntity) {
        emailDao.deleteEmail(email)
    }

    suspend fun createAccount(account: AccountEntity): Int {
        return accountDao.insertAccount(account).toInt()
    }

    // Seed mock database if empty — transaction ensures atomicity
    suspend fun prepopulateDatabaseIfEmpty() {
        val currentAccounts = accountDao.getAllAccounts().first()
        if (currentAccounts.isEmpty()) {
            database.withTransaction {
            // 1. Create Default Accounts
            val gmailAccountId = accountDao.insertAccount(
                AccountEntity(
                    emailAddress = "ahmedalbannatech@gmail.com",
                    displayName = "أحمد البنا (العمل)",
                    provider = "Gmail",
                    incomingServer = "imap.gmail.com",
                    outgoingServer = "smtp.gmail.com",
                    port = 993,
                    colorHex = "#EA4335"
                )
            ).toInt()

            val outlookAccountId = accountDao.insertAccount(
                AccountEntity(
                    emailAddress = "ahmed.al_banna@outlook.com",
                    displayName = "أحمد البنا (شخصي)",
                    provider = "Outlook",
                    incomingServer = "outlook.office365.com",
                    outgoingServer = "smtp.office365.com",
                    port = 993,
                    colorHex = "#0078D4"
                )
            ).toInt()

            // Time helper
            val calendar = Calendar.getInstance()

            // 2. Sample Emails for Gmail (Work)
            calendar.add(Calendar.MINUTE, -10)
            val emailGmail1 = EmailEntity(
                accountId = gmailAccountId,
                senderName = "مؤيد الحارثي - شركة الخليج",
                senderEmail = "m.harithi@gulftech.sa",
                recipientEmail = "ahmedalbannatech@gmail.com",
                subject = "🎯 مناقشة عرض الشراكة البرمجية وتكامل قاعدة البيانات للمشروع الجديد",
                body = """
                    <p>أهلاً بك يا مهندس أحمد، أتمنى أن تكون بأفضل حال.</p>
                    <p>لقد قمنا بمراجعة التحليل المبدئي ومخطط قاعدة بيانات <b>Room Database</b> المقترح لتطبيق البريد الإلكتروني الجديد، ونحن متحمسون جداً لاعتماده ومتابعة تنفيذ معمارية <b>MVVM</b> التي طرحتها.</p>
                    <p>نقترح عقد اجتماع سريع عبر Google Meet غداً الأحد الساعة 1:00 ظهراً لمناقشة التفاصيل التقنية النهائية والبدء الفعلي في التطوير. هل هذا التوقيت مناسب لك؟</p>
                    <p>وتفضلوا بقبول خالص الاحترام والتقدير،<br><b>مؤيد الحارثي - مدير قسم التقنية</b></p>
                """.trimIndent(),
                timestamp = calendar.timeInMillis,
                isRead = false,
                isStarred = true,
                folder = "Inbox",
                hasAttachments = true
            )

            calendar.add(Calendar.HOUR, -2)
            val emailGmail2 = EmailEntity(
                accountId = gmailAccountId,
                senderName = "مجلة التقنية العربية",
                senderEmail = "newsletter@arabtech.com",
                recipientEmail = "ahmedalbannatech@gmail.com",
                subject = "🚀 أحدث مستجدات الذكاء الاصطناعي وهندسة الهواتف لعام 2026",
                body = """
                    <div style="font-family: Arial, sans-serif; direction: rtl;">
                        <h2>مرحباً أحمد، إليك نشرتك الأسبوعية من عالم التقنية:</h2>
                        <ul>
                            <li><b>Jetpack Compose 2.2:</b> كيف غيرت أدوات المعاينة والأداء الديناميكي طريقة رسم الواجهات لأندرويد بالكامل؟</li>
                            <li><b>Kotlin 2.2:</b> تفاصيل مجمّع K2 الجديد والتحسينات المذهلة في سرعة البناء واستهلاك الذاكرة.</li>
                            <li><b>أمن التطبيقات:</b> أفضل الممارسات لإدارة مفاتيح الـ API وفصل الصلاحيات في الهاتف وتفادي الثغرات الشائعة.</li>
                        </ul>
                        <p>قراءة ممتعة ونلتقي في الأسبوع القادم!</p>
                    </div>
                """.trimIndent(),
                timestamp = calendar.timeInMillis,
                isRead = true,
                isStarred = false,
                folder = "Inbox",
                hasAttachments = false
            )

            calendar.add(Calendar.DAY_OF_YEAR, -1)
            val emailGmail3 = EmailEntity(
                accountId = gmailAccountId,
                senderName = "المهندس أحمد البنا",
                senderEmail = "ahmedalbannatech@gmail.com",
                recipientEmail = "manager@gulftech.sa",
                cc = "ceo@gulftech.sa",
                subject = "💡 تقرير أداء النظم وفحص استقرار الاتصال بالخادم",
                body = """
                    <p>عزيزي المدير المهني،</p>
                    <p>أرفق لكم طيه تقرير الأداء الفني للأسبوع الماضي. تم بحمد الله معالجة مشكلة تداخل الطلبات وحماية واجهة التطبيق من التجمد (UI Bottleneck) عبر تحويل العمليات الثقيلة إلى <b>Dispatchers.Default</b> و <b>Dispatchers.IO</b> باستخدام الكوروتين.</p>
                    <p>التطبيق يعمل الآن بكفاءة وبمعدل استجابة مثالي دون أي تجميد للـ UI thread.</p>
                    <br>دمتم في أمان الله،<br><b>م. أحمد البنا - مطور تطبيقات أول</b>
                """.trimIndent(),
                timestamp = calendar.timeInMillis,
                isRead = true,
                isStarred = false,
                folder = "Sent",
                hasAttachments = true
            )

            calendar.add(Calendar.DAY_OF_YEAR, -2)
            val emailGmail4 = EmailEntity(
                accountId = gmailAccountId,
                senderName = "الفائز المحظوظ",
                senderEmail = "spammer99@lottery-win-fast.xyz",
                recipientEmail = "ahmedalbannatech@gmail.com",
                subject = "💰 تهانينا! لقد ربحت 1,500,000 دولار كاش - انقر هنا فوراً للتسلم!",
                body = """
                    <h3>مبروووك! أنت الفائز المحظوظ بالقرعة الكبرى!</h3>
                    <p>تم اختيار بريدك الإلكتروني للفوز بالمرتبة الأولى. الرجاء تسجيل بياناتك البنكية ورقم هويتك عبر هذا الرابط المشبوه لاستلام شيكك حالاً.</p>
                    <p>عرض مؤقت لـ 24 ساعة فقط!</p>
                """.trimIndent(),
                timestamp = calendar.timeInMillis,
                isRead = false,
                isStarred = false,
                folder = "Spam",
                hasAttachments = false
            )

            calendar.add(Calendar.DAY_OF_YEAR, -3)
            val emailGmail5 = EmailEntity(
                accountId = gmailAccountId,
                senderName = "الأمن والسياسة لجوجل",
                senderEmail = "security-noreply@accounts.google.com",
                recipientEmail = "ahmedalbannatech@gmail.com",
                subject = "⚠️ تنبيه أمني: تسجيل دخول جديد من متصفح غير مألوف",
                body = """
                    <p>تم رصد تسجيل دخول جديد لحسابك على جهاز Windows من متصفح Firefox في مدينة الدمام، المملكة العربية السعودية.</p>
                    <p>إذا لم تكن أنت من قام بهذا الإجراء، يرجى الدخول فوراً وتغيير كلمة المرور وتفعيل ميزة التحقق بخطوتين لحماية حسابك.</p>
                """.trimIndent(),
                timestamp = calendar.timeInMillis,
                isRead = true,
                isStarred = false,
                folder = "Trash",
                hasAttachments = false
            )

            val mailIds = emailDao.insertEmails(listOf(emailGmail1, emailGmail2, emailGmail3, emailGmail4, emailGmail5))

            // Add attachments mockup — use returned IDs (fixes orphan FK bug)
            if (mailIds.size >= 3) {
                emailDao.insertAttachments(listOf(
                    AttachmentEntity(
                        emailId = mailIds[0].toInt(), // Moayad's email (emailGmail1)
                        fileName = "Room_Schema_Proposal.pdf",
                        fileSize = "2.4 MB",
                        fileType = "application/pdf"
                    ),
                    AttachmentEntity(
                        emailId = mailIds[2].toInt(), // Sent report (emailGmail3)
                        fileName = "Performance_Report_June_2026.xlsx",
                        fileSize = "1.1 MB",
                        fileType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    )
                ))
            }


            // 3. Sample Emails for Outlook (Personal)
            val calendarPersonal = Calendar.getInstance()
            calendarPersonal.add(Calendar.MINUTE, -30)
            val emailOutlook1 = EmailEntity(
                accountId = outlookAccountId,
                senderName = "مؤسسة التدريب التقني الرقمي",
                senderEmail = "courses@techtraining.sa",
                recipientEmail = "ahmed.al_banna@outlook.com",
                subject = "🎓 تذكير: غداً البداية المباشرة لكورس Jetpack Compose المتقدم والـ RTL واجهات",
                body = """
                    <p>أهلاً أحمد،</p>
                    <p>نهنئك على إكمال حجزك في الدورة المتقدمة لتصميم وهندسة واجهات أندرويد الحديثة. غداً الأحد تبدأ المحاضرة الأولى التفاعلية، وسنتطرق فيها إلى:</p>
                    <ol>
                        <li><b>دعم العربية الكامل (RTL Auto-Mirroring)</b> وتنسيق الهوامش بشكل هندسي مرن ومناسب لعكس الاتجاهات.</li>
                        <li><b>شريط التنقل الجانبي Drawer</b> والمكونات الطافية ومثبتات الأداء بـ StateFlow.</li>
                        <li>تصميم الـ Adaptive layouts للشاشات الكبيرة واللوائح المزدوجة.</li>
                    </ol>
                    <p>روابط الحضور ستصلك قبل نصف ساعة من البدء. نتمنى لك مسيرة تدريبية حافلة ومثمرة!</p>
                """.trimIndent(),
                timestamp = calendarPersonal.timeInMillis,
                isRead = false,
                isStarred = false,
                folder = "Inbox",
                hasAttachments = false
            )

            calendarPersonal.add(Calendar.HOUR, -6)
            val emailOutlook2 = EmailEntity(
                accountId = outlookAccountId,
                senderName = "أبو سعود (جروب العائلة)",
                senderEmail = "abo-saud@familyhub.sa",
                recipientEmail = "ahmed.al_banna@outlook.com",
                subject = "🏡 دعوة مأدبة عشاء العائلة الكبرى يوم الجمعة",
                body = """
                    <p>مرحباً يا غالي يا بوشهاب،</p>
                    <p>أخيراً عاد ابن العم خالد من السفر ويسعدنا جداً دعوتكم جميعاً مع العائلة الكريمة في ديواننا العامر يوم الجمعة القادم بعد صلاة العشاء مباشرة لتناول العشاء والاحتفاء بعودته سالماً غانماً.</p>
                    <p>حضورك يبهجنا ويسعدنا، لا تتعذر ببرمجة تطبيقاتك هذا الأسبوع!</p>
                    <p>أخوك ومحبك، أبو سعود</p>
                """.trimIndent(),
                timestamp = calendarPersonal.timeInMillis,
                isRead = true,
                isStarred = true,
                folder = "Inbox",
                hasAttachments = false
            )

            emailDao.insertEmails(listOf(emailOutlook1, emailOutlook2))
            }
        }
    }
}
