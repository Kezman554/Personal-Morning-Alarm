package com.personalmorningalarm.data.remote

import androidx.test.core.app.ApplicationProvider
import com.personalmorningalarm.data.remote.AlfredSettings.Companion.DEFAULT_HOST
import com.personalmorningalarm.data.remote.AlfredSettings.Companion.normalise
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AlfredSettingsTest {

    private val settings =
        AlfredSettings(ApplicationProvider.getApplicationContext())

    @Test
    fun `defaults to Alfred's address on port 8200 until configured`() {
        assertEquals("192.168.1.100:8200", settings.getHost())
        assertEquals("http://192.168.1.100:8200/", settings.baseUrl())
    }

    @Test
    fun `saved host round-trips into the base URL`() {
        settings.setHost("10.0.0.5:9000")

        assertEquals("10.0.0.5:9000", settings.getHost())
        assertEquals("http://10.0.0.5:9000/", settings.baseUrl())
    }

    @Test
    fun `a bare host gets the default port`() {
        assertEquals("alfred.local:8200", normalise("alfred.local"))
    }

    @Test
    fun `strips a pasted scheme, path and surrounding space`() {
        assertEquals("10.0.0.5:8200", normalise("  http://10.0.0.5:8200/daily-schedule  "))
        assertEquals("10.0.0.5:8200", normalise("https://10.0.0.5:8200"))
        assertEquals("10.0.0.5:8200", normalise("10.0.0.5:8200/"))
    }

    @Test
    fun `blank input falls back to the default rather than an unusable URL`() {
        assertEquals(DEFAULT_HOST, normalise("   "))

        settings.setHost("")
        assertEquals(DEFAULT_HOST, settings.getHost())
    }

    @Test
    fun `accepts the addresses Alfred actually lives at`() {
        assertTrue(AlfredSettings.isValid("192.168.1.100:8200"))
        assertTrue(AlfredSettings.isValid("alfred.local"))
        assertTrue(AlfredSettings.isValid("http://10.0.0.5:8200/"))
        // Blank normalises to the default, which is usable.
        assertTrue(AlfredSettings.isValid(""))
    }

    @Test
    fun `rejects addresses that could never build a URL`() {
        assertFalse("empty port", AlfredSettings.isValid("192.168.1.100:"))
        assertFalse("port out of range", AlfredSettings.isValid("192.168.1.100:99999"))
        assertFalse("port zero", AlfredSettings.isValid("192.168.1.100:0"))
        assertFalse("non-numeric port", AlfredSettings.isValid("192.168.1.100:abc"))
        assertFalse("spaces in host", AlfredSettings.isValid("not a host"))
    }
}
