package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.EmailEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailListPane(
    emails: List<EmailEntity>,
    selectedEmailId: Int?,
    selectedFolder: String,
    onEmailClick: (EmailEntity) -> Unit,
    onStarToggle: (EmailEntity) -> Unit,
    isRefreshing: Boolean = false,
    onRefresh: (() -> Unit)? = null
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
                contentDescription = stringResource(R.string.empty_reader),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.no_mail_here),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.inbox_empty_desc),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    } else {
        val list: @Composable () -> Unit = {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp, top = 8.dp)
            ) {
                items(emails, key = { it.id }, contentType = { "email" }) { email ->
                    val isSelected = email.id == selectedEmailId
                    // Phase 4.4 — Swipe-to-star: long-press already toggles, pull-to-refresh via box
                    EmailItem(
                        email = email,
                        isSelected = isSelected,
                        onClick = { onEmailClick(email) },
                        onStarToggle = { onStarToggle(email) }
                    )
                }
            }
        }
        if (onRefresh != null) {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize()
            ) { list() }
        } else {
            list()
        }
    }
}
