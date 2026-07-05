package com.personalmorningalarm.challenge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the Stage 1 shake challenge's timing/threshold core
 * ([ShakeAccumulator], which [ShakeChallenge] delegates to). Covers threshold
 * detection, cumulative duration tracking (pause tolerance + interval cap), and
 * the progress values the challenge forwards to its onProgress callback.
 *
 * Time is injected so accrual is deterministic — the live challenge feeds
 * System.currentTimeMillis(); here we feed a controlled millisecond clock.
 */
class ShakeChallengeTest {

    private val target = 1_000L
    private val threshold = 5f

    private fun accumulator() = ShakeAccumulator(
        targetDurationMs = target,
        shakeThreshold = threshold,
        pauseToleranceMs = 500L,
        maxIntervalMs = 100L
    )

    // Above-threshold force sample and a below-threshold "still" sample.
    private val shake = 20f
    private val still = 0f

    @Test
    fun `first sample only establishes the baseline, no progress`() {
        val acc = accumulator()
        val progress = acc.onSample(shake, now = 1_000L)
        assertEquals(0L, acc.accumulatedShakeMs)
        assertEquals(0f, progress, 0f)
    }

    @Test
    fun `below-threshold samples never accrue time`() {
        val acc = accumulator()
        acc.onSample(still, 1_000L) // baseline
        var t = 1_000L
        repeat(40) { // 2s of "still" at 50ms intervals
            t += 50
            acc.onSample(still, t)
        }
        assertEquals(0L, acc.accumulatedShakeMs)
        assertFalse(acc.isComplete)
    }

    @Test
    fun `sustained shaking accrues to the target and completes`() {
        val acc = accumulator()
        acc.onSample(shake, 0L) // baseline
        var t = 0L
        var lastProgress = 0f
        repeat(20) { // 20 x 50ms = 1000ms = target
            t += 50
            lastProgress = acc.onSample(shake, t)
        }
        assertEquals(target, acc.accumulatedShakeMs)
        assertTrue(acc.isComplete)
        assertTrue(acc.completed)
        assertEquals(1f, lastProgress, 0f)
    }

    @Test
    fun `progress rises proportionally with accrued shake time`() {
        val acc = accumulator()
        acc.onSample(shake, 0L) // baseline
        var t = 0L
        repeat(10) { // 500ms accrued
            t += 50
            acc.onSample(shake, t)
        }
        assertEquals(500L, acc.accumulatedShakeMs)
        assertEquals(0.5f, acc.progress, 0.0001f)
        assertFalse(acc.isComplete)
    }

    @Test
    fun `progress never exceeds 1 even when overshooting the target`() {
        val acc = accumulator()
        acc.onSample(shake, 0L)
        var t = 0L
        repeat(40) { // 2000ms — double the target
            t += 50
            acc.onSample(shake, t)
        }
        assertTrue(acc.accumulatedShakeMs > target)
        assertEquals(1f, acc.progress, 0f)
    }

    @Test
    fun `pause tolerance bridges a brief low-motion instant between shakes`() {
        val acc = accumulator()
        acc.onSample(still, 0L)       // baseline at t=0
        acc.onSample(shake, 50L)      // shake -> +50ms (lastShake=50)
        // A "still" sample 50ms later is within the 500ms tolerance, so it still counts.
        acc.onSample(still, 100L)     // +50ms
        assertEquals(100L, acc.accumulatedShakeMs)
    }

    @Test
    fun `time stops accruing once the pause tolerance is exceeded`() {
        val acc = accumulator()
        acc.onSample(still, 0L)       // baseline
        acc.onSample(shake, 50L)      // +50ms, lastShake=50
        // 650ms after the last shake > 500ms tolerance: no further accrual.
        acc.onSample(still, 700L)
        assertEquals(50L, acc.accumulatedShakeMs)
    }

    @Test
    fun `a single interval is capped so a stalled sensor cannot jump the timer`() {
        val acc = accumulator()
        acc.onSample(shake, 0L)       // baseline
        acc.onSample(shake, 50L)      // +50ms
        // Big gap, but this sample is itself a shake so it counts — capped at 100ms,
        // not the full 950ms elapsed.
        acc.onSample(shake, 1_000L)
        assertEquals(150L, acc.accumulatedShakeMs)
    }

    @Test
    fun `rebaseline preserves accrued progress but does not count the gap`() {
        val acc = accumulator()
        acc.onSample(shake, 0L)       // baseline
        acc.onSample(shake, 50L)      // +50
        acc.onSample(shake, 100L)     // +50 => 100ms accrued

        acc.rebaseline()              // e.g. user switched shaking arms

        // A long real-time gap passes; the first post-rebaseline sample is a new
        // baseline, so none of that gap is counted.
        acc.onSample(shake, 10_000L)  // baseline again, no accrual
        assertEquals(100L, acc.accumulatedShakeMs)

        acc.onSample(shake, 10_050L)  // +50
        assertEquals(150L, acc.accumulatedShakeMs)
    }

    @Test
    fun `reset clears accrued progress and completion`() {
        val acc = accumulator()
        acc.onSample(shake, 0L)
        var t = 0L
        repeat(20) { t += 50; acc.onSample(shake, t) }
        assertTrue(acc.completed)

        acc.reset()
        assertEquals(0L, acc.accumulatedShakeMs)
        assertFalse(acc.completed)
        assertFalse(acc.isComplete)
        assertEquals(0f, acc.progress, 0f)
    }
}
