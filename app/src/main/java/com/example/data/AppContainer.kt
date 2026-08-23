package com.example.data

import android.content.Context

class AppContainer(private val context: Context) {
    val database: EmailDatabase by lazy { EmailDatabase.getDatabase(context) }
    val emailRepository: EmailRepository by lazy { EmailRepository(database) }
}
