package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.viewmodel.ComposeStatus
import com.example.viewmodel.EmailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeEmailPane(
    viewModel: EmailViewModel,
    onCloseClick: () -> Unit
) {
    val to by viewModel.composeTo.collectAsStateWithLifecycle()
    val cc by viewModel.composeCc.collectAsStateWithLifecycle()
    val bcc by viewModel.composeBcc.collectAsStateWithLifecycle()
    val subject by viewModel.composeSubject.collectAsStateWithLifecycle()
    val body by viewModel.composeBody.collectAsStateWithLifecycle()
    val attachments by viewModel.composeAttachments.collectAsStateWithLifecycle()
    val composeStatus by viewModel.composeStatus.collectAsStateWithLifecycle()

    var showExtras by remember { mutableStateOf(false) }

    LaunchedEffect(composeStatus) {
        if (composeStatus is ComposeStatus.Success) {
            onCloseClick()
        }
    }

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
            IconButton(onClick = onCloseClick) {
                Icon(Icons.Default.Close, contentDescription = "إلغاء")
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "كتابة بريد جديد",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = { viewModel.addMockAttachment() }) {
                Icon(Icons.Default.Add, contentDescription = "إرفاق ملف")
            }

            // Gemini Smart Compose
            IconButton(
                onClick = { viewModel.suggestSmartCompose(body.ifBlank { subject.ifBlank { to } }) },
                enabled = composeStatus !is ComposeStatus.Loading
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = "اقتراح ذكي",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(
                onClick = { viewModel.sendEmail() },
                enabled = composeStatus !is ComposeStatus.Loading
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "إرسال",
                    tint = if (composeStatus is ComposeStatus.Loading) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.primary
                )
            }
        }

        if (composeStatus is ComposeStatus.Loading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        if (composeStatus is ComposeStatus.Error) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "خطأ",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = (composeStatus as ComposeStatus.Error).message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 12.sp
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = to,
                    onValueChange = { viewModel.onComposeToChange(it) },
                    label = { Text("إلى المستلم (To)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showExtras = !showExtras }) {
                        Text(if (showExtras) "إخفاء Cc / Bcc" else "إظهار Cc / Bcc", fontSize = 12.sp)
                    }
                }
            }

            if (showExtras) {
                item {
                    OutlinedTextField(
                        value = cc,
                        onValueChange = { viewModel.onComposeCcChange(it) },
                        label = { Text("نسخة كربونية (Cc)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = bcc,
                        onValueChange = { viewModel.onComposeBccChange(it) },
                        label = { Text("نسخة مخفية (Bcc)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            item {
                OutlinedTextField(
                    value = subject,
                    onValueChange = { viewModel.onComposeSubjectChange(it) },
                    label = { Text("موضوع الرسالة (Subject)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                OutlinedTextField(
                    value = body,
                    onValueChange = { viewModel.onComposeBodyChange(it) },
                    label = { Text("اكتب محتوى الرسالة هنا...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (attachments.isNotEmpty()) {
                item {
                    Text(
                        text = "الملفات المرفقة (${attachments.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(attachments) { index, att ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Attachment",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = att.fileName,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.widthIn(max = 120.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = { viewModel.removeMockAttachment(index) },
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Delete",
                                            modifier = Modifier.size(12.dp)
                                        )
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
