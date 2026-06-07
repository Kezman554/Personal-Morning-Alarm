package com.personalmorningalarm.challenge

/**
 * Nuclear dismissal challenge: type an exact confirmation phrase. Matching is
 * case-insensitive and tolerant of surrounding/duplicate whitespace, but the
 * words themselves must be correct. Pure verifier — the host owns the input UI.
 */
class TypingChallenge(val phrase: String = REQUIRED_PHRASE) {

    /** True if [input] matches the phrase (case-insensitive, whitespace-normalised). */
    fun matches(input: String): Boolean =
        input.normalise().equals(phrase.normalise(), ignoreCase = true)

    private fun String.normalise(): String = trim().replace(WHITESPACE, " ")

    companion object {
        const val REQUIRED_PHRASE = "I am awake and getting up"
        private val WHITESPACE = Regex("\\s+")
    }
}
