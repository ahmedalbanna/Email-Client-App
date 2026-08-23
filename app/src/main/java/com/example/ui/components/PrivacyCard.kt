package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.EmailApplication
import com.example.R
import kotlinx.coroutines.launch

@Composable
fun PrivacyCard(modifier: Modifier = Modifier) {
    val app = LocalContext.current.applicationContext as? EmailApplication
    val aiGuardrail = app?.container?.aiGuardrail
    val scope = rememberCoroutineScope()
    val aiEnabled by aiGuardrail?.isAiEnabled?.collectAsState(initial = true) ?: remember { mutableStateOf(true) }
    val redactEnabled by aiGuardrail?.isRedactEnabled?.collectAsState(initial = true) ?: remember { mutableStateOf(true) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("الخصوصية — ما يُخزَّن محلياً", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Text(
                "• الحسابات والرسائل والمرفقات: 3 جداول Room (email_database) — مستثناة من النسخ السحابي، مسموحة للنقل فقط.\n" +
                "• بيانات الاعتماد: EncryptedSharedPreferences (AES256) — مستثناة من النسخ.\n" +
                "• المسودات والإعدادات: DataStore محلي.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
            HorizontalDivider()
            Text("الذكاء الاصطناعي — Gemini", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            Text(
                stringResource(R.string.ai_disclaimer) + " المعالجة عبر Gemini فقط عند تفعيل الميزة.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 15.sp
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("تفعيل Gemini", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Switch(checked = aiEnabled, onCheckedChange = { scope.launch { aiGuardrail?.setAiEnabled(it) } })
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("إخفاء البيانات الحساسة قبل الإرسال", fontSize = 11.sp)
                Switch(checked = redactEnabled, onCheckedChange = { scope.launch { aiGuardrail?.setRedactEnabled(it) } })
            }
            HorizontalDivider()
            Text(
                "الشبكة: cleartext محظور (networkSecurityConfig)، لا يتم إرسال مفتاح GEMINI في الـ APK بدون وكيل خادمي.",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
