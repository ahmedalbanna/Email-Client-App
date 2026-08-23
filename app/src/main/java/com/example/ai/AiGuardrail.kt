package com.example.ai

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.aiPrefs by preferencesDataStore(name = "ai_prefs")

/**
 * Phase 3.3 — Guardrails: opt-in, PII redaction, fallback.
 */
class AiGuardrail(private val context: Context) {

    private object Keys {
        val AI_ENABLED = booleanPreferencesKey("ai_enabled")
        val REDACT_PII = booleanPreferencesKey("redact_pii")
    }

    val isAiEnabled: Flow<Boolean> = context.aiPrefs.data.map { it[Keys.AI_ENABLED] ?: true }
    val isRedactEnabled: Flow<Boolean> = context.aiPrefs.data.map { it[Keys.REDACT_PII] ?: true }

    suspend fun setAiEnabled(enabled: Boolean) {
        context.aiPrefs.edit { it[Keys.AI_ENABLED] = enabled }
    }

    suspend fun setRedactEnabled(enabled: Boolean) {
        context.aiPrefs.edit { it[Keys.REDACT_PII] = enabled }
    }

    /** Redact emails, IBANs, phones before sending to AI. */
    fun redactPii(text: String, enabled: Boolean = true): String {
        if (!enabled) return text
        var out = text
        out = out.replace(Regex("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", RegexOption.IGNORE_CASE), "[بريد محجوب]")
        out = out.replace(Regex("\\b\\d{16,}\\b"), "[رقم محجوب]")
        out = out.replace(Regex("\\b05\\d{8}\\b"), "[جوال محجوب]")
        out = out.replace(Regex("SA\\d{2}[A-Z0-9]{18,}", RegexOption.IGNORE_CASE), "[آيبان محجوب]")
        return out
    }

    fun disclaimer(): String = GeminiConfig.DISCLAIMER
}
