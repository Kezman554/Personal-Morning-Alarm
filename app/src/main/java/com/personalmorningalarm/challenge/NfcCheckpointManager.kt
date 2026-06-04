package com.personalmorningalarm.challenge

import com.personalmorningalarm.data.entity.NfcTag

/**
 * Drives the Stage 2 NFC checkpoint sequence. The tag order is randomised at
 * construction (i.e. fresh each morning) so the user can't go on autopilot.
 * Feed each scanned tag's hardware id to [onTagScanned] and act on the result.
 */
class NfcCheckpointManager(tags: List<NfcTag>) {

    private val ordered: List<NfcTag> = tags.shuffled()
    private var index = 0

    val total: Int = ordered.size

    /** 1-based number of the checkpoint currently being sought. */
    val currentNumber: Int get() = (index + 1).coerceAtMost(total)

    /** The tag the user must tap next, or null once the sequence is complete. */
    val currentTag: NfcTag? get() = ordered.getOrNull(index)

    val isComplete: Boolean get() = index >= total

    fun onTagScanned(tagId: String): CheckpointResult {
        val expected = currentTag ?: return CheckpointResult.AlreadyComplete
        return if (tagId.equals(expected.tagId, ignoreCase = true)) {
            index++
            if (isComplete) CheckpointResult.SequenceComplete
            else CheckpointResult.Correct(currentTag!!)
        } else {
            CheckpointResult.Wrong(expected)
        }
    }
}

sealed interface CheckpointResult {
    /** Right tag; [next] is the new checkpoint to seek. */
    data class Correct(val next: NfcTag) : CheckpointResult

    /** Wrong tag; [expected] is the one still being sought. */
    data class Wrong(val expected: NfcTag) : CheckpointResult

    /** The final checkpoint was just tapped. */
    data object SequenceComplete : CheckpointResult

    /** A tag was scanned after the sequence already finished. */
    data object AlreadyComplete : CheckpointResult
}
