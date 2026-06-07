package com.personalmorningalarm.challenge

/**
 * Nuclear dismissal challenge: tap ANY registered NFC tag (unlike Stage 2's
 * ordered checkpoint sequence — at the nuclear stage we just need proof the user
 * physically reached a tag). Pure verifier; the host runs the NFC reader and
 * feeds scanned hardware ids to [isRegistered].
 *
 * If no tags are registered the challenge can't be satisfied by tapping, so
 * [hasNoTags] lets the host auto-complete it rather than trapping the user.
 */
class NfcChallenge(registeredTagIds: Collection<String>) {

    private val ids: Set<String> = registeredTagIds.map { it.uppercase() }.toSet()

    val hasNoTags: Boolean get() = ids.isEmpty()

    fun isRegistered(tagId: String): Boolean = tagId.uppercase() in ids
}
