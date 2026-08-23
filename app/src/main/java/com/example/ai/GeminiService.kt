package com.example.ai

import com.example.data.EmailEntity

/**
 * Phase 3 — Gemini AI abstraction.
 * Fake implementation provides deterministic Arabic responses without network.
 * Real impl would call Firebase AI / Generative AI REST with GEMINI_API_KEY.
 */
interface GeminiService {

    /** Summarize email thread in 3 Arabic bullets. */
    suspend fun summarizeEmail(email: EmailEntity): Result<String>

    /** Suggest subject + body for smart compose given context. */
    suspend fun suggestCompose(context: String, tone: String = "رسمي"): Result<ComposeSuggestion>

    /** Classify spam likelihood 0..1 (1 = spam). */
    suspend fun classifySpam(email: EmailEntity): Result<SpamResult>

    /** Semantic search — rank emails by relevance to query (local embedding fallback). */
    suspend fun rankByRelevance(query: String, emails: List<EmailEntity>): Result<List<EmailEntity>>

    data class ComposeSuggestion(val subject: String, val body: String)
    data class SpamResult(val score: Float, val label: String, val reasons: List<String>)
}
