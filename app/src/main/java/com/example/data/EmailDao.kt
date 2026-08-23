package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY id ASC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity): Long

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Delete
    suspend fun deleteAccount(account: AccountEntity)

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getAccountById(id: Int): AccountEntity?
}

@Dao
interface EmailDao {
    @Query("SELECT * FROM emails WHERE accountId = :accountId AND folder = :folder ORDER BY timestamp DESC")
    fun getEmailsByFolder(accountId: Int, folder: String): Flow<List<EmailEntity>>

    @Query("SELECT * FROM emails WHERE accountId = :accountId AND isStarred = 1 ORDER BY timestamp DESC")
    fun getStarredEmails(accountId: Int): Flow<List<EmailEntity>>

    @Query("SELECT * FROM emails WHERE id = :id LIMIT 1")
    fun getEmailByIdFlow(id: Int): Flow<EmailEntity?>

    @Query("SELECT * FROM emails WHERE id = :id LIMIT 1")
    suspend fun getEmailById(id: Int): EmailEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmail(email: EmailEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmails(emails: List<EmailEntity>): List<Long>

    @Update
    suspend fun updateEmail(email: EmailEntity)

    @Query("UPDATE emails SET isRead = :isRead WHERE id = :id")
    suspend fun updateReadStatus(id: Int, isRead: Boolean)

    @Query("UPDATE emails SET isStarred = :isStarred WHERE id = :id")
    suspend fun updateStarredStatus(id: Int, isStarred: Boolean)

    @Query("UPDATE emails SET folder = :folder WHERE id = :id")
    suspend fun moveEmailToFolder(id: Int, folder: String)

    @Delete
    suspend fun deleteEmail(email: EmailEntity)

    // Search query searching through subject, body, and sender details — ESCAPE '\' supports wildcard escaping
    @Query("""
        SELECT * FROM emails 
        WHERE accountId = :accountId 
          AND (senderName LIKE :query ESCAPE '\' OR senderEmail LIKE :query ESCAPE '\' OR subject LIKE :query ESCAPE '\' OR body LIKE :query ESCAPE '\') 
        ORDER BY timestamp DESC
    """)
    fun searchEmails(accountId: Int, query: String): Flow<List<EmailEntity>>

    // Attachments
    @Query("SELECT * FROM attachments WHERE emailId = :emailId")
    fun getAttachmentsForEmail(emailId: Int): Flow<List<AttachmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachments(attachments: List<AttachmentEntity>)
}
