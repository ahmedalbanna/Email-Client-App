package com.example.ai

import com.example.BuildConfig

object GeminiConfig {
    val apiKey: String
        get() = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

    val isConfigured: Boolean
        get() = apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY" && apiKey.length > 10

    const val DISCLAIMER = "يتم استخدام الذكاء الاصطناعي لمعالجة محتوى البريد محلياً؛ لا يتم حفظ محتوى الرسائل."
}
