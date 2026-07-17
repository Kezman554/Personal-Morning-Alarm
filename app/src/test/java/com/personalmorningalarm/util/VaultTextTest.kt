package com.personalmorningalarm.util

import android.graphics.Typeface
import android.text.Spanned
import android.text.style.StyleSpan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Vault authoring syntax must never reach the screen — no literal [[ ]] or **.
 */
@RunWith(RobolectricTestRunner::class)
class VaultTextTest {

    @Test
    fun `a plain wikilink becomes its target`() {
        assertEquals(
            "See pi-admin-guide §9a",
            VaultText.stripWikiLinks("See [[pi-admin-guide]] §9a")
        )
    }

    @Test
    fun `an aliased wikilink becomes the alias`() {
        assertEquals(
            "Ask Bob about it",
            VaultText.stripWikiLinks("Ask [[people/robert-smith|Bob]] about it")
        )
    }

    @Test
    fun `a section link reads the way Obsidian shows it`() {
        assertEquals(
            "Check pi-admin-guide > Networking",
            VaultText.stripWikiLinks("Check [[pi-admin-guide#Networking]]")
        )
    }

    @Test
    fun `an aliased section link still prefers the alias`() {
        assertEquals(
            "Check the wifi notes",
            VaultText.stripWikiLinks("Check [[pi-admin-guide#Networking|the wifi notes]]")
        )
    }

    @Test
    fun `a link to a heading in the same note keeps the heading`() {
        assertEquals("See Budget below", VaultText.stripWikiLinks("See [[#Budget]] below"))
    }

    @Test
    fun `an embed loses its bang too`() {
        assertEquals("diagram", VaultText.stripWikiLinks("![[diagram]]"))
    }

    @Test
    fun `several links in one task are all resolved`() {
        assertEquals(
            "Link pi-admin-guide to vault-sync",
            VaultText.stripWikiLinks("Link [[pi-admin-guide]] to [[projects/vault-sync|vault-sync]]")
        )
    }

    @Test
    fun `text without links is untouched`() {
        val plain = "Trim front hedges"
        assertEquals(plain, VaultText.stripWikiLinks(plain))
    }

    @Test
    fun `an unclosed bracket is left alone rather than eating the line`() {
        val broken = "Sort [[unfinished link"
        assertEquals(broken, VaultText.stripWikiLinks(broken))
    }

    @Test
    fun `render resolves links and emphasis together, leaving no markup`() {
        val rendered = VaultText.render("Fix **fence** — see [[home/garden|garden notes]]")

        assertEquals("Fix fence — see garden notes", rendered.toString())
        assertFalse(rendered.toString().contains("["))
        assertFalse(rendered.toString().contains("*"))

        val spanned = rendered as Spanned
        val spans = spanned.getSpans(0, rendered.length, StyleSpan::class.java)
        assertEquals(1, spans.size)
        assertEquals(Typeface.BOLD, spans[0].style)
        assertEquals("fence", rendered.substring(spanned.getSpanStart(spans[0]), spanned.getSpanEnd(spans[0])))
    }

    /** The exact string Alfred is serving today — the case that motivated this. */
    @Test
    fun `a real rolling-todo item comes out clean`() {
        val real = "Disable Pi WiFi power-save (wlan0) — stops it going unreachable " +
            "overnight; do before/with the vault-sync work. See [[pi-admin-guide]] §9a"

        val rendered = VaultText.render(real).toString()

        assertFalse(rendered.contains("[["))
        assertFalse(rendered.contains("]]"))
        assertEquals(true, rendered.endsWith("See pi-admin-guide §9a"))
    }
}
