package com.example.ui

import com.example.ui.util.HtmlUtil
import org.junit.Assert.*
import org.junit.Test

class DateFormatUtilTest {

    @Test
    fun htmlToPlainText_stripsTags() {
        val html = "<p>Hello</p><br/>World <b>bold</b>"
        val plain = HtmlUtil.toPlainText(html)
        assertFalse(plain.contains("<"))
        assertTrue(plain.contains("Hello"))
        assertTrue(plain.contains("World"))
    }

    @Test
    fun htmlPreview_truncates() {
        val html = "<p>" + "a".repeat(200) + "</p>"
        val preview = HtmlUtil.toPreviewSnippet(html, 50)
        assertTrue(preview.length <= 51) // 50 + ellipsis
        assertTrue(preview.endsWith("…"))
    }

    @Test
    fun html_handlesArabic() {
        val html = "<p>مرحبا يا أحمد</p>"
        val plain = HtmlUtil.toPlainText(html)
        assertTrue(plain.contains("مرحبا"))
    }

    @Test
    fun html_handlesEmpty() {
        assertEquals("", HtmlUtil.toPlainText(""))
        assertEquals("", HtmlUtil.toPlainText("   "))
    }
}
