package com.personalmorningalarm.challenge

/**
 * Common contract for every dismissal challenge (shake, NFC, math, typing...).
 *
 * A host sets [onProgress] and [onComplete], then calls [start]. The challenge
 * reports incremental progress and fires [onComplete] once satisfied. Callbacks
 * are delivered on the main thread, so hosts may touch UI directly.
 */
interface ChallengeInterface {

    /** Progress from 0.0 (nothing done) to 1.0 (complete). */
    var onProgress: ((Float) -> Unit)?

    /** Invoked exactly once when the challenge is satisfied. */
    var onComplete: (() -> Unit)?

    /** Begins listening / running the challenge. Idempotent. */
    fun start()

    /** Stops the challenge and releases any resources (e.g. sensor listeners). */
    fun stop()
}
