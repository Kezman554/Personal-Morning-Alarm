package com.personalmorningalarm.data.model

/**
 * One item from Alfred's /chalkboard (the vault's rolling to-do).
 *
 * Ticked items stay in the response until the Pi's overnight housekeeping sweeps
 * them, marked done in [line] — there is no separate completion field, the raw
 * markdown is the truth.
 *
 * All fields are nullable because they come off the wire: [date] is genuinely
 * optional (absent on some items), [line] is absent from responses cached before
 * the API started sending it, and [task] is defensive against a malformed entry.
 */
data class ChalkboardTaskDto(
    val task: String?,
    val date: String?,
    /**
     * The item's raw markdown line in the vault, e.g. "- [ ] Fix fence (2026-07-05)".
     * This is the targeting key for tick/drop — sent back verbatim, never edited
     * locally beyond the optimistic "[ ]"→"[x]" flip.
     */
    val line: String? = null
)
