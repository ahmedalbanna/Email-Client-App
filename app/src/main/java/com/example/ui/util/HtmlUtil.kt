package com.example.ui.util

import androidx.core.text.HtmlCompat

/**
 * Phase 2.5 — Proper HTML→plain rendering.
 * Uses HtmlCompat which handles entities, <br>, <p>, <ul> etc. Better than Regex.
 */
object HtmlUtil {
    fun toPlainText(html: String): String {
        if (html.isBlank()) return ""
        // Normalize common tags before HtmlCompat for better line breaks
        val normalized = html
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("</p>", RegexOption.IGNORE_CASE), "\n\n")
            .replace(Regex("<p[^>]*>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("</li>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<li[^>]*>", RegexOption.IGNORE_CASE), "• ")
        return try {
            HtmlCompat.fromHtml(normalized, HtmlCompat.FROM_HTML_MODE_COMPACT).toString()
                .replace(Regex("\n{3,}"), "\n\n")
                .trim()
        } catch (e: Exception) {
            // Fallback to simple strip
            html.replace(Regex("<[^>]*>"), " ").trim()
        }
    }

    fun toPreviewSnippet(html: String, maxLength: Int = 120): String {
        val plain = toPlainText(html)
        return if (plain.length <= maxLength) plain else plain.take(maxLength) + "…"
    }
}
