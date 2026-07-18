package com.personalmorningalarm.data.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Alfred's /daily-schedule/week response: the current plan week, every day keyed
 * by ISO date. Days with nothing planned appear explicitly as empty lists; a week
 * with no plan yet (the Sunday before the weekly review) comes back well-formed
 * with no days rather than as an error.
 *
 * Everything is nullable because it comes off the wire.
 */
data class WeekScheduleDto(
    val week: String?,
    val start: String?,
    val end: String?,
    val days: Map<String, List<ScheduleTaskDto>>?
)

/** One page of the week screen: a date and what was planned for it (possibly nothing). */
data class WeekDay(
    val date: LocalDate,
    val tasks: List<ScheduleTaskDto>
)

/**
 * Turns the wire shape into the ordered day pages the week screen swipes through.
 * The plan's start and end are hard edges — no paging into a previous or next week.
 */
object WeekSchedule {

    /**
     * Every day from the plan's start to its end, in order, empty where Alfred
     * listed nothing. An empty result means there is no plan this week.
     */
    fun days(dto: WeekScheduleDto?): List<WeekDay> {
        // A plan lists its days explicitly (empty ones included), so no days at
        // all is "no plan this week" — even if a start/end range came along.
        val dayMap = dto?.days?.takeIf { it.isNotEmpty() } ?: return emptyList()
        val start = parse(dto.start)
        val end = parse(dto.end)
        val dates = if (start != null && end != null && !end.isBefore(start) &&
            ChronoUnit.DAYS.between(start, end) < MAX_PLAN_DAYS
        ) {
            generateSequence(start) { it.plusDays(1) }.takeWhile { !it.isAfter(end) }.toList()
        } else {
            // A missing or nonsensical range still shows whatever days parse.
            dayMap.keys.mapNotNull(::parse).sorted()
        }
        return dates.map { date -> WeekDay(date, dayMap[date.toString()].orEmpty()) }
    }

    /**
     * Where the pager opens: today's page, clamped to the plan's edges when today
     * falls outside it (a stale cached week, say).
     */
    fun anchorIndex(days: List<WeekDay>, today: LocalDate): Int {
        if (days.isEmpty()) return 0
        val index = days.indexOfFirst { it.date == today }
        if (index >= 0) return index
        return if (today.isBefore(days.first().date)) 0 else days.lastIndex
    }

    private fun parse(iso: String?): LocalDate? = try {
        iso?.trim()?.let(LocalDate::parse)
    } catch (e: Exception) {
        null
    }

    /** A "week" claiming more days than this is a malformed range, not a plan. */
    private const val MAX_PLAN_DAYS = 14L
}
