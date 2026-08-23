package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.util.parseColor
import com.example.viewmodel.AccountCreationStatus
import com.example.viewmodel.EmailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountPane(
    viewModel: EmailViewModel,
    onCloseClick: () -> Unit
) {
    val address by viewModel.accountAddress.collectAsStateWithLifecycle()
    val name by viewModel.accountDisplayName.collectAsStateWithLifecycle()
    val provider by viewModel.accountProvider.collectAsStateWithLifecycle()
    val hostIn by viewModel.accountIncomingServer.collectAsStateWithLifecycle()
    val hostOut by viewModel.accountOutgoingServer.collectAsStateWithLifecycle()
    val port by viewModel.accountPort.collectAsStateWithLifecycle()
    val colorHex by viewModel.accountColorHex.collectAsStateWithLifecycle()
    val creationStatus by viewModel.accountCreationStatus.collectAsStateWithLifecycle()

    var showManualSettings by remember { mutableStateOf(false) }

    LaunchedEffect(creationStatus) {
        if (creationStatus is AccountCreationStatus.Success) {
            onCloseClick()
            viewModel.resetAccountCreation()
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
                Icon(Icons.Default.Close, contentDescription = "الغاء")
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "إضافة حساب بريد إلكتروني",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (creationStatus is AccountCreationStatus.Loading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        if (creationStatus is AccountCreationStatus.Error) {
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
                        contentDescription = "خطأ من النظام",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = (creationStatus as AccountCreationStatus.Error).message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 12.sp
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "يرتبط تطبيقنا بأي خدمة بريد آمنة تدعم الـ IMAP و SMTP.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                Text(
                    text = "مزود الخدمة المفضل",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))

                val providers = listOf("Gmail", "Outlook", "Yahoo", "تخصيص يدوي")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    providers.forEach { p ->
                        val isSelected = (p == provider) || (p == "تخصيص يدوي" && provider !in listOf("Gmail", "Outlook", "Yahoo"))
                        val badgeBg = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        val badgeTextColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(badgeBg)
                                .clickable { viewModel.updateAccountProvider(if (p == "تخصيص يدوي") "Corporate" else p) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(p, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = badgeTextColor)
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = address,
                    onValueChange = { viewModel.onAccountAddressChange(it) },
                    label = { Text("عنوان البريد الإلكتروني") },
                    placeholder = { Text("example@domain.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { viewModel.onAccountDisplayNameChange(it) },
                    label = { Text("اسم الحساب (أو الاسم المعروض)") },
                    placeholder = { Text("أحمد البنا") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Text(
                    text = "لون تمييز الحساب",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                val paletteColors = listOf("#EA4335", "#0078D4", "#6001D2", "#4CAF50", "#FF9800", "#E91E63")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    paletteColors.forEach { hex ->
                        val isSelected = colorHex == hex
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(parseColor(hex))
                                .clickable { viewModel.onAccountColorHexChange(hex) }
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }
                    }
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("الإعدادات التقنية المتقدمة (خوادم IMAP/SMTP)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Switch(
                        checked = showManualSettings,
                        onCheckedChange = { showManualSettings = it }
                    )
                }
            }

            if (showManualSettings) {
                item {
                    OutlinedTextField(
                        value = hostIn,
                        onValueChange = { viewModel.onAccountIncomingServerChange(it) },
                        label = { Text("خادم استلام البريد (IMAP)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = hostOut,
                        onValueChange = { viewModel.onAccountOutgoingServerChange(it) },
                        label = { Text("خادم إرسال البريد (SMTP)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = port,
                            onValueChange = { viewModel.onAccountPortChange(it) },
                            label = { Text("المنفذ الآمن (Port)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .align(Alignment.CenterVertically)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("تشفير SSL/TLS نشط", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.createAccount() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = creationStatus !is AccountCreationStatus.Loading
                ) {
                    Text("التحقق وربط الحساب بالشفرة الآمنة", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}
