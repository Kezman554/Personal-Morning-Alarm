package com.personalmorningalarm.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Turning Alfred's /chalkboard array into display-ready items. */
class RollingTodoTest {

    private fun task(task: String?, date: String? = null, line: String? = null) =
        ChalkboardTaskDto(task, date, line)

    @Test
    fun `keeps API order`() {
        val items = RollingTodo.items(
            listOf(task("Plant the bamboo"), task("Cancel Beer52"), task("Trim front hedges"))
        )

        assertEquals(
            listOf("Plant the bamboo", "Cancel Beer52", "Trim front hedges"),
            items.map { it.task }
        )
    }

    @Test
    fun `an ISO date is formatted for the secondary line`() {
        val items = RollingTodo.items(listOf(task("Plant the bamboo", "2026-07-05")))

        assertEquals("5 Jul 2026", items.single().date)
    }

    @Test
    fun `a missing date gives no line rather than the string null`() {
        val items = RollingTodo.items(listOf(task("Sort belt bag", null)))

        assertNull(items.single().date)
    }

    @Test
    fun `a blank date gives no line either`() {
        assertNull(RollingTodo.items(listOf(task("Sort belt bag", "   "))).single().date)
        assertNull(RollingTodo.items(listOf(task("Sort belt bag", ""))).single().date)
    }

    @Test
    fun `an unparseable date is shown as sent rather than dropped`() {
        val items = RollingTodo.items(listOf(task("Sort tin cupboard", "last Tuesday")))

        assertEquals("last Tuesday", items.single().date)
    }

    @Test
    fun `wikilinks are resolved before display`() {
        val items = RollingTodo.items(listOf(task("See [[pi-admin-guide]] §9c", "2026-07-13")))

        assertEquals("See pi-admin-guide §9c", items.single().task)
    }

    @Test
    fun `entries with no task text are dropped`() {
        val items = RollingTodo.items(listOf(task(null, "2026-07-13"), task("  "), task("Fix fence")))

        assertEquals(listOf("Fix fence"), items.map { it.task })
    }

    @Test
    fun `an empty response produces no items`() {
        assertTrue(RollingTodo.items(emptyList()).isEmpty())
    }

    // --- the raw line: targeting key for writes, and where done state lives ---

    @Test
    fun `the raw line rides along untouched as the targeting key`() {
        val line = "- [ ] Fix fence (2026-07-05)"
        val items = RollingTodo.items(listOf(task("Fix fence", "2026-07-05", line)))

        assertEquals(line, items.single().line)
    }

    @Test
    fun `a ticked line marks the item done`() {
        val items = RollingTodo.items(
            listOf(
                task("Fix fence", line = "- [x] Fix fence (2026-07-05)"),
                task("Shout louder", line = "- [X] Shout louder")
            )
        )

        assertTrue(items.all { it.done })
    }

    @Test
    fun `an unticked line leaves the item open`() {
        val items = RollingTodo.items(listOf(task("Fix fence", line = "- [ ] Fix fence")))

        assertEquals(false, items.single().done)
    }

    @Test
    fun `no line means open and untargetable, from data cached before lines existed`() {
        val item = RollingTodo.items(listOf(task("Fix fence"))).single()

        assertNull(item.line)
        assertEquals(false, item.done)
    }
}
