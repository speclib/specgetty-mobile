package io.github.mipmip.specgettyondroid.spec

/**
 * A spec is read by the rules OpenSpec's own parser uses. Reading by the same
 * rules keeps two questions apart that are easily answered as one: whether a
 * file is a spec, which OpenSpec decides, and how to draw it, which the screen
 * decides.
 */
object SpecParser {

    fun parse(name: String, content: String): ParsedSpec {
        val (lines, heads) = Markdown.maskedLines(content)
        val problems = mutableListOf<SpecProblem>()
        fun add(line: Int, text: String) = problems.add(SpecProblem(line, text))

        val purposeAt = heads.indexOfFirst { Markdown.PURPOSE.containsMatchIn(it) }
        val purpose = if (purposeAt >= 0) {
            Markdown.sectionBody(lines, heads, purposeAt, 2).joinToString("\n").trim()
        } else {
            ""
        }
        when {
            purposeAt < 0 -> add(
                0,
                "There is no `## Purpose` section. Every spec needs one: a sentence " +
                    "or two on what the capability is for.",
            )

            purpose.isEmpty() -> add(
                purposeAt + 1,
                "The `## Purpose` section is empty. OpenSpec reads an empty Purpose " +
                    "as no Purpose at all.",
            )
        }

        // A delta header is a fault in a main spec, and the reason the section
        // below it is never parsed.
        heads.forEachIndexed { i, l ->
            if (Markdown.DELTA.containsMatchIn(l)) {
                add(
                    i + 1,
                    "`${l.trim()}` is a delta header. It belongs in a change, under " +
                        "`openspec/changes/<name>/specs/`. A main spec keeps its " +
                        "requirements under `## Requirements`, and openspec parses only " +
                        "that section, so everything below this heading is invisible to " +
                        "validate, list and archive.",
                )
            }
        }

        val reqAt = heads.indexOfFirst { Markdown.REQUIREMENTS.containsMatchIn(it) }
        var reqEnd = lines.size
        if (reqAt >= 0) {
            for (i in reqAt + 1 until lines.size) {
                val l = Markdown.headingLevel(heads[i])
                if (l in 1..2) {
                    reqEnd = i
                    break
                }
            }
        } else {
            add(
                0,
                "There is no `## Requirements` section. A main spec keeps every " +
                    "requirement under that one heading.",
            )
        }

        data class Found(val line: Int, val title: String)

        val found = heads.mapIndexedNotNull { i, l ->
            Markdown.REQUIREMENT.find(l)?.let { Found(i, it.groupValues[1].trim()) }
        }
        if (found.isEmpty()) {
            add(
                0,
                "The file has no requirements. A requirement is a " +
                    "`### Requirement: <name>` heading.",
            )
        }

        fun inSection(line: Int) = reqAt >= 0 && line > reqAt && line < reqEnd

        val outside = found.filterNot { inSection(it.line) }.map { it.line + 1 }
        if (outside.isNotEmpty() && reqAt >= 0) {
            add(
                outside.first(),
                "${Markdown.plural(outside.size, "requirement sits", "requirements sit")} " +
                    "outside the `## Requirements` section, at ${Markdown.lineList(outside)}. " +
                    "Main specs parse requirements only inside that section, so these are " +
                    "invisible to validate, list and archive.",
            )
        }

        val nodes = mutableListOf<SpecNode>()
        if (purpose.isNotEmpty()) {
            nodes.add(SpecNode(NodeKind.PURPOSE, "Purpose", body = purpose, path = "purpose"))
        }

        val noScenario = mutableListOf<Int>()
        found.forEachIndexed { k, r ->
            if (!inSection(r.line)) return@forEachIndexed

            var stop = reqEnd
            if (k + 1 < found.size && found[k + 1].line < stop) stop = found[k + 1].line

            var bodyEnd = stop
            val scenLines = mutableListOf<Int>()
            for (i in r.line + 1 until stop) {
                if (Markdown.SCENARIO.containsMatchIn(heads[i])) {
                    if (scenLines.isEmpty()) bodyEnd = i
                    scenLines.add(i)
                }
            }

            nodes.add(
                SpecNode(
                    kind = NodeKind.REQUIREMENT,
                    title = r.title,
                    body = lines.subList(r.line + 1, bodyEnd).joinToString("\n").trim(),
                    path = "req/${r.title}",
                ),
            )

            var kept = 0
            scenLines.forEachIndexed { n, at ->
                val end = if (n + 1 < scenLines.size) scenLines[n + 1] else stop
                val body = lines.subList(at + 1, end)
                // OpenSpec does not count a scenario heading with nothing under
                // it, so neither does this.
                if (body.joinToString("\n").isBlank()) return@forEachIndexed
                kept++
                val title = Markdown.scenarioName(heads[at])
                nodes.add(
                    SpecNode(
                        kind = NodeKind.SCENARIO,
                        title = title,
                        parts = partsOf(body),
                        path = "req/${r.title}/scen/$title",
                    ),
                )
            }
            if (kept == 0) noScenario.add(r.line + 1)
        }
        if (noScenario.isNotEmpty()) {
            add(
                noScenario.first(),
                "${Markdown.plural(noScenario.size, "requirement has", "requirements have")} " +
                    "no scenario, at ${Markdown.lineList(noScenario)}. Every requirement " +
                    "needs at least one `#### ` heading with content under it.",
            )
        }

        if (problems.isNotEmpty()) return ParsedSpec(SpecTree(name), problems)
        return ParsedSpec(SpecTree(name, nodes), emptyList())
    }
}
