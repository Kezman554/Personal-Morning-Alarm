package com.personalmorningalarm.data.model

/**
 * One unchecked item from Alfred's /chalkboard (the vault's rolling to-do).
 *
 * The endpoint returns only unchecked items — the app does no filtering, and
 * never writes back, so there is no completion field to carry.
 *
 * Both fields are nullable because they come off the wire: [date] is genuinely
 * optional (absent on some items), and [task] is defensive against a malformed
 * entry.
 */
data class ChalkboardTaskDto(
    val task: String?,
    val date: String?
)
