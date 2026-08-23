package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.EmailEntity
import com.example.ui.util.rememberListDateFormat
import java.util.Date

@Composable
fun EmailItem(
    email: EmailEntity,
    isSelected: Boolean,
    onClick: () -> Unit,
    onStarToggle: () -> Unit
) {
    val unreadWeight = if (!email.isRead) FontWeight.Bold else FontWeight.Normal
    val unreadColor = if (!email.isRead) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    val dateFormat = rememberListDateFormat()
    val dateStr = remember(email.timestamp) { dateFormat.format(Date(email.timestamp)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer
            else if (!email.isRead) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(24.dp),
        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null
    ) {
        Row(
            modifier = Modifier
                .clickable { onClick() }
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            val initials = email.senderName.take(1)
            val (avatarBg, avatarText) = remember(email.senderEmail) {
                val index = Math.abs(email.senderEmail.hashCode()) % 4
                when (index) {
                    0 -> Pair(Color(0xFF0B57D0), Color.White)
                    1 -> Pair(Color(0xFFC2E7FF), Color(0xFF001D35))
                    2 -> Pair(Color(0xFFEADDFF), Color(0xFF21005D))
                    else -> Pair(Color(0xFFFFDADA), Color(0xFF410002))
                }
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(avatarBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    color = avatarText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = email.senderName,
                        fontSize = 14.sp,
                        fontWeight = if (!email.isRead) FontWeight.Bold else FontWeight.SemiBold,
                        color = unreadColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = dateStr,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = email.subject,
                    fontSize = 13.sp,
                    fontWeight = unreadWeight,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                val strippedSnippet = remember(email.body) {
                    email.body.replace(Regex("<[^>]*>"), " ").trim()
                }

                Text(
                    text = strippedSnippet,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (email.hasAttachments) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            Icons.Default.AttachFile,
                            contentDescription = "مرفق",
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "يحتوي على مرفقات",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.height(56.dp)
            ) {
                if (!email.isRead) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                } else {
                    Spacer(modifier = Modifier.size(10.dp))
                }

                IconButton(
                    onClick = onStarToggle,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "تمييز بنجمة",
                        tint = if (email.isStarred) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
