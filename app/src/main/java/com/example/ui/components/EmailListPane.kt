package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.EmailEntity

@Composable
fun EmailListPane(
    emails: List<EmailEntity>,
    selectedEmailId: Int?,
    selectedFolder: String,
    onEmailClick: (EmailEntity) -> Unit,
    onStarToggle: (EmailEntity) -> Unit
) {
    if (emails.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Email,
                contentDescription = "فارغ",
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "لا توجد رسائل بريد هنا",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "صندوق البريد الخاص بك محدث ومزامر بالكامل ومحفوظ محلياً.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp, top = 8.dp)
        ) {
            items(emails, key = { it.id }, contentType = { "email" }) { email ->
                val isSelected = email.id == selectedEmailId
                EmailItem(
                    email = email,
                    isSelected = isSelected,
                    onClick = { onEmailClick(email) },
                    onStarToggle = { onStarToggle(email) }
                )
            }
        }
    }
}
