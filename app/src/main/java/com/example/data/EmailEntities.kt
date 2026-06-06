package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val emailAddress: String,
    val displayName: String,
    val provider: String, // e.g. "Gmail", "Outlook", "Corporate"
    val incomingServer: String = "",
    val outgoingServer: String = "",
    val port: Int = 993,
    val colorHex: String = "#4285F4" // Brand/Visual color for this account
)

@Entity(tableName = "emails")
data class EmailEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val accountId: Int, // Foreign key to AccountEntity if requested, but kept as Int for simpler joins
    val senderName: String,
    val senderEmail: String,
    val recipientEmail: String,
    val cc: String = "",
    val bcc: String = "",
    val subject: String,
    val body: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val isStarred: Boolean = false,
    val folder: String = "Inbox", // "Inbox", "Sent", "Drafts", "Spam", "Trash"
    val hasAttachments: Boolean = false
)

@Entity(tableName = "attachments")
data class AttachmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val emailId: Int,
    val fileName: String,
    val fileSize: String,
    val fileType: String
)
