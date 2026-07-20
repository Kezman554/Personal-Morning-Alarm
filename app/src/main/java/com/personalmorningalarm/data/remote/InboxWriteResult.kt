package com.personalmorningalarm.data.remote

/**
 * Outcome of an inbox capture. Simpler than [ShoppingWriteResult] because a capture
 * creates a file rather than targeting an existing line — there is no stale-target
 * case to recover from, so the only question is whether Alfred took it.
 */
sealed interface InboxWriteResult {

    /** The capture landed in `0-inbox/`. */
    data object Done : InboxWriteResult

    /**
     * Alfred answered, and refused. Retrying will fail identically, so a queued
     * capture that gets this is failed rather than left to stall the FIFO forever.
     */
    data object Rejected : InboxWriteResult

    /** Alfred didn't answer. Worth retrying — this is the queue's whole reason to exist. */
    data object Unreachable : InboxWriteResult
}
