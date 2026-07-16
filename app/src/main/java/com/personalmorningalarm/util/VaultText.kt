package com.personalmorningalarm.util

/**
 * Makes a raw line from the Obsidian vault fit to show on screen.
 *
 * Alfred serves note text verbatim, so it still carries the vault's authoring
 * syntax — `[[wikilinks]]` and `**emphasis**`. None of that means anything to
 * someone half-awake at 6am, so it is resolved to what a reader would see in
 * Obsidian rather than shown raw.
 *
 * The Alfred-backed screens all share this: they read the same vault, so text
 * from one can carry anything text from another can.
 */
object VaultText {

    // target, optional #section, optional |alias — mirroring Obsidian's own link
    // form. A leading `!` (an embed) is consumed too. None of the parts may span
    // a bracket, so an unclosed `[[` is left alone rather than eating the line.
    private val WIKILINK = Regex("""!?\[\[([^\[\]|#]*)(?:#([^\[\]|]+))?(?:\|([^\[\]]+))?]]""")

    /**
     * Replaces each wikilink with the text Obsidian would render: the alias if
     * there is one, otherwise the target, with a `#section` shown as "target >
     * section" the way Obsidian displays it.
     */
    fun stripWikiLinks(source: String): String {
        if (!source.contains("[[")) return source
        return WIKILINK.replace(source) { match ->
            val target = match.groupValues[1].trim()
            val section = match.groupValues[2].trim()
            val alias = match.groupValues[3].trim()
            when {
                alias.isNotEmpty() -> alias
                target.isEmpty() -> section // [[#Heading]] — a link within the note
                section.isEmpty() -> target
                else -> "$target > $section"
            }
        }
    }

    /** Display-ready text: wikilinks resolved, then emphasis rendered as styling. */
    fun render(source: String): CharSequence = MarkdownRenderer.render(stripWikiLinks(source))
}
