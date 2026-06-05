package com.personalmorningalarm.challenge

import com.personalmorningalarm.data.model.ContentType

/**
 * Decides which content screen (if any) fills each gap between NFC checkpoints.
 * With N tags there are N-1 internal gaps; the enabled content types are cycled
 * across them in display order. Empty list -> no content.
 */
class ContentScreenManager(private val enabledTypes: List<ContentType>) {

    val hasContent: Boolean get() = enabledTypes.isNotEmpty()

    /** Content for the gap after the (0-based) checkpoint just completed, or null. */
    fun contentForGap(gapIndex: Int): ContentType? {
        if (enabledTypes.isEmpty()) return null
        return enabledTypes[gapIndex % enabledTypes.size]
    }
}
