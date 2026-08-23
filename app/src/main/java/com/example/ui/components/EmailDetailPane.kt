package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AttachmentEntity
import com.example.data.EmailEntity
import com.example.ui.util.HtmlUtil
import com.example.ui.util.rememberDetailDateFormat
import kotlinx.coroutines.launch
import java.util.Date

@Composable
fun EmailDetailPane(
    email: EmailEntity,
    attachments: List<AttachmentEntity>,
    onBackClick: () -> Unit,
    onStarToggle: () -> Unit,
    onFolderMove: (Int, String) -> Unit,
    onReplyClick: () -> Unit,
    isTablet: Boolean,
    onSummarizeClick: (() -> Unit)? = null,
    onClassifySpamClick: (() -> Unit)? = null
) {
    val dateFormat = rememberDetailDateFormat()
    val dateStr = remember(email.timestamp) { dateFormat.format(Date(email.timestamp)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isTablet) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (onSummarizeClick != null) {
                IconButton(onClick = onSummarizeClick) {
                    Icon(Icons.Default.Info, contentDescription = "تلخيص بالذكاء")
                }
            }

            if (onClassifySpamClick != null) {
                IconButton(onClick = onClassifySpamClick) {
                    Icon(Icons.Default.Warning, contentDescription = "فحص مزعج")
                }
            }

            IconButton(onClick = onReplyClick) {
                Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = "رد")
            }

            IconButton(onClick = onStarToggle) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "تفضيل",
                    tint = if (email.isStarred) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }

            if (email.folder != "Trash") {
                IconButton(onClick = { onFolderMove(email.id, "Trash") }) {
                    Icon(Icons.Default.Delete, contentDescription = "نقل المهملات")
                }
            } else {
                IconButton(onClick = { onFolderMove(email.id, "Inbox") }) {
                    Icon(Icons.Default.Email, contentDescription = "استعادة لصندوق الوارد")
                }
            }

            if (email.folder != "Spam") {
                IconButton(onClick = { onFolderMove(email.id, "Spam") }) {
                    Icon(Icons.Default.Warning, contentDescription = "بريد مزعج")
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            item {
                Text(
                    text = email.subject,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 28.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = "مرسل",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = email.senderName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "من: <${email.senderEmail}>",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "إلى: ${email.recipientEmail}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = dateStr,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (email.cc.isNotBlank() || email.bcc.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "نسخة كربونية (Cc): ${email.cc}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            }

            item {
                val cleanBody = remember(email.body) {
                    HtmlUtil.toPlainText(email.body)
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = cleanBody,
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            if (attachments.isNotEmpty()) {
                item {
                    Text(
                        text = "المرفقات (${attachments.size})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )

                    Column {
                        attachments.forEach { attachment ->
                            var isDownloading by remember { mutableStateOf(false) }
                            var progress by remember { mutableStateOf(0f) }
                            var isDownloaded by remember { mutableStateOf(false) }
                            val scope = rememberCoroutineScope()

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                border = if (isDownloaded) CardDefaults.outlinedCardBorder() else null
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.AttachFile,
                                        contentDescription = "مرفق",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = attachment.fileName,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "الحجم: ${attachment.fileSize} • النوع: ${attachment.fileType.substringAfter("/")}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        if (isDownloading) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            LinearProgressIndicator(
                                                progress = { progress },
                                                modifier = Modifier.fillMaxWidth(),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Button(
                                        onClick = {
                                            if (!isDownloading && !isDownloaded) {
                                                isDownloading = true
                                                scope.launch {
                                                    while (progress < 1.0f) {
                                                        kotlinx.coroutines.delay(100)
                                                        progress += 0.1f
                                                    }
                                                    isDownloading = false
                                                    isDownloaded = true
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isDownloaded) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        if (isDownloaded) {
                                            Text("تم الحفظ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondary)
                                        } else if (isDownloading) {
                                            Text("جاري التنزل", fontSize = 11.sp)
                                        } else {
                                            Text("حفظ", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
