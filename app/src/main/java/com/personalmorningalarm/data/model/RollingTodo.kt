package com.personalmorningalarm.data.model

import com.personalmorningalarm.util.VaultText
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** One rolling to-do item, cleaned up and ready to render. */
data class RollingTodoItem(
    /** Wikilinks resolved; still carries emphasis markers for the renderer. */
    val task: String,
    /** Display date, or null when the item has none — never the string "null". */
    val date: String?,
    /** Raw vault line — the targeting key for tick/drop. Null on pre-line cached data. */
    val line: String? = null,
    /** Ticked in the vault, awaiting the overnight sweep. Shown struck through, not hidden. */
    val done: Boolean = false
)

/**
 * Turns Alfred's /chalkboard array into display-ready items: entries with no text
 * dropped, wikilinks resolved, and the date formatted for the faint secondary line
 * (omitted entirely when absent, rather than rendered as an empty or "null" line).
 */
object RollingTodo {

    // Items are dated by day in the vault (e.g. 2026-04-08). Anything else is
    // shown as sent rather than guessed at or dropped.
    private val DISPLAY_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.UK)

    // "- [x]" at the start of the raw line. Done state lives in the markdown
    // itself — the API sends no separate flag.
    private val TICKED = Regex("""^\s*-\s*\[[xX]]""")

    fun items(tasks: List<ChalkboardTaskDto>): List<RollingTodoItem> =
        tasks
            // A task with no text is nothing to show, whatever its date.
            .filter { !it.task.isNullOrBlank() }
            .map {
                RollingTodoItem(
                    task = VaultText.stripWikiLinks(it.task!!.trim()),
                    date = displayDate(it.date),
                    line = it.line,
                    done = it.line != null && TICKED.containsMatchIn(it.line)
                )
            }

    /**
     * ISO dates render as "8 Apr 2026". A date in any other shape is passed
     * through as-is — it's the vault's text, and showing it is better than
     * dropping information because it didn't parse. Absent or blank gives null,
     * so the screen omits the line.
     */
    private fun displayDate(raw: String?): String? {
        val trimmed = raw?.trim()
        if (trimmed.isNullOrEmpty()) return null
        return try {
            LocalDate.parse(trimmed).format(DISPLAY_FORMAT)
        } catch (e: Exception) {
            trimmed
        }
    }
}
