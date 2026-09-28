package io.github.mipmip.specgettyondroid.spec

import com.github.difflib.DiffUtils
import com.github.difflib.patch.DeltaType

/**
 * The source text of one requirement, sliced by the same grammar the parsers
 * read. Slicing rather than reconstructing from the outline: a difference
 * between two requirements should be a difference between what the two files
 * say, not between two renderings of them.
 */
object RequirementSource {

    fun extract(content: String, title: String): String? {
        val (lines, heads) = Markdown.maskedLines(content)

        val start = heads.indexOfFirst { line ->
            Markdown.REQUIREMENT.find(line)?.groupValues?.get(1)?.trim() == title
        }
        if (start < 0) return null

        var end = lines.size
        for (i in start + 1 until lines.size) {
            val level = Markdown.headingLevel(heads[i])
            val isRequirement = Markdown.REQUIREMENT.containsMatchIn(heads[i])
            // The next requirement, or any heading at or above the section
            // level, ends this one.
            if (isRequirement || (level in 1..2)) {
                end = i
                break
            }
        }
        return lines.subList(start, end).joinToString("\n").trimEnd()
    }
}

enum class DiffMark { COMMON, ORIGINAL_ONLY, PROPOSED_ONLY }

data class DiffLine(val mark: DiffMark, val text: String)

data class Comparison(
    val capability: String,
    val requirement: String,
    val original: String,
    val proposed: String,
) {
    val diff: List<DiffLine> by lazy { unified(original.lines(), proposed.lines()) }

    val isIdentical: Boolean get() = original.trim() == proposed.trim()

    private fun unified(from: List<String>, to: List<String>): List<DiffLine> {
        val patch = DiffUtils.diff(from, to)
        val out = mutableListOf<DiffLine>()
        var cursor = 0

        patch.deltas.sortedBy { it.source.position }.forEach { delta ->
            for (i in cursor until delta.source.position) {
                out.add(DiffLine(DiffMark.COMMON, from[i]))
            }
            when (delta.type) {
                DeltaType.INSERT -> delta.target.lines.forEach {
                    out.add(DiffLine(DiffMark.PROPOSED_ONLY, it))
                }

                DeltaType.DELETE -> delta.source.lines.forEach {
                    out.add(DiffLine(DiffMark.ORIGINAL_ONLY, it))
                }

                else -> {
                    delta.source.lines.forEach { out.add(DiffLine(DiffMark.ORIGINAL_ONLY, it)) }
                    delta.target.lines.forEach { out.add(DiffLine(DiffMark.PROPOSED_ONLY, it)) }
                }
            }
            cursor = delta.source.position + delta.source.lines.size
        }
        for (i in cursor until from.size) {
            out.add(DiffLine(DiffMark.COMMON, from[i]))
        }
        return out
    }
}
