package com.personalmorningalarm.challenge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Unit tests for the nuclear-alarm [MathChallenge]: problem generation (operands,
 * operator, non-negative subtraction) and answer validation.
 */
class MathChallengeTest {

    @Test
    fun `correct answer validates`() {
        val challenge = MathChallenge(Random(1))
        assertTrue(challenge.isCorrect(challenge.answer.toString()))
    }

    @Test
    fun `wrong answer is rejected`() {
        val challenge = MathChallenge(Random(1))
        assertFalse(challenge.isCorrect((challenge.answer + 1).toString()))
    }

    @Test
    fun `surrounding whitespace is ignored`() {
        val challenge = MathChallenge(Random(2))
        assertTrue(challenge.isCorrect("  ${challenge.answer}  "))
    }

    @Test
    fun `non-numeric input is rejected, never crashes`() {
        val challenge = MathChallenge(Random(3))
        assertFalse(challenge.isCorrect(""))
        assertFalse(challenge.isCorrect("   "))
        assertFalse(challenge.isCorrect("abc"))
        assertFalse(challenge.isCorrect("12x"))
    }

    @Test
    fun `answer always matches the operands in the question text`() {
        // Sweep many seeds so both operators and a wide operand range are covered.
        repeat(500) { seed ->
            val challenge = MathChallenge(Random(seed.toLong()))
            val text = challenge.questionText
            val isAddition = text.contains("+")
            val parts = text.split(if (isAddition) "+" else "−").map { it.trim().toInt() }
            val (left, right) = parts[0] to parts[1]

            // Operands are two-digit (10..99) per the generator.
            assertTrue("left operand $left out of range in '$text'", left in 10..99)
            assertTrue("right operand $right out of range in '$text'", right in 10..99)

            val expected = if (isAddition) left + right else left - right
            assertEquals("answer mismatch for '$text'", expected, challenge.answer)
        }
    }

    @Test
    fun `subtraction problems never have a negative answer`() {
        repeat(500) { seed ->
            val challenge = MathChallenge(Random(seed.toLong()))
            if (challenge.questionText.contains("−")) {
                assertTrue(
                    "negative answer ${challenge.answer} for '${challenge.questionText}'",
                    challenge.answer >= 0
                )
            }
        }
    }

    @Test
    fun `same seed produces an identical problem`() {
        val a = MathChallenge(Random(42))
        val b = MathChallenge(Random(42))
        assertEquals(a.questionText, b.questionText)
        assertEquals(a.answer, b.answer)
    }
}
