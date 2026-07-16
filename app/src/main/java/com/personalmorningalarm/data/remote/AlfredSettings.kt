package com.personalmorningalarm.data.remote

import android.content.Context
import com.personalmorningalarm.util.ThemeManager

/**
 * The Alfred Vault API's address on the local network, stored as "host:port".
 * Shared by every Alfred-backed content screen — the address is configured once
 * in settings, not per screen.
 */
class AlfredSettings(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(ThemeManager.PREFS_NAME, Context.MODE_PRIVATE)

    /** The saved "host:port", or [DEFAULT_HOST] if never configured. */
    fun getHost(): String =
        prefs.getString(KEY_HOST, null)?.takeIf { it.isNotBlank() } ?: DEFAULT_HOST

    fun setHost(host: String) {
        prefs.edit().putString(KEY_HOST, normalise(host)).apply()
    }

    /** Base URL for Retrofit — always absolute, always trailing-slashed. */
    fun baseUrl(): String = "http://${getHost()}/"

    companion object {
        /** Alfred serves the API on port 8200. */
        const val DEFAULT_PORT = 8200
        const val DEFAULT_HOST = "192.168.1.100:$DEFAULT_PORT"

        private const val KEY_HOST = "alfred_host"

        /** A hostname or IPv4 address, then a port. IPv6 isn't supported. */
        private val HOST_PORT = Regex("""^[A-Za-z0-9.\-]+:(\d{1,5})$""")

        /**
         * Whether [input] normalises to an address we could actually build a URL
         * from. A bad address doesn't crash anything — the fetch just fails and the
         * screen falls back — but it fails at 6am, so settings rejects it up front.
         */
        fun isValid(input: String): Boolean {
            val port = HOST_PORT.matchEntire(normalise(input))
                ?.groupValues?.get(1)?.toIntOrNull() ?: return false
            return port in 1..65535
        }

        /**
         * Tolerates what people actually type: a pasted scheme, a trailing slash or
         * path, surrounding spaces, or a bare host with no port (which gets
         * [DEFAULT_PORT]). Blank input falls back to [DEFAULT_HOST].
         */
        fun normalise(input: String): String {
            val trimmed = input.trim()
                .removePrefix("https://")
                .removePrefix("http://")
                .substringBefore('/')
                .trim()
            if (trimmed.isEmpty()) return DEFAULT_HOST
            return if (trimmed.contains(':')) trimmed else "$trimmed:$DEFAULT_PORT"
        }
    }
}
