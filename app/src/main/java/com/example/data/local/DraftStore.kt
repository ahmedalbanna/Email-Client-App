package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.draftDataStore by preferencesDataStore(name = "draft_store")

/**
 * Phase 2.4 — Draft auto-save via DataStore.
 * Persists unsent compose fields so drafts survive process death.
 */
class DraftStore(private val context: Context) {

    private object Keys {
        val TO = stringPreferencesKey("draft_to")
        val CC = stringPreferencesKey("draft_cc")
        val BCC = stringPreferencesKey("draft_bcc")
        val SUBJECT = stringPreferencesKey("draft_subject")
        val BODY = stringPreferencesKey("draft_body")
    }

    data class Draft(
        val to: String = "",
        val cc: String = "",
        val bcc: String = "",
        val subject: String = "",
        val body: String = ""
    )

    val draftFlow: Flow<Draft> = context.draftDataStore.data.map { prefs ->
        Draft(
            to = prefs[Keys.TO] ?: "",
            cc = prefs[Keys.CC] ?: "",
            bcc = prefs[Keys.BCC] ?: "",
            subject = prefs[Keys.SUBJECT] ?: "",
            body = prefs[Keys.BODY] ?: ""
        )
    }

    suspend fun saveDraft(draft: Draft) {
        context.draftDataStore.edit { prefs ->
            prefs[Keys.TO] = draft.to
            prefs[Keys.CC] = draft.cc
            prefs[Keys.BCC] = draft.bcc
            prefs[Keys.SUBJECT] = draft.subject
            prefs[Keys.BODY] = draft.body
        }
    }

    suspend fun clearDraft() {
        context.draftDataStore.edit { it.clear() }
    }
}
