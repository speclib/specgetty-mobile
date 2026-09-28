package io.github.mipmip.specgettyondroid.spec

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScenarioContentTest {

    private fun scenario(tree: SpecTree, title: String): SpecNode =
        tree.nodes.first { it.kind == NodeKind.SCENARIO && it.title == title }

    private fun joined(node: SpecNode) = node.parts.joinToString(" ") { it.text }

    @Test
    fun `bare uppercase clauses are kept`() {
        val node = scenario(Fixtures.parse("bare-uppercase-clauses"), "enable airplane mode")
        assertTrue(node.parts.isNotEmpty())
        val text = joined(node)
        listOf(
            "airplane mode is off",
            "clicks the airplane mode toggle",
            "rfkill block all",
        ).forEach { assertTrue("$it was dropped:\n$text", text.contains(it)) }
    }

    @Test
    fun `bare uppercase clauses keep their line boundaries`() {
        val node = scenario(Fixtures.parse("bare-uppercase-clauses"), "enable airplane mode")
        assertEquals("three lines, three parts", 3, node.parts.size)
        assertTrue(node.parts.all { it.kind == PartKind.PROSE })
    }

    @Test
    fun `bold title case clauses are kept as prose`() {
        val tree = Fixtures.parse("bold-title-clauses")
        tree.nodes.filter { it.kind == NodeKind.SCENARIO }.forEach {
            assertTrue("scenario '${it.title}' is empty", it.parts.isNotEmpty())
        }
        val node = scenario(tree, "Initialize Docusaurus Package")
        assertTrue(
            "**Given** is not laid out as a clause by design",
            node.parts.none { it.kind == PartKind.CLAUSE },
        )
        assertTrue(joined(node).contains("the monorepo root"))
    }

    @Test
    fun `a prose only scenario is one prose part`() {
        val node = scenario(Fixtures.parse("prose-scenario"), "Site settings (Future)")
        assertEquals(1, node.parts.size)
        assertEquals(PartKind.PROSE, node.parts.single().kind)
        assertTrue(node.parts.single().text.contains("deferred to a future change"))
    }

    @Test
    fun `clauses and prose keep their order`() {
        val node = scenario(Fixtures.parse("mixed-parts"), "Clauses around a paragraph")
        assertEquals(3, node.parts.size)
        assertEquals(PartKind.CLAUSE, node.parts[0].kind)
        assertEquals("WHEN", node.parts[0].keyword)
        assertEquals(PartKind.PROSE, node.parts[1].kind)
        assertTrue(node.parts[1].text.contains("Rationale"))
        assertEquals(PartKind.CLAUSE, node.parts[2].kind)
        assertEquals("THEN", node.parts[2].keyword)
    }

    @Test
    fun `a horizontal rule is not content and does not swallow what follows`() {
        val tree = Fixtures.parse("mixed-parts")
        tree.nodes.forEach { node ->
            node.parts.forEach {
                assertFalse("'${node.title}' kept a separator: ${it.text}", it.text.contains("---"))
            }
        }
        assertTrue(tree.indexOfPath("req/The one after the rule") >= 0)
    }

    @Test
    fun `a clause spanning several source lines becomes one clause`() {
        val node = scenario(Fixtures.parse("canonical"), "A clause over three lines")
        assertEquals(2, node.parts.size)
        assertEquals(
            "a clause whose text runs on across a second source line and a third",
            node.parts[0].text,
        )
        assertEquals("AND", node.parts[1].keyword)
    }

    @Test
    fun `every clause keyword is read`() {
        val src = """
            ## Purpose
            Long enough to be a purpose of a capability.

            ## Requirements

            ### Requirement: k
            The system SHALL do it.

            #### Scenario: all four
            - **GIVEN** a
            - **WHEN** b
            - **THEN** c
            - **AND** d
        """.trimIndent()
        val result = SpecParser.parse("k", src)
        assertTrue("${result.problems}", result.isSpec)
        val keywords = result.tree.nodes.flatMap { it.parts }.map { it.keyword }
        assertEquals(listOf("GIVEN", "WHEN", "THEN", "AND"), keywords)
    }

    @Test
    fun `the clause test stays narrow`() {
        data class Case(val line: String, val isClause: Boolean, val keyword: String)
        listOf(
            Case("- **WHEN** a thing", true, "WHEN"),
            Case("- WHEN a thing", true, "WHEN"),
            Case("- **GIVEN** a thing", true, "GIVEN"),
            Case("- **AND** a thing", true, "AND"),
            Case("- **THEN** a thing", true, "THEN"),
            Case("- **When** a thing", false, ""),
            Case("**WHEN** a thing", false, ""),
            Case("WHEN a thing", false, ""),
            Case("- something else", false, ""),
        ).forEach { case ->
            val (kw, _, ok) = clauseOf(case.line)
            assertEquals(case.line, case.isClause, ok)
            if (ok) assertEquals(case.line, case.keyword, kw)
        }
    }

    @Test
    fun `a keyword that is only the start of a word does not open a paragraph`() {
        assertFalse(opensWithClauseKeyword("ANDROID is not a keyword"))
        assertFalse(opensWithClauseKeyword("WHENEVER it happens"))
        assertTrue(opensWithClauseKeyword("WHEN it happens"))
        assertTrue(opensWithClauseKeyword("- WHEN it happens"))
        assertTrue(opensWithClauseKeyword("**THEN** it happens"))
        assertTrue(opensWithClauseKeyword("AND: a colon counts"))
    }

    @Test
    fun `a thematic break is recognised in every CommonMark spelling`() {
        listOf("---", "***", "___", "- - -", "  ---  ", "-----").forEach {
            assertTrue(it, Markdown.isThematicBreak(it))
        }
        listOf("--", "-*-", "a---", "- item", "").forEach {
            assertFalse(it, Markdown.isThematicBreak(it))
        }
    }

    @Test
    fun `a scenario name loses its prefix and its closing hashes`() {
        assertEquals("Rendering", Markdown.scenarioName("#### Scenario: Rendering"))
        assertEquals("Edge case", Markdown.scenarioName("#### Edge case ####"))
        assertEquals("Parsing", Markdown.scenarioName("#### Parsing"))
        assertEquals("x", Markdown.scenarioName("#### scenario: x"))
    }
}
