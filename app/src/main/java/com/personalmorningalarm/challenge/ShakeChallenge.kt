package com.personalmorningalarm.challenge

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Stage 1 challenge: dismiss by vigorously shaking the phone for a cumulative
 * [targetDurationMs] (default 15s). "Shaking" is a large change in acceleration
 * magnitude between samples (gravity cancels out in the delta), so orientation
 * doesn't matter. Time only accrues while actively shaking; a short pause
 * tolerance bridges the brief low-motion instant at each shake reversal.
 */
class ShakeChallenge(
    context: Context,
    private val targetDurationMs: Long = DEFAULT_TARGET_DURATION_MS,
    private val shakeThreshold: Float = DEFAULT_SHAKE_THRESHOLD
) : ChallengeInterface, SensorEventListener {

    override var onProgress: ((Float) -> Unit)? = null
    override var onComplete: (() -> Unit)? = null

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var running = false
    private var completed = false

    private var lastMagnitude = 0f
    private var lastSampleTime = 0L
    private var lastShakeTime = 0L
    private var accumulatedShakeMs = 0L

    override fun start() {
        if (running) return
        val sensor = accelerometer
        if (sensor == null) {
            Log.w(TAG, "No accelerometer available; ShakeChallenge cannot run")
            return
        }
        // Re-baseline timing only — accumulated progress is preserved across
        // stop/start so a pause (e.g. switching shaking arms) doesn't lose it.
        lastSampleTime = 0L
        lastMagnitude = 0f
        lastShakeTime = 0L
        running = true
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
    }

    /** Clears accumulated progress so the challenge can run from scratch. */
    fun reset() {
        completed = false
        lastMagnitude = 0f
        lastSampleTime = 0L
        lastShakeTime = 0L
        accumulatedShakeMs = 0L
    }

    override fun stop() {
        if (!running) return
        running = false
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!running || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val magnitude = sqrt(x * x + y * y + z * z)
        val now = System.currentTimeMillis()

        // First sample only establishes baselines; nothing to accumulate yet.
        if (lastSampleTime == 0L) {
            lastMagnitude = magnitude
            lastSampleTime = now
            return
        }

        val delta = abs(magnitude - lastMagnitude)
        lastMagnitude = magnitude
        if (delta > shakeThreshold) {
            lastShakeTime = now
        }

        // Count this interval only if a strong shake landed within the tolerance window.
        val activelyShaking = now - lastShakeTime <= PAUSE_TOLERANCE_MS
        if (activelyShaking) {
            val interval = (now - lastSampleTime).coerceAtMost(MAX_INTERVAL_MS)
            accumulatedShakeMs += interval
        }
        lastSampleTime = now

        val progress = (accumulatedShakeMs.toFloat() / targetDurationMs).coerceIn(0f, 1f)
        onProgress?.invoke(progress)

        if (!completed && accumulatedShakeMs >= targetDurationMs) {
            completed = true
            stop()
            onComplete?.invoke()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for shake detection.
    }

    companion object {
        private const val TAG = "PMA"

        // Tuned down across on-device tests: 15.0f then 8.0f both needed
        // near-maximal effort (only ~11% of vigorous shaking counted at 8.0f).
        // 4.0f registers a comfortable shake; raise if it becomes too easy.
        const val DEFAULT_SHAKE_THRESHOLD = 4.0f
        const val DEFAULT_TARGET_DURATION_MS = 15_000L

        /**
         * How long after the last strong shake we still count as "shaking".
         * Wide enough to bridge the low-motion instant between shake reversals
         * so continuous shaking accrues smoothly.
         */
        private const val PAUSE_TOLERANCE_MS = 500L

        /** Caps a single interval so a stalled sensor can't jump the timer. */
        private const val MAX_INTERVAL_MS = 100L
    }
}
