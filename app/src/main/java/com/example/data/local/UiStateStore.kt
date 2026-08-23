package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.uiStateStore by preferencesDataStore(name = "ui_state")

/**
 * Phase 4.2 — Persist selectedAccount/selectedFolder/searchQuery via DataStore
 * as lightweight complement to SavedStateHandle.
 */
class UiStateStore(private val context: Context) {

    private object Keys {
        val SELECTED_ACCOUNT_ID = intPreferencesKey("selected_account_id")
        val SELECTED_FOLDER = stringPreferencesKey("selected_folder")
        val SEARCH_QUERY = stringPreferencesKey("search_query")
    }

    val selectedAccountIdFlow: Flow<Int?> = context.uiStateStore.data.map { it[Keys.SELECTED_ACCOUNT_ID] }
    val selectedFolderFlow: Flow<String> = context.uiStateStore.data.map { it[Keys.SELECTED_FOLDER] ?: "Inbox" }
    val searchQueryFlow: Flow<String> = context.uiStateStore.data.map { it[Keys.SEARCH_QUERY] ?: "" }

    suspend fun setSelectedAccountId(id: Int?) {
        context.uiStateStore.edit { prefs ->
            if (id == null) prefs.remove(Keys.SELECTED_ACCOUNT_ID) else prefs[Keys.SELECTED_ACCOUNT_ID] = id
        }
    }

    suspend fun setSelectedFolder(folder: String) {
        context.uiStateStore.edit { it[Keys.SELECTED_FOLDER] = folder }
    }

    suspend fun setSearchQuery(query: String) {
        context.uiStateStore.edit { it[Keys.SEARCH_QUERY] = query }
    }
}
