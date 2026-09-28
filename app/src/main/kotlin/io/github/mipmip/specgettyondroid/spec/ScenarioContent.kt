package io.github.mipmip.specgettyondroid.spec

/** The four keywords a scenario bullet may open with. */
private val CLAUSE_KEYWORDS = listOf("GIVEN", "WHEN", "THEN", "AND")

/**
 * Reads a keyword bullet, or reports that the line is not one.
 *
 * A bulleted, upper-case keyword, which is what the OpenSpec template shows.
 * Deliberately narrow: since nothing is dropped, which shapes get the keyword
 * laid out is presentation, and the rest is shown as the prose it is.
 */
internal fun clauseOf(line: String): Triple<String, String, Boolean> {
    val trimmed = line.trim()
    if (!trimmed.startsWith("- ")) return Triple("", "", false)
    val rest = trimmed.removePrefix("- ").trim()
    for (kw in CLAUSE_KEYWORDS) {
        for (form in listOf("**$kw**", kw)) {
            if (rest.startsWith(form)) {
                return Triple(kw, rest.removePrefix(form).trim(), true)
            }
        }
    }
    return Triple("", "", false)
}

/**
 * Whether a line begins with a clause keyword, allowing the shapes the clause
 * reader does not lay out. A keyword has to be a word here, not a prefix of
 * one, so `SHALLOW` and `ANDROID` are not keywords.
 */
internal fun opensWithClauseKeyword(line: String): Boolean {
    var rest = line.trimStart(' ', '\t')

    if (rest.length > 1 && (rest[0] == '-' || rest[0] == '*' || rest[0] == '+') && rest[1] == ' ') {
        rest = rest.substring(2)
    }
    var bold = false
    if (rest.startsWith("**")) {
        bold = true
        rest = rest.substring(2)
    }

    for (kw in CLAUSE_KEYWORDS) {
        if (!rest.startsWith(kw)) continue
        var after = rest.substring(kw.length)
        if (bold) {
            if (!after.startsWith("**")) continue
            after = after.substring(2)
        }
        if (after.isNotEmpty() && after[0] != ' ' && after[0] != ':' && after[0] != ',') continue
        return true
    }
    return false
}

/**
 * Reads a scenario's content into ordered parts. Nothing is discarded for being
 * in an unrecognised shape: a line opening with a keyword bullet starts a
 * clause, a line after one continues it, a blank line ends it, and anything
 * else is prose in the place it was written.
 */
internal fun partsOf(content: List<String>): List<SpecPart> {
    val parts = mutableListOf<SpecPart>()
    var kind: PartKind? = null
    var keyword = ""
    var text = ""

    fun flush() {
        if (kind == null) return
        val t = text.trim()
        if (t.isNotEmpty() || keyword.isNotEmpty()) {
            parts.add(SpecPart(kind!!, keyword, t))
        }
        kind = null
        keyword = ""
        text = ""
    }

    for (line in content) {
        val trimmed = line.trim()
        when {
            trimmed.isEmpty() -> flush()

            // A separator in the file, not part of the behaviour described.
            Markdown.isThematicBreak(trimmed) -> flush()

            else -> {
                val (kw, rest, isClause) = clauseOf(line)
                if (isClause) {
                    flush()
                    kind = PartKind.CLAUSE
                    keyword = kw
                    text = rest
                    continue
                }
                // A line opening with a clause keyword starts a paragraph of
                // its own. Markdown would glue it to the line above, which for
                // a scenario written without bullets turns three statements
                // into one run-on sentence.
                if (kind == PartKind.PROSE && opensWithClauseKeyword(line)) flush()
                if (kind == null) kind = PartKind.PROSE
                if (text.isNotEmpty()) text += " "
                text += trimmed
            }
        }
    }
    flush()
    return parts
}
