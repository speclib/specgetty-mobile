package io.github.mipmip.specgettyondroid.index

enum class MatchKind {
    /** Fuzzy subsequence on the change name. The default. */
    FUZZY_NAME,

    /** A literal substring of the change name. Selected by the sigil. */
    LITERAL_NAME,

    /** A literal substring of the change's text and its name. */
    BODY,
}

/**
 * Names are matched by fuzzy subsequence because they are short and
 * half-remembered; bodies are matched literally because a fuzzy subsequence
 * matches nearly any document of real length.
 */
data class Query(
    val kind: MatchKind,
    val term: String,
    val caseSensitive: Boolean,
) {
    val isEmpty: Boolean get() = term.isEmpty()

    /**
     * Smart case: an all-lower-case term matches case-insensitively, a term
     * carrying any upper case matches exactly.
     */
    fun contains(haystack: String): Boolean = if (caseSensitive) {
        haystack.contains(term)
    } else {
        haystack.contains(term, ignoreCase = true)
    }

    companion object {
        const val LITERAL_NAME_SIGIL = '\''
        const val BODY_SIGIL = ':'

        fun parse(raw: String): Query {
            val kind = when {
                raw.startsWith(LITERAL_NAME_SIGIL) -> MatchKind.LITERAL_NAME
                raw.startsWith(BODY_SIGIL) -> MatchKind.BODY
                else -> MatchKind.FUZZY_NAME
            }
            val term = if (kind == MatchKind.FUZZY_NAME) raw else raw.substring(1)
            return Query(kind, term, term.any(Char::isUpperCase))
        }
    }
}
