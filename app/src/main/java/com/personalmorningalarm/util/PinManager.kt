package com.personalmorningalarm.util

import android.content.Context
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Stores and verifies the optional 4-digit "alarm PIN" that guards changes to the
 * alarm time, the enable/disable toggle, and the Stage 2 time limit.
 *
 * The PIN is never stored in clear text: a per-install random salt is generated on
 * [setPin] and only SHA-256(salt + pin) is persisted, in private SharedPreferences.
 * A 4-digit PIN is low-entropy by nature (this is a wake-up nuisance guard, not a
 * security boundary), but salting + hashing keeps the raw digits off disk.
 */
class PinManager(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** True once a PIN has been set (and not since disabled). */
    fun isPinSet(): Boolean = prefs.contains(KEY_HASH) && prefs.contains(KEY_SALT)

    /** Stores [pin] as a salted hash, replacing any existing PIN. */
    fun setPin(pin: String) {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        prefs.edit()
            .putString(KEY_SALT, salt.toHex())
            .putString(KEY_HASH, hash(pin, salt))
            .apply()
    }

    /** True if [pin] matches the stored PIN. False if no PIN is set. */
    fun verify(pin: String): Boolean {
        val salt = prefs.getString(KEY_SALT, null)?.fromHex() ?: return false
        val stored = prefs.getString(KEY_HASH, null) ?: return false
        return constantTimeEquals(stored, hash(pin, salt))
    }

    /** Removes the PIN entirely; afterwards [isPinSet] is false. */
    fun clearPin() {
        prefs.edit().remove(KEY_SALT).remove(KEY_HASH).apply()
    }

    private fun hash(pin: String, salt: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt)
        return md.digest(pin.toByteArray(Charsets.UTF_8)).toHex()
    }

    /** Length-aware, value-comparing equality that doesn't short-circuit per char. */
    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].code xor b[i].code)
        return diff == 0
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.fromHex(): ByteArray =
        ByteArray(length / 2) { substring(it * 2, it * 2 + 2).toInt(16).toByte() }

    private companion object {
        const val PREFS_NAME = "pma_security"
        const val KEY_SALT = "pin_salt"
        const val KEY_HASH = "pin_hash"
        const val SALT_BYTES = 16
    }
}
