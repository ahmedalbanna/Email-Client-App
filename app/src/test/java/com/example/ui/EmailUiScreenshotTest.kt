package com.example.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.EmailEntity
import com.example.ui.components.EmailItem
import com.example.ui.components.EmailListPane
import com.example.ui.components.EmptyReaderState
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class EmailUiScreenshotTest {

    @get:Rule val composeRule = createComposeRule()

    private val sampleEmail = EmailEntity(
        accountId = 1,
        senderName = "مؤيد الحارثي",
        senderEmail = "m.harithi@gulftech.sa",
        recipientEmail = "ahmed@b.com",
        subject = "🎯 مناقشة عرض الشراكة البرمجية",
        body = "<p>مرحبا أحمد، نقترح اجتماع غدا.</p>",
        timestamp = System.currentTimeMillis(),
        isRead = false,
        isStarred = true,
        folder = "Inbox",
        hasAttachments = true
    )

    @Test
    fun emailItem_unread_starred() {
        composeRule.setContent {
            MyApplicationTheme { EmailItem(email = sampleEmail, isSelected = false, onClick = {}, onStarToggle = {}) }
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/email_item.png")
    }

    @Test
    fun emailList_empty() {
        composeRule.setContent {
            MyApplicationTheme { EmailListPane(emails = emptyList(), selectedEmailId = null, selectedFolder = "Inbox", onEmailClick = {}, onStarToggle = {}) }
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/email_list_empty.png")
    }

    @Test
    fun emptyReader() {
        composeRule.setContent {
            MyApplicationTheme { EmptyReaderState() }
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/empty_reader.png")
    }

    @Test
    fun composeError_state() {
        // Simple error surface snapshot
        composeRule.setContent {
            MyApplicationTheme {
                androidx.compose.material3.Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxSize()) {
                    androidx.compose.material3.Text("الرجاء إدخال بريد إلكتروني صحيح", color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/compose_error.png")
    }
}
