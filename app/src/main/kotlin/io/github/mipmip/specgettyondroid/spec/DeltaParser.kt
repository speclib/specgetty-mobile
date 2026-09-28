package io.github.mipmip.specgettyondroid.spec

/**
 * A spec file in a change is a delta, and almost every rule that makes a main
 * spec valid inverts in it: a delta header is the structure rather than a
 * fault, Purpose is written only for a capability the change introduces, and a
 * requirement being removed carries a reason where a scenario would be.
 */
object DeltaParser {

    /** A removal names a reason instead, and a rename names only the two names. */
    private fun needsScenarios(op: String): Boolean = op != Op.REMOVED && op != Op.RENAMED

    fun pathOf(capability: String, rest: String): String = "cap/$capability/$rest"

    fun parse(capability: String, content: String): ParsedSpec {
        val (lines, heads) = Markdown.maskedLines(content)
        val problems = mutableListOf<SpecProblem>()
        fun add(line: Int, text: String) = problems.add(SpecProblem(line, text))

        val nodes = mutableListOf<SpecNode>()

        // A `## Requirements` section is the main-spec form. OpenSpec reads
        // only delta headers in a change.
        heads.indexOfFirst { Markdown.REQUIREMENTS.containsMatchIn(it) }.let { at ->
            if (at >= 0) {
                add(
                    at + 1,
                    "`## Requirements` is the heading a main spec uses. In a change " +
                        "openspec reads only `## ADDED`, `## MODIFIED`, `## REMOVED` and " +
                        "`## RENAMED Requirements`, so nothing under this heading will be " +
                        "applied when the change is archived.",
                )
            }
        }

        // Purpose, which a delta carries only for a capability the change
        // introduces. Absent is the ordinary case and not a fault.
        heads.indexOfFirst { Markdown.PURPOSE.containsMatchIn(it) }.let { at ->
            if (at >= 0) {
                val body = Markdown.sectionBody(lines, heads, at, 2).joinToString("\n").trim()
                if (body.isEmpty()) {
                    add(
                        at + 1,
                        "The `## Purpose` section is empty. Either write one or remove " +
                            "the heading; archive copies it into the new spec.",
                    )
                } else {
                    nodes.add(
                        SpecNode(
                            kind = NodeKind.PURPOSE,
                            title = "Purpose",
                            body = body,
                            capability = capability,
                            path = pathOf(capability, "purpose"),
                        ),
                    )
                }
            }
        }

        data class OpAt(val line: Int, val op: String)

        val ops = heads.mapIndexedNotNull { i, l ->
            Markdown.OP_HEADER.find(l)?.let { OpAt(i, it.groupValues[1].uppercase()) }
        }
        fun opFor(line: Int): String = ops.lastOrNull { it.line < line }?.op ?: ""

        data class ReqAt(val line: Int, val title: String)

        val found = heads.mapIndexedNotNull { i, l ->
            Markdown.REQUIREMENT.find(l)?.let { ReqAt(i, it.groupValues[1].trim()) }
        }
        if (found.isEmpty()) {
            add(
                0,
                "The file has no requirements. A delta needs at least one " +
                    "`### Requirement: <name>` under a delta header.",
            )
        }

        val orphans = mutableListOf<Int>()
        val noScenario = mutableListOf<Int>()

        found.forEachIndexed { k, r ->
            val op = opFor(r.line)
            if (op.isEmpty()) {
                orphans.add(r.line + 1)
                return@forEachIndexed
            }

            var stop = if (k + 1 < found.size) found[k + 1].line else lines.size
            // An operation heading below this requirement ends it too, so the
            // last requirement of a section does not swallow the next header.
            ops.forEach { o -> if (o.line > r.line && o.line < stop) stop = o.line }

            var bodyEnd = stop
            val scenLines = mutableListOf<Int>()
            for (i in r.line + 1 until stop) {
                if (Markdown.SCENARIO.containsMatchIn(heads[i])) {
                    if (scenLines.isEmpty()) bodyEnd = i
                    scenLines.add(i)
                }
            }

            val reqPath = pathOf(capability, "req/${r.title}")
            nodes.add(
                SpecNode(
                    kind = NodeKind.REQUIREMENT,
                    title = r.title,
                    body = lines.subList(r.line + 1, bodyEnd).joinToString("\n").trim(),
                    op = op,
                    capability = capability,
                    path = reqPath,
                ),
            )

            var kept = 0
            scenLines.forEachIndexed { n, at ->
                val end = if (n + 1 < scenLines.size) scenLines[n + 1] else stop
                val body = lines.subList(at + 1, end)
                if (body.joinToString("\n").isBlank()) return@forEachIndexed
                kept++
                val title = Markdown.scenarioName(heads[at])
                nodes.add(
                    SpecNode(
                        kind = NodeKind.SCENARIO,
                        title = title,
                        parts = partsOf(body),
                        op = op,
                        capability = capability,
                        path = "$reqPath/scen/$title",
                    ),
                )
            }
            if (kept == 0 && needsScenarios(op)) noScenario.add(r.line + 1)
        }

        if (orphans.isNotEmpty()) {
            add(
                orphans.first(),
                "${Markdown.plural(orphans.size, "requirement sits", "requirements sit")} " +
                    "under no delta header, at ${Markdown.lineList(orphans)}. openspec " +
                    "applies a requirement only under `## ADDED`, `## MODIFIED`, " +
                    "`## REMOVED` or `## RENAMED Requirements`.",
            )
        }
        if (noScenario.isNotEmpty()) {
            add(
                noScenario.first(),
                "${Markdown.plural(noScenario.size, "requirement has", "requirements have")} " +
                    "no scenario, at ${Markdown.lineList(noScenario)}. An added or modified " +
                    "requirement needs at least one `#### ` heading with content under it; " +
                    "only a removal or a rename may go without.",
            )
        }

        if (problems.isNotEmpty()) return ParsedSpec(SpecTree(capability), problems)
        return ParsedSpec(SpecTree(capability, nodes), emptyList())
    }
}
