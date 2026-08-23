package com.example.util

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.telemetryStore by preferencesDataStore(name = "telemetry_prefs")

/**
 * Phase 7.2 — Opt-in, non-PII telemetry.
 * Records sync latency, screen perf, crash breadcrumbs locally; never logs email content.
 */
class Telemetry(private val context: Context) {

    private object Keys {
        val ENABLED = booleanPreferencesKey("telemetry_enabled")
        val LAST_SYNC_MS = longPreferencesKey("last_sync_ms")
        val LAST_SYNC_TIME = longPreferencesKey("last_sync_time")
    }

    val isEnabled: Flow<Boolean> = context.telemetryStore.data.map { it[Keys.ENABLED] ?: false }

    suspend fun setEnabled(enabled: Boolean) {
        context.telemetryStore.edit { it[Keys.ENABLED] = enabled }
    }

    suspend fun recordSyncLatency(latencyMs: Long) {
        if (!isEnabled.first()) return
        context.telemetryStore.edit {
            it[Keys.LAST_SYNC_MS] = latencyMs
            it[Keys.LAST_SYNC_TIME] = System.currentTimeMillis()
        }
        android.util.Log.d("Telemetry", "sync latency ${latencyMs}ms")
    }

    suspend fun recordScreenPerf(screen: String, latencyMs: Long) {
        if (!isEnabled.first()) return
        android.util.Log.d("Telemetry", "screen $screen perf ${latencyMs}ms")
    }
}
