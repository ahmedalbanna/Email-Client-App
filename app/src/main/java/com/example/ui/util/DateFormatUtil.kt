package com.example.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun rememberListDateFormat(): SimpleDateFormat {
    return remember { SimpleDateFormat("dd LLL yyyy, hh:mm a", Locale("ar")) }
}

@Composable
fun rememberDetailDateFormat(): SimpleDateFormat {
    return remember { SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale("ar")) }
}

fun formatDate(timestamp: Long, formatter: SimpleDateFormat): String {
    return formatter.format(Date(timestamp))
}
