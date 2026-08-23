package com.example.ui.util

import androidx.compose.ui.graphics.Color

fun parseColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color(0xFF4285F4)
    }
}
