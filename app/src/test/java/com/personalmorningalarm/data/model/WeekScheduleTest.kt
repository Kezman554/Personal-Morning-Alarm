package com.personalmorningalarm.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * The week payload → day pages mapping the week screen pages through. The plan's
 * start and end are hard edges; days Alfred didn't list still get an (empty) page.
 */
class WeekScheduleTest {

    private val gym = ScheduleTaskDto("Gym", "am")
    private val read = ScheduleTaskDto("Read", null)

    private fun dto(
        start: String? = "2026-07-13",
        end: String? = "2026-07-18",
        days: Map<String, List<ScheduleTaskDto>>? = emptyMap()
    ) = WeekScheduleDto(week = "2026-W29", start = start, end = end, days = days)

    @Test
    fun `every day from start to end gets a page, in order`() {
        val days = WeekSchedule.days(
            dto(days = mapOf("2026-07-13" to listOf(gym), "2026-07-15" to listOf(read)))
        )

        assertEquals(6, days.size)
        assertEquals(LocalDate.parse("2026-07-13"), days.first().date)
        assertEquals(LocalDate.parse("2026-07-18"), days.last().date)
        assertEquals(listOf(gym), days[0].tasks)
        assertEquals(listOf(read), days[2].tasks)
    }

    @Test
    fun `a day listed with no items is an explicit empty page, not a gap`() {
        val days = WeekSchedule.days(
            dto(end = "2026-07-14", days = mapOf("2026-07-14" to emptyList()))
        )

        assertEquals(2, days.size)
        assertTrue(days.all { it.tasks.isEmpty() })
    }

    @Test
    fun `no days at all means no plan this week`() {
        assertTrue(WeekSchedule.days(null).isEmpty())
        assertTrue(WeekSchedule.days(dto(days = null)).isEmpty())
        assertTrue(WeekSchedule.days(dto(days = emptyMap())).isEmpty())
    }

    @Test
    fun `a broken range falls back to the days that parse`() {
        val listed = mapOf("2026-07-15" to listOf(gym), "2026-07-13" to listOf(read))

        for (broken in listOf(
            dto(start = null, days = listed),
            dto(start = "not-a-date", days = listed),
            dto(start = "2026-07-18", end = "2026-07-13", days = listed), // end before start
            dto(start = "2026-01-01", end = "2026-12-31", days = listed) // absurd span
        )) {
            val days = WeekSchedule.days(broken)
            assertEquals(listOf("2026-07-13", "2026-07-15"), days.map { it.date.toString() })
        }
    }

    @Test
    fun `unparseable day keys are skipped in the fallback, not fatal`() {
        val days = WeekSchedule.days(
            dto(start = null, end = null, days = mapOf("garbage" to listOf(gym), "2026-07-13" to listOf(read)))
        )

        assertEquals(1, days.size)
        assertEquals(listOf(read), days[0].tasks)
    }

    /** A populated plan week: one listed day is enough to build the full range. */
    private val plannedWeek = dto(days = mapOf("2026-07-13" to listOf(gym)))

    @Test
    fun `the pager anchors on today`() {
        val days = WeekSchedule.days(plannedWeek)

        assertEquals(2, WeekSchedule.anchorIndex(days, LocalDate.parse("2026-07-15")))
    }

    @Test
    fun `today outside the plan clamps to the nearest edge`() {
        val days = WeekSchedule.days(plannedWeek)

        // A stale cached week: today may be past its end (or, oddly, before its start).
        assertEquals(5, WeekSchedule.anchorIndex(days, LocalDate.parse("2026-07-25")))
        assertEquals(0, WeekSchedule.anchorIndex(days, LocalDate.parse("2026-07-01")))
        assertEquals(0, WeekSchedule.anchorIndex(emptyList(), LocalDate.parse("2026-07-18")))
    }
}
