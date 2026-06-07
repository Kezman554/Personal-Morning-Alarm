package com.personalmorningalarm.challenge

import kotlin.random.Random

/**
 * Nuclear dismissal challenge: solve one random two-digit arithmetic problem
 * (e.g. "37 + 58", "94 − 47"). Subtraction is arranged so the answer is never
 * negative. Pure verifier — the host owns the input UI and calls [isCorrect].
 */
class MathChallenge(random: Random = Random.Default) {

    private val left: Int
    private val right: Int
    private val isAddition: Boolean = random.nextBoolean()

    /** The expected answer. */
    val answer: Int

    init {
        val a = random.nextInt(10, 100)
        val b = random.nextInt(10, 100)
        if (isAddition) {
            left = a
            right = b
            answer = a + b
        } else {
            left = maxOf(a, b)
            right = minOf(a, b)
            answer = left - right
        }
    }

    /** Human-readable problem, e.g. "37 + 58". */
    val questionText: String
        get() = "$left ${if (isAddition) "+" else "−"} $right"

    /** True if [input] parses to the correct answer (whitespace ignored). */
    fun isCorrect(input: String): Boolean = input.trim().toIntOrNull() == answer
}
