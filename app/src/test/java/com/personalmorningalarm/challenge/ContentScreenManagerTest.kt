package com.personalmorningalarm.challenge

import com.personalmorningalarm.data.model.ContentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentScreenManagerTest {

    @Test
    fun `each enabled type shows once in order then no more`() {
        val manager = ContentScreenManager(listOf(ContentType.QUOTE, ContentType.STRETCH))
        assertEquals(ContentType.QUOTE, manager.contentForGap(0))
        assertEquals(ContentType.STRETCH, manager.contentForGap(1))
        // More gaps than enabled types must NOT cycle back round.
        assertNull(manager.contentForGap(2))
        assertNull(manager.contentForGap(3))
    }

    @Test
    fun `empty list never produces content`() {
        val manager = ContentScreenManager(emptyList())
        assertFalse(manager.hasContent)
        assertNull(manager.contentForGap(0))
    }

    @Test
    fun `hasContent reflects enabled types`() {
        assertTrue(ContentScreenManager(listOf(ContentType.QUOTE)).hasContent)
    }
}
