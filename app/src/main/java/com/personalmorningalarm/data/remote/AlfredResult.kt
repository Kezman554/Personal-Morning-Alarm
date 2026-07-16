package com.personalmorningalarm.data.remote

/**
 * Outcome of an Alfred fetch. Every Alfred-backed screen renders the same three
 * cases: live data, cached data from a previous fetch, or nothing at all.
 */
sealed interface AlfredResult<out T> {

    /** Fetched from Alfred just now. */
    data class Fresh<T>(val data: T) : AlfredResult<T>

    /** Alfred was unreachable; this is the last response it gave, [cachedAtMillis] ago. */
    data class Stale<T>(val data: T, val cachedAtMillis: Long) : AlfredResult<T>

    /** Alfred was unreachable and nothing was cached — the screen shows its fallback. */
    data object Unavailable : AlfredResult<Nothing>
}
