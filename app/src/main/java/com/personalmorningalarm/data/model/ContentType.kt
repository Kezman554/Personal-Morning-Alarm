package com.personalmorningalarm.data.model

/** Type of content screen shown between NFC checkpoints in Stage 2. */
enum class ContentType {
    QUOTE,
    STRETCH,
    PLACEHOLDER;

    companion object {
        /**
         * The names this build understands. Content-toggle queries filter on these,
         * so a row naming a type this build has never heard of is skipped instead of
         * blowing up the read (see [com.personalmorningalarm.data.Converters]).
         *
         * Lazy rather than a plain initialiser so it can't observe a half-built
         * enum during class init.
         */
        val knownNames: List<String> by lazy { entries.map { it.name } }
    }
}
