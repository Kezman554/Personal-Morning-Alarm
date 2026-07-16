package com.personalmorningalarm.util

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan

/**
 * Renders the inline emphasis Alfred's task strings carry — **bold** and *italic* —
 * as styled text, so the markers themselves never reach the screen.
 *
 * Deliberately not a markdown parser: it handles the emphasis that actually turns
 * up in a task line and leaves everything else (including an unpaired asterisk,
 * which isn't emphasis) alone.
 */
object MarkdownRenderer {

    // Bold is listed first so `**x**` is matched as bold rather than as an italic
    // `*` wrapping `*x*`. `.+?` is lazy, so each run stops at its nearest closer,
    // and neither alternative may span a line break.
    private val EMPHASIS = Regex("""\*\*([^*\n]+)\*\*|\*([^*\n]+)\*""")

    fun render(source: String): CharSequence {
        if (!source.contains('*')) return source

        val out = SpannableStringBuilder()
        var cursor = 0
        for (match in EMPHASIS.findAll(source)) {
            out.append(source, cursor, match.range.first)

            val bold = match.groupValues[1]
            val text = bold.ifEmpty { match.groupValues[2] }
            val style = if (bold.isNotEmpty()) Typeface.BOLD else Typeface.ITALIC

            val start = out.length
            out.append(text)
            out.setSpan(StyleSpan(style), start, out.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            cursor = match.range.last + 1
        }
        out.append(source, cursor, source.length)
        return out
    }
}
