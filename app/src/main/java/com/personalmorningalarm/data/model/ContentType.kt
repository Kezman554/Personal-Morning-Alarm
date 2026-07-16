package com.personalmorningalarm.data.model

/** Type of content screen shown between NFC checkpoints in Stage 2. */
enum class ContentType {
    QUOTE,
    STRETCH,
    PLACEHOLDER,

    /** Today's schedule, read from the Alfred Vault API. */
    DAILY_SCHEDULE
}
