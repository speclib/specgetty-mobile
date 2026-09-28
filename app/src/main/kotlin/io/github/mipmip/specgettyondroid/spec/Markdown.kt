package io.github.mipmip.specgettyondroid.spec

/**
 * The mechanical half both grammars share. OpenSpec masks fences before any
 * heading test, so a `#### Scenario:` shown as an example inside a fence is
 * content rather than a scenario.
 */
internal object Markdown {

    val HEADING = Regex("""^(#{1,6})\s+(.+)$""")
    val REQUIREMENTS = Regex("""^##\s+Requirements\s*$""", RegexOption.IGNORE_CASE)
    val PURPOSE = Regex("""^##\s+Purpose\s*$""", RegexOption.IGNORE_CASE)
    val REQUIREMENT = Regex("""^###\s+Requirement:\s*(.+?)\s*$""", RegexOption.IGNORE_CASE)
    val SCENARIO = Regex("""^####\s+""")
    val DELTA = Regex(
        """^##\s+(ADDED|MODIFIED|REMOVED|RENAMED)\s+Requirements\s*$""",
        RegexOption.IGNORE_CASE,
    )
    val OP_HEADER = Regex("""^##\s+([A-Za-z]+)\s+Requirements\s*$""", RegexOption.IGNORE_CASE)
    val FENCE = Regex("""^\s*(```|~~~)""")
    val ATX_CLOSE = Regex("""[ \t]#+\s*$""")
    val SCENARIO_PREFIX = Regex("""^Scenario:\s*""", RegexOption.IGNORE_CASE)

    /** Marks every line inside a fenced block, and the fences themselves. */
    fun fenceMask(lines: List<String>): BooleanArray {
        val mask = BooleanArray(lines.size)
        var inFence = false
        lines.forEachIndexed { i, l ->
            if (FENCE.containsMatchIn(l)) {
                mask[i] = true
                inFence = !inFence
            } else {
                mask[i] = inFence
            }
        }
        return mask
    }

    /**
     * Two views of the same file: the content, and the content with every
     * fenced line blanked. Every heading test reads the second, so none of
     * them has to remember the mask.
     */
    fun maskedLines(content: String): Pair<List<String>, List<String>> {
        val normalised = content
            .removePrefix("\uFEFF")
            .replace("\r\n", "\n")
            .replace('\r', '\n')
        val raw = normalised.split("\n")
        val mask = fenceMask(raw)

        val lines = raw.map { it.trimEnd(' ', '\t') }
        val heads = lines.mapIndexed { i, l -> if (mask[i]) "" else l }
        return lines to heads
    }

    fun headingLevel(line: String): Int =
        HEADING.find(line)?.groupValues?.get(1)?.length ?: 0

    /**
     * The lines under a heading, up to the next heading at or above its level,
     * which is how OpenSpec delimits a section.
     */
    fun sectionBody(lines: List<String>, heads: List<String>, at: Int, level: Int): List<String> {
        for (i in at + 1 until lines.size) {
            val l = headingLevel(heads[i])
            if (l in 1..level) return lines.subList(at + 1, i)
        }
        return lines.subList(at + 1, lines.size)
    }

    /**
     * A CommonMark horizontal rule: three or more of `-`, `_` or `*`, all the
     * same character, with nothing else but spaces.
     */
    fun isThematicBreak(s: String): Boolean {
        var mark = ' '
        var count = 0
        for (r in s) {
            when {
                r == ' ' || r == '\t' -> Unit
                r == '-' || r == '_' || r == '*' -> {
                    if (mark == ' ') mark = r
                    if (r != mark) return false
                    count++
                }

                else -> return false
            }
        }
        return count >= 3
    }

    /**
     * The label the author reads: the heading text, less an optional CommonMark
     * closing `#` run and an optional `Scenario:` prefix. OpenSpec counts any
     * level-four heading as a scenario, so the prefix is stripped where present
     * rather than required.
     */
    fun scenarioName(line: String): String {
        var t = SCENARIO.replace(line, "")
        t = ATX_CLOSE.replace(t, "")
        t = SCENARIO_PREFIX.replace(t.trim(), "")
        return t.trim()
    }

    fun plural(n: Int, one: String, many: String): String =
        if (n == 1) "1 $one" else "$n $many"

    /** Names up to four lines, and counts the rest. */
    fun lineList(lines: List<Int>): String {
        val show = 4
        val parts = mutableListOf<String>()
        lines.forEachIndexed { i, l ->
            if (i == show) {
                return parts.joinToString(", ") + " and ${lines.size - show} more"
            }
            parts.add("line $l")
        }
        return parts.joinToString(", ")
    }
}
