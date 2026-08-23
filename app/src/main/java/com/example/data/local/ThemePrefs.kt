package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore by preferencesDataStore(name = "theme_prefs")

class ThemePrefs(private val context: Context) {

    private object Keys {
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val FORCE_DARK = booleanPreferencesKey("force_dark")
        val FORCE_RTL = booleanPreferencesKey("force_rtl")
    }

    val dynamicColorFlow: Flow<Boolean> = context.themeDataStore.data.map { it[Keys.DYNAMIC_COLOR] ?: false }
    val forceDarkFlow: Flow<Boolean?> = context.themeDataStore.data.map {
        if (it.contains(Keys.FORCE_DARK)) it[Keys.FORCE_DARK] else null
    }
    val forceRtlFlow: Flow<Boolean?> = context.themeDataStore.data.map {
        if (it.contains(Keys.FORCE_RTL)) it[Keys.FORCE_RTL] else null
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.themeDataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun setForceDark(enabled: Boolean?) {
        context.themeDataStore.edit {
            if (enabled == null) it.remove(Keys.FORCE_DARK) else it[Keys.FORCE_DARK] = enabled
        }
    }

    suspend fun setForceRtl(enabled: Boolean?) {
        context.themeDataStore.edit {
            if (enabled == null) it.remove(Keys.FORCE_RTL) else it[Keys.FORCE_RTL] = enabled
        }
    }
}
