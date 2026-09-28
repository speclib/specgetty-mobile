package io.github.mipmip.specgettyondroid.spec

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The table the rest of the parser tests lean on. Each fixture has to land on
 * the same side of the grammar as it does in specgetty.
 */
class SpecParserGrammarTest {

    private data class Case(val name: String, val isSpec: Boolean, val because: String)

    private val table = listOf(
        Case("canonical", true, ""),
        Case("bare-uppercase-clauses", true, ""),
        Case("bold-title-clauses", true, ""),
        Case("prose-scenario", true, ""),
        Case("mixed-parts", true, ""),
        Case("heading-without-prefix", true, ""),
        Case("heading-in-fence", true, ""),
        Case("empty-scenario", true, ""),
        Case("delta-header", false, "delta header"),
        Case("no-purpose", false, "`## Purpose`"),
        Case("requirements-outside", false, "outside the `## Requirements` section"),
        Case("no-requirements", false, "no requirements"),
        Case("requirement-without-scenario", false, "no scenario"),
    )

    @Test
    fun `every fixture lands where it should`() {
        table.forEach { case ->
            val result = SpecParser.parse(case.name, Fixtures.read(case.name))
            if (case.isSpec) {
                assertTrue("${case.name}: ${result.problems}", result.isSpec)
                assertTrue(
                    "${case.name} parsed to no requirements",
                    result.tree.requirementCount > 0,
                )
                assertTrue("${case.name} parsed to no scenarios", result.tree.scenarioCount > 0)
            } else {
                assertTrue("${case.name} should not be a spec", result.problems.isNotEmpty())
                assertEquals(
                    "${case.name} yields no outline",
                    0,
                    result.tree.nodes.size,
                )
                assertTrue(
                    "${case.name}: no problem mentions ${case.because}: ${result.problems}",
                    result.problems.any { it.text.contains(case.because) },
                )
            }
        }
    }

    @Test
    fun `no scenario of any parseable fixture is empty`() {
        var checked = 0
        Fixtures.all.forEach { name ->
            val result = SpecParser.parse(name, Fixtures.read(name))
            if (!result.isSpec) return@forEach
            result.tree.nodes.filter { it.kind == NodeKind.SCENARIO }.forEach { node ->
                checked++
                assertTrue(
                    "$name: scenario '${node.title}' has no content",
                    node.parts.isNotEmpty(),
                )
            }
        }
        assertTrue("checked only $checked scenarios", checked >= 10)
    }

    @Test
    fun `a fence hides its headings but keeps its content`() {
        val tree = Fixtures.parse("heading-in-fence")
        assertTrue(tree.nodes.none { it.title == "not a requirement" })
        assertTrue(tree.nodes.none { it.title == "not a scenario" })
        assertEquals(1, tree.requirementCount)
        assertEquals(1, tree.scenarioCount)

        val joined = tree.nodes.filter { it.kind == NodeKind.SCENARIO }
            .flatMap { it.parts }
            .joinToString(" ") { it.text }
        assertTrue("the fenced content was dropped: $joined", joined.contains("Page Title"))
    }

    @Test
    fun `the headings are case insensitive`() {
        val src = Fixtures.read("canonical")
            .replace("## Purpose", "## purpose")
            .replace("## Requirements", "## REQUIREMENTS")
            .replace("### Requirement: The first thing", "### requirement: The first thing")
        val result = SpecParser.parse("x", src)
        assertTrue("${result.problems}", result.isSpec)
        assertTrue(result.tree.indexOfPath("req/The first thing") >= 0)
    }

    @Test
    fun `a requirements heading is not a requirement`() {
        val src = Fixtures.read("canonical")
            .replace("### Requirement: The second thing", "### Requirements of a kind")
        val result = SpecParser.parse("x", src)
        assertTrue("${result.problems}", result.isSpec)
        assertTrue(
            result.tree.nodes.none {
                it.kind == NodeKind.REQUIREMENT && it.title.startsWith("of a kind")
            },
        )
    }

    @Test
    fun `any level four heading is a scenario, named by its text`() {
        val tree = Fixtures.parse("heading-without-prefix")
        val got = tree.nodes.filter { it.kind == NodeKind.SCENARIO }.map { it.title }
        assertEquals(listOf("Parsing", "Rendering", "Edge case"), got)
    }

    @Test
    fun `a scenario heading with nothing under it is not a scenario`() {
        val tree = Fixtures.parse("empty-scenario")
        assertTrue(tree.nodes.none { it.title == "Never filled in" })
        assertEquals(1, tree.scenarioCount)
    }

    @Test
    fun `every delta header spelling is reported as one`() {
        listOf("ADDED", "MODIFIED", "REMOVED", "RENAMED").forEach { kind ->
            val src = Fixtures.read("delta-header")
                .replace("## ADDED Requirements", "## $kind Requirements")
            val result = SpecParser.parse("x", src)
            assertTrue(
                "$kind: ${result.problems}",
                result.problems.any { it.text.contains("delta header") && it.text.contains(kind) },
            )
        }
    }

    @Test
    fun `a problem about a particular line names it`() {
        val content = Fixtures.read("delta-header")
        val want = content.lines().indexOfFirst { it.startsWith("## ADDED") } + 1
        val result = SpecParser.parse("x", content)
        val delta = result.problems.first { it.text.contains("delta header") }
        assertEquals(want, delta.line)

        val outside = SpecParser.parse("x", Fixtures.read("requirements-outside"))
            .problems.first { it.text.contains("outside") }
        assertTrue("a problem about lines must name one", outside.line > 0)
        assertTrue(outside.text.contains("line "))
    }

    @Test
    fun `a problem says what to do about it`() {
        val result = SpecParser.parse("x", Fixtures.read("delta-header"))
        val text = result.problems.first { it.text.contains("delta header") }.text
        listOf("change", "## Requirements", "invisible").forEach {
            assertTrue("the reason should mention $it: $text", text.contains(it))
        }
    }

    @Test
    fun `a file with several faults reports all of them`() {
        val result = SpecParser.parse("x", "# Nothing\n\nNo structure at all.\n")
        assertTrue("${result.problems}", result.problems.size >= 3)
        assertTrue(result.problems.any { it.text.contains("`## Purpose`") })
        assertTrue(result.problems.any { it.text.contains("`## Requirements`") })
        assertTrue(result.problems.any { it.text.contains("no requirements") })
    }

    @Test
    fun `an empty purpose is reported`() {
        val src = """
            # x

            ## Purpose

            ## Requirements

            ### Requirement: a
            Prose.

            #### Scenario: s
            - **WHEN** a
            - **THEN** b
        """.trimIndent()
        val result = SpecParser.parse("x", src)
        assertTrue(
            "${result.problems}",
            result.problems.any { it.text.contains("`## Purpose` section is empty") },
        )
    }
}
