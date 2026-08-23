package com.example.ai

import com.example.data.EmailEntity
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Fake Gemini — deterministic, offline, Arabic-first.
 * Swap with FirebaseAiGeminiService for production when GEMINI_API_KEY is configured.
 */
class FakeGeminiService : GeminiService {

    override suspend fun summarizeEmail(email: EmailEntity): Result<String> {
        delay(700)
        val plain = email.body.replace(Regex("<[^>]*>"), " ").trim().take(300)
        val summary = buildString {
            append("• الموضوع: ${email.subject}\n")
            append("• المرسل: ${email.senderName} <${email.senderEmail}>\n")
            append("• الملخص: ${plain.take(120)}…\n")
            if (email.hasAttachments) append("• يحتوي على مرفقات\n")
            append("• الإجراء الموصى به: ${recommendAction(email)}")
        }
        return Result.success(summary)
    }

    override suspend fun suggestCompose(context: String, tone: String): Result<GeminiService.ComposeSuggestion> {
        delay(900)
        val subject = when {
            context.contains("اجتماع", ignoreCase = true) -> "تأكيد موعد الاجتماع — $tone"
            context.contains("تقرير", ignoreCase = true) -> "متابعة تقرير الأداء"
            context.contains("شراكة", ignoreCase = true) -> "مقترح شراكة — نقاط المتابعة"
            else -> "رد: $context".take(50)
        }
        val body = buildString {
            append("السلام عليكم،\n\n")
            if (tone == "رسمي") append("أود متابعة بخصوص: $context\n\n")
            else append("حبيت أتابع معك بخصوص: $context\n\n")
            append("أقترح النقاط التالية:\n")
            append("1. تأكيد الموعد والمتطلبات\n")
            append("2. مشاركة المستندات ذات الصلة\n")
            append("3. تحديد الخطوة التالية\n\n")
            append("بانتظار ردكم الكريم.\nمع خالص التحية")
        }
        return Result.success(GeminiService.ComposeSuggestion(subject, body))
    }

    override suspend fun classifySpam(email: EmailEntity): Result<GeminiService.SpamResult> {
        delay(300)
        val spamKeywords = listOf("ربحت", "مبروك", "فلوس", "bank", "lottery", "win", "click here", "مليون")
        val bodyLower = (email.subject + " " + email.body).lowercase()
        val hits = spamKeywords.count { bodyLower.contains(it.lowercase()) }
        val fromSpamDomain = email.senderEmail.contains("lottery") || email.senderEmail.contains("xyz")
        val score = (hits * 0.25f + if (fromSpamDomain) 0.5f else 0f).coerceIn(0f, 1f)
        val label = when {
            score >= 0.7f -> "بريد مزعج"
            score >= 0.4f -> "مشبوه"
            else -> "آمن"
        }
        val reasons = mutableListOf<String>()
        if (hits > 0) reasons += "كلمات مفتاحية مشبوهة ($hits)"
        if (fromSpamDomain) reasons += "نطاق مرسل غير موثوق"
        if (reasons.isEmpty()) reasons += "لا توجد إشارات مزعجة"
        return Result.success(GeminiService.SpamResult(score, label, reasons))
    }

    override suspend fun rankByRelevance(query: String, emails: List<EmailEntity>): Result<List<EmailEntity>> {
        delay(200)
        if (query.isBlank()) return Result.success(emails)
        val q = query.lowercase()
        // Simple TF-style ranking + Arabic normalization fallback
        val scored = emails.map { e ->
            val text = (e.subject + " " + e.senderName + " " + e.body).lowercase()
            var score = 0f
            if (text.contains(q)) score += 10f
            // token overlap
            val tokens = q.split(Regex("\\s+"))
            score += tokens.count { text.contains(it) } * 2f
            // Recency boost (small)
            score += (1f / (1 + (System.currentTimeMillis() - e.timestamp) / 86400000f)) * 0.5f
            // Tiny random to simulate embedding variance
            score += Random.nextFloat() * 0.3f
            e to score
        }.sortedByDescending { it.second }.map { it.first }
        return Result.success(scored)
    }

    private fun recommendAction(email: EmailEntity): String = when (email.folder) {
        "Spam" -> "تجاهل/حذف"
        "Trash" -> "لا إجراء"
        else -> if (email.subject.contains("اجتماع") || email.subject.contains("مناقشة")) "الرد وتأكيد الموعد" else "القراءة والأرشفة"
    }
}
